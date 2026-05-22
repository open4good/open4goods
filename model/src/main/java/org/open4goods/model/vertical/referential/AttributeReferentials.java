package org.open4goods.model.vertical.referential;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified cross-referential block applied to a single attribute definition.
 * <p>
 * Mirrors the vertical-level {@link TaxonomyReferentials} block but at attribute
 * granularity. Each entry list links the Nudger attribute to one or more external
 * features (Icecat feature IDs, EPREL feature names, ETIM feature IDs, Wikidata
 * properties).
 * <p>
 * All fields default to empty lists; the block is optional and backward-compatible
 * with attribute YAMLs that still rely on the legacy {@code icecatFeaturesIds} /
 * {@code eprelFeatureNames} scalar fields on {@code AttributeConfig}.
 */
public class AttributeReferentials
{
    /** Icecat feature references ({@code featureId} + human-readable {@code name}). */
    private List<IcecatFeatureReferential> icecat = new ArrayList<>();

    /** EPREL feature references ({@code featureName} scoped by {@code productGroup}). */
    private List<EprelFeatureReferential> eprel = new ArrayList<>();

    /** ETIM feature references ({@code featureId} + human-readable {@code name}). */
    private List<EtimFeatureReferential> etim = new ArrayList<>();

    /** Wikidata property references ({@code pid} like {@code P2048}). */
    private List<WikidataPropertyReferential> wikidata = new ArrayList<>();

    public AttributeReferentials()
    {
    }

    public List<IcecatFeatureReferential> getIcecat()
    {
        return icecat;
    }

    public void setIcecat(List<IcecatFeatureReferential> icecat)
    {
        this.icecat = icecat == null ? new ArrayList<>() : icecat;
    }

    public List<EprelFeatureReferential> getEprel()
    {
        return eprel;
    }

    public void setEprel(List<EprelFeatureReferential> eprel)
    {
        this.eprel = eprel == null ? new ArrayList<>() : eprel;
    }

    public List<EtimFeatureReferential> getEtim()
    {
        return etim;
    }

    public void setEtim(List<EtimFeatureReferential> etim)
    {
        this.etim = etim == null ? new ArrayList<>() : etim;
    }

    public List<WikidataPropertyReferential> getWikidata()
    {
        return wikidata;
    }

    public void setWikidata(List<WikidataPropertyReferential> wikidata)
    {
        this.wikidata = wikidata == null ? new ArrayList<>() : wikidata;
    }

    /**
     * @return {@code true} when every referential list is empty.
     */
    public boolean isEmpty()
    {
        return icecat.isEmpty() && eprel.isEmpty() && etim.isEmpty() && wikidata.isEmpty();
    }
}
