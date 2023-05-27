package org.open4goods.model.crawlers;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
public class ApiSynchConfig {

	@NotBlank
	private String nodeName = UUID.randomUUID().toString();

	/** The node url, that will be used to trigger indexation request **/
	@NotBlank
	private String nodeUrl = "http://localhost:8080";

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(final String nodeName) {
		this.nodeName = nodeName;
	}

	public String getNodeUrl() {
		return nodeUrl;
	}

	public void setNodeUrl(final String nodeUrl) {
		this.nodeUrl = nodeUrl;
	}

}
