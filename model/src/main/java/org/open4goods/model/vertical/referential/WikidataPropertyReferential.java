package org.open4goods.model.vertical.referential;

/**
 * A Wikidata property reference associated with a single attribute.
 * <p>
 * Wikidata properties carry a stable {@code Pxxxx} identifier (e.g.
 * {@code P2048} for {@code height}). They provide the canonical semantic
 * anchor that links Nudger attributes to external knowledge graphs and to
 * other taxonomies (ETIM, Icecat) via Wikidata bridges.
 */
public class WikidataPropertyReferential
{
    /** Wikidata property identifier (e.g. "P2048"). */
    private String pid;

    /** English property label for documentation purposes (e.g. "height"). */
    private String name;

    public WikidataPropertyReferential()
    {
    }

    public WikidataPropertyReferential(String pid, String name)
    {
        this.pid = pid;
        this.name = name;
    }

    public String getPid()
    {
        return pid;
    }

    public void setPid(String pid)
    {
        this.pid = pid;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
