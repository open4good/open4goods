package org.open4goods.model.vertical.referential;

/**
 * A Wikidata entity reference associated with a vertical.
 * <p>
 * Each entry links the vertical's concept to a Wikidata item via its Q-identifier.
 * Multiple entries are allowed when a vertical spans several Wikidata concepts.
 */
public class WikidataReferential
{
    /** Wikidata Q-identifier (e.g. "Q174488"). */
    private String qid;

    /** Human-readable English label for documentation purposes. */
    private String name;

    public WikidataReferential()
    {
    }

    public WikidataReferential(String qid, String name)
    {
        this.qid = qid;
        this.name = name;
    }

    public String getQid()
    {
        return qid;
    }

    public void setQid(String qid)
    {
        this.qid = qid;
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
