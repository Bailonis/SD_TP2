package tp1.server.resources;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.Discovery;
import tp1.api.DomainInfo;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.server.SpreadsheetServer;

@Singleton
public class SpreadsheetResource implements RestSpreadsheets {

	public static final int TIMEOUT = 10000;
	public static final int MAX_RETRIES = 3;
	public static final int RETRY_PERIOD = 10000;
	public static final String DOMAIN_FORMAT_REST = "https://%s:%d/rest";
	protected Client client;
	protected ClientConfig config;
	protected String domain;
	protected String serverUri;
	protected Discovery record;
	protected int counter;

	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	private final Map<String, List<String>> usersId = new HashMap<String, List<String>>();
	private static Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

	public SpreadsheetResource(String domain, Discovery record) throws UnknownHostException {
		this.config = new ClientConfig();
		this.config.property(ClientProperties.CONNECT_TIMEOUT, TIMEOUT);
		this.config.property(ClientProperties.READ_TIMEOUT, TIMEOUT);
		this.domain = domain;
		this.record = record;
		this.serverUri = String.format(DOMAIN_FORMAT_REST, InetAddress.getLocalHost().getHostAddress(),
				SpreadsheetServer.PORT);

		this.client = ClientBuilder.newClient(config);
		this.counter = 0;

	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		Log.info("createSpreadsheet : " + sheet);

		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0 || sheet.getSharedWith() == null
				|| sheet.getRawValues() == null) {
			Log.info("Speadshhet object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		Log.info("Checking userId");
		User user = this.getUserRest(sheet.getOwner(), password);
		if (user == null) {
			Log.info("Password does not match.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		synchronized (this) {
			String newId = domain + "_" + counter;
			String newURL = serverUri + "/spreadsheets/" + newId;
			sheet.setSheetURL(newURL);
			sheet.setSheetId(newId);
			counter++;

			// Add the sheet to the map of sheets
			List<String> list = usersId.get(sheet.getOwner());
			if (list == null)
				list = new LinkedList<String>();
			list.add(sheet.getSheetId());
			usersId.put(sheet.getOwner(), list);
			sheets.put(sheet.getSheetId(), sheet);
		}

		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
		if (sheetId == null) {
			Log.info("Wrong parameters.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			Spreadsheet s = this.sheets.get(sheetId);
			User user = this.getUserRest(s.getOwner(), password);
			if (user == null || !user.getPassword().equals(password)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
			Log.info("Spread shit " + sheetId + " deleted with success.");
			List<String> list = this.usersId.get(s.getOwner());
			list.remove(sheetId);
			sheets.remove(sheetId);
		}
		throw new WebApplicationException(Status.NO_CONTENT);

	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

		if (sheetId == null || userId == null) {
			Log.info("Wrong parameters.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this.sheets) {

			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			Spreadsheet s = this.sheets.get(sheetId);
			if (!this.usersId.containsKey(s.getOwner())) {
				Log.info("Owner does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			String aux = userId + "@" + domain;
			Log.info("Checking userId");
			User user = this.getUserRest(userId, password);
			if (user == null) {
				Log.info("Password does not match.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}

			if (!s.getOwner().equals(userId) && !s.getSharedWith().contains(aux)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		}
		return this.sheets.get(sheetId);
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
		if (sheetId == null || userId == null) {
			Log.info("Wrong parameters.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		Log.info("Checking userId");
		User user = this.getUserRest(userId, password);
		if (user == null) {
			Log.info("Password does not match.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			Spreadsheet s = this.sheets.get(sheetId);
			String aux = userId + "@" + domain;

			if (!s.getOwner().equals(userId) && !s.getSharedWith().contains(aux)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		}
		SpreadsheetEngineImpl aux = (SpreadsheetEngineImpl) SpreadsheetEngineImpl.getInstance();
		String[][] result = aux.computeSpreadsheetValues(this.sheets.get(sheetId));
		return result;
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		if (sheetId == null || userId == null || cell == null || rawValue == null) {
			Log.info("Wrong parameters.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		Log.info("Checking userId");
		User user = this.getUserRest(userId, password);
		if (user == null) {
			Log.info("Password does not match.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			Spreadsheet s = this.sheets.get(sheetId);
			s.setCellRawValue(cell, rawValue);

		}

	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {

		Log.info("shareSpreadsheet : " + sheetId);

		if (sheetId == null || userId == null) {
			Log.info("Wrong parameters.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		String id = userId.substring(0, userId.indexOf('@'));
		Log.info("new id: " + id + ".");
		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			boolean u = this.getUserRest(id);
			if (!u) {
				Log.info("User does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			Spreadsheet s = this.sheets.get(sheetId);
			User owner = this.getUserRest(s.getOwner(), password);
			if (owner == null || !owner.getPassword().equals(password)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
			if (s.isSharedWith(userId)) {
				Log.info("Spreadsheet alredy shared with " + userId + ".");
				throw new WebApplicationException(Status.CONFLICT);
			}
			Log.info("Spread shit " + sheetId + " shared with success.");
			s.getSharedWith().add(userId);
		}

	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {

		Log.info("shareSpreadsheet : " + sheetId);

		if (sheetId == null || userId == null) {
			Log.info("Wrong parameters.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		String id = userId.substring(0, userId.indexOf('@'));
		Log.info("new id: " + id + ".");
		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			boolean u = this.getUserRest(id);
			if (!u) {
				Log.info("User does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			Spreadsheet s = this.sheets.get(sheetId);
			User owner = this.getUserRest(s.getOwner(), password);
			if (owner == null || !owner.getPassword().equals(password)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}

			Log.info("Spread shit " + sheetId + " shared with success.");
			s.getSharedWith().remove(userId);
		}

	}

	@Override
	public void deleteUserSpreadsheets(String userId) {

		synchronized (this) {
			List<String> list = usersId.get(userId);
			for (String temp : list) {
				sheets.remove(temp);
				Log.info("spreadsheet " + temp + " deleted also the owner " + userId);
			}
		}
	}

	protected User getUserRest(String userId, String password) {
		Response r = null;
		DomainInfo info = null;
		String serviceName = domain;
		try {
			info = record.knownUrisOf(serviceName);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		WebTarget target = client.target(info.getUri()).path(UsersResource.PATH);
		target = target.queryParam("password", password).queryParam("userId", userId);
		Log.info("target: " + target);
		short retries = 0;
		boolean success = false;
		while (!success && retries < MAX_RETRIES) {
			try {
				r = target.path(userId).request().accept(MediaType.APPLICATION_JSON).get();
				Log.info("r " + r);
				if (r.getStatus() == Status.FORBIDDEN.getStatusCode()) {
					Log.info("User either doesn't exist or the password is incorrect");
					return null;
				}
				success = true;
			} catch (ProcessingException pe) {
				Log.info("Could not communicate with the UserResource. What?");
				pe.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {

				}
			}

		}
		return r.readEntity(User.class);
	}

	protected boolean getUserRest(String userId) {
		Response r = null;
		DomainInfo info = null;
		String serviceName = domain;
		try {
			info = record.knownUrisOf(serviceName);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		WebTarget target = client.target(info.getUri()).path(UsersResource.PATH);
		target = target.queryParam("procura", userId);
		Log.info("target: " + target);
		short retries = 0;
		boolean success = false;
		while (!success && retries < MAX_RETRIES) {
			try {
				r = target.request().accept(MediaType.APPLICATION_JSON).get();
				Log.info("r " + r);

				if (r.getStatus() == Status.FORBIDDEN.getStatusCode()) {
					Log.info("User either doesn't exist or the password is incorrect");
					return false;
				}
				success = true;
			} catch (ProcessingException pe) {
				Log.info("Could not communicate with the UserResource. What?");
				pe.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {

				}
			}

		}
		return true;
	}

	@Override
	public String[][] getSpreadsheet(String sheet, String user) {
		Spreadsheet s = sheets.get(sheet);
		SpreadsheetEngineImpl aux = (SpreadsheetEngineImpl) SpreadsheetEngineImpl.getInstance();
		String[][] result = aux.computeSpreadsheetValues(s);
		return result;
	}

}
