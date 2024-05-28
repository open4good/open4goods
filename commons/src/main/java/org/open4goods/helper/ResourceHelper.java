package org.open4goods.helper;

public class ResourceHelper {

	

	/**
	 * Check if an url is an image
	 * 
	 *  TODO : extensions from conf
	 *  TODO : move to service
	 * @param value
	 * @return
	 */
	 public static boolean isImage(String value) {
		  String val = value.toLowerCase().trim();
		  return val.endsWith(".jpg") || val.endsWith(".png") || val.endsWith(".jpeg")  || val.endsWith(".wlp") || val.endsWith(".gif") || val.endsWith(".bmp") || val.endsWith(".tiff") || val.endsWith(".ico");
		 }
}
