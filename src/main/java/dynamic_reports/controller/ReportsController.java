package dynamic_reports.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dynamic_reports.dto.ReportConfig;
import dynamic_reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
public class ReportsController {

    private final ReportService reportService;

    @PostMapping("/report")
    public ResponseEntity<byte[]> generateReport(@RequestBody ReportConfig reqBody) {

        // Log the incoming request
        log.info("Received request to generate report with name: {}", reqBody.getName());

        byte[] excelFile = null;

        try {
            // Validate the request body (optional, could also be done in service layer)
            if (reqBody == null || reqBody.getDatasource() == null || reqBody.getMeta() == null) {
                log.error("Missing required fields in the report configuration.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Missing required fields in the report configuration.".getBytes(StandardCharsets.UTF_8));
            }

            // Generate the report by calling the service method
            excelFile = reportService.generateReport(reqBody);

        } catch (Exception e) {
            // Log the exception for debugging purposes
            log.error("An error occurred while generating the report: {}", e.getMessage(), e);

            // Return a structured error response (could be a JSON object with message and error code)
            String errorMessage = "An error occurred: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMessage.getBytes(StandardCharsets.UTF_8));
        }

        // Set headers for the response
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=report.xlsx");
        headers.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        // Return the response with Excel file and headers
        log.info("Successfully generated report: {}", reqBody.getName());
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelFile);
    }
}
