package tp1.impl.clt.soap;

import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;

import java.net.URI;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import tp1.api.service.java.Result;
import tp1.api.service.java.Result.ErrorCode;
import tp1.impl.clt.common.RetryClient;

/**
* 
* Shared behavior among SOAP clients.
* 
* Holds endpoint information.
* 
* Translates soap responses/exceptions to Result<T> for interoperability.
*  
* @author smduarte
*
*/
abstract class SoapClient extends RetryClient {
	
	protected static final String SHEETS_WSDL = "/spreadsheets/?wsdl";
	protected static final String USERS_WSDL = "/users/?wsdl";
	protected final URI uri;

	public SoapClient(URI uri) {
		this.uri = uri;
	}

	static interface ResultSupplier<T> {
		T get() throws Exception;
	}

	static interface VoidSupplier {
		void run() throws Exception;
	}

	protected <T> Result<T> tryCatchResult(ResultSupplier<T> sup) {
		try {
			T result = sup.get();
			return ok(result);	
		} 
		catch (Exception e) {			
			if( e instanceof WebServiceException )
				throw new RuntimeException( e.getMessage() );
			
			e.printStackTrace();
			return error(getErrorCodeFrom(e));
		}
	}

	protected <T> Result<T> tryCatchVoid(VoidSupplier r) {
		try {
			r.run();
			return ok();
		}
		catch (Exception e) {
			if( e instanceof WebServiceException )
				throw new RuntimeException( e.getMessage() );
			
			e.printStackTrace();
			return error(getErrorCodeFrom(e));
		}
	}

	@Override
	public String toString() {
		return uri.toString();
	}

	static private ErrorCode getErrorCodeFrom(Exception e) {
		return switch (e.getMessage()) {
			case "OK" -> ErrorCode.OK;
			case "CONFLICT" -> ErrorCode.CONFLICT;
			case "NOT_FOUND" -> ErrorCode.NOT_FOUND;
			case "FORBIDDEN" -> ErrorCode.FORBIDDEN;
			case "INTERNAL_ERROR" -> ErrorCode.INTERNAL_ERROR;
			case "NOT_IMPLEMENTED" -> ErrorCode.NOT_IMPLEMENTED;
			default -> ErrorCode.INTERNAL_ERROR;
		};
	}

	void setTimeouts(BindingProvider port ) {
		port.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
		port.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, READ_TIMEOUT);		
	}

}
