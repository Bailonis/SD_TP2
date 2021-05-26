package tp1.impl.srv.proxy;

import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import tp1.api.Spreadsheet;
import tp1.api.service.java.Spreadsheets;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.srv.common.ProxySpreadsheets;
import tp1.impl.srv.rest.RestResource;
import tp1.impl.srv.rest.SpreadsheetsResources;
import tp1.impl.srv.rest.SpreadsheetsRestServer;
import tp1.impl.utils.IP;

@Singleton
@Path(RestSpreadsheets.PATH)
public class SpreadsheetsResourceProxy extends RestResource implements RestSpreadsheets {
	private static Logger Log = Logger.getLogger(SpreadsheetsResources.class.getName());

	final Spreadsheets impl;

	public SpreadsheetsResourceProxy() {
		var uri = String.format("https://%s:%d/rest%s", IP.hostAddress(), SpreadsheetsRestServer.PORT, PATH);
		impl = new ProxySpreadsheets(uri);
	}

	public String createSpreadsheet(Spreadsheet sheet, String password) {
		Log.info(String.format("REST createSpreadsheet: sheet = %s\n", sheet));

		return super.resultOrThrow(impl.createSpreadsheet(sheet, password));
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
		Log.info(String.format("REST getSpreadsheet: sheetId = %s, userId = %s\n", sheetId, userId));

		return super.resultOrThrow(impl.getSpreadsheet(sheetId, userId, password));
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteSpreadsheets(String userId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String[][] fetchSpreadsheetValues(String sheetId, String userId) {
		// TODO Auto-generated method stub
		return null;
	}
}
