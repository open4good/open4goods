package org.open4goods.model.vertical.referential;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified cross-referential taxonomy block for a vertical configuration.
 * <p>
 * Each entry list may contain multiple mappings, allowing a single Nudger vertical
 * to span several categories within the same external taxonomy (e.g. portable
 * vs. split air conditioners in ETIM). The lists are intentionally kept separate
 * per taxonomy so consuming code can iterate only the relevant standard.
 * <p>
 * Use a {@code referentials:} top-level block in the vertical YAML to populate
 * this object. All fields default to empty lists, making the block optional and
 * backward-compatible.
 */
public class TaxonomyReferentials
{
    /** Wikidata entity references ({@code qid} + human-readable {@code name}). */
    private List<WikidataReferential> wikidata = new ArrayList<>();

    /** Google Product Taxonomy references ({@code id} + {@code name} path). */
    private List<GoogleTaxonomyReferential> googleTaxonomy = new ArrayList<>();

    /** Icecat category references ({@code categoryId} + {@code name}). */
    private List<IcecatReferential> icecat = new ArrayList<>();

    /**
     * ETIM (European Technical Information Model) class references.
     * Multiple entries are normal when a vertical covers several ETIM product classes.
     */
    private List<EtimReferential> etim = new ArrayList<>();

    /** EPREL product group references ({@code group} string). */
    private List<EprelReferential> eprel = new ArrayList<>();

    /** GS1 GPC brick references — reserved for future integration. */
    private List<Gs1GpcReferential> gs1Gpc = new ArrayList<>();

    /** eCl@ss class references — reserved for future integration. */
    private List<EclassReferential> eclass = new ArrayList<>();

    public TaxonomyReferentials()
    {
    }

    public List<WikidataReferential> getWikidata()
    {
        return wikidata;
    }

    public void setWikidata(List<WikidataReferential> wikidata)
    {
        this.wikidata = wikidata == null ? new ArrayList<>() : wikidata;
    }

    public List<GoogleTaxonomyReferential> getGoogleTaxonomy()
    {
        return googleTaxonomy;
    }

    public void setGoogleTaxonomy(List<GoogleTaxonomyReferential> googleTaxonomy)
    {
        this.googleTaxonomy = googleTaxonomy == null ? new ArrayList<>() : googleTaxonomy;
    }

    public List<IcecatReferential> getIcecat()
    {
        return icecat;
    }

    public void setIcecat(List<IcecatReferential> icecat)
    {
        this.icecat = icecat == null ? new ArrayList<>() : icecat;
    }

    public List<EtimReferential> getEtim()
    {
        return etim;
    }

    public void setEtim(List<EtimReferential> etim)
    {
        this.etim = etim == null ? new ArrayList<>() : etim;
    }

    public List<EprelReferential> getEprel()
    {
        return eprel;
    }

    public void setEprel(List<EprelReferential> eprel)
    {
        this.eprel = eprel == null ? new ArrayList<>() : eprel;
    }

    public List<Gs1GpcReferential> getGs1Gpc()
    {
        return gs1Gpc;
    }

    public void setGs1Gpc(List<Gs1GpcReferential> gs1Gpc)
    {
        this.gs1Gpc = gs1Gpc == null ? new ArrayList<>() : gs1Gpc;
    }

    public List<EclassReferential> getEclass()
    {
        return eclass;
    }

    public void setEclass(List<EclassReferential> eclass)
    {
        this.eclass = eclass == null ? new ArrayList<>() : eclass;
    }
}
