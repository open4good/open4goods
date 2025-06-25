package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.nudgerfrontapi.dto.RequestMetadata;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class AbstractDTO {



    @Schema(description = "Timing metadata", nullable = true)
    private RequestMetadata metadatas;

	public RequestMetadata getMetadatas() {
		return metadatas;
	}

	public void setMetadatas(RequestMetadata metadatas) {
		this.metadatas = metadatas;
	}




}
