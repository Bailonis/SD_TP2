package tp1.impl.srv.common;

import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;
import static tp1.api.service.java.Result.ErrorCode.BAD_REQUEST;
import static tp1.api.service.java.Result.ErrorCode.CONFLICT;
import static tp1.api.service.java.Result.ErrorCode.FORBIDDEN;
import static tp1.api.service.java.Result.ErrorCode.NOT_FOUND;

import java.util.HashSet;
import java.util.LinkedList;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.java.Result;
import tp1.api.service.java.Spreadsheets;
import tp1.engine.AbstractSpreadsheet;
import tp1.engine.CellRange;
import tp1.engine.SpreadsheetEngine;
import tp1.impl.clt.SpreadsheetsClientFactory;
import tp1.impl.clt.UsersClientFactory;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.impl.srv.Domain;
import tp1.impl.srv.proxy.SpreadsheetsProxyServer;
import tp1.impl.srv.proxy.requests.Create;
import tp1.impl.srv.proxy.requests.Delete;
import tp1.impl.srv.proxy.requests.DownloadFile;

public class ProxySpreadsheets implements Spreadsheets {
	private static Logger Log = Logger.getLogger(JavaSpreadsheets.class.getName());

	private static final long USER_CACHE_CAPACITY = 100;
	private static final long USER_CACHE_EXPIRATION = 120;
	private static final long VALUES_CACHE_CAPACITY = 100;
	private static final long VALUES_CACHE_EXPIRATION = 120;
	private static final Pattern SPREADSHEETS_URI_PATTERN = Pattern.compile("(.+)/spreadsheets/(.+)");

	private static final Set<String> DUMMY_SET = new HashSet<>();

	final String baseUri;
	final SpreadsheetEngine engine;
	final AtomicInteger counter = new AtomicInteger();

	final Map<String, Spreadsheet> sheets = new ConcurrentHashMap<>();
	final Map<String, Set<String>> userSheets = new ConcurrentHashMap<>();

	private static Gson json = new Gson();

	final String DOMAIN = '@' + Domain.get();

	LoadingCache<String, User> users = CacheBuilder.newBuilder().maximumSize(USER_CACHE_CAPACITY)
			.expireAfterWrite(USER_CACHE_EXPIRATION, TimeUnit.SECONDS).build(new CacheLoader<>() {
				@Override
				public User load(String userId) throws Exception {
					return UsersClientFactory.get().fetchUser(userId).value();
				}
			});

	/*
	 * This cache stores spreadsheet values. For local domain spreadsheets, the key
	 * is the sheetId, for remote domain spreadsheets, the key is the sheetUrl.
	 */
	Cache<String, String[][]> sheetValuesCache = CacheBuilder.newBuilder().maximumSize(VALUES_CACHE_CAPACITY)
			.expireAfterWrite(VALUES_CACHE_EXPIRATION, TimeUnit.SECONDS).build();

	public ProxySpreadsheets(String baseUri) {
		engine = SpreadsheetEngineImpl.getInstance();
		this.baseUri = baseUri;
	}

	@Override
	public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
		if (badSheet(sheet) || password == null || wrongPassword(sheet.getOwner(), password))
			return error(BAD_REQUEST);

		synchronized (sheets) {
			var sheetId = sheet.getOwner() + "-" + counter.getAndIncrement() + DOMAIN;
			sheet.setSheetId(sheetId);
			sheet.setSheetURL(String.format("%s/%s", baseUri, sheetId));
			sheet.setSharedWith(ConcurrentHashMap.newKeySet());
			// sheets.put(sheetId, sheet);//acho que se pode tirar
			userSheets.computeIfAbsent(sheet.getOwner(), (k) -> ConcurrentHashMap.newKeySet()).add(sheetId);
			String owner = id2owner(sheetId);
			String path = String.format("/%s/sheets/%s/%s", SpreadsheetsProxyServer.hostname, owner, sheetId);

			Create.run(path, sheet);
			return ok(sheetId);
		}
	}

	@Override
	public Result<Void> deleteSpreadsheet(String sheetId, String password) {
		if (badParam(sheetId))
			return error(BAD_REQUEST);
		String owner = id2owner(sheetId);
		String path = String.format("/%s/sheets/%s/%s", SpreadsheetsProxyServer.hostname, owner, sheetId);

		String sheetString = DownloadFile.run(path);
		var sheet = json.fromJson(sheetString, Spreadsheet.class);

		if (sheet == null)
			return error(NOT_FOUND);

		if (badParam(password) || wrongPassword(sheet.getOwner(), password))
			return error(FORBIDDEN);

		else {
			List<String> deletePaths = new LinkedList<>();
			deletePaths.add(path);
			Delete.run(deletePaths);
			// sheets.remove(sheetId);
			userSheets.computeIfAbsent(sheet.getOwner(), (k) -> ConcurrentHashMap.newKeySet()).remove(sheetId);
			return ok();
		}
	}

	@Override
	public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
		if (badParam(sheetId) || badParam(userId))
			return error(BAD_REQUEST);
		String folder = "sheets." + id2domain(sheetId);
		String owner = id2owner(sheetId);
		String path = String.format("/%s/sheets/%s/%s", folder, owner, sheetId);
		String sheetString = DownloadFile.run(path);
		var sheet = json.fromJson(sheetString, Spreadsheet.class);

		if (sheet == null || userId == null || getUser(userId) == null)
			return error(NOT_FOUND);

		if (badParam(password) || wrongPassword(userId, password) || !sheet.hasAccess(userId, DOMAIN))
			return error(FORBIDDEN);
		else {

			return ok(sheet);
		}
	}

	@Override
	public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
		if (badParam(sheetId) || badParam(userId))
			return error(BAD_REQUEST);

		String folder = "sheets." + id2domain(sheetId);
		String owner = id2owner(sheetId);
		String path = String.format("/%s/sheets/%s/%s", folder, owner, sheetId);
		String sheetString = DownloadFile.run(path);
		var sheet = json.fromJson(sheetString, Spreadsheet.class);
		if (sheet == null)
			return error(NOT_FOUND);

		if (badParam(password) || wrongPassword(sheet.getOwner(), password))
			return error(FORBIDDEN);

		if (sheet.getSharedWith().add(userId)) {
			Create.run(path, sheet);
			return ok();
		} else
			return error(CONFLICT);
	}

	@Override
	public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
		if (badParam(sheetId) || badParam(userId))
			return error(BAD_REQUEST);

		String folder = "sheets." + id2domain(sheetId);
		String owner = id2owner(sheetId);
		String path = String.format("/%s/sheets/%s/%s", folder, owner, sheetId);
		String sheetString = DownloadFile.run(path);
		var sheet = json.fromJson(sheetString, Spreadsheet.class);
		if (sheet == null)
			return error(NOT_FOUND);

		if (badParam(password) || wrongPassword(sheet.getOwner(), password))
			return error(FORBIDDEN);

		if (sheet.getSharedWith().remove(userId)) {
			sheetValuesCache.invalidate(sheetId);
			Create.run(path, sheet);
			return ok();
		} else
			return error(NOT_FOUND);
	}

	@Override
	public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		if (badParam(sheetId) || badParam(userId) || badParam(cell) || badParam(rawValue))
			return error(BAD_REQUEST);

		String folder = "sheets." + id2domain(sheetId);
		String owner = id2owner(sheetId);
		String path = String.format("/%s/sheets/%s/%s", folder, owner, sheetId);
		String sheetString = DownloadFile.run(path);
		var sheet = json.fromJson(sheetString, Spreadsheet.class);
		if (sheet == null)
			return error(NOT_FOUND);

		if (badParam(password) || wrongPassword(userId, password))
			return error(FORBIDDEN);

		sheet.setCellRawValue(cell, rawValue);
		sheetValuesCache.invalidate(sheetId);
		Create.run(path, sheet);
		return ok();
	}

	@Override
	public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
		if (badParam(sheetId) || badParam(userId))
			return error(BAD_REQUEST);

		String folder = "sheets." + id2domain(sheetId);
		String owner = id2owner(sheetId);
		String path = String.format("/%s/sheets/%s/%s", folder, owner, sheetId);
		String sheetString = DownloadFile.run(path);
		var sheet = json.fromJson(sheetString, Spreadsheet.class);
		if (sheet == null)
			return error(NOT_FOUND);

		if (badParam(password) || wrongPassword(userId, password) || !sheet.hasAccess(userId, DOMAIN))
			return error(FORBIDDEN);

		var values = getComputedValues(sheetId);
		if (values != null)
			return ok(values);
		else
			return error(BAD_REQUEST);
	}

	@Override
	public Result<String[][]> fetchSpreadsheetValues(String sheetId, String userId) {
		if (badParam(sheetId) || badParam(userId))
			return error(BAD_REQUEST);

		String folder = "sheets." + id2domain(sheetId);
		String owner = id2owner(sheetId);
		String path = String.format("/%s/sheets/%s/%s", folder, owner, sheetId);
		String sheetString = DownloadFile.run(path);
		var sheet = json.fromJson(sheetString, Spreadsheet.class);
		if (sheet == null)
			return error(NOT_FOUND);

		if (!sheet.hasAccess(userId, DOMAIN))
			return error(FORBIDDEN);

		var values = getComputedValues(sheetId);
		if (values != null)
			return ok(values);
		else
			return error(BAD_REQUEST);
	}

	@Override
	public Result<Void> deleteSpreadsheets(String userId) {

		List<String> deletePaths = new LinkedList<>();

		String path = String.format("/%s/sheets/%s", SpreadsheetsProxyServer.hostname, userId);

		deletePaths.add(path);

		Delete.run(deletePaths);

		return ok();
	}

	class SpreadsheetAdaptor implements AbstractSpreadsheet {

		final Spreadsheet sheet;

		SpreadsheetAdaptor(Spreadsheet sheet) {
			this.sheet = sheet;
		}

		@Override
		public int rows() {
			return sheet.getRows();
		}

		@Override
		public int columns() {
			return sheet.getColumns();
		}

		@Override
		public String sheetId() {
			return sheet.getSheetId();
		}

		@Override
		public String cellRawValue(int row, int col) {
			return sheet.getCellRawValue(row, col);
		}

		@Override
		public String[][] getRangeValues(String sheetURL, String range) {
			var x = resolveRangeValues(sheetURL, range, sheet.getOwner() + DOMAIN);
			Log.info("getRangeValues:" + sheetURL + " for::: " + range + "--->" + x);
			return x;
		}
	}

	private User getUser(String userId) {
		try {
			return users.get(userId);
		} catch (Exception x) {
			x.printStackTrace();
			return null;
		}
	}

	private String[][] getComputedValues(String sheetId) {
		try {
			var values = sheetValuesCache.getIfPresent(sheetId);
			if (values == null) {

				String folder = "sheets." + id2domain(sheetId);
				String owner = id2owner(sheetId);
				String path = String.format("/%s/sheets/%s/%s", folder, owner, sheetId);
				String sheetString = DownloadFile.run(path);
				var sheet = json.fromJson(sheetString, Spreadsheet.class);
				values = engine.computeSpreadsheetValues(new SpreadsheetAdaptor(sheet));
				sheetValuesCache.put(sheetId, values);
			}
			return values;
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	private String url2Id(String url) {
		int i = url.lastIndexOf('/');
		return url.substring(i + 1);
	}

	private String id2domain(String id) {
		int i = id.lastIndexOf('@');
		return id.substring(i + 1);
	}

	private String id2owner(String id) {
		if (id.contains("-")) {
			int i = id.indexOf('-');
			return id.substring(0, i);
		} else
			return "ups";
	}

	private boolean badParam(String str) {
		return str == null || str.length() == 0;
	}

	private boolean badSheet(Spreadsheet sheet) {
		return sheet == null || !sheet.isValid();
	}

	private boolean wrongPassword(String userId, String password) {
		var user = getUser(userId);
		return user == null || !user.getPassword().equals(password);
	}

	/*
	 * Return range values from cache, otherwise compute full values if the sheet is
	 * local, or import the full values from the remote server, storing the result
	 * in the cache
	 */
	public String[][] resolveRangeValues(String sheetUrl, String range, String userId) {
		String id = url2Id(sheetUrl);
		String folder = "sheets." + id2domain(id);
		String owner = id2owner(id);
		String path = String.format("/%s/sheets/%s/%s", folder, owner, id);
		String[][] values = null;

		String sheetString = DownloadFile.run(path);
		var sheet = json.fromJson(sheetString, Spreadsheet.class);
		if (sheet != null)
			values = getComputedValues(sheet.getSheetId());
		else {
			var m = SPREADSHEETS_URI_PATTERN.matcher(sheetUrl);
			if (m.matches()) {

				var uri = m.group(1);
				var sheetId = m.group(2);
				var result = SpreadsheetsClientFactory.with(uri).fetchSpreadsheetValues(sheetId, userId);
				if (result.isOK()) {
					values = result.value();
					sheetValuesCache.put(sheetUrl, values);
				}
			}
			values = sheetValuesCache.getIfPresent(sheetUrl);
		}
		return values == null ? null : new CellRange(range).extractRangeValuesFrom(values);
	}
}
