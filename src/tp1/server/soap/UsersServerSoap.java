package tp1.server.soap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import jakarta.xml.ws.Endpoint;
import tp1.api.Discovery;
import tp1.server.InsecureHostnameVerifier;
import tp1.server.resources.soap.UsersResourceSoap;

public class UsersServerSoap {

	private static Logger Log = Logger.getLogger(UsersServerSoap.class.getName());

	public static Discovery record;

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "users";
	public static final String SOAP_USERS_PATH = "/soap/users";

	public static void main(String[] args) throws Exception {

		String machine = InetAddress.getLocalHost().getHostName();
		String serverURI = String.format("https://%s:%s/soap", machine, PORT);
		String domain = args[0];

		HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());// aqui
		HttpsConfigurator configurator = new HttpsConfigurator(SSLContext.getDefault());// aqui
		// Create an HTTP server, accepting requests at PORT (from all local interfaces)
		HttpsServer server = HttpsServer.create(new InetSocketAddress(machine, PORT), 0);// aqui

		server.setHttpsConfigurator(configurator);// aqui

		// Provide an executor to create threads as needed...
		server.setExecutor(Executors.newCachedThreadPool());

		System.out.println(String.format("\n%s Server ready @ %s\n", SERVICE, serverURI));

		record = new Discovery(SERVICE, serverURI, domain);

		record.start();
		Endpoint soapUsersEndpoint = Endpoint.create(new UsersResourceSoap(domain, record));

		soapUsersEndpoint.publish(server.createContext(SOAP_USERS_PATH));

		server.start();

		Log.info(String.format("\n%s Server ready @ %s\n", SERVICE, serverURI));

	}

}
