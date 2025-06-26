package org.open4goods.nudgerfrontapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class AbstractDTO {



    @Schema(description = "Request metadata", nullable = true)
    private RequestMetadata metadatas;

	public RequestMetadata getMetadatas() {
		return metadatas;
	}

	public void setMetadatas(RequestMetadata metadatas) {
		this.metadatas = metadatas;
	}




}
