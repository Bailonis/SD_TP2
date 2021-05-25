package tp1.impl.srv.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import tp1.impl.srv.Domain;

public class UsersRestServer extends AbstractRestServer {
	public static final int PORT = 3456;
	public static final String SERVICE_NAME = "users";
	
	private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());

	UsersRestServer( int port ) {
		super( Log, SERVICE_NAME, port);
	}
	
	
	@Override
	void registerResources(ResourceConfig config) {
		config.register( UsersResources.class ); 
		config.register( CustomLoggingFilter.class);
	}
	
	
	public static void main(String[] args) throws Exception {
		Domain.set(args.length > 0 ? args[0] : "?");	
		int port = args.length < 2 ? PORT : Integer.valueOf(args[1]);

		Log.setLevel( Level.ALL );
		
		new UsersRestServer(port).start();
	}	
}