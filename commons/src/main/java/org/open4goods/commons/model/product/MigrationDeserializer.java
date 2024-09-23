package org.open4goods.commons.model.product;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.groovy.parser.antlr4.util.StringUtils;
import org.open4goods.commons.model.data.Resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO(p1,design) : Remove once migration done
 */
public class MigrationDeserializer extends JsonDeserializer<Map<String, Resource>> {

    @Override
    public Map<String, Resource> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        // Use the ObjectMapper to parse the JSON
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode rootNode = mapper.readTree(p);

        // Create the Map we want to deserialize into
        Map<String, Resource> resourceMap = new HashMap<>();

        // Iterate over the elements of the JSON Set
        Iterator<JsonNode> elements = rootNode.elements();
        while (elements.hasNext()) {
            JsonNode elementNode = elements.next();
            // Convert the elementNode to a Resource object
            Resource resource = mapper.treeToValue(elementNode, Resource.class);

            // Extract the key from the Resource (using getKeyField() in this case)
            String key = resource.getUrl();

            if (!StringUtils.isEmpty(key)) {
	            // Put the resource into the map with the extracted key
	            resourceMap.put(key, resource);
            }
        }

        return resourceMap;
    }
}
