package tp1.server;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.server.resources.UsersResource;
import tp1.api.Discovery;

public class UsersServer {

	private static Logger Log = Logger.getLogger(UsersServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "users";

	public static Discovery record;

	public static void main(String[] args) {
		try {
			String machine = InetAddress.getLocalHost().getHostName();
			HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());//aqui
			
			String domain = args[0];
			ResourceConfig config = new ResourceConfig();

			String serverURI = String.format("https://%s:%s/rest", machine, PORT);//aqui
			record = new Discovery(SERVICE, serverURI, domain);

			record.start();
			config.register(new UsersResource(domain, record));
			JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());//aqui

			Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

			// More code can be executed here...

		} catch (Exception e) {
			Log.severe(e.getMessage());
		}
	}

}
