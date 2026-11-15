package org.open4goods.nudgerfrontapi.service.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.shopping.css.v1.CssProductInput;

/**
 * Client that never calls the network (GOU-2081): records the calls, logs them, and can be told to fail for chosen
 * identifiers. Used by the tests and by {@code front.css.sync.simulated=true}.
 */
public class SimulatedCssProductsClient implements CssProductsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedCssProductsClient.class);

    private final List<CssProductInput> inserted = Collections.synchronizedList(new ArrayList<>());
    private final List<String> deleted = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> failing = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void insert(CssProductInput input) {
        failIfRequested(input.getRawProvidedId());
        inserted.add(input);
        LOGGER.info("CSS simulated insert {}", input.getRawProvidedId());
    }

    @Override
    public void delete(String rawProvidedId, String contentLanguage, String feedLabel) {
        failIfRequested(rawProvidedId);
        deleted.add(rawProvidedId);
        LOGGER.info("CSS simulated delete {}", rawProvidedId);
    }

    /**
     * @param rawProvidedId identifier whose calls fail until {@link #stopFailing()}
     */
    public void failFor(String rawProvidedId) {
        failing.add(rawProvidedId);
    }

    /** Stops every simulated failure. */
    public void stopFailing() {
        failing.clear();
    }

    /**
     * @return a copy of the inputs inserted so far
     */
    public List<CssProductInput> inserted() {
        synchronized (inserted) {
            return new ArrayList<>(inserted);
        }
    }

    /**
     * @return a copy of the identifiers deleted so far
     */
    public List<String> deleted() {
        synchronized (deleted) {
            return new ArrayList<>(deleted);
        }
    }

    private void failIfRequested(String rawProvidedId) {
        if (failing.contains(rawProvidedId)) {
            throw new CssClientException("SIMULATED_FAILURE", "Simulated failure for " + rawProvidedId, null);
        }
    }
}
