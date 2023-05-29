package org.open4goods.helper;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;

/**
 * Created by Goulven on 19/08/2014.
 */
public class GenericFileLogger {

	private static final String FILE_ROTATION_SIZE = "200MB";
	private static final String PATTERN = "%d{dd MMM yyyy HH:mm:ss.SSS} ;  %-5level ; %msg%n";
	private static final int MAX_NUMBER_OF_FILE = 5;


	private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();
	private static final Map<Logger, String> filePaths = new ConcurrentHashMap<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	/**
	 * Get a programatic file logger
	 *
	 * @param provider
	 * @return
	 */
	public static org.slf4j.Logger initLogger(final String provider, final Level level, final String path,
			final boolean toConsole) {

		Logger logbackLogger = loggers.get(provider);
		if (null == logbackLogger) {
			final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
			encoder.setContext(loggerContext);
			encoder.setPattern(PATTERN);

			encoder.start();
			if (toConsole) {
				final ConsoleAppender ca = new ConsoleAppender<>();
				ca.setEncoder(encoder);

				ca.setContext(loggerContext);
				ca.setName(provider);

				ca.start();

				// attach the rolling file appender to the logger of your choice
				logbackLogger = loggerContext.getLogger("GENERIC_LOGGER." + provider);
				logbackLogger.addAppender(ca);
				logbackLogger.setLevel(level);
				logbackLogger.setAdditive(true);
				// Adding it in the uidMap

				loggers.put(provider, logbackLogger);
			} else {

				//				// Creating dirs if it does not exists
				new File(path).mkdirs();
				final String file = path + "/" + provider + ".log";

				final PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
				logEncoder.setContext(loggerContext);
				logEncoder.setPattern(PATTERN);
				logEncoder.start();


				final RollingFileAppender<ILoggingEvent> rfAppender = new RollingFileAppender<>();
				rfAppender.setContext(loggerContext);
				rfAppender.setFile(file);

				final FixedWindowRollingPolicy fwRollingPolicy = new FixedWindowRollingPolicy();
				fwRollingPolicy.setContext(loggerContext);
				fwRollingPolicy.setFileNamePattern(path + "/" + provider + "-%i.log.zip");
				fwRollingPolicy.setMaxIndex(MAX_NUMBER_OF_FILE);
				fwRollingPolicy.setParent(rfAppender);
				fwRollingPolicy.start();

				final SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
				triggeringPolicy.setMaxFileSize(FileSize.valueOf(FILE_ROTATION_SIZE));
				triggeringPolicy.start();

				rfAppender.setEncoder(encoder);
				rfAppender.setRollingPolicy(fwRollingPolicy);
				rfAppender.setTriggeringPolicy(triggeringPolicy);
				rfAppender.start();

				// attach the rolling file appender to the logger of your choice
				logbackLogger = loggerContext.getLogger("GENERIC_LOGGER." + provider);
				logbackLogger.addAppender(rfAppender);
				logbackLogger.setLevel(level);
				logbackLogger.setAdditive(false);



				// Adding it in the uidMap
				loggers.put(provider, logbackLogger);
				filePaths.put(logbackLogger, file);
			}
		}
		return logbackLogger;

	}

	public static File getLoggerFile(final org.slf4j.Logger logger) {
		return new File(filePaths.get(logger));
	}
}
