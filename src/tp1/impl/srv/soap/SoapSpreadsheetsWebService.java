package tp1.impl.srv.soap;

import java.util.logging.Logger;

import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.service.java.Result;
import tp1.api.service.java.Spreadsheets;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.impl.srv.common.JavaSpreadsheets;
import tp1.impl.utils.IP;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SoapSpreadsheetsWebService implements SoapSpreadsheets {

	private static Logger Log = Logger.getLogger(SoapSpreadsheetsWebService.class.getName());

	final Spreadsheets impl;

	public SoapSpreadsheetsWebService() {
		var uri = String.format("https://%s:%d/soap/%s", IP.hostAddress(), SpreadsheetsSoapServer.PORT, SoapSpreadsheets.NAME);
		impl = new JavaSpreadsheets(uri);
	}

	/*
	 * Given a Result<T> returns T value or throws a SheetsException with the corresponding error message
	 */
	private <T> T resultOrThrow(Result<T> result) throws SheetsException {
		if (result.isOK())
			return result.value();
		else
			throw new SheetsException(result.error().name());
	}

	public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
		Log.info(String.format("SOAP createSpreadsheet: sheet = %s\n", sheet));

		return resultOrThrow(impl.createSpreadsheet(sheet, password));
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
		Log.info(String.format("SOAP deleteSpreadsheet: sheetId = %s\n", sheetId));

		resultOrThrow(impl.deleteSpreadsheet(sheetId, password));
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info(String.format("SOAP getSpreadsheet: sheetId = %s, userId = %s\n", sheetId, userId));

		return resultOrThrow(impl.getSpreadsheet(sheetId, userId, password));
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info(String.format("SOAP shareSpreadsheet: sheetId = %s, userId = %s\n", sheetId, userId));

		resultOrThrow(impl.shareSpreadsheet(sheetId, userId, password));
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info(String.format("SOAP unshareSpreadsheet: sheetId = %s, userId = %s\n", sheetId, userId));

		resultOrThrow(impl.unshareSpreadsheet(sheetId, userId, password));
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
			throws SheetsException {
		Log.info(String.format("SOAP updateCell: sheetId = %s, cell= %s, rawValue = %s, userId = %s\n", sheetId, cell,
				rawValue, userId));

		resultOrThrow(impl.updateCell(sheetId, cell, rawValue, userId, password));
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
		Log.info(String.format("SOAP getSpreadsheetValues: sheetId = %s, userId = %s\n", sheetId, userId));
		return resultOrThrow(impl.getSpreadsheetValues(sheetId, userId, password));
	}

	@Override
	public void deleteSpreadsheets(String userId) throws SheetsException {
		Log.info(String.format("SOAP deleteSpreadsheets: userId = %s\n", userId));

		resultOrThrow(impl.deleteSpreadsheets(userId));
	}

	@Override
	public String[][] fetchSpreadsheetValues(String sheetId, String userId) throws SheetsException {
		Log.info(String.format("SOAP fetchSpreadsheetValues: sheetId = %s, userId = %s\n", sheetId, userId));
		return resultOrThrow( impl.fetchSpreadsheetValues(sheetId, userId));
	}

}
