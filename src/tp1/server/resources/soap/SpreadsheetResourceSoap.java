package tp1.server.resources.soap;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;


import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.jws.WebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Discovery;
import tp1.api.DomainInfo;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.impl.engine.SpreadsheetEngineImpl;

import tp1.server.resources.SpreadsheetResource;
import tp1.server.soap.SpreadsheetServerSoap;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetResourceSoap implements SoapSpreadsheets {
	private final Map<String, Spreadsheet> sheets;
	private final Map<String, List<String>> usersId;
	private static Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());
	public static final String USERS_WSDL = String.format("/%s/?wsdl", SoapUsers.NAME);
	public static final QName USER_QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
	public static final int TIMEOUT = 1000;
	public static final String DOMAIN_FORMAT_SOAP = "https://%s:%d/soap";
	public static final int MAX_RETRIES = 3;
	public static final int RETRY_PERIOD = 10000;

	private String domain;
	private Discovery record;
	private String serverUri;
	private int counter;

	public SpreadsheetResourceSoap(String domain, Discovery record) throws UnknownHostException {
		this.sheets = new HashMap<String, Spreadsheet>();
		this.usersId = new HashMap<String, List<String>>();
		this.domain = domain;
		this.record = record;
		this.serverUri = String.format(DOMAIN_FORMAT_SOAP, InetAddress.getLocalHost().getHostAddress(),
				SpreadsheetServerSoap.PORT);
		this.counter = 0;
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
		Log.info("createSpreadsheet : " + sheet);

		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0
				|| sheet.getRawValues() == null) {
			Log.info("Speadsheet object invalid.");
			throw new SheetsException("Speadsheet object invalid.");
		}
		if (sheet.getSharedWith() == null) {
			Set<String> empty = new HashSet<String>();
			sheet.setSharedWith(empty);
		}

		Log.info("Checking userId");
		User user = null;
		try {
			user = this.getUserSoap(sheet.getOwner(), password);
		} catch (UsersException e) {

			e.printStackTrace();
		}
		if (user == null) {
			Log.info("Password does not match.");
			throw new SheetsException("Password does not match.");
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
	public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
		if (sheetId == null) {
			Log.info("Wrong parameters.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new SheetsException("Spreadsheet does not exist.");
			}

			Spreadsheet s = this.sheets.get(sheetId);
			User user = null;
			try {
				user = this.getUserSoap(s.getOwner(), password);
			} catch (UsersException e) {
				e.printStackTrace();
			}
			if (user == null || !user.getPassword().equals(password)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new SheetsException("You are not the owner of this spreadsheet.");
			}
			Log.info("Spread shit " + sheetId + " deleted with success.");
			List<String> list = this.usersId.get(s.getOwner());
			list.remove(sheetId);
			sheets.remove(sheetId);
		}

	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		if (sheetId == null || userId == null) {
			Log.info("Wrong parameters.");
			throw new SheetsException("Wrong parameters.");
		}

		synchronized (this.sheets) {

			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new SheetsException("Spreadsheet does not exist.");
			}
			Spreadsheet s = this.sheets.get(sheetId);
			if (!this.usersId.containsKey(s.getOwner())) {
				Log.info("Owner does not exist.");
				throw new SheetsException("Owner does not exist.");
			}
			String aux = userId + "@" + domain;
			Log.info("Checking userId");
			User user = null;
			try {
				user = this.getUserSoap(userId, password);
			} catch (UsersException e) {
				e.printStackTrace();
			}
			if (user == null) {
				Log.info("Password does not match.");
				throw new SheetsException("Password does not match.");
			}

			if (!s.getOwner().equals(userId) && (s.getSharedWith() == null || !s.getSharedWith().contains(aux))) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new SheetsException("You are not the owner of this spreadsheet.");
			}
		}
		return this.sheets.get(sheetId);
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info("shareSpreadsheet : " + sheetId);

		if (sheetId == null || userId == null) {
			Log.info("Wrong parameters.");
			throw new SheetsException("Wrong parameters.");
		}
		String id = userId.substring(0, userId.indexOf('@'));
		Log.info("new id: " + id + ".");
		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new SheetsException("Spreadsheet does not exist.");
			}
			boolean u = false;
			String d = userId.split("@")[1];
			try {
				u = this.hasUserSoap(id, d);
			} catch (UsersException e) {

				e.printStackTrace();
			}
			if (!u) {
				Log.info("User does not exist.");
				throw new SheetsException("User does not exist.");
			}

			Spreadsheet s = this.sheets.get(sheetId);
			User owner = null;
			try {
				owner = this.getUserSoap(s.getOwner(), password);
			} catch (UsersException e) {

				e.printStackTrace();
			}
			if (owner == null || !owner.getPassword().equals(password)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new SheetsException("You are not the owner of this spreadsheet.");
			}

			if (s.isSharedWith(userId)) {
				Log.info("Spreadsheet alredy shared with " + userId + ".");
				throw new SheetsException("Spreadsheet alredy shared with " + userId + ".");
			}

			Log.info("Spread shit " + sheetId + " shared with success.");
			s.getSharedWith().add(userId);
		}

	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info("shareSpreadsheet : " + sheetId);

		if (sheetId == null || userId == null) {
			Log.info("Wrong parameters.");
			throw new SheetsException("Wrong parameters.");
		}
		String id = userId.substring(0, userId.indexOf('@'));
		Log.info("new id: " + id + ".");
		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new SheetsException("Spreadsheet does not exist.");
			}
			boolean u = false;
			String d = userId.split("@")[1];
			try {

				u = this.hasUserSoap(id, d);
			} catch (UsersException e) {

				e.printStackTrace();
			}
			if (!u) {
				Log.info("User does not exist.");
				throw new SheetsException("User does not exist.");
			}

			Spreadsheet s = this.sheets.get(sheetId);
			User owner = null;
			try {
				owner = this.getUserSoap(s.getOwner(), password);
			} catch (UsersException e) {

				e.printStackTrace();
			}
			if (owner == null || !owner.getPassword().equals(password)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new SheetsException("You are not the owner of this spreadsheet.");
			}

			Log.info("Spread shit " + sheetId + " shared with success.");
			s.getSharedWith().remove(userId);
		}

	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
			throws SheetsException {
		if (sheetId == null || userId == null || cell == null || rawValue == null) {
			Log.info("Wrong parameters.");
			throw new SheetsException("Wrong parameters.");
		}

		Log.info("Checking userId");
		User user = null;
		try {
			user = this.getUserSoap(userId, password);
		} catch (UsersException e) {
			e.printStackTrace();
		}
		if (user == null) {
			Log.info("Password does not match.");
			throw new SheetsException("Password does not match.");
		}
		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new SheetsException("Spreadsheet does not exist.");
			}
			Spreadsheet s = this.sheets.get(sheetId);
			s.setCellRawValue(cell, rawValue);

		}

	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
		if (sheetId == null || userId == null) {
			Log.info("Wrong parameters.");
			throw new SheetsException("Wrong parameters.");
		}
		Log.info("Checking userId");
		User user = null;
		try {
			user = this.getUserSoap(userId, password);
		} catch (UsersException e) {
			e.printStackTrace();
		}
		if (user == null) {
			Log.info("Password does not match.");
			throw new SheetsException("Password does not match.");
		}
		synchronized (this.sheets) {
			if (!this.sheets.containsKey(sheetId)) {
				Log.info("Spreadsheet does not exist.");
				throw new SheetsException("Spreadsheet does not exist.");
			}
			Spreadsheet s = this.sheets.get(sheetId);
			String aux = userId + "@" + domain;

			if (!s.getOwner().equals(userId) && !s.getSharedWith().contains(aux)) {
				Log.info("You are not the owner of this spreadsheet.");
				throw new SheetsException("You are not the owner of this spreadsheet.");
			}
		}
		SpreadsheetEngineImpl aux = (SpreadsheetEngineImpl) SpreadsheetEngineImpl.getInstance();
		String[][] result = aux.computeSpreadsheetValues(this.sheets.get(sheetId));
		return result;
	}

	public Spreadsheet getSpreadsheetComputed(String sheet) {
		Spreadsheet s = sheets.get(sheet);
		// SpreadsheetEngineImpl aux = (SpreadsheetEngineImpl)
		// SpreadsheetEngineImpl.getInstance();
		// String[][] result = aux.computeSpreadsheetValues(s);
		return s;
	}

	public void deleteUserSpreadsheets(String userId) {

		synchronized (this) {
			List<String> list = usersId.get(userId);
			for (String temp : list) {
				sheets.remove(temp);
				Log.info("spreadsheet " + temp + " deleted also the owner " + userId);
			}
		}
	}

	protected User getUserSoap(String userId, String password) throws UsersException {
		User user = null;

		SoapUsers userService = null;
		DomainInfo info = null;
		try {
			info = record.knownUrisOf(domain);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}

		try {
			Service service = Service.create(new URL(info.getUri() + USERS_WSDL), USER_QNAME);
			userService = service.getPort(SoapUsers.class);
		} catch (MalformedURLException e) {
			Log.info("getUser: Bad Url");
			return null;
		} catch (WebServiceException e) {
			Log.info("getUser: Failed to getUser to " + domain + ". Retrying...");
			return null;
		}

		((BindingProvider) userService).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, TIMEOUT);
		((BindingProvider) userService).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, TIMEOUT);

		short retries = 0;
		boolean success = false;
		while (!success && retries < MAX_RETRIES) {

			
			try {
				user = userService.getUser(userId, password);
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
	
				}
				System.out.println("Retrying to execute request.");

			}
		}
		return user;

	}

	protected boolean hasUserSoap(String userId, String d) throws UsersException {
		boolean result = false;

		SoapUsers userService = null;
		DomainInfo info = null;
		String serviceName = d;
		try {
			info = record.knownUrisOf(serviceName);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}

		try {
			Service service = Service.create(new URL(info.getUri() + USERS_WSDL), USER_QNAME);
			userService = service.getPort(SoapUsers.class);
		} catch (MalformedURLException e) {
			Log.info("getUser: Bad Url");
			return false;
		} catch (WebServiceException e) {
			Log.info("getUser: Failed to getUser to " + domain + ". Retrying...");
			return false;
		}

		((BindingProvider) userService).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, TIMEOUT);
		((BindingProvider) userService).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, TIMEOUT);
		
		short retries = 0;
		boolean success = false;
		while (!success && retries < MAX_RETRIES) {

			
			try {
				result = userService.hasUsers(userId);
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
	
				}
				System.out.println("Retrying to execute request.");

			}
		}

		return result;
	}

}
