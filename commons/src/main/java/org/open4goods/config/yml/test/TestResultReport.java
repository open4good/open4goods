package org.open4goods.config.yml.test;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.data.DataFragment;

public class TestResultReport {


	private String url;

	private List<String> messages = new ArrayList<>();

	private DataFragment data;

	private String datasourceConfigName;



	public String getDatasourceConfigName() {
		return datasourceConfigName;
	}


	public void setDatasourceConfigName(final String datasourceConfigName) {
		this.datasourceConfigName = datasourceConfigName;
	}


	public TestResultReport(final DataFragment data, final String datasourceConfigName) {
		this.data= data;
		this.datasourceConfigName = datasourceConfigName;
	}


	public void addMessage(final String string) {
		messages.add(string);

	}


	public List<String> getMessages() {
		return messages;
	}

	public void setMessages(final List<String> messages) {
		this.messages = messages;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(final String url) {
		this.url = url;
	}


	public DataFragment getData() {
		return data;
	}


	public void setData(final DataFragment data) {
		this.data = data;
	}



}
