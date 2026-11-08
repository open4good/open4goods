package org.open4goods.services.contribution.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * One outbound affiliation click ({@code /contrib/<token>}), written by front-api and aggregated by the api Metriks
 * endpoint ({@code /api/metriks/affiliation}).
 * <p>
 * The document carries <b>no personal data</b>: no IP, no user agent, and not the full target URL (only its host).
 * Every field is an indexed {@code keyword}, except {@code ts} ({@code date}, epoch milliseconds), so that clicks can
 * be counted per network and per merchant with Elasticsearch aggregations.
 */
@Document(indexName = AffiliationClick.INDEX_NAME, createIndex = true)
public class AffiliationClick {

    /** Name of the Elasticsearch index. */
    public static final String INDEX_NAME = "affiliation-clicks";

    /** Field names, shared with the readers of the index. */
    public static final String FIELD_TS = "ts";
    public static final String FIELD_NETWORK = "network";
    public static final String FIELD_MERCHANT = "merchant";
    public static final String FIELD_ORIGIN_PATH = "originPath";
    public static final String FIELD_CONTENT_PATH = "contentPath";

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    /** Click time, epoch milliseconds. */
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private long ts;

    /** {@link AffiliationNetwork#key()} of the target host. */
    @Field(type = FieldType.Keyword)
    private String network;

    /** Datasource name of the offer (the merchant). */
    @Field(type = FieldType.Keyword)
    private String merchant;

    /** GTIN of the product the offer belongs to, {@code null} when the token does not carry it. */
    @Field(type = FieldType.Keyword)
    private String gtin;

    /** Path of the page the click came from, {@code null} when unknown. */
    @Field(type = FieldType.Keyword)
    private String originPath;

    /**
     * Path of the article or guide ({@code /blog/...}, {@code /guides/...}) the offer was reached from, sent by the
     * frontend in {@code X-Affiliation-Content}; {@code null} when absent or invalid. Added by GOU-1828: older
     * documents do not have it.
     */
    @Field(type = FieldType.Keyword)
    private String contentPath;

    /** Host of the target URL, {@code null} when the URL has no readable host. */
    @Field(type = FieldType.Keyword)
    private String targetHost;

    public AffiliationClick() {
    }

    /**
     * Builds a click towards {@code targetUrl}: the network and the target host are deduced from the URL, which is
     * itself not stored.
     *
     * @param merchant   datasource name of the offer
     * @param targetUrl  affiliation target URL (only its host is kept)
     * @param gtin       product GTIN, or {@code null}
     * @param originPath path of the originating page, or {@code null}
     * @param ts         click time, epoch milliseconds
     * @return a new click with a random id, without {@code contentPath}
     */
    public static AffiliationClick of(String merchant, String targetUrl, String gtin, String originPath, long ts) {
        return of(merchant, targetUrl, gtin, originPath, null, ts);
    }

    /**
     * Builds a click towards {@code targetUrl}: the network and the target host are deduced from the URL, which is
     * itself not stored.
     *
     * @param merchant    datasource name of the offer
     * @param targetUrl   affiliation target URL (only its host is kept)
     * @param gtin        product GTIN, or {@code null}
     * @param originPath  path of the originating page, or {@code null}
     * @param contentPath path of the article or guide the offer was reached from, or {@code null}
     * @param ts          click time, epoch milliseconds
     * @return a new click with a random id
     */
    public static AffiliationClick of(String merchant, String targetUrl, String gtin, String originPath,
            String contentPath, long ts) {
        AffiliationClick click = new AffiliationClick();
        click.id = UUID.randomUUID().toString();
        click.ts = ts;
        click.targetHost = AffiliationNetwork.hostOf(targetUrl);
        click.network = AffiliationNetwork.fromUrl(targetUrl).key();
        click.merchant = merchant;
        click.gtin = gtin;
        click.originPath = originPath;
        click.contentPath = contentPath;
        return click;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }

    public String getOriginPath() {
        return originPath;
    }

    public void setOriginPath(String originPath) {
        this.originPath = originPath;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }
}
