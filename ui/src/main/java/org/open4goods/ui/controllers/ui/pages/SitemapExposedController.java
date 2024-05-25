package org.open4goods.ui.controllers.ui.pages;

import java.util.Arrays;
import java.util.List;


// TODO : Doc
public interface SitemapExposedController {

	SitemapEntry getExposedUrls();
	
	default List<SitemapEntry> getMultipleExposedUrls() {
		return Arrays.asList(getExposedUrls());
	}
}
