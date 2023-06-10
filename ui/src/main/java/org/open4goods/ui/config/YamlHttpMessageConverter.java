package org.open4goods.ui.config;
///////////////////////////////
	// The converter for yaml messages
	///////////////////////////////

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.yaml.snakeyaml.Yaml;

class YamlHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

		public YamlHttpMessageConverter() {
			super(new MediaType("text", "yaml"));
		}

		@Override
		protected boolean supports(final Class<?> clazz) {
			return true;
		}

		@Override
		protected T readInternal(final Class<? extends T> clazz, final HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			final Yaml yaml = new Yaml();
			final T t = yaml.loadAs(inputMessage.getBody(), clazz);
			return t;
		}

		@Override
		protected void writeInternal(final T t, final HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {
			final Yaml yaml = new Yaml();

			final OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody());
			yaml.dump(t, writer);
			writer.close();
		}
	}