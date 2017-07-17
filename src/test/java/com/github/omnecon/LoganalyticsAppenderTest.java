package com.github.omnecon;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

public class LoganalyticsAppenderTest {

	private static final String workspaceId = "YourWorkspaceId";
	private static final String sharedKey = "YourShardKey";
	private static final String apiVersion = "YourApiVersion";
	private static final String logType = "YourLogType";

	
	public static void loadConfig(String loggerConfig) throws JoranException {

		System.setProperty("logback.configurationFile", loggerConfig);

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		ContextInitializer ci = new ContextInitializer(loggerContext);
		URL url = ci.findURLOfDefaultConfigurationFile(true);

		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		loggerContext.reset();
		configurator.doConfigure(url);

	}

	@Test
	public void testLoadDefaultConfiguration() throws JoranException, IOException {
		
		loadConfig("logback-test.xml");

		Logger logger = (Logger) LoggerFactory.getLogger("root");

		LoganalyticsAppender appender =(LoganalyticsAppender) logger.getAppender("LoganalyticsTest");

		Assert.assertNotNull(appender);

		Assert.assertEquals(workspaceId, appender.getWorkspaceId());
		Assert.assertEquals(sharedKey, appender.getSharedKey());
		Assert.assertEquals(logType, appender.getLogType());
		Assert.assertEquals(apiVersion, appender.getApiVersion());

	}

	@Test
	public void testLoadAsyncConfiguration() throws JoranException {

		loadConfig("logback-test-async.xml");

		Logger logger = (Logger) LoggerFactory.getLogger("root");

		AsyncAppender appenderAsync = (AsyncAppender) logger.getAppender("LoganalyticsTestAsync");
		Assert.assertNotNull(appenderAsync);

		LoganalyticsAppender appender = (LoganalyticsAppender) appenderAsync.getAppender("LoganalyticsTest");
		Assert.assertNotNull(appender);

		Assert.assertEquals(workspaceId, appender.getWorkspaceId());
		Assert.assertEquals(sharedKey, appender.getSharedKey());
		Assert.assertEquals(logType, appender.getLogType());
		Assert.assertEquals(apiVersion, appender.getApiVersion());

	}
	
	@Test
	public void testISO8601Timestamp() throws JoranException {
		// Format: YYYY-MM-DDThh:mm:ssZ according to https://docs.microsoft.com/en-us/azure/log-analytics/log-analytics-data-collector-api 

		LoganalyticsAppender appender = new LoganalyticsAppender();
		
		String date = appender.buildDateString();
		
		date = appender.buildIso8601TimeString(new Date());
		
		System.out.println(date);
	}
}
