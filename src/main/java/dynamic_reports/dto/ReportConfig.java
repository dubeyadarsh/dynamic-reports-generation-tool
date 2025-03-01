package dynamic_reports.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ReportConfig {

	private String name;
	private Meta meta;
	private Datasource datasource;
	
	@Data
	public static class Meta {
	    private Map<String, ColumnConfig> fixed;
	    private Map<String, CustomColumn> custom;
	    private List<Filter> filters;
//	    private List<Sorting> sorting;
	
	}
	  
	@Data
    public static class ColumnConfig {
        private String name;
        private Integer mappedIndex;
        private String format;

    }

	@Data
    public static class CustomColumn {
        private String mappedTo;
        private String name;
        private String action;

    }

	@Data
    public static class Filter {
        private String column;
        private String condition;
        private String value;

    }
	@Data
    public static class Datasource {
        private String spName;
    	private List<String> spParams;

    }
	
//	@Data
//    public static class Sorting {
//        private String column;
//        private String order;
//
//    }
	
	
}
