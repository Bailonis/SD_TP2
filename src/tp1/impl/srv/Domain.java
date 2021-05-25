package tp1.impl.srv;

/**
 * 
 * Stores the server's domain.
 * 
 * @author smduarte
 *
 */
public class Domain {

	private static String domain;
	
	public static String get() {
		return domain;
	}
	
	public static void set( String d ) {
		domain = d;
	}
}
