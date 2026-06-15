package org.open4goods.services.feedservice.service;

import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;

/**
 * Indexes feed-produced data fragments into the owning application store.
 */
@FunctionalInterface
public interface DataFragmentIndexer {

    void index(DataFragment dataFragment, String datasourceConfigName) throws ValidationException;
}
