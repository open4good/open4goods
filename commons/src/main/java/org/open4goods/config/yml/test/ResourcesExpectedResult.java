package org.open4goods.config.yml.test;

import java.util.Set;

import org.open4goods.model.data.Resource;

public class ResourcesExpectedResult extends NumericExpectedResult {

	private Boolean hasPdf;

	public void test(final Set<Resource> resources, final TestResultReport ret) {
		testCollection(resources, "resources", ret);


		if (hasPdf != null && hasPdf.booleanValue()) {
			boolean found = false;
			for (final Resource r : resources) {
				if (r.getUrl().contains(".pdf")) {
					found = true;
					break;
				}
			}

			if (!found) {
				ret.addMessage("was expecting a PDF resource");
			}
		}
	}

	public Boolean getHasPdf() {
		return hasPdf;
	}

	public void setHasPdf(final Boolean hasPdf) {
		this.hasPdf = hasPdf;
	}

}
