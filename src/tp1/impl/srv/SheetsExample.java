package tp1.impl.srv;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import tp1.engine.CellRange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class SheetsExample {
	private String[][] mat;
	public SheetsExample(String sheetId,CellRange r, String range)
			throws IOException, GeneralSecurityException {
		mat  = new String[r.rows()][r.cols()];
		Sheets sheetsService = createSheetsService();
		Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values().get(sheetId, range)
				.setKey("AIzaSyDcJWrjg9Mhwo93FXts3Sk2mymNCRMo0HU");

		ValueRange response = request.execute();
		List<List<Object>> values = response.getValues();
		
		if (values == null || values.isEmpty()) {
			System.out.println("No data found.");
		} else {
			for( int i = 0; i < r.rows(); i++ ) {
				for( int j = 0; j < r.cols(); j++) {
			        mat[i][j] = (String) values.get(i).get(j);
			    }
			}
		}
	}

	public static Sheets createSheetsService() throws IOException, GeneralSecurityException{
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

		GoogleCredential credential = new GoogleCredential();

		return new Sheets.Builder(httpTransport, jsonFactory, credential).setApplicationName("SpreadsheetsGoogle")
				.build();
	}
	public String[][] getValue() {
		return mat;
	}

}
