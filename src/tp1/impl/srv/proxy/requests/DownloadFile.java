package tp1.impl.srv.proxy.requests;

import tp1.impl.srv.proxy.SpreadsheetsProxyServer;
import tp1.impl.srv.proxy.arguments.DownloadFileArgs;

import java.io.IOException;
import java.util.logging.Logger;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import tp1.impl.srv.proxy.ProxyRequest;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.WebApplicationException;
import org.pac4j.scribe.builder.api.DropboxApi20;

public class DownloadFile {
	private static final String DOWNLOAD_FILE_URL = "https://content.dropboxapi.com/2/files/download";

	private static Logger Log = Logger.getLogger(SpreadsheetsProxyServer.class.getName());

	private static String execute(String filePath) throws JsonSyntaxException, IOException, ClassNotFoundException {
		OAuthRequest downloadFile = new OAuthRequest(Verb.POST, DOWNLOAD_FILE_URL);
		OAuth20Service service = new ServiceBuilder(ProxyRequest.apiKey).apiSecret(ProxyRequest.apiSecret)
				.build(DropboxApi20.INSTANCE);
		OAuth2AccessToken accessToken = new OAuth2AccessToken(ProxyRequest.accessTokenStr);
		Gson json = new Gson();

		downloadFile.addHeader("Content-Type", ProxyRequest.OCTET_CONTENT_TYPE);
		downloadFile.addHeader("Dropbox-API-Arg", json.toJson(new DownloadFileArgs(filePath)));

		service.signRequest(accessToken, downloadFile);

		Response r = null;

		try {
			Long curr = System.currentTimeMillis();
			r = service.execute(downloadFile);
			Log.info("Time Elapsed Download: " + (System.currentTimeMillis() - curr));
		} catch (Exception e) {
			Log.info("Damn dropbox!");
			return null;
		}

		if (r.getCode() == 200) {
			String jstring = new String(r.getBody().getBytes());
			return jstring;
		} else if (r.getCode() == 409) {
			Log.info("DownloadFile: File does not exist");
			throw new WebApplicationException(Status.CONFLICT);
		} else {
			System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
			try {
				System.err.println(r.getBody());
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
			return null;
		}
	}

	public static String run(String filePath) {
		Log.info("Downloading " + filePath);
		boolean success = false;
		String o = null;

		for (int i = 0; i < ProxyRequest.RETRIES; i++) {
			try {
				o = execute(filePath);
				if (o != null) {
					success = true;
					break;
				}
				Log.info("I SLEEP");
				Thread.sleep(1000);
			} catch (WebApplicationException e) {
				break;
			} catch (Exception e) {
				Log.info("SearchFile: What the frog");
			}

		}

		if (success) {
			Log.info("File: " + filePath + " was downloaded");
			return o;
		} else {
			Log.info("File: " + filePath + " was NOT found");
			return null;
		}
	}
}
