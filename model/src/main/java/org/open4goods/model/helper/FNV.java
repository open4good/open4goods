package org.open4goods.model.helper;

import java.nio.charset.StandardCharsets;

/**
 * FNV-1a hash implementation using native long arithmetic.
 * The BigInteger-based variant was previously used and caused ~14% of all CPU
 * samples due to allocating ~200 BigInteger objects per hash call.
 *
 * @author goulven
 */
public class FNV {

	private static final long FNV_OFFSET_64 = 0xcbf29ce484222325L;
	private static final long FNV_PRIME_64  = 0x100000001b3L;

	private static final int  FNV_OFFSET_32 = 0x811c9dc5;
	private static final int  FNV_PRIME_32  = 0x01000193;

	public static String hash32(final String input) {
		return Integer.toUnsignedString(fnv1a_32(input.getBytes(StandardCharsets.UTF_8)));
	}

	public static String hash64(final String input) {
		return Long.toUnsignedString(fnv1a_64(input.getBytes(StandardCharsets.UTF_8)));
	}

	public static int fnv1a_32(final byte[] data) {
		int hash = FNV_OFFSET_32;
		for (final byte b : data) {
			hash ^= (b & 0xff);
			hash *= FNV_PRIME_32;
		}
		return hash;
	}

	public static long fnv1a_64(final byte[] data) {
		long hash = FNV_OFFSET_64;
		for (final byte b : data) {
			hash ^= (b & 0xffL);
			hash *= FNV_PRIME_64;
		}
		return hash;
	}

	public static int fnv1_32(final byte[] data) {
		int hash = FNV_OFFSET_32;
		for (final byte b : data) {
			hash *= FNV_PRIME_32;
			hash ^= (b & 0xff);
		}
		return hash;
	}

	public static long fnv1_64(final byte[] data) {
		long hash = FNV_OFFSET_64;
		for (final byte b : data) {
			hash *= FNV_PRIME_64;
			hash ^= (b & 0xffL);
		}
		return hash;
	}
}