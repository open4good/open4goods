package org.open4goods.nudgerfrontapi.service.css;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * File storage of the {@link CssSyncState} (GOU-2081). The state survives a restart; an OS file lock
 * ({@code <file>.lock}) prevents two instances sharing the file from running at the same time, and the file is
 * replaced atomically so a crash never leaves a half-written state.
 * <p>
 * Choice: front-api is deployed as a single jar per environment (beta, prod), so a local file is enough. Two
 * instances on different hosts would each send their own diff: give them one shared volume, or enable the sync on
 * one instance only.
 * </p>
 */
public class CssSyncStateStore {

    private final Path file;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * @param file state file, its directory is created when missing
     */
    public CssSyncStateStore(Path file) {
        this.file = file;
    }

    /** Held lock of a run, released by {@link #close()}. */
    public final class Lock implements AutoCloseable {
        private final FileChannel channel;
        private final FileLock lock;

        private Lock(FileChannel channel, FileLock lock) {
            this.channel = channel;
            this.lock = lock;
        }

        @Override
        public void close() {
            try {
                lock.release();
                channel.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Takes the run lock without waiting.
     *
     * @return the lock, or null when another instance or run holds it
     * @throws IOException when the lock file cannot be created
     */
    public Lock tryLock() throws IOException {
        Path lockFile = file.resolveSibling(file.getFileName() + ".lock");
        if (lockFile.getParent() != null) {
            Files.createDirectories(lockFile.getParent());
        }
        FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return null;
            }
            return new Lock(channel, lock);
        } catch (java.nio.channels.OverlappingFileLockException e) {
            channel.close();
            return null;
        }
    }

    /**
     * @return the stored state, empty when the file does not exist
     * @throws IOException when the file exists but cannot be read
     */
    public CssSyncState load() throws IOException {
        if (!Files.exists(file)) {
            return CssSyncState.empty();
        }
        CssSyncState state = json.readValue(file.toFile(), CssSyncState.class);
        return new CssSyncState(state.entries() == null ? new TreeMap<>() : new TreeMap<>(state.entries()));
    }

    /**
     * Writes the state atomically.
     *
     * @param state state to keep
     * @throws IOException when the file cannot be written
     */
    public void save(CssSyncState state) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        json.writeValue(tmp.toFile(), state);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
