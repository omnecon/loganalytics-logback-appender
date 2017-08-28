package com.github.omnecon;

import java.io.IOException;
import java.net.URL;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

// Only used for standalone performance tests
public class LoganalyticsAppenderPerformanceTest {

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

	private long toMB(long byteValue) {
		return byteValue / 1024 / 1024;
	}

	//@Test
	public void testLoadDefaultConfiguration() throws JoranException, IOException {

		loadConfig("logback-test-async-perf.xml");

		Logger logger = (Logger) LoggerFactory.getLogger("root");

		AsyncAppender asyncAppender = (AsyncAppender) logger.getAppender("LoganalyticsTestAsyncPerf");
		LoganalyticsAppender appender = (LoganalyticsAppender) asyncAppender.getAppender("LoganalyticsTestPerf");

		Assert.assertNotNull(asyncAppender);

		Runtime rt = Runtime.getRuntime();

		System.out.println("MaxFlushTime: " + asyncAppender.getMaxFlushTime());
		System.out.println("QueueSize: " + asyncAppender.getQueueSize());
		System.out.println("NumberOfElementsInQueue: " + asyncAppender.getNumberOfElementsInQueue());
		System.out.println("RemainingCapacity: " + asyncAppender.getRemainingCapacity());
		System.out.println("TotalMemory [MB]: " + toMB(rt.totalMemory()));
		System.out.println("FreeMemory [MB]: " + toMB(rt.freeMemory()));
		System.out.println("MemoryUsage [MB]: " + new Long(toMB(rt.totalMemory() - rt.freeMemory())));
		appender.STATISTICS.print();
		
		long start = System.nanoTime();
		IntStream.range(0, 10000).forEach(i -> {
			logger.error("hello world " + i);
		});
		long stop = System.nanoTime();

		System.out.println("Init [ms]: " + String.valueOf((stop - start) / 1000 / 1000));

		IntStream.range(0, 100).forEach(i -> System.out.print("_"));
		System.out.println("");

		wait(100000); // 100 sec --> 407 events; with pool: 1549 events

		System.out.println("");

		System.out.println("MaxFlushTime: " + asyncAppender.getMaxFlushTime());
		System.out.println("NumberOfElementsInQueue: " + asyncAppender.getNumberOfElementsInQueue());
		System.out.println("QueueSize: " + asyncAppender.getQueueSize());
		System.out.println("RemainingCapacity: " + asyncAppender.getRemainingCapacity());
		System.out.println("TotalMemory: " + toMB(rt.totalMemory()));
		System.out.println("FreeMemory: " + toMB(rt.freeMemory()));
		System.out.println("MemoryUsage: " + new Long(toMB(rt.totalMemory() - rt.freeMemory())));
		appender.STATISTICS.print();

	}

	private void wait(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
