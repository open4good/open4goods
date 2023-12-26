package org.open4goods.aggregation;
//package org.open4goods.api.services.aggregation;
//
//import static org.junit.Assert.fail;
//
//import java.io.File;
//import java.io.IOException;
//
//import org.apache.commons.io.FileUtils;
//import org.junit.Test;
//
//import org.open4goods.api.services.aggregation.services.ImageClassificationService;
//
//public class ImageClassificationTest {
//
//
//	@Test
//	public void testClassification() {
//
//		try {
//			ImageClassificationService classifier = new ImageClassificationService(FileUtils.openInputStream(new File("/home/goulven/Bureau/models_training/graph.pb")),
//					FileUtils.openInputStream(new File("/home/goulven/Bureau/models_training/graph_labels.txt")));
//
//
//			for (File f : new File("/home/goulven/Bureau/training_tvs/").listFiles() ) {
//				System.out.println( f.getAbsolutePath() + " : " +classifier.predict(f.getAbsolutePath()));
//			}
//
//
//
//		} catch (IOException e) {
//
//			e.printStackTrace();
//			fail (e.getMessage());
//		}
//
//	}
//
//}
