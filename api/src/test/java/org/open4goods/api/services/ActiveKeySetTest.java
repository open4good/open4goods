package org.open4goods.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActiveKeySetTest {

	@Test
	void everyAddedKeyIsFoundAcrossChunks() {
		int n = ActiveKeySet.CHUNK_SIZE * 2 + 123;
		ActiveKeySet set = new ActiveKeySet();
		for (int i = 0; i < n; i++) {
			set.add("key-" + i + ".cache.webp");
		}
		set.seal();
		for (int i = 0; i < n; i++) {
			assertTrue(set.contains("key-" + i + ".cache.webp"), "referenced key classified orphan: " + i);
		}
		assertEquals(n, set.size());
	}

	@Test
	void unknownKeysAreNotFoundAndDuplicatesCountOnce() {
		ActiveKeySet set = new ActiveKeySet();
		set.add("a");
		set.add("a");
		set.add("b");
		set.seal();
		assertEquals(2, set.size());
		assertTrue(set.contains("a"));
		assertFalse(set.contains("c"));
	}

	@Test
	void emptySetContainsNothing() {
		ActiveKeySet set = new ActiveKeySet();
		set.seal();
		assertFalse(set.contains("a"));
		assertEquals(0, set.size());
	}

	@Test
	void containsBeforeSealIsRejected() {
		assertThrows(IllegalStateException.class, () -> new ActiveKeySet().contains("a"));
	}

	/** Synthetic volume: 8 bytes per key, far below the ~100 bytes of a HashSet entry (GOU-1142). */
	@Test
	void memoryFootprintIsAboutEightBytesPerKey() {
		int n = 5_000_000;
		Runtime rt = Runtime.getRuntime();
		System.gc();
		long before = rt.totalMemory() - rt.freeMemory();
		ActiveKeySet set = new ActiveKeySet();
		for (int i = 0; i < n; i++) {
			set.add(Integer.toHexString(i * 31) + "-" + i);
		}
		set.seal();
		System.gc();
		long perKey = (rt.totalMemory() - rt.freeMemory() - before) / n;
		assertTrue(perKey < 24, "bytes per key: " + perKey);
		assertTrue(set.contains(Integer.toHexString(77 * 31) + "-77"));
	}
}
