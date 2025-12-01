package org.open4goods.model.datafragment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.Attribute;

class DataFragmentTest {

    @Test
    void addAttributeStoresSource() {
        DataFragment fragment = new DataFragment();

        fragment.addAttribute("Color", "Red", "en", "123", "icecat.biz");

        Attribute attribute = fragment.getAttributes().iterator().next();
        assertThat(attribute.getSource()).isEqualTo("icecat.biz");
    }
}
