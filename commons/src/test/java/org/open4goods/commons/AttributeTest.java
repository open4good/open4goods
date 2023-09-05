package org.open4goods.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.open4goods.model.BarcodeType;
import org.open4goods.model.data.UnindexedKeyValTimestamp;
import org.open4goods.model.product.AggregatedAttribute;
import org.open4goods.services.BarcodeValidationService;

public class AttributeTest {

	@Test
	public void testAttributeElection() {
		AggregatedAttribute agg = new AggregatedAttribute();

		// 2 points for 1
		agg.addAttribute("name1", new UnindexedKeyValTimestamp("provider1", "val1"));
		agg.addAttribute("name1", new UnindexedKeyValTimestamp("provider2", "val1"));

		// 1 point for 4
		agg.addAttribute("name1", new UnindexedKeyValTimestamp("provider3", "val4"));

		
		// 3 points for "2"
		agg.addAttribute("name1", new UnindexedKeyValTimestamp("provider4", "val2"));

		agg.addAttribute("name1", new UnindexedKeyValTimestamp("provider5", "val2"));

		agg.addAttribute("name1", new UnindexedKeyValTimestamp("provider6", "val2"));
		
		// Count for one "2", because of erasure
		agg.addAttribute("name1", new UnindexedKeyValTimestamp("provider7", "val3"));
		agg.addAttribute("name1", new UnindexedKeyValTimestamp("provider7", "val2"));

		assertEquals(agg.bestValue(), "val2");
		
		assertEquals(agg.getPonderedvalues(), 4);
	}
	//

}
