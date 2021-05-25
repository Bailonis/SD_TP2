package tp1.impl.clt.soap;

import java.net.URI;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import tp1.api.User;
import tp1.api.service.java.Result;
import tp1.api.service.java.Users;
import tp1.api.service.soap.SoapUsers;
import tp1.impl.utils.Url;

public class SoapUsersClient extends SoapClient implements Users {

	private SoapUsers impl;
	
	public SoapUsersClient( URI uri ) {
		super( uri );
	}
	
	synchronized private SoapUsers impl() {
		if (impl == null) {
			QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
			
			Service service = Service.create(Url.from(super.uri + USERS_WSDL), QNAME);

			QName port = new QName(SoapUsers.NAMESPACE, "SoapUsersWebServicePort");
			this.impl = service.getPort(port, tp1.api.service.soap.SoapUsers.class);
			super.setTimeouts( (BindingProvider)impl);
		}
		return impl;
	}

	@Override
	public Result<String> createUser(User user) {
		return super.tryCatchResult(() -> impl().createUser(user));
	}

	@Override
	public Result<User> getUser(String userId, String password) {
		return super.tryCatchResult(() -> impl().getUser(userId, password));
	}

	@Override
	public Result<User> updateUser(String userId, String password, User user) {
		return super.tryCatchResult(() -> impl().updateUser(userId, password, user));
	}

	@Override
	public Result<User> deleteUser(String userId, String password) {
		return super.tryCatchResult(() -> impl().deleteUser(userId, password));
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		return super.tryCatchResult(() -> impl().searchUsers(pattern));
	}

	@Override
	public Result<User> fetchUser(String userId) {
		return super.tryCatchResult(() -> impl().fetchUser(userId));
	}
}
