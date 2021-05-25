package tp1.impl.clt;

import java.net.URI;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import tp1.api.service.java.Spreadsheets;
import tp1.impl.clt.common.RetrySpreadsheetsClient;
import tp1.impl.clt.rest.RestSpreadsheetsClient;
import tp1.impl.clt.soap.SoapSpreadsheetsClient;
import tp1.impl.discovery.Discovery;
import tp1.impl.srv.Domain;

public class SpreadsheetsClientFactory {
	private static final String SERVICE = "sheets";

	private static final String REST = "/rest";
	private static final String SOAP = "/soap";

	private static final long CACHE_CAPACITY = 10;
	
	static LoadingCache<URI, Spreadsheets> sheets = CacheBuilder.newBuilder().maximumSize(CACHE_CAPACITY)
			.build(new CacheLoader<>() {
				@Override
				public Spreadsheets load(URI uri) throws Exception {
					Spreadsheets client;
					if (uri.toString().endsWith(REST))
						client = new RestSpreadsheetsClient(uri);
					else if (uri.toString().endsWith(SOAP))
						client = new SoapSpreadsheetsClient(uri);
					else
						throw new RuntimeException("Unknown service type..." + uri);
					
					return new RetrySpreadsheetsClient(client);
				}
			});
	
	public static Spreadsheets get() {
		return get(String.format("%s:%s", Domain.get(), SERVICE));
	}
	
	public static Spreadsheets get( String fullname ) {
		URI[] uris = Discovery.getInstance().findUrisOf(String.format("%s:%s", Domain.get(), SERVICE), 1);
		return with(uris[0].toString());
	}
	
	public static Spreadsheets with(String uriString) {
		return sheets.getUnchecked( URI.create(uriString));					
	}
}
