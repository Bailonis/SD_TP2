package tp1.impl.clt;

import java.net.URI;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import tp1.api.service.java.Users;
import tp1.impl.clt.common.RetryUsersClient;
import tp1.impl.clt.rest.RestUsersClient;
import tp1.impl.clt.soap.SoapUsersClient;
import tp1.impl.discovery.Discovery;
import tp1.impl.srv.Domain;

public class UsersClientFactory {
	private static final String SERVICE = "users";
	private static final String REST = "/rest";
	private static final String SOAP = "/soap";

	private static final long CACHE_CAPACITY = 10;

	static LoadingCache<URI, Users> users = CacheBuilder.newBuilder().maximumSize(CACHE_CAPACITY)
			.build(new CacheLoader<>() {
				@Override
				public Users load(URI uri) throws Exception {
					Users client;
					if (uri.toString().endsWith(REST))
						client = new RestUsersClient(uri);
					else if (uri.toString().endsWith(SOAP))
						client = new SoapUsersClient(uri);
					else
						throw new RuntimeException("Unknown service type..." + uri);

					return new RetryUsersClient(client);
				}
			});

	public static Users get() {
		return get(String.format("%s:%s", Domain.get(), SERVICE));
	}

	public static Users get( String fullName)  {
		URI[] uris = Discovery.getInstance().findUrisOf(fullName, 1);
		return getByUri(uris[0].toString());
	}

	
	public static Users getByUri(String uriString) {
		try {
			return users.get(URI.create(uriString));
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}
}
