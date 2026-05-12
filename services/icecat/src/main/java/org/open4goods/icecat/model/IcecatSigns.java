package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Wrapper for a list of localized unit-of-measure signs (the {@code <Signs>} element). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatSigns {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Sign")
    private List<IcecatSign> signs;

    /** Returns the sign for the given Icecat language ID, or {@code null} if not found. */
    public String getSignForLang(int langId) {
        if (signs == null) {
            return null;
        }
        return signs.stream()
                .filter(s -> s.getLangId() == langId)
                .map(IcecatSign::getEffectiveSign)
                .findFirst()
                .orElse(null);
    }

    public List<IcecatSign> getSigns() {
        return signs != null ? signs : Collections.emptyList();
    }

    public void setSigns(List<IcecatSign> signs) {
        this.signs = signs;
    }
}
