package org.open4goods.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.api.services.BatchService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/** GOU-2174: batch timings file, written in append mode. */
class BatchTimingsLogFileTest {

	@Test
	void linesAreAppendedAcrossRestarts(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("batch-timings.log");
		Logger timings = (Logger) LoggerFactory.getLogger(BatchService.TIMINGS_LOGGER);
		Level previous = timings.getLevel();
		timings.setLevel(Level.INFO);
		try {
			BatchTimingsLogFile first = new BatchTimingsLogFile(file.toString(), "", "");
			first.start();
			timings.info("Start of batch run-1");
			first.stop();

			BatchTimingsLogFile second = new BatchTimingsLogFile(file.toString(), "", "");
			second.start();
			timings.info("End of batch in 12 s run-2");
			second.stop();

			String content = Files.readString(file);
			assertTrue(content.contains("Start of batch run-1"), content);
			assertTrue(content.contains("End of batch in 12 s run-2"), content);

			timings.info("after stop");
			assertTrue(!Files.readString(file).contains("after stop"));
		} finally {
			timings.setLevel(previous);
		}
	}

	@Test
	void disabledWithoutLogDirectory() {
		BatchTimingsLogFile none = new BatchTimingsLogFile("", "", "");
		if (!Files.isDirectory(Path.of("/opt/open4goods/logs"))) {
			assertNull(none.resolveFile());
		}
		assertEquals(Path.of("/x/batch-timings.log").toString(), new BatchTimingsLogFile("", "/x", "").resolveFile());
	}
}
