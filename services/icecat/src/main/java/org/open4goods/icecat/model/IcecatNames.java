package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Wrapper for a list of {@link IcecatName} entries (the {@code <Names>} element). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatNames {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Name")
    private List<IcecatName> names;

    /** Returns the name for the given Icecat language ID, or {@code null} if not found. */
    public String getNameForLang(int langId) {
        if (names == null) {
            return null;
        }
        return names.stream()
                .filter(n -> n.getLangId() == langId)
                .map(IcecatName::getEffectiveName)
                .findFirst()
                .orElse(null);
    }

    public List<IcecatName> getNames() {
        return names != null ? names : Collections.emptyList();
    }

    public void setNames(List<IcecatName> names) {
        this.names = names;
    }
}
