package org.open4goods.commons.config.yml.test;

import org.open4goods.commons.model.data.DataFragment;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
public class TestCsvLine extends TestUrl {

	@NotBlank
	private String csvLine;


	@Override
	public TestResultReport test(final DataFragment data, final String datasourceConfName) {

		final TestResultReport ret = super.test(data, datasourceConfName);

		// Also testing the expected URL
		if (null != url) {
			if (!data.getUrl().equals(url) ) {
				ret.addMessage("Was expecting url " + url+ ", we have : " + data.getUrl());
			}
		}
		return ret;
	}


	public String getCsvLine() {
		return csvLine;
	}


	public void setCsvLine(final String csvLine) {
		this.csvLine = csvLine;
	}


}
