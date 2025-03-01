package dynamic_reports.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
public class ReportServiceBkp {

//	@Value("${reports.header.outward}")
//	private String outwardHeader;
//	
//	@Value("${reports.header.error}")
//	private String errorHeader;
	
	private final JdbcTemplate jdbcTemplate;
	
	public byte[] generateReport(ReportConfig reportConfig) throws IOException {
//		String[] headerArr = getHeaderByReportName(reportConfig.getName());
		List<String> filterList = reportConfig.getDatasource().getSpParams();
		String spName = reportConfig.getDatasource().getSpName();

		List<Map<String, Object>> actualData = null;
		try {
			actualData = fetchDataFromStoredProcedure(filterList,reportConfig.getName(),spName);
		} catch (Exception e) {
			String errorMessage = "Invalid Data Source !" + " " + e.getLocalizedMessage();
			return errorMessage.getBytes(StandardCharsets.UTF_8);
		}
		if(actualData == null) return new byte[0];
		Map<String, ReportConfig.ColumnConfig> fixedData = Optional.ofNullable(reportConfig.getMeta().getFixed()).orElse(new HashMap<>());
		
		List<Map<String, Object>> requiredData = new ArrayList<>();
		
		actualData.forEach( dataItem -> {
			
			Map<String,Object> mapData = new LinkedHashMap<>();
			
			for(Entry<String, ColumnConfig> entry:  fixedData.entrySet()) {
				Integer index = entry.getValue().getMappedIndex();
//				if(index<headerArr.length && index>=0) {
				if(index>=0) {
//					String headerName = headerArr[index].trim();
					Object value = getDataByIndex(dataItem,index);
//					Object value = dataItem.get(headerName);
					String formattedData = formatData(value,entry.getValue().getFormat());

					mapData.put(entry.getValue().getName(),formattedData);
					
					}
			}

			if(!mapData.isEmpty()) requiredData.add(mapData);

			
		});
    	List<ReportConfig.Filter> filters = Optional.ofNullable(reportConfig.getMeta().getFilters()).orElse(new ArrayList());
    	applyFilters(requiredData ,filters,fixedData);
    	return writeDataToExcel(requiredData);
		// 1) get all data
		// 2) use filter
		// 3) get required fixed data
		// 4) add custom column
	}
 
//    private String[] getHeaderByReportName(String reportName) {
//		switch(reportName.toLowerCase()) {
//		case "outward" : return outwardHeader.split(",");
//		case "error" : return errorHeader.split(",");
//		default :  return new String[0];
//		}
//    }
	public static Object getDataByIndex(Map<String, Object> dataItem, int index) {
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
	public List<Map<String, Object>> fetchDataFromStoredProcedure(List<String> filterList , String reportName,String spName) throws Exception {
		

		StringBuilder spParams = new StringBuilder();
		for(String s : filterList) {
			spParams.append("'").append(s).append("',");
		}
		String params = spParams.toString();
		params = params.substring(0,params.length()-1);
		
//		String storedProcCall = switch(reportName.toLowerCase()) {
//		case "outward" -> "CALL `sp_reports_SalesRegister`("+params+")";
//		case "error"  -> "CALL `sp_reports_ErrorRegister`("+params+")";
//		default -> null;
//		}  ;
		String storedProcCall = "CALL `" +spName+"`("+params+")";
		try {
			return jdbcTemplate.queryForList(storedProcCall);
		}catch(Exception e) {
			throw e;
		}
	   }
   
   
	    public String formatData(Object data, String format) {
	        if(format==null) return String.valueOf(data);
	        String formattedData = null;

	        if (format.contains("dd") && format.contains("MM")) {
	            // Case 1: If data is already a String, parse it into Date and reformat
	            if (data instanceof String) {
	                String dateString = (String) data;
	                try {
	                    // Try to parse the String into a Date object using the format
	                    SimpleDateFormat sdfInput = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
	                    Date parsedDate = sdfInput.parse(dateString);  // Parse string to Date
	                    SimpleDateFormat sdfOutput = new SimpleDateFormat(format, Locale.ROOT);
	                    formattedData = sdfOutput.format(parsedDate);  // Format Date to the desired output
	                } catch (Exception e) {
	                    // If parsing fails, return the original data
	                    formattedData = (String) data;
	                }
	            } 
	            // Case 2: If data is already a Date, just format it
	            else if (data instanceof Date) {
	                try {
	                    SimpleDateFormat sdfOutput = new SimpleDateFormat(format, Locale.ROOT);
	                    formattedData = sdfOutput.format((Date) data);
	                } catch (Exception e) {
	                    // Handle any formatting errors if necessary
	                    formattedData = (String) data;
	                }
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
	            	formattedData= (String) data;
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
