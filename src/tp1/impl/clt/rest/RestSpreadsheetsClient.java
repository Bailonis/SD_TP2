package tp1.impl.clt.rest;

import java.net.URI;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.service.java.Result;
import tp1.api.service.java.Spreadsheets;
import tp1.api.service.rest.RestSpreadsheets;

public class RestSpreadsheetsClient extends RestClient implements Spreadsheets {
	private static final String PASSWORD = "password";
	private static final String USERID = "userId";
	private static final String VALUES = "/values";
	private static final String SHEETS = "/sheets";
	private static final String FETCH = "/fetch";

	public RestSpreadsheetsClient(URI serverUri) {
		super(serverUri, RestSpreadsheets.PATH);
	}

	@Override
	public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
		Response r = target
				.queryParam(PASSWORD, password)
				.request()
				.accept(  MediaType.APPLICATION_JSON)
				.post( Entity.entity(sheet, MediaType.APPLICATION_JSON));
		return super.responseContents(r, Status.OK, new GenericType<String>() {});
	}

	@Override
	public Result<Void> deleteSpreadsheet(String sheetId, String password) {
		Response r = target
				.path("/" + sheetId)
				.queryParam(PASSWORD, password)
				.request()
				.accept(  MediaType.APPLICATION_JSON)
				.delete();
		return super.responseContents(r, Status.NO_CONTENT, null);
	}

	@Override
	public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
		Response r = target.path("/")
				.path(sheetId)
				.queryParam(PASSWORD, password)
				.queryParam(USERID, userId)
				.request()
				.accept(  MediaType.APPLICATION_JSON)
				.get();
		return super.responseContents(r, Status.OK, new GenericType<Spreadsheet>() {});
	}

	@Override
	public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
		Response r = target.path(String.format("/%s/share/%s", sheetId, userId))
				.queryParam(PASSWORD, password)
				.request()
				.post(Entity.json(""));
		return verifyResponse(r, Status.NO_CONTENT);
	}

	@Override
	public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
		Response r = target.path(String.format("/%s/share/%s", sheetId, userId))
				.queryParam(PASSWORD, password)
				.request()
				.delete();
		return verifyResponse(r, Status.NO_CONTENT);
	}

	@Override
	public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		Response r = target.path(String.format("/%s/%s", sheetId, cell))
				.queryParam(PASSWORD, password)
				.queryParam(USERID, userId)
				.request()
				.put(Entity.entity(MediaType.APPLICATION_JSON, rawValue));
		
		return verifyResponse(r, Status.NO_CONTENT);
	}

	@Override
	public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
		Response r = target
				.path(sheetId).path(VALUES)
				.queryParam(PASSWORD, password)
				.queryParam(USERID, userId)
				.request()
				.accept(  MediaType.APPLICATION_JSON)
				.get();
		
		return super.responseContents(r, Status.OK, new GenericType<String[][]>() {});
	}

	@Override
	public Result<Void> deleteSpreadsheets( String userId) {
		Response r = target
				.path(userId).path(SHEETS)
				.request()
				.accept(  MediaType.APPLICATION_JSON)
				.delete();
		
		return super.verifyResponse(r, Status.NO_CONTENT);
	}

	@Override
	public Result<String[][]> fetchSpreadsheetValues(String sheetId, String userId) {
		Response r = target.path( sheetId).path(FETCH)
				.queryParam(USERID, userId)
				.request()
				.accept(  MediaType.APPLICATION_JSON)
				.get();
		
		return super.responseContents(r, Status.OK, new GenericType<String[][]>() {});
	}


}
