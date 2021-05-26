package tp1.impl.srv.proxy;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.impl.discovery.Discovery;
import tp1.impl.srv.Domain;
import tp1.impl.srv.proxy.requests.CreateDirectory;
import tp1.impl.srv.proxy.requests.Delete;
import tp1.impl.srv.rest.CustomLoggingFilter;
import tp1.impl.srv.rest.GenericExceptionMapper;
import tp1.impl.srv.rest.InsecureHostnameVerifier;

public class SpreadsheetsProxyServer {
    static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "sheets";
	public static String hostname;
	protected static String SERVER_BASE_URI = "https://%s:%s/rest";

	

	public static String secret;

	public static void main(String[] args) throws UnknownHostException {
		Domain.set(args.length > 0 ? args[0] : "?");
		String ip = InetAddress.getLocalHost().getHostAddress();
		String serverURI = String.format(SERVER_BASE_URI, ip, PORT);
		String FullServiceName = String.format("%s:%s", Domain.get(), SERVICE);
		
		secret = args[1];
		
		hostname = InetAddress.getLocalHost().getHostName();

		boolean freshStart = Boolean.parseBoolean(args[0]);
		String dirName = "/" + hostname;

		if(freshStart){		
			Delete.run(dirName);

			CreateDirectory.run(dirName);

			CreateDirectory.run(dirName + "/sheets");

		}

		HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
		Discovery.getInstance().announce(FullServiceName, serverURI);
		ResourceConfig config = new ResourceConfig();

		config.register(SpreadsheetsResourceProxy.class);
		config.register( GenericExceptionMapper.class );
		config.register( CustomLoggingFilter.class);
		
		
		try {
			JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
	}
}
