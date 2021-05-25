package tp1.impl.srv.soap;

import java.util.List;
import java.util.logging.Logger;

import jakarta.jws.WebService;
import tp1.api.User;
import tp1.api.service.java.Result;
import tp1.api.service.java.Users;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.impl.srv.common.JavaUsers;

@WebService(serviceName=SoapUsers.NAME, targetNamespace=SoapUsers.NAMESPACE, endpointInterface=SoapUsers.INTERFACE)
public class SoapUsersWebService implements SoapUsers {

	private static Logger Log = Logger.getLogger(SoapUsersWebService.class.getName());

	final Users impl ;
	
	public SoapUsersWebService() {
		impl = new JavaUsers();
	}

	private <T> T resultOrThrow(Result<T> result) throws UsersException {
		if (result.isOK())
			return result.value();
		else
			throw new UsersException(result.error().name());
	}

	@Override
	public String createUser(User user) throws UsersException {
	
		Log.info(String.format("SOAP createUser: user = %s\n", user));

		return resultOrThrow( impl.createUser( user ));
	}

	@Override
	public User getUser(String userId, String password) throws UsersException  {
		Log.info(String.format("SOAP getUser: userId = %s password=%s\n", userId, password));

		return resultOrThrow( impl.getUser(userId, password));
	}


	@Override
	public User updateUser(String userId, String password, User user) throws UsersException  {
		Log.info(String.format("SOAP updateUser: userId = %s, user = %s\n", userId, user));

		return resultOrThrow( impl.updateUser(userId, password, user));
	}


	@Override
	public User deleteUser(String userId, String password) throws UsersException  {
		Log.info(String.format("SOAP deleteUser: userId = %s\n", userId));
		
		return resultOrThrow( impl.deleteUser(userId, password));
	}

	
	@Override
	public List<User> searchUsers(String pattern) throws UsersException  {
		Log.info(String.format("SOAP searchUsers: pattern = %s", pattern));
		
		return resultOrThrow( impl.searchUsers(pattern));
	}

	
	@Override
	public User fetchUser(String userId) throws UsersException {
		Log.info(String.format("SOAP fetchUser: pattern = %s", userId));
		return resultOrThrow( impl.fetchUser(userId));
	}
}
