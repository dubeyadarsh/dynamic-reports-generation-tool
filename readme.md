# Dynamic Report Generation System

## Overview
The **Dynamic Report Generation System** is a robust, metadata-driven framework built with **Spring Boot**, **MySQL**, and **Apache POI**. It dynamically maps, formats, and filters large datasets, generating **customized Excel/CSV reports** with seamless **database integration via stored procedures**.

## Features
- **Metadata-Driven Reporting:** Dynamically generates reports based on metadata configurations.
- **Customizable Output Formats:** Supports **Excel (XLSX)** and **CSV** formats.
- **Stored Procedure Integration:** Fetches and processes data efficiently via **MySQL stored procedures**.
- **Adaptive Data Processing Pipeline:** Implements configurable transformations, validations, and conditional filters.
- **High-Performance Processing:** Optimized for large datasets with structured error handling.

## Tech Stack
- **Backend:** Spring Boot (Java)
- **Database:** MySQL (Stored Procedures)
- **Data Processing:** Apache POI (Excel), CSV Handling
- **Architecture:** Microservices compatible

## Installation
1. Clone the repository:
   ```sh
   git clone https://github.com/your-repo/dynamic-report-generator.git
   cd dynamic-report-generator
   ```
2. Configure database connection in `application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/your_db
       username: your_username
       password: your_password
   ```
3. Build and run the project:
   ```sh
   mvn clean install
   mvn spring-boot:run
   ```

## Usage
### Generating Reports
Send a **POST** request to:
```http
POST /api/reports/generate
```
With a JSON payload specifying the required parameters:
```json
{
  "reportType": "EXCEL",
  "filters": {
    "dateRange": "2024-01-01 to 2024-12-31",
    "status": "ACTIVE"
  }
}
```


## Contributing
1. Fork the repository.
2. Create a new branch: `feature/your-feature`.
3. Commit changes and push to your branch.
4. Open a Pull Request.

## License
This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

