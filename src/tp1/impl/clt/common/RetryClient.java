package tp1.impl.clt.common;

import java.util.function.Supplier;
import java.util.logging.Logger;

import tp1.api.service.java.Result;
import tp1.api.service.java.Result.ErrorCode;
import tp1.impl.utils.Sleep;

/**
 * Shared client behavior.
 * 
 * Used to retry an operation in a loop.
 * 
 * @author smduarte
 *
 */
public abstract class RetryClient {
	private static Logger Log = Logger.getLogger(RetryClient.class.getName());

	protected static final int READ_TIMEOUT = 5000;
	protected static final int CONNECT_TIMEOUT = 5000;

	protected static final int RETRY_SLEEP = 1000;
	protected static final int MAX_RETRIES = 10;
	
	// higher order function to retry forever a call until it succeeds
	// and return an object of some type T to break the loop
	protected <T> Result<T> reTry(Supplier<Result<T>> func) {
		for (int i = 0; i < MAX_RETRIES; i++)
			try {
				return func.get();
			} catch (Exception x) {
				Log.fine("Exception: " + x.getMessage());
				x.printStackTrace();
				Sleep.ms(RETRY_SLEEP);
			}
		return Result.error( ErrorCode.TIMEOUT );
	}
}