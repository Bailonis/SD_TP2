package tp1.impl.srv.rest;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.impl.discovery.Discovery;
import tp1.impl.srv.Domain;
import tp1.impl.utils.IP;

public abstract class AbstractRestServer {
	
	protected static String SERVER_BASE_URI = "https://%s:%s/rest";

	final int port;
	final String service;
	final private Logger Log;
	
	protected AbstractRestServer(Logger log, String service, int port) {
		this.service = service;
		this.port = port;
		this.Log = log;
	}


	protected void start() {
		String ip = IP.hostAddress();
		String serverURI = String.format(SERVER_BASE_URI, ip, port);
		String FullServiceName = String.format("%s:%s", Domain.get(), service);
		HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
		ResourceConfig config = new ResourceConfig();
		Discovery.getInstance().announce(FullServiceName, serverURI);
		registerResources( config );
		
		try {
			JdkHttpServerFactory.createHttpServer(URI.create(serverURI.replace(ip,"0.0.0.0")), config, SSLContext.getDefault());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		Log.info(String.format("%s Server ready @ %s\n",  service, serverURI));

	}
	
	abstract void registerResources( ResourceConfig config );
	
	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
}