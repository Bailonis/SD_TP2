package tp1.impl.srv.soap;


import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import jakarta.xml.ws.Endpoint;
import tp1.impl.discovery.Discovery;
import tp1.impl.srv.Domain;
import tp1.impl.srv.rest.InsecureHostnameVerifier;
import tp1.impl.utils.IP;


public class UsersSoapServer {
	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}
	public static final int PORT = 13456;
	public static final String SERVICE_NAME = "users";
	public static String SERVER_BASE_URI = "https://%s:%s/soap";
	public static final String SOAP_USERS_PATH = "/soap/users";

	private static Logger Log = Logger.getLogger(UsersSoapServer.class.getName());

	public static void main(String[] args) throws Exception {
		Domain.set(args.length > 0 ? args[0] : "?");

//		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

		Log.setLevel(Level.FINER);

		String ip = IP.hostAddress();
		String serverURI = String.format(SERVER_BASE_URI, ip, PORT);

		HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());// aqui
		
		HttpsConfigurator configurator = new HttpsConfigurator(SSLContext.getDefault());// aqui
		// Create an HTTP server, accepting requests at PORT (from all local interfaces)
		HttpsServer server = HttpsServer.create(new InetSocketAddress(ip, PORT), 0);// aqui

		server.setHttpsConfigurator(configurator);// aqui
		
		server.setExecutor(Executors.newCachedThreadPool());
		
		Discovery.getInstance().announce(String.format("%s:%s",Domain.get(), SERVICE_NAME), serverURI);

		Endpoint soapUsersEndpoint = Endpoint.create(new SoapUsersWebService());
		soapUsersEndpoint.publish(server.createContext(SOAP_USERS_PATH));
		//Endpoint.publish(serverURI, new SoapUsersWebService());

		Log.info(String.format("%s Soap Server ready @ %s\n", SERVICE_NAME, serverURI));
		
		server.start();
	}
}
