
package org.open4goods.config.yml.attributes;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author goulven
 *
 */
public class NamesConfig {

	private String key;


	private Localisable value ;


	public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}


	public Localisable getValue() {
		return value;
	}


	public void setValue(Localisable prompts) {
		this.value = prompts;
	}


}
