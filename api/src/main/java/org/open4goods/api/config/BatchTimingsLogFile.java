package org.open4goods.api.config;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.open4goods.api.services.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Adds a dedicated, append-only rolling file to the batch timings logger (GOU-2174), so that the
 * {@code Start of batch} / {@code End of batch in ... s} lines survive api restarts (nohup-api.log is overwritten
 * and only one previous copy is kept).
 *
 * <p>The appender is added programmatically, next to the existing ones: the logger stays additive, so the lines are
 * still written to the console (captured by nohup), and the Spring Boot logging configuration is not replaced.
 * Rotation: {@code 10 MB} per file, 10 files kept, 100 MB at most.</p>
 */
@Component
public class BatchTimingsLogFile {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchTimingsLogFile.class);

	static final String APPENDER_NAME = "BATCH_TIMINGS_FILE";
	private static final String DEFAULT_BETA_PROD_LOG_DIR = "/opt/open4goods/logs";
	private static final String FILE_NAME = "batch-timings.log";

	private final String configuredFile;
	private final String loggingPath;
	private final String loggingFileName;
	private RollingFileAppender<ILoggingEvent> appender;

	public BatchTimingsLogFile(@Value("${api.batch.timings-log-file:}") String configuredFile,
			@Value("${logging.file.path:}") String loggingPath, @Value("${logging.file.name:}") String loggingFileName) {
		this.configuredFile = configuredFile;
		this.loggingPath = loggingPath;
		this.loggingFileName = loggingFileName;
	}

	/**
	 * @return the file to write, or {@code null} when none applies (dev, tests): the configured one, else
	 *         {@code batch-timings.log} in {@code logging.file.path}, next to {@code logging.file.name}, or in
	 *         {@code /opt/open4goods/logs} when this directory exists.
	 */
	String resolveFile() {
		if (!configuredFile.isBlank()) {
			return configuredFile;
		}
		if (!loggingPath.isBlank()) {
			return new File(loggingPath, FILE_NAME).getPath();
		}
		if (!loggingFileName.isBlank() && new File(loggingFileName).getParentFile() != null) {
			return new File(new File(loggingFileName).getParentFile(), FILE_NAME).getPath();
		}
		if (new File(DEFAULT_BETA_PROD_LOG_DIR).isDirectory()) {
			return new File(DEFAULT_BETA_PROD_LOG_DIR, FILE_NAME).getPath();
		}
		return null;
	}

	@PostConstruct
	void start() {
		String file = resolveFile();
		if (file == null || !(LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
			return;
		}
		try {
			PatternLayoutEncoder encoder = new PatternLayoutEncoder();
			encoder.setContext(context);
			encoder.setPattern("%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p [%t] %logger{36} : %m%n");
			encoder.setCharset(StandardCharsets.UTF_8);
			encoder.start();

			RollingFileAppender<ILoggingEvent> rolling = new RollingFileAppender<>();
			rolling.setContext(context);
			rolling.setName(APPENDER_NAME);
			rolling.setFile(file);
			rolling.setAppend(true);
			rolling.setEncoder(encoder);

			SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy = new SizeAndTimeBasedRollingPolicy<>();
			policy.setContext(context);
			policy.setParent(rolling);
			policy.setFileNamePattern(file + ".%d{yyyy-MM-dd}.%i.gz");
			policy.setMaxFileSize(FileSize.valueOf("10MB"));
			policy.setMaxHistory(10);
			policy.setTotalSizeCap(FileSize.valueOf("100MB"));
			policy.start();
			rolling.setRollingPolicy(policy);
			rolling.start();

			((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(BatchService.TIMINGS_LOGGER)).addAppender(rolling);
			appender = rolling;
			LOGGER.info("Batch timings are also written to {}", file);
		} catch (RuntimeException e) {
			LOGGER.warn("Cannot write the batch timings file {}: {}", file, e.getMessage());
		}
	}

	@PreDestroy
	void stop() {
		if (appender != null) {
			((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(BatchService.TIMINGS_LOGGER)).detachAppender(appender);
			appender.stop();
			appender = null;
		}
	}
}
