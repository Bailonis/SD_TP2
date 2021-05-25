package tp1.impl.clt.soap;

import java.net.URI;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import tp1.api.Spreadsheet;
import tp1.api.service.java.Result;
import tp1.api.service.java.Spreadsheets;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.impl.utils.Url;

public class SoapSpreadsheetsClient extends SoapClient implements Spreadsheets {

	private SoapSpreadsheets impl;
	
	public SoapSpreadsheetsClient( URI uri ) {
		super( uri );
	}
	
	synchronized private SoapSpreadsheets impl() {
		if (impl == null) {
			var QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);

			var service = Service.create(Url.from(super.uri +SHEETS_WSDL), QNAME);

			this.impl = service.getPort(tp1.api.service.soap.SoapSpreadsheets.class);
			super.setTimeouts((BindingProvider)impl);
		}
		return impl;
	}

	@Override
	public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
		return tryCatchResult( () -> impl().createSpreadsheet(sheet, password));
	}

	@Override
	public Result<Void> deleteSpreadsheet(String sheetId, String password) {
		return tryCatchVoid( () -> impl().deleteSpreadsheet(sheetId, password));
	}

	@Override
	public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
		return tryCatchResult( () -> impl().getSpreadsheet(sheetId, userId, password));
	}

	@Override
	public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
		return tryCatchVoid( () -> impl().shareSpreadsheet(sheetId, userId, password));
	}

	@Override
	public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
		return tryCatchVoid( () -> impl().unshareSpreadsheet(sheetId, userId, password));
	}

	@Override
	public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		return tryCatchVoid( () -> impl().updateCell(sheetId, cell, rawValue, userId, password));
	}

	@Override
	public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
		return tryCatchResult( () -> impl().getSpreadsheetValues(sheetId, userId, password));
	}

	@Override
	public Result<Void> deleteSpreadsheets(String userId) {
		return tryCatchVoid( () -> impl().deleteSpreadsheets( userId ));
	}

	@Override
	public Result<String[][]> fetchSpreadsheetValues(String sheetId, String userId) {
		return tryCatchResult( () -> impl().fetchSpreadsheetValues(sheetId, userId));
	}
}
