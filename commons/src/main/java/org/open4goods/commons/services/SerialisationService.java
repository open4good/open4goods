package org.open4goods.commons.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.model.data.DataFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Service
public class SerialisationService {

	private static final Logger logger = LoggerFactory.getLogger(SerialisationService.class);

	private final ObjectMapper jsonMapper = new ObjectMapper()
			//			.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

			.setSerializationInclusion(Include.NON_EMPTY)
			.setSerializationInclusion(Include.NON_NULL)

			;

	private final ObjectWriter jsonMapperWithPretttyPrint = new ObjectMapper().writerWithDefaultPrettyPrinter();

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

			.setSerializationInclusion(Include.NON_EMPTY)
			.setSerializationInclusion(Include.NON_NULL);

	public SerialisationService() {
		super();
	}

	/**
	 * JSON object serialisation
	 * @return
	 */
	public String toJson(final Object o) {
		try {
			return jsonMapper.writeValueAsString(o);
		} catch (final JsonProcessingException e) {
			logger.error("Error while serialising {} to JSON", o, e);
			return null;
		}
	}

	/**
	 * YAML object serialisation
	 * @param o
	 * @return
	 */
	public String toYaml(final Object o) {
		try {
			return yamlMapper.writeValueAsString(o);
		} catch (final JsonProcessingException e) {
			logger.error("Error while serialising {} to JSON", o, e);
			return null;
		}
	}

	/**
	 * JSON object serialisation, with prettyprint options
	 * @param o
	 * @param prettyPrint
	 * @return
	 */
	public String toJson(final Object o, final boolean prettyPrint) {
		if (prettyPrint) {
			try {
				return jsonMapperWithPretttyPrint.writeValueAsString(o);
			} catch (final JsonProcessingException e) {
				logger.error("Error while serialising {} to JSON (with prettyPrint)", o, e);
				return null;
			}
		} else {
			return toJson(o);
		}
	}

	/**
	 * JSON object de-serialisation
	 * @param input
	 * @param valueType
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public <T> T fromJson(final String input, final Class<T> valueType)
			throws JsonParseException, JsonMappingException, IOException {
		return jsonMapper.readValue(input, valueType);
	}
	public Map<String, String> fromJson(String value, TypeReference<HashMap<String, String>> typeRef) 		throws JsonParseException, JsonMappingException, IOException {
		return jsonMapper.readValue(value, typeRef);
	}




	/**
	 * YAML object de-serialisation
	 * @param input
	 * @param valueType
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public <T> T fromYaml(final String input, final Class<T> valueType)
			throws JsonParseException, JsonMappingException, IOException {
		return yamlMapper.readValue(input, valueType);
	}

	public <T> T fromYaml(final String input, final CollectionType valueType)
			throws JsonParseException, JsonMappingException, IOException {
		return yamlMapper.readValue(input, valueType);
	}

	public <T> T fromYaml(final InputStream input, final Class<T> valueType)
			throws IOException {
		return yamlMapper.readValue(input, valueType);
	}











	/**
	 * Binary object de-serialisation
	 * @param bytes
	 * @param c
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public <T> T fromBytes(final byte[] bytes, final Class<T> c) throws JsonParseException, JsonMappingException, IOException {
		return jsonMapper.readValue(bytes,c);
	}

	/**
	 * Binary object serialisation
	 * @param o
	 * @return
	 */
	public byte[] toBytes(final Object o) {
		return toJson(o).getBytes();
	}



	//	/**
	//	 * Binary object de-serialisation
	//	 * @param bytes
	//	 * @param c
	//	 * @return
	//	 * @throws IOException
	//	 * @throws JsonMappingException
	//	 * @throws JsonParseException
	//	 */
	//	public <T> T fromBinaryB64(String base64, Class<T> c) throws JsonParseException, JsonMappingException, IOException {
	//		byte[] decoded = Base64.getDecoder().decode(base64);
	//		return fromBytes(decoded, c);
	//	}
	//
	//	/**
	//	 * Binary object serialisation
	//	 * @param o
	//	 * @return
	//	 */
	//	public String toBinaryB64(Object o) {
	//		byte[] bytes = toBytes(o);
	//		byte[] encoded = Base64.getEncoder().encode(bytes);
	//		return new String(encoded);
	//	}

	/**
	 *
	 * @return the Jackson json obect-mapper
	 */
	public ObjectMapper getJsonMapper() {
		return jsonMapper;
	}


	/**
	 *
	 * @return the Jackson yaml obect-mapper
	 */
	public ObjectMapper getYamlMapper() {
		return yamlMapper;
	}



	/**
	 * At server side, use ZipOutputStream to zip text to byte array, then convert
	 * byte array to base64 string, so it can be trasnfered via http request.
	 */
	//TODO(gof) : perf : zip that content
	public  String compressString(final String srcTxt)
			throws IOException {
		//	    ByteArrayOutputStream rstBao = new ByteArrayOutputStream();
		//	    GZIPOutputStream zos = new GZIPOutputStream(rstBao);
		//	    zos.write(srcTxt.getBytes());
		//	    IOUtils.closeQuietly(zos);
		//
		//	    byte[] bytes = rstBao.toByteArray();
		// In my solr project, I use org.apache.solr.co mmon.util.Base64.
		// return = org.apache.solr.common.util.Base64.byteArrayToBase64(bytes, 0,
		// bytes.length);
		return org.apache.commons.codec.binary.Base64.encodeBase64String(srcTxt.getBytes());
	}


	/**
	 * When client receives the zipped base64 string, it first decode base64
	 * String to byte array, then use ZipInputStream to revert the byte array to a
	 * string.
	 */
	//TODO(gof) : perf : zip that content
	public static String uncompressString(final String zippedBase64Str)
			throws IOException {
		//	    String result = null;
		//
		//	    // In my solr project, I use org.apache.solr.common.util.Base64.
		//	    // byte[] bytes =
		//	    // org.apache.solr.common.util.Base64.base64ToByteArray(zippedBase64Str);
		//	    byte[] bytes = org.apache.commons.codec.binary.Base64.decodeBase64(zippedBase64Str);
		//	    GZIPInputStream zi = null;
		//	    try {
		//	      zi = new GZIPInputStream(new ByteArrayInputStream(bytes));
		//	      result = IOUtils.toString(zi);
		//	    } finally {
		//	      IOUtils.closeQuietly(zi);
		//	    }
		//	    return result;

		return new String( org.apache.commons.codec.binary.Base64.decodeBase64(zippedBase64Str));
	}

	

}
