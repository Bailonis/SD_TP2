package tp1.api.service.java;

import tp1.api.Spreadsheet;


public interface Spreadsheets {

	Result<String> createSpreadsheet(Spreadsheet sheet, String password );

	Result<Void> deleteSpreadsheet(String sheetId, String password);

	Result<Spreadsheet> getSpreadsheet(String sheetId , String userId, String password);
			
	Result<Void> shareSpreadsheet( String sheetId, String userId, String password);
	
	Result<Void> unshareSpreadsheet( String sheetId, String userId, String password);
	
	Result<Void> updateCell( String sheetId, String cell, String rawValue, String userId, String password);

	Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password);
		
	Result<Void> deleteSpreadsheets( String userId );
	
	Result<String[][]> fetchSpreadsheetValues( String sheetId, String userId );
}
