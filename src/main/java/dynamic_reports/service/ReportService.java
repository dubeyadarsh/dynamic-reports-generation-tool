package dynamic_reports.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import dynamic_reports.dto.ReportConfig;
import dynamic_reports.dto.ReportConfig.ColumnConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReportService {

    @Value("${reports.header.outward}")
    private String outwardHeader;
    
    @Value("${reports.header.error}")
    private String errorHeader;
    
    private final JdbcTemplate jdbcTemplate;
    
    public byte[] generateReport(ReportConfig reportConfig) throws IOException {
        // Validate reportConfig to ensure required parameters are provided
        if (reportConfig == null || reportConfig.getDatasource() == null || reportConfig.getMeta() == null) {
            log.error("Invalid report configuration: missing required fields");
            return createErrorResponse("Invalid report configuration: missing required fields");
        }

        List<String> filterList = reportConfig.getDatasource().getSpParams();
        String spName = reportConfig.getDatasource().getSpName();

        if (filterList == null || filterList.isEmpty()) {
            log.error("No filters provided for the stored procedure");
            return createErrorResponse("No filters provided for the stored procedure");
        }

        // Fetch data from stored procedure
        List<Map<String, Object>> actualData = null;
        try {
            actualData = fetchDataFromStoredProcedure(filterList, reportConfig.getName(), spName);
        } catch (Exception e) {
            log.error("Error fetching data from stored procedure", e);
            return createErrorResponse("Error fetching data from stored procedure: " + e.getLocalizedMessage());
        }

        // If no data is returned, return an empty response
        if (actualData == null || actualData.isEmpty()) {
            log.warn("No data returned from the stored procedure");
            return createErrorResponse("No data returned from the stored procedure");
        }

        Map<String, ReportConfig.ColumnConfig> fixedData = Optional.ofNullable(reportConfig.getMeta().getFixed()).orElse(new HashMap<>());
        
        List<Map<String, Object>> requiredData = new ArrayList<>();
        
        try {
            processFixedData(actualData, fixedData, requiredData);
        } catch (Exception e) {
            log.error("Error processing fixed data", e);
            return createErrorResponse("Error processing fixed data: " + e.getLocalizedMessage());
        }

        List<ReportConfig.Filter> filters = Optional.ofNullable(reportConfig.getMeta().getFilters()).orElse(new ArrayList<>());
        try {
            applyFilters(requiredData, filters, fixedData);
        } catch (Exception e) {
            log.error("Error applying filters", e);
            return createErrorResponse("Error applying filters: " + e.getLocalizedMessage());
        }

        // Write data to Excel
        try {
            return writeDataToExcel(requiredData);
        } catch (IOException e) {
            log.error("Error writing data to Excel", e);
            return createErrorResponse("Error writing data to Excel: " + e.getLocalizedMessage());
        }
    }

    private void processFixedData(List<Map<String, Object>> actualData, Map<String, ReportConfig.ColumnConfig> fixedData, List<Map<String, Object>> requiredData) {
        actualData.forEach(dataItem -> {
            Map<String, Object> mapData = new LinkedHashMap<>();
            
            for (Entry<String, ColumnConfig> entry : fixedData.entrySet()) {
                Integer index = entry.getValue().getMappedIndex();
                if (index >= 0) {
                    Object value = getDataByIndex(dataItem, index);
                    String formattedData = formatData(value, entry.getValue().getFormat());

                    mapData.put(entry.getValue().getName(), formattedData);
                }
            }

            if (!mapData.isEmpty()) requiredData.add(mapData);
        });
    }

    private static Object getDataByIndex(Map<String, Object> dataItem, int index) {
        Iterator<Map.Entry<String, Object>> iterator = dataItem.entrySet().iterator();
        int currentIndex = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            if (currentIndex == index) {
                return entry.getValue(); // Return value at the given index
            }
            currentIndex++;
        }
        return null; // Return null if index is out of bounds
    }

    private void applyFilters(List<Map<String, Object>> data, List<ReportConfig.Filter> filters,Map<String, ReportConfig.ColumnConfig> fixedData) {
        if (filters != null) {
            for (ReportConfig.Filter filter : filters) {
                String column = filter.getColumn();
                String condition = filter.getCondition();
                String value = filter.getValue();
               
                for(String key : fixedData.keySet()) {
                	if(key.equals(column)) {  // if filter column name is equal to key of the meta then only below filter will be applied
                		String upColumn = fixedData.get(key).getName();
                		 if (condition.equals(">")) {
                             data.removeIf(row -> ((Comparable) row.get(upColumn)).compareTo(value) <= 0);
                         } else if (condition.equals("<")) {
                             data.removeIf(row -> ((Comparable) row.get(upColumn)).compareTo(value) >= 0);
                         } else if(condition.equals("contains")) {
                             data.removeIf(row -> ( value.contains((CharSequence) row.get(upColumn))) );
                         }
                	}
                }
              

                // Filter data based on conditions
               
            }
        }
    }

    private byte[] createErrorResponse(String errorMessage) {
        return ("Error: " + errorMessage).getBytes(StandardCharsets.UTF_8);
    }

    public List<Map<String, Object>> fetchDataFromStoredProcedure(List<String> filterList, String reportName, String spName) throws Exception {
        StringBuilder spParams = new StringBuilder();
        for (String s : filterList) {
            spParams.append("'").append(s).append("',");
        }
        String params = spParams.toString();
        params = params.substring(0, params.length() - 1);

        String storedProcCall = "CALL `" + spName + "`(" + params + ")";

        try {
            return jdbcTemplate.queryForList(storedProcCall);
        } catch (Exception e) {
            throw new Exception("Error executing stored procedure: " + e.getMessage());
        }
    }

    public String formatData(Object data, String format) {
        if (format == null) return String.valueOf(data);

        String formattedData = null;

        if (format.contains("dd") && format.contains("MM")) {
            // Case 1: If data is already a String, parse it into Date and reformat
            if (data instanceof String) {
                String dateString = (String) data;
                try {
                    SimpleDateFormat sdfInput = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
                    Date parsedDate = sdfInput.parse(dateString);  // Parse string to Date
                    SimpleDateFormat sdfOutput = new SimpleDateFormat(format, Locale.ROOT);
                    formattedData = sdfOutput.format(parsedDate);  // Format Date to the desired output
                } catch (ParseException e) {
                    formattedData = (String) data;  // Return original if parsing fails
                }
            } else if (data instanceof Date) {
                SimpleDateFormat sdfOutput = new SimpleDateFormat(format, Locale.ROOT);
                formattedData = sdfOutput.format((Date) data);
            }
            return formattedData;
        }

        switch (format.toLowerCase()) {
            case "uppercase":
                if (data instanceof String) {
                    formattedData = ((String) data).toUpperCase(Locale.ROOT);
                }
                break;
            case "lowercase":
                if (data instanceof String) {
                    formattedData = ((String) data).toLowerCase(Locale.ROOT);
                }
                break;
            case "sentencecase":
                if (data instanceof String) {
                    String str = (String) data;
                    formattedData = str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
                }
                break;
            default:
                formattedData = (String) data;
                break;
        }

        return formattedData;
    }

    public byte[] writeDataToExcel(List<Map<String, Object>> data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("report");

        if (data == null || data.isEmpty()) {
            workbook.close();
            return new byte[0];
        }

        // Create header row
        Row headerRow = sheet.createRow(0);
        Map<String, Object> headerMap = data.get(0);
        String[] headers = headerMap.keySet().toArray(new String[0]);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Create data rows
        int rowIndex = 1;
        for (Map<String, Object> rowData : data) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.createCell(i);
                Object value = rowData.get(headers[i]);
                if (value != null) {
                    cell.setCellValue(value.toString());
                }
            }
        }

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        workbook.write(byteOut);
        workbook.close();

        return byteOut.toByteArray();
    }
}
