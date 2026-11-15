package org.open4goods.api.services.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * GOU-2266: the restore only reads a file of the backup folder, a resume needs a resumable last run, and a run left
 * RUNNING by a stopped api is read back INTERRUPTED.
 */
class BackfillServiceTest {

	@TempDir
	Path tmp;

	private BackfillService service() {
		return new BackfillService(mock(BackfillEngine.class), tmp.resolve("status.json"), tmp.resolve("backups"));
	}

	@Test
	void restoreOnlyReadsAFileOfTheBackupFolder() throws IOException {
		BackfillService service = service();
		Files.createDirectories(tmp.resolve("backups"));
		Path backup = Files.writeString(tmp.resolve("backups").resolve("decitre-purge-1.jsonl"), "",
				StandardCharsets.UTF_8);
		Files.writeString(tmp.resolve("secret.txt"), "x", StandardCharsets.UTF_8);

		assertThat(service.resolveBackup("decitre-purge-1.jsonl")).isEqualTo(backup.toAbsolutePath().normalize());
		assertThatThrownBy(() -> service.resolveBackup("../secret.txt")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.resolveBackup(tmp.resolve("secret.txt").toString()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.resolveBackup("missing.jsonl")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void restoreRefusesTheBackupOfAnotherOperation() throws IOException {
		BackfillService service = service();
		Files.createDirectories(tmp.resolve("backups"));
		Files.writeString(tmp.resolve("backups").resolve("nature-1.jsonl"), "", StandardCharsets.UTF_8);

		assertThatThrownBy(() -> service.startRestore(new DecitrePurgeOperation(), "nature-1.jsonl", 10, 0))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("decitre-purge");
	}

	@Test
	void resumeWithOtherParametersIsRefused() throws IOException {
		Files.writeString(tmp.resolve("status.json"), """
				{"state":"STOPPED","operation":"decitre-purge","dryRun":false,"restore":false,
				 "parameters":{"keys":["ATTRIBUTS"]},
				 "report":{"operation":"decitre-purge","cursor":{"pitId":"pit-1","searchAfter":[42]}}}
				""", StandardCharsets.UTF_8);
		BackfillService service = service();

		assertThatThrownBy(() -> service.start(new DecitrePurgeOperation(), new BackfillOptions(false, 10, 0, true)))
				.isInstanceOf(IllegalStateException.class);
		assertThat(service.getStatus().parameters()).isEqualTo(java.util.Map.of("keys", java.util.List.of("ATTRIBUTS")));
	}

	@Test
	void resumeWithoutAResumableRunIsRefused() {
		assertThatThrownBy(() -> service().start(new DecitrePurgeOperation(), new BackfillOptions(false, 10, 0, true)))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void runningStatusIsReadBackInterrupted() throws IOException {
		Files.writeString(tmp.resolve("status.json"), """
				{"state":"RUNNING","operation":"decitre-purge","dryRun":false,"restore":false,
				 "report":{"operation":"decitre-purge","scannedProducts":5,
				           "cursor":{"pitId":"pit-1","searchAfter":[42]}}}
				""", StandardCharsets.UTF_8);

		BackfillStatus status = service().getStatus();

		assertThat(status.state()).isEqualTo(BackfillStatus.State.INTERRUPTED);
		assertThat(status.report().cursor().searchAfter()).containsExactly(42L);
		assertThat(Files.readString(tmp.resolve("status.json"), StandardCharsets.UTF_8)).contains("INTERRUPTED");
	}
}
