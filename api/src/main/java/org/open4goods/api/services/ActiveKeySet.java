package org.open4goods.api.services;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Memory bounded set of resource cache keys, used by the orphan resource cleanup (GOU-1142).
 * <p>
 * Keys are not stored but reduced to a 64 bit fingerprint, kept in a sorted {@code long[]}: 8 bytes per
 * key instead of about 100 for a {@code HashSet<String>} (78.6 M keys in prod: ~0.6 GB instead of 7.6 GB).
 * <p>
 * The set is safe for the cleanup: a fingerprint collision can only make {@link #contains(String)} answer
 * true for a key that was never added, which keeps an orphan file. It can never answer false for an added
 * key, so a referenced file is never classified as orphan.
 * <p>
 * Lifecycle: {@link #add(String)} while filling, then {@link #seal()} once, then {@link #contains(String)}.
 * Not thread safe while filling.
 */
final class ActiveKeySet {

	/** Fingerprints per chunk of the filling phase (8 MB), so that growing never copies a huge array. */
	static final int CHUNK_SIZE = 1 << 20;

	/** Heap kept free on top of the strictly needed allocation. */
	private static final long HEAP_RESERVE_BYTES = 256L * 1024 * 1024;

	private long[][] chunks = new long[16][];
	private int chunkCount;
	private int lastChunkFill = CHUNK_SIZE;
	private long size;

	private long[] sorted;

	/** Raised when the heap cannot hold the key set: the run must be abandoned before an OutOfMemoryError. */
	static final class InsufficientMemoryException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		InsufficientMemoryException(String message) {
			super(message);
		}
	}

	/**
	 * Registers a key.
	 *
	 * @throws InsufficientMemoryException when the heap cannot take one more chunk
	 */
	void add(String key) {
		if (sorted != null) {
			throw new IllegalStateException("sealed");
		}
		if (lastChunkFill == CHUNK_SIZE) {
			// Sealing needs one contiguous array on top of the chunks : reserve room for it from the start
			requireHeap((size + CHUNK_SIZE) * Long.BYTES * 2L, "storing active cache keys");
			if (chunkCount == chunks.length) {
				chunks = Arrays.copyOf(chunks, chunkCount * 2);
			}
			chunks[chunkCount++] = new long[CHUNK_SIZE];
			lastChunkFill = 0;
		}
		chunks[chunkCount - 1][lastChunkFill++] = fingerprint(key);
		size++;
	}

	/** Sorts and deduplicates the fingerprints; must be called once, before {@link #contains(String)}. */
	void seal() {
		if (sorted != null) {
			return;
		}
		requireHeap(size * Long.BYTES, "sorting active cache keys");
		long[] all = new long[(int) size];
		int pos = 0;
		for (int i = 0; i < chunkCount; i++) {
			int len = i == chunkCount - 1 ? lastChunkFill : CHUNK_SIZE;
			System.arraycopy(chunks[i], 0, all, pos, len);
			pos += len;
			chunks[i] = null;
		}
		chunks = null;
		Arrays.sort(all);
		int unique = 0;
		for (int i = 0; i < all.length; i++) {
			if (i == 0 || all[i] != all[i - 1]) {
				all[unique++] = all[i];
			}
		}
		sorted = unique == all.length ? all : Arrays.copyOf(all, unique);
	}

	/** @return true when the key was added (or, with probability ~n/2^64, when another key shares its fingerprint) */
	boolean contains(String key) {
		if (sorted == null) {
			throw new IllegalStateException("not sealed");
		}
		return Arrays.binarySearch(sorted, fingerprint(key)) >= 0;
	}

	/** @return the number of distinct fingerprints once sealed, the number of keys added before */
	long size() {
		return sorted != null ? sorted.length : size;
	}

	private static void requireHeap(long neededBytes, String purpose) {
		Runtime rt = Runtime.getRuntime();
		long available = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
		if (available < neededBytes + HEAP_RESERVE_BYTES) {
			throw new InsufficientMemoryException("insufficient heap for " + purpose + ": " + (available >> 20)
					+ " MB available, " + ((neededBytes + HEAP_RESERVE_BYTES) >> 20) + " MB needed");
		}
	}

	/** 64 bit FNV-1a over the UTF-8 bytes, finished by the murmur3 avalanche to spread keys evenly. */
	static long fingerprint(String key) {
		long h = 0xcbf29ce484222325L;
		for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
			h = (h ^ (b & 0xff)) * 0x100000001b3L;
		}
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= h >>> 33;
		return h;
	}
}
