package org.open4goods.icecat.services;

/** Raised when an operator requests a rebuild for a registry hash no longer installed. */
public class StaleRegistryProjectionException extends RuntimeException {

    public StaleRegistryProjectionException(String expectedHash, String installedHash) {
        super("requested registry hash " + expectedHash + " does not match installed hash " + installedHash);
    }
}
