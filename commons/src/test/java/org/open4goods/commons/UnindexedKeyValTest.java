package org.open4goods.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.model.data.UnindexedKeyVal;

public class UnindexedKeyValTest {

	@Test
	public void test() {
	
		Set<UnindexedKeyVal> test = new HashSet<>();
		
		test.add(new UnindexedKeyVal("1", "val1"));
		test.add(new UnindexedKeyVal("1", "val2"));
		test.add(new UnindexedKeyVal("2", "val2"));
		
		
		// Testing invalid
		assertEquals(2, test.size());





	}
	//




}
