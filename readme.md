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
POST /report
```
With a JSON payload specifying the required parameters:
```json
{
  "name": "outward",
  "datasource":{
    "spName":"sp_reports_SalesRegister",
    "spParams":["xyz", "01-01-2025", "20-01-2025", "","","",""]
  },
  "meta": {
    "fixed": {
      "column1": {
        "name": "Supplier GSTIN",
        "mappedIndex": 1,
        "format": "uppercase"
      },
      "column2": {
        "name": "Invoice Ref No.",
        "mappedIndex": 0,
        "format": "sentencecase"
      },
      "column3": {
        "name": "Document Type",
        "mappedIndex": 2,
        "format": "lowercase"
      },
      "column4": {
        "name": "Invoice Amount",
        "mappedIndex": 73,
        "format": null
      },
      "column5": {
        "name": "Invoice Date",
        "mappedIndex": 41,
        "format": "dd/MM/yyyy"
      }
    },
    "filters": [
      {
        "column": "Invoice Amount",
        "condition": ">",
        "value": 1000
      },
      {
        "column": "column5",
        "condition": "<",
        "value": "13/01/2025"
      }
    ]
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

