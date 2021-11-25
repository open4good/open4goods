package org.open4goods.helper;

import java.math.BigInteger;

/**
 * Big up to https://github.com/jakedouglas/fnv-java !! ;)
 *
 * @author goulven
 *
 */
public class FNV {
	private static final BigInteger INIT32 = new BigInteger("811c9dc5", 16);
	private static final BigInteger INIT64 = new BigInteger("cbf29ce484222325", 16);
	private static final BigInteger PRIME32 = new BigInteger("01000193", 16);
	private static final BigInteger PRIME64 = new BigInteger("100000001b3", 16);
	private static final BigInteger MOD32 = new BigInteger("2").pow(32);
	private static final BigInteger MOD64 = new BigInteger("2").pow(64);

	public static String hash32(final String input) {
		return fnv1a_32(input.getBytes()).toString();
	}

	public static String hash64(final String input) {
		return fnv1a_64(input.getBytes()).toString();
	}

	public static BigInteger fnv1_32(final byte[] data) {
		BigInteger hash = INIT32;

		for (final byte b : data) {
			hash = hash.multiply(PRIME32).mod(MOD32);
			hash = hash.xor(BigInteger.valueOf(b & 0xff));
		}

		return hash;
	}

	public static BigInteger fnv1_64(final byte[] data) {
		BigInteger hash = INIT64;

		for (final byte b : data) {
			hash = hash.multiply(PRIME64).mod(MOD64);
			hash = hash.xor(BigInteger.valueOf(b & 0xff));
		}

		return hash;
	}

	public static BigInteger fnv1a_32(final byte[] data) {
		BigInteger hash = INIT32;

		for (final byte b : data) {
			hash = hash.xor(BigInteger.valueOf(b & 0xff));
			hash = hash.multiply(PRIME32).mod(MOD32);
		}

		return hash;
	}

	public static BigInteger fnv1a_64(final byte[] data) {
		BigInteger hash = INIT64;

		for (final byte b : data) {
			hash = hash.xor(BigInteger.valueOf(b & 0xff));
			hash = hash.multiply(PRIME64).mod(MOD64);
		}

		return hash;
	}
}