package org.open4goods.api.services.metriks;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SeoTemplateTest {

    @Test
    void classifiesNudgerUrlsByTemplate() {
        assertThat(SeoTemplate.classify("https://nudger.fr/televiseurs/8806097084532-tv-samsung-tq77s90fae-2025"))
                .isEqualTo(SeoTemplate.FICHE);
        assertThat(SeoTemplate.classify("https://nudger.fr/8806097084532-tv-samsung")).isEqualTo(SeoTemplate.FICHE);
        assertThat(SeoTemplate.classify("https://nudger.fr/televiseurs")).isEqualTo(SeoTemplate.CATEGORIE);
        assertThat(SeoTemplate.classify("https://nudger.fr/televiseurs/")).isEqualTo(SeoTemplate.CATEGORIE);
        assertThat(SeoTemplate.classify("https://nudger.fr/televiseurs/televiseur-55-pouces"))
                .isEqualTo(SeoTemplate.SOUS_CATEGORIE);
        assertThat(SeoTemplate.classify("https://nudger.fr/blog/quelle-taille-de-tv-choisir-pour-son-salon?x=1"))
                .isEqualTo(SeoTemplate.BLOG);
        assertThat(SeoTemplate.classify("https://nudger.fr/guides/acheter-un-lave-linge")).isEqualTo(SeoTemplate.GUIDE);
        assertThat(SeoTemplate.classify("https://nudger.fr/opendata/gtin")).isEqualTo(SeoTemplate.OPENDATA);
        assertThat(SeoTemplate.classify("https://nudger.fr/files/Rapport.PDF")).isEqualTo(SeoTemplate.PDF);
        assertThat(SeoTemplate.classify("https://nudger.fr/")).isEqualTo(SeoTemplate.AUTRE);
        assertThat(SeoTemplate.classify(null)).isEqualTo(SeoTemplate.AUTRE);
    }
}
