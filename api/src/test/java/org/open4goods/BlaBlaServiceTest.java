//package org.open4goods;
//		import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.fail;
//
//import org.junit.jupiter.api.Test;
//import org.open4goods.exceptions.InvalidParameterException;
//import org.open4goods.model.attribute.Attribute;
//import org.open4goods.model.attribute.AttributeType;
//import org.open4goods.model.attribute.Cardinality;
//import org.open4goods.model.data.DataFragment;
//import org.open4goods.model.data.Rating;
//import org.open4goods.model.data.RatingType;
//import org.open4goods.model.product.AggregatedAttribute;
//import org.open4goods.model.product.Product;
//import org.open4goods.services.EvaluationService;
//import org.open4goods.services.textgen.BlaBlaSecGenerator;
//import org.open4goods.services.textgen.BlablaService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//		
//		public class BlaBlaServiceTest {
//		
//			private static final Logger LOGGER = LoggerFactory.getLogger(BlaBlaServiceTest.class);
//		
//			private @Autowired
//		    final EvaluationService evaluationService = new EvaluationService();
//		
//		
//			@Test
//			public void seqGenTest() {
//				for (int j = 0; j < 5; j++) {
//					final Integer id = ("c" + (+j) + 'b' + j * 2).hashCode();
//					final BlaBlaSecGenerator secGen = new BlaBlaSecGenerator(id);
//					for (int i = 0; i < 10; i++) {
//						assertNotNull(secGen.getNextAlea(9));
//					}
//				}
//		
//			}
//		
//			@Test
//			public void failBlablaTest() throws InvalidParameterException {
//		
//				final Product data = getTestData();
//		
//				final BlablaService bbs = new BlablaService(evaluationService);
//		
//				String id;
//				String source;
//				String expected;
//		
//				///////////////////////////////////////////////
//				// Testing reouput HTML
//				///////////////////////////////////////////////
//				// Simple
//				id = "THOMSON-B";
//				source = "hello <div> my <b class='a'>friend</b></div>";
//				expected = "hello <div> my <b class='a'>friend</b></div>";
//				testBlabla(bbs, data, id, source, expected);
//		
//				///////////////////////////////////////////////
//				// Testing null
//				///////////////////////////////////////////////
//				try {
//					id = "THOMSON-B";
//		//		source =  "hello <div> my <b class='a'>friend</b></div>";
//		//		expected =  "hello <div> my <b class='a'>friend</b></div>";
//					testBlabla(bbs, null, id, null, null);
//					fail("Should have fail");
//				} catch (InvalidParameterException e) {
//				}
//		
//				///////////////////////////////////////////////
//				// Testing badly formed fast or
//				///////////////////////////////////////////////
//		
//				id = "THOMSON-B";
//				source = "hello || gros | gros |";
//				expected = "hello || gros | gros |";
//				testBlabla(bbs, data, id, source, expected);
//		
//				///////////////////////////////////////////////
//				// Testing badly formed HTML
//				///////////////////////////////////////////////
//		
//				try {
//					id = "THOMSON-B";
//					source = "hello <div> my <b class='a'>friend";
//					expected = "hello <div> my <b class='a'>friend";
//					testBlabla(bbs, data, id, source, expected);
//					fail("Should have fail");
//				} catch (Exception e) {
//		
//				}
//		
//			}
//		
//			@Test
//			public void basicBlablaTest() throws InvalidParameterException {
//		
//				final Product data = getTestData();
//		
//				final BlablaService bbs = new BlablaService(evaluationService);
//		
//				String id;
//				String source;
//				String expected;
//		
//				///////////////////////////////////////////////
//				// Testing simple or and seqgen
//				///////////////////////////////////////////////
//				// Simple
//				id = "THOMSON-B";
//				source = "hello";
//				expected = "hello";
//				testBlabla(bbs, data, id, source, expected);
//		
//				/////////////////////////////////
//				// Fast or test
//				////////////////////////////////////
//				id = "THOMSON-C";
//				source = "hello || toto | titi | tata  ||";
//				expected = "hello tata";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "THOMSON-D";
//				source = "hello || toto | titi | tata  ||";
//				expected = "hello toto";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "THOMSON-B";
//				source = "hello || toto | titi | tata  ||";
//				expected = "hello titi";
//				testBlabla(bbs, data, id, source, expected);
//		
//				// multiple
//		
//				id = "c";
//				source = "hello || good |  bad  || || guy  | girl ||";
//				expected = "hello good girl";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "d";
//				source = "hello || good |  bad  || || guy  | girl ||";
//				expected = "hello bad guy";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "e";
//				source = "hello || good |  bad  || || guy  | girl ||";
//				expected = "hello good guy";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "f";
//				source = "hello || good |  bad  || || guy  | girl ||";
//				expected = "hello bad girl";
//				testBlabla(bbs, data, id, source, expected);
//		
//			}
//		
//			@Test
//			public void basicBlablaIfTest() throws InvalidParameterException {
//		
//				final Product data = getTestData();
//		
//				final BlablaService bbs = new BlablaService(evaluationService);
//		
//				String id;
//				String source;
//				String expected;
//		
//				///////////////////////////////////////////////
//				// Testing <if true> and <if false>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				source = "hello <if true='1==1'> girl </if> <if false='1==1'> guy </if> ";
//				expected = "hello girl";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "f";
//				source = "hello <if true='1==1'> girl </if> and <if false='2==1'> guy </if> ";
//				expected = "hello girl and guy";
//				testBlabla(bbs, data, id, source, expected);
//		
//				///////////////////////////////////////////////
//				// Testing <embeded>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				source = "hello <if true='1==1'> girl and  <if false='2==1'> ||big|small|| guy </if>  </if> ";
//				expected = "hello girl and small guy";
//				testBlabla(bbs, data, id, source, expected);
//		
//				///////////////////////////////////////////////
//				// Testing <embeded>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				source = "hello <if true='1==1'> girl </if> and <if false='2==1'> guy </if> ";
//				expected = "hello girl and guy";
//				testBlabla(bbs, data, id, source, expected);
//		
//			}
//		
//			@Test
//			public void scoreTest() throws InvalidParameterException {
//		
//				final Product data = getTestData();
//		
//				final BlablaService bbs = new BlablaService(evaluationService);
//		
//				String id;
//				String source;
//				String expected;
//		
//				///////////////////////////////////////////////
//				// Testing <if true> and <if false>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				source = "hello <if score-1='rse'> girl </if> ";
//				expected = "hello";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "f";
//				source = "hello <if score-2='rse'> girl </if> ";
//				expected = "hello";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "f";
//				source = "hello <if score-3='rse'> girl </if> ";
//				expected = "hello";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "f";
//				source = "hello <if score-4='rse'> girl </if> ";
//				expected = "hello girl";
//				testBlabla(bbs, data, id, source, expected);
//		
//				id = "f";
//				source = "hello <if score-5='rse'> girl </if> ";
//				expected = "hello";
//				testBlabla(bbs, data, id, source, expected);
//		
//				// Multiple conditions
//		
//				id = "f";
//				source = "hello <if score-4='rse'> girl </if> ";
//				expected = "hello girl";
//				testBlabla(bbs, data, id, source, expected);
//		//		
//				id = "f";
//				source = "hello <if score-1='rse' score-3='rse2'> girl </if> ";
//				expected = "hello";
//				testBlabla(bbs, data, id, source, expected);
//		
//			}
//		
//			@Test
//			public void ifSpelTest() throws InvalidParameterException {
//		
//				final Product data = getTestData();
//		
//				final BlablaService bbs = new BlablaService(evaluationService);
//		
//				String id;
//				String source;
//				String expected;
//		
//				///////////////////////////////////////////////
//				// Testing <if true> and <if false>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				source = "hello <if true='attributes.get(\"TYPE_ECRAN\").rawValue==\"LCD\"'> girl </if> <if false='1==1'> guy2 </if> ";
//				expected = "hello girl";
//				testBlabla(bbs, data, id, source, expected);
//		
//			}
//		
//			
//			@Test
//			public void testTemplates() throws InvalidParameterException {
//		
//				final Product data = getTestData();
//		
//				final BlablaService bbs = new BlablaService(evaluationService);
//		
//				String id;
//				String source;
//				String expected;
//		
//				///////////////////////////////////////////////
//				// Testing <if true> and <if false>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				data.setId(id);
//				source = "hello ! <if true='1==1'> un || truc |bidule | machin || nommé [[${data.getId()}]]  </if>";
//				expected = "hello ! un truc nommé f";
//				testBlabla(bbs, data, id, source, expected);
//		
//			}
//			
//			
//			@Test
//			public void testVars() throws InvalidParameterException {
//		
//				final Product data = getTestData();
//		
//				final BlablaService bbs = new BlablaService(evaluationService);
//		
//				String id;
//				String source;
//				String expected;
//		
//				///////////////////////////////////////////////
//				// Testing <if true> and <if false>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				data.setId(id);
//				source = "hello        <if true='1==1'> ${BRAND}</if> !";
//				expected = "hello THOMSON !";
//				testBlabla(bbs, data, id, source, expected);
//		
//				
//				///////////////////////////////////////////////
//				// Testing <if true> and <if false>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				data.setId(id);
//				source = "hello        <if true='1==1'> ${MODEL}</if> !";
//				expected = "hello UV5665EN !";
//				testBlabla(bbs, data, id, source, expected);
//				
//			}
//		
//			
//			
//			
//			@Test
//			public void testThymeleaf() throws InvalidParameterException {
//		
//				final Product data = getTestData();
//		
//				final BlablaService bbs = new BlablaService(evaluationService);
//		
//				String id;
//				String source;
//				String expected;
//		
//				///////////////////////////////////////////////
//				// Testing <if true> and <if false>
//				///////////////////////////////////////////////
//		
//				id = "f";
//				data.setId(id);
//				source = "hello ! <if true='1==1'> un || truc |bidule | machin || nommé [[${data.getId()}]]  </if>";
//				expected = "hello ! un truc nommé f";
//				testBlabla(bbs, data, id, source, expected);
//		
//			}
//		
//			private Product getTestData() {
//				final Product data = new Product();
//				final AggregatedAttribute a1 = new AggregatedAttribute("TYPE_ECRAN", "LCD");
//				final Attribute a2 = new Attribute("BRAND", "THOMSON");
//				final Attribute a3 = new Attribute("MODEL", "UV5665EN");
//		
//				final Rating r2 = new Rating();
//		//		r2.setAverageValue(3.0);
//				r2.setMax(5.0);
//				r2.setValue(3.8);
//				r2.setName("rse2");
//		
//				Cardinality c = new Cardinality("rse2", RatingType.RSE);
//				c.setMax(5.0);
//				c.setAverage(3.0);
//		
//		//		r2.setCardinality(c);
//		
//				data.getAggregatedRatings().put(RatingType.RSE, r2);
//		
//				data.getRatings().add(r2);
//		
//				final Rating r = new Rating();
//		//		r.setAverageValue(3.0);
//				r.setMax(5.0);
//				r.setValue(3.9);
//				r.setName("rse");
//		
//				c = new Cardinality("rse", RatingType.RSE);
//				c.setMax(5.0);
//				c.setAverage(3.0);
//		
//		//		r.setCardinality(c);
//		
//				data.getAggregatedRatings().put(RatingType.RSE, r);
//				data.getRatings().add(r);
//		
//				
//			
//				
//				data.getAttributes().getAttributes().pu
//				data.addAttribute(a1, new DataFragment(), AttributeType.TEXT);
//				data.addAttribute(a2, new DataFragment(), AttributeType.TEXT);
//				data.addAttribute(a3, new DataFragment(), AttributeType.TEXT);
//				
//				
//				return data;
//			}
//		
//			private void testBlabla(final BlablaService bbs, final Product data, String id, String source,
//					String expected) throws InvalidParameterException {
//				String current;
//				if (null != data) {
//					data.setId(id);
//				}
//				
//				current = bbs.generateBlabla(source, data);
//				LOGGER.info(" {} -----> {}", current, source);
//				assertEquals(expected, current);
//			}
//		
//		}
//	
//
