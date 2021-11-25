package org.open4goods.config.yml.test;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;

public class AttributesExpectedResult extends NumericExpectedResult {

	public Map<ReferentielKey, String> referentiel;

	public Map<String, String> classical;

	public Map<String, Set<String>> multivalued;


	public void test(final Map<String, String> referentielAttributes, final DataFragment pd, final TestResultReport ret) {

		// Testing for referentiel
		if (null != referentiel) {

			for (final Entry<ReferentielKey, String> entry : referentiel.entrySet()) {

				if (!referentielAttributes.containsKey(entry.getKey().toString())) {
					ret.addMessage("Missing referentiel attribute : " + entry.getKey());
				} else if (!referentielAttributes.get(entry.getKey().toString()).equals(entry.getValue())) {
					ret.addMessage("Was expecting " + entry.getValue() + " for referentiel attribute  " + entry.getKey()
							+ ", we have " + referentielAttributes.get(entry.getKey().toString()));
				}
			}
		}

		// Testing for classical
		if (null != classical) {

			for (final Entry<String, String> entry : classical.entrySet()) {

				if (null == pd.getAttribute(entry.getKey())) {
					ret.addMessage("Missing attribute : " + entry.getKey());
				} else if (!entry.getValue().equals(pd.getAttribute(entry.getKey()).getRawValue().toString())) {
					ret.addMessage("Was expecting " + entry.getValue() + " for attribute  " + entry.getKey()
							+ ", we have " + pd.getAttribute(entry.getKey()).getRawValue().toString());
				}

			}
		}

		// Testing for classical
		if (null != multivalued) {

			for (final Entry<String, Set<String>> entry : multivalued.entrySet()) {

				if (null == pd.getAttribute(entry.getKey())) {
					ret.addMessage("Missing multivalued attribute : " + entry.getKey());
				} else{
					final Attribute attr = pd.getAttribute(entry.getKey());
					if (entry.getValue().size() != attr.valuesSize() || !entry.getValue().containsAll(attr.stringValues()) ) {
						ret.addMessage("Was expecting " + entry.getValue() + " for attribute  " + entry.getKey() + ", we have " + attr.stringValues());
					}
				}

			}
		}

	}

	public Map<ReferentielKey, String> getReferentiel() {
		return referentiel;
	}

	public void setReferentiel(final Map<ReferentielKey, String> referentiel) {
		this.referentiel = referentiel;
	}

	public Map<String, String> getClassical() {
		return classical;
	}

	public void setClassical(final Map<String, String> classical) {
		this.classical = classical;
	}





}
