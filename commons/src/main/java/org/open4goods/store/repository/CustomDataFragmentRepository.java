package org.open4goods.store.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.open4goods.model.data.DataFragment;

public interface CustomDataFragmentRepository  {

	/**
	 * Return all the (eventually) previously existing data fragments
	 * @param buffer
	 * @return
	 */
    Map<String, DataFragment> get(Set<DataFragment> buffer);
////
////	/**
////	 * Export all distinct relation ids for a given indice.
////	 * @param query
////	 * @param indice
////	 * @return
////	 */
//    Set<String>  exportQualifiers(String relationIndexName);

	/**
	 * Export all dataFragments matching a specific query from the main indice
	 * @param query
	 * @param indice
	 * @return
	 */
    Stream<DataFragment> export(String query);
	
    /**
     * Get datafragments by gtin
     * @param gtin
     * @return
     */
	Stream<DataFragment> getByGtin(String gtin);
    /**
     * Export the GTINs
     * @return
     */
    Stream<String> exportGtin();

    /**
     * Export the GTINs, with a given limit
     * @return
     */
    Stream<String> exportGtin(Long limit);
    
	
//	/**
//	 * Delete a DataFragment by id in the master collection
//	 * @param id
//	 */
//	void deleteById(String id);

	/**
	 * Retrieve all previous versions of the input DataFragments, merge the previously existing ones with  the input, then store the updated version
	 * @param buffer
	 */
//	@Timed(value="repository.DataFragmentRepository.store()",description="Retrieve all previous versions of the input DataFragments, merge the previously existing ones with  the input, then store the updated version")
//    default void store(final Set<DataFragment> buffer) {
//		
//		if (buffer == null || 0== buffer.size()) {			
//			return;
//		}
//		
//		final Map<String,DataFragment> existings = get(buffer);
//		
//
//		// Updating the previously existing ones
//		for (final DataFragment newFrag : buffer) {
//			final DataFragment existing = existings.get(newFrag.getUrl());
//			if (null != existing) {
//				existing.addVersion(newFrag);
//			} else {
//				existings.put(newFrag.getUrl(), newFrag);
//			}
//		}
//
////		 Persistence of the buffer
//		save(existings.values());
//
//	}
//	
	
	void save(Collection<DataFragment> collection);

	Map<String, Set<DataFragment>> getByGtin(Set<String> gtin);


}
