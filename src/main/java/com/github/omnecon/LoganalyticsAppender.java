package com.github.omnecon;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class LoganalyticsAppender extends AppenderBase<ILoggingEvent> {

	private static final String HTTP_METHOD = "POST";
	private static final String CONTENT_TYPE = "application/json";
	private static final String RESOURCE = "/api/logs";

	// Mandatory values from logback config
	private String workspaceId = null;
	private String sharedKey = null;
	private String logType = null;

	// Optional values from logback-config
	private String apiVersion = "2016-04-01"; // default API version

	private boolean fieldLogLevel = true;
	private boolean fieldMessage = true;
	private boolean fieldLoggerName = false;
	private boolean fieldThreadName = false;

	private boolean utcTime = false;

	private final static Gson gson = new Gson();

	public LoganalyticsAppender() {
		super();
		addInfo("[LoganalyticsAppender] Created new LoganalyticsAppender (ID: " + System.identityHashCode(this) + ")");
	}

	@Override
	public void start() {
		super.start();
		addInfo("[LoganalyticsAppender] Started (ID: " + System.identityHashCode(this) + ") ...");
		addInfo("[LoganalyticsAppender] workspaceId: " + this.workspaceId);
		addInfo("[LoganalyticsAppender] logType: " + this.logType);
		addInfo("[LoganalyticsAppender] apiVersion: " + this.apiVersion);
		addInfo("[LoganalyticsAppender] fieldLogLevel: " + this.fieldLogLevel);
		addInfo("[LoganalyticsAppender] fieldMessage: " + this.fieldMessage);
		addInfo("[LoganalyticsAppender] fieldLoggerName: " + this.fieldLoggerName);
		addInfo("[LoganalyticsAppender] fieldThreadName: " + this.fieldThreadName);
		addInfo("[LoganalyticsAppender] utcTime: " + this.utcTime);

		if (this.workspaceId == null) {
			throw new IllegalArgumentException("Mandatory field: 'workspaceId' not set");
		}
		if (this.sharedKey == null) {
			throw new IllegalArgumentException("Mandatory field: 'sharedKey' not set");
		}
		if (this.logType == null) {
			throw new IllegalArgumentException("Mandatory field: 'logType' not set");
		}
	}

	@Override
	public void stop() {
		super.stop();
		addInfo("[LoganalyticsAppender] Stopped (ID: " + System.identityHashCode(this) + ")");
	}

	@Override
	public synchronized void doAppend(ILoggingEvent eventObject) {
		super.doAppend(eventObject);
	}

	private String buildAuthorizationHeader(String message)
			throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(Base64.decodeBase64(this.sharedKey), "HmacSHA256"));
		String signature = new String(Base64.encodeBase64(mac.doFinal(message.getBytes("UTF-8"))));
		String authHeader = "SharedKey " + workspaceId + ":" + signature;
		return authHeader;
	}

	protected String buildIso8601TimeString(Date date) {

		DateFormat df = null;
		if (utcTime) {
			df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'"); // 'Z'
																		// for
																		// UTC
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
		} else {
			df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}

		return df.format(date);
	}

	protected String buildDateString() {
		// create ISO 8601 date string
		SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		return fmt.format(Calendar.getInstance().getTime()) + " GMT";
	}

	protected void postData(String dateOfRequest, String authHeader, String json) throws IOException {

		String url = "https://" + workspaceId + ".ods.opinsights.azure.com/api/logs?api-version=" + apiVersion;

		HttpPost req = new HttpPost(url);

		req.addHeader("Content-type", CONTENT_TYPE);
		req.addHeader("Log-Type", logType);
		req.addHeader("Authorization", authHeader);
		req.addHeader("x-ms-date", dateOfRequest);
		req.addHeader("time-generated-field", "DateValue");

		req.setEntity(new ByteArrayEntity(StringUtils.getBytesUtf8(json)));

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			CloseableHttpResponse result = httpClient.execute(req);

			int status = result.getStatusLine().getStatusCode();
			if (status / 100 != 2) {
				String responseBody = null;
				responseBody = EntityUtils.toString(result.getEntity());
				addError("[LoganalyticsAppender] Error (status-code " + status + "): " + responseBody);
			}
		}
	}

	@Override
	protected void append(ILoggingEvent event) {

		try {
			Map<String, String> logEvent = new LinkedHashMap<String, String>();

			if (fieldLogLevel) {
				logEvent.put("LogLevel", event.getLevel().toString());
			}

			if (fieldMessage) {
				logEvent.put("Message", event.getMessage());
			}

			if (fieldLoggerName) {
				logEvent.put("LoggerName", event.getLoggerName());
			}

			if (fieldThreadName) {
				logEvent.put("ThreadName", event.getThreadName());
			}

			String timeGenerated = buildIso8601TimeString(new Date(event.getTimeStamp()));

			logEvent.put("DateValue", timeGenerated);

			String json = gson.toJson(logEvent);

			String dateOfRequest = buildDateString();

			String signatureString = String.format("%s\n%d\n%s\nx-ms-date:%s\n%s", HTTP_METHOD, json.length(),
					CONTENT_TYPE, dateOfRequest, RESOURCE);

			String authHeader = buildAuthorizationHeader(signatureString);

			postData(dateOfRequest, authHeader, json);

		} catch (Exception e) {
			addError("[LoganalyticsAppender] Error when posting data to Data collector API: " + e);
		}

	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public String getSharedKey() {
		return sharedKey;
	}

	public void setSharedKey(String sharedKey) {
		this.sharedKey = sharedKey;
	}

	public String getLogType() {
		return logType;
	}

	public void setLogType(String logType) {
		this.logType = logType;
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public boolean isFieldLogLevel() {
		return fieldLogLevel;
	}

	public void setFieldLogLevel(boolean fieldLogLevel) {
		this.fieldLogLevel = fieldLogLevel;
	}

	public boolean isFieldMessage() {
		return fieldMessage;
	}

	public void setFieldMessage(boolean fieldMessage) {
		this.fieldMessage = fieldMessage;
	}

	public boolean isFieldLoggerName() {
		return fieldLoggerName;
	}

	public void setFieldLoggerName(boolean fieldLoggerName) {
		this.fieldLoggerName = fieldLoggerName;
	}

	public boolean isFieldThreadName() {
		return fieldThreadName;
	}

	public void setFieldThreadName(boolean fieldThreadName) {
		this.fieldThreadName = fieldThreadName;
	}

	public boolean isUtcTime() {
		return utcTime;
	}

	public void setUtcTime(boolean utcTime) {
		this.utcTime = utcTime;
	}

}
