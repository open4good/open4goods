package org.open4goods.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.open4goods.helper.IdHelper;

public class IdHelperTest {

	@Test
	public void test() {
		assertEquals(IdHelper.getCategoryName("IMAGE   eT SON>ACCESSOIRES >MEUBLES SUPPORTS TV"), "ACCESSOIRES>MEUBLES SUPPORTS TV");
		assertEquals(IdHelper.getCategoryName(""), null);
		
	}
//	
	
	
	@Test
	// Test as of 10/2020 : makes a "at most" 2 tags, remove accents
	public void testAccentAndSplit() {
		assertEquals(IdHelper.getCategoryName("TV>VIDÉO>HOME CINÉMA>VIDEOPROJECTION>ACCESSOIRES>LAMPE VIDÉOPROJECTEUR"), "ACCESSOIRES>LAMPE VIDEOPROJECTEUR");
		assertEquals(IdHelper.getCategoryName(">Téléviseur"), "TELEVISEUR");
	}
	
	

}
