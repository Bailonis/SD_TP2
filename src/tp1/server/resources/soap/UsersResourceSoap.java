package tp1.server.resources.soap;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.jws.WebService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.DomainInfo;
import tp1.api.User;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.server.resources.SpreadsheetResource;
import tp1.api.Discovery;

@WebService(serviceName = SoapUsers.NAME, targetNamespace = SoapUsers.NAMESPACE, endpointInterface = SoapUsers.INTERFACE)
public class UsersResourceSoap implements SoapUsers {
	private final Map<String, User> users;
	private String domain;
	private Discovery record;
	private ClientConfig config;

	private Client client;
	private static Logger Log = Logger.getLogger(UsersResourceSoap.class.getName());
	public static final String SHEETS_WSDL = String.format("/%s/?wsdl", SoapSpreadsheets.NAME);
	public static final QName SHEETS_QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
	public static final int TIMEOUT = 1000;
	public static final int MAX_RETRIES = 3;
	public static final int RETRY_PERIOD = 10000;

	public UsersResourceSoap(String domain, Discovery record) {
		this.users = new HashMap<String, User>();
		this.config = new ClientConfig();
		this.config.property(ClientProperties.CONNECT_TIMEOUT, TIMEOUT);
		this.config.property(ClientProperties.READ_TIMEOUT, TIMEOUT);
		this.client = ClientBuilder.newClient(config);
		this.domain = domain;
		this.record = record;
	}

	@Override
	public String createUser(User user) throws UsersException {
		if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
				|| user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new UsersException("Invalid user instance.");
		}

		synchronized (this.users) {
			// Check if userId does not exist exists, if not return HTTP CONFLICT (409)
			if (users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new UsersException("User already exists.");
			}

			// Add the user to the map of users
			users.put(user.getUserId(), user);
		}

		return user.getUserId();
	}

	@Override
	public User getUser(String userId, String password) throws UsersException {
		Log.info("getUser : user = " + userId + "; pwd = " + password);
		if (userId == null) {
			Log.info("UserId null.");
			throw new UsersException("UserId null.");
		}
		synchronized (this.users) {
			User user = users.get(userId);
			// Check if user exists
			if (user == null) {
				Log.info("User does not exist.");
				throw new UsersException("User does not exist.");
			}

			// Check if the password is correct
			if (!user.getPassword().equals(password) || password == null) {
				Log.info("Password is incorrect.");
				throw new UsersException("Password is incorrect.");
			}

			return user;
		}
	}

	@Override
	public User updateUser(String userId, String password, User user) throws UsersException {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
		User existingUser;

		// Check if user is valid, if not return HTTP BAD REQUEST (400)
		if (userId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId or passwrod null.");
		}
		synchronized (this.users) {
			existingUser = this.users.get(userId);

			// Check if user exists
			if (existingUser == null) {
				Log.info("User does not exist.");
				throw new UsersException("User does not exist.");
			}

			// Check if the password is correct
			if (!existingUser.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new UsersException("Password is incorrect.");
			}

			existingUser.setEmail(user.getEmail() == null ? existingUser.getEmail() : user.getEmail());

			existingUser.setFullName(user.getFullName() == null ? existingUser.getFullName() : user.getFullName());

			existingUser.setPassword(user.getPassword() == null ? existingUser.getPassword() : user.getPassword());
		}
		return existingUser;
	}

	@Override
	public User deleteUser(String userId, String password) throws UsersException {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);

		User user;

		// Check if user is valid, if not return HTTP BAD REQUEST (400)
		if (userId == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId null.");
		}
		synchronized (this.users) {
			user = this.users.get(userId);

			// Check if user exists
			if (user == null) {
				Log.info("User does not exist.");
				throw new UsersException("User does not exist.");
			}

			// Check if the password is correct
			if (!user.getPassword().equals(password) || password == null) {
				Log.info("Password is incorrect.");
				throw new UsersException("Password is incorrect.");
			}
			this.deleteSpreadsheetsSoap(userId);
			this.users.remove(userId);
		}
		return user;
	}

	@Override
	public List<User> searchUsers(String pattern) throws UsersException {
		if (pattern == null) {
			Log.info("Pattern null.");
			throw new UsersException("Pattern null.");
		}
		Log.info("searchUsers : pattern = " + pattern);
		String aux = pattern.toLowerCase();
		List<User> us = new LinkedList<User>();
		synchronized (this.users) {
			users.entrySet().forEach(entry -> {
				if (entry.getValue().getFullName().toLowerCase().contains(aux)) {
					us.add(entry.getValue());
				}
			});

			if (us.size() == 0)
				Log.info("User does not exist.");
		}
		return us;
	}

	@Override
	public boolean hasUsers(String pattern) {

		Log.info("hasUsers : pattern = " + pattern);
		String aux = pattern.toLowerCase();
		List<User> us = new LinkedList<User>();
		users.entrySet().forEach(entry -> {
			if (entry.getValue().getUserId().toLowerCase().contains(aux)) {
				us.add(entry.getValue());
			}
		});

		if (us.size() == 0) {
			Log.info("User does not exist.");
			return false;
		}

		return true;
	}

	protected void deleteSpreadsheetsSoap(String userId) throws UsersException {

		Response r = null;

		DomainInfo info = null;
		String serviceName = domain;
		try {
			info = record.knownUrisOf(serviceName);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		if (info.getUri().contains("rest")) {
			WebTarget target = client.target(info.getUri()).path(SpreadsheetResource.PATH);
			target = target.queryParam("userId", userId);
			Log.info("target: " + target);
			short retries = 0;
			boolean success = false;
			while (!success && retries < MAX_RETRIES) {
				try {
					r = target.path("/remove").request().delete();
					Log.info("r " + r);
					if (r.getStatus() == Status.FORBIDDEN.getStatusCode()) {
						Log.info("User either doesn't exist or the password is incorrect");
					}
					success = true;
				} catch (ProcessingException pe) {
					Log.info("Could not communicate with the SpreadsheetResource. What?");
					pe.printStackTrace();
					retries++;
					try {
						Thread.sleep(RETRY_PERIOD);
					} catch (InterruptedException e) {

					}
				}

			}

		} else {
			SoapSpreadsheets sheetsService = null;

			try {
				Service service = Service.create(new URL(info.getUri() + SHEETS_WSDL), SHEETS_QNAME);
				sheetsService = service.getPort(SoapSpreadsheets.class);
			} catch (MalformedURLException e) {
				Log.info("getUser: Bad Url");

			} catch (WebServiceException e) {
				Log.info("getUser: Failed to getUser to " + domain + ". Retrying...");

			}

			((BindingProvider) sheetsService).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT,
					TIMEOUT);
			((BindingProvider) sheetsService).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT,
					TIMEOUT);

			short retries = 0;
			boolean success = false;
			while (!success && retries < MAX_RETRIES) {

				try {
					sheetsService.deleteUserSpreadsheets(userId);
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

		}

	}
}
