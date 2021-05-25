package tp1.impl.clt.rest;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.java.Result;
import tp1.api.service.java.Users;
import tp1.api.service.rest.RestUsers;

public class RestUsersClient extends RestClient implements Users {
	private static final String EXT = "/x";
	private static final String PASSWORD = "password";
	private static final String QUERY = "query";

	public RestUsersClient(URI serverUri) {
		super(serverUri, RestUsers.PATH);
	}


	@Override
	public Result<String> createUser(User user) {
		Response r = target
				.request()
				.accept(  MediaType.APPLICATION_JSON)
				.post( Entity.entity(user, MediaType.APPLICATION_JSON));
		return super.responseContents(r, Status.OK, new GenericType<String>() {});
	}

	@Override
	public Result<User> updateUser(String userId, String password, User user) {
		Response r = target.path( userId)
				.queryParam(PASSWORD, password)
				.request()
				.accept(  MediaType.APPLICATION_JSON)
				.put(Entity.entity(user, MediaType.APPLICATION_JSON));
		
		return super.responseContents(r, Status.OK, new GenericType<User>() {});
	}

	@Override
	public Result<User> getUser(String userId, String password) {
		Response r = target.path(userId)
				.queryParam(PASSWORD, password)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get();
		return super.responseContents(r, Status.OK, new GenericType<User>() {});
	}

	@Override
	public Result<User> deleteUser(String userId, String password) {
		Response r = target.path(userId)
				.queryParam(PASSWORD, password)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.delete();
		
		return super.responseContents(r, Status.OK, new GenericType<User>() {});
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Response r = target.path("/")
				.queryParam(QUERY, pattern)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get();
		
		return super.responseContents(r, Status.OK, new GenericType<List<User>>() {});
	}

	@Override
	public Result<User> fetchUser(String userId) {
		Response r = target.path(userId).path(EXT)
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.get();
		return super.responseContents(r, Status.OK, new GenericType<User>() {});
	}
}
