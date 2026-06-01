package org.open4goods.model.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.model.exceptions.ValidationException;

class ResourceTest {

	@Test
	void nameFromUrlUsesPathBasenameWithoutQueryString() throws ValidationException {
		Resource resource = new Resource("https://objects.icecat.biz/objects/120914055_50621854.mp4");
		resource.setCacheKey("fallback");

		assertThat(resource.nameFromUrl()).isEqualTo("120914055_50621854.mp4");
	}

	@Test
	void nameFromUrlUsesPathBasenameBeforeQueryString() throws ValidationException {
		Resource resource = new Resource("https://example.org/documents/manual.pdf?version=1");
		resource.setCacheKey("fallback");

		assertThat(resource.nameFromUrl()).isEqualTo("manual.pdf");
	}
}
