package org.open4goods.services.opendata.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.Localisable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for the OpenData service.
 */
@Configuration
@ConfigurationProperties(prefix = "open-data-config")
public class OpenDataConfig {

    /**
     * Maximum download speed per client in kilobytes.
     */
    private int downloadSpeedKb = 256;

    /**
     * Maximum concurrent downloads allowed.
     */
    private int concurrentDownloads = 4;

    /**
     * Flag indicating whether scheduled generation is allowed.
     */
    private Boolean generationEnabled = false;

    /**
     * Public base URL (for example {@code https://data.nudger.fr}) used by front-api to build dataset download
     * links. Empty keeps the legacy {@code api.resource-root-path} based links.
     */
    private String publicBaseUrl = "";

    /**
     * Base URL the ui application redirects (HTTP 301) dataset downloads to. Empty keeps the throttled local
     * stream.
     */
    private String redirectBaseUrl = "";

    /**
     * Root folder where OpenData assets are stored.
     */
    private String rootFolder = File.separator + "opt" + File.separator + "open4goods" + File.separator;

    /**
     * Sub-folder storing OpenData exports.
     */
    private String openDataFolder = "opendata";

    private String openDataArchiveFilename = "full.zip";
    private String isbnFilename = "isbn.zip";
    private String tmpIsbnFilename = "isbn-tmp.zip";
    private String gtinFilename = "gtin.zip";
    private String tmpGtinFilename = "gtin-tmp.zip";

    /**
     * Sub-folder of {@link #openDataFolder} where a generation pass writes its files before the atomic rename. It is
     * on the same file system as the published files and is deleted after each pass, successful or not.
     */
    private String tmpFolder = "tmp";

    /**
     * Manifest listing every dataset, written last: it is the validation point of a pass (GOU-2237 plan).
     */
    private String manifestFilename = "manifest.json";
    private String gtinManifestFilename = "gtin-manifest.json";
    private String isbnManifestFilename = "isbn-manifest.json";

    /**
     * Public object names of the zips (R2 keys, {@code data.nudger.fr/{name}}), written in the manifests.
     */
    /**
     * Public site the product URLs of the impact datasets point to (canonical path appended).
     */
    private String siteBaseUrl = "https://nudger.fr";

    private String gtinPublicFilename = "gtin-open-data.zip";
    private String isbnPublicFilename = "isbn-open-data.zip";

    /**
     * Cron of the weekly generation, run by the api only (GOU-2248): Sunday 01:30, outside the batch window (from
     * 12:30 server time) and the release window. {@code -} disables the schedule, the manual trigger stays.
     */
    private String generationCron = "0 30 1 * * SUN";

    /**
     * Time zone of {@link #generationCron}.
     */
    private String generationZone = "Europe/Paris";

    /**
     * Products per Elasticsearch page. A projected product weighs about 0.7 KB (1.6 KB max on a production sample of
     * 200, 15/11/2026), so 2000 per page is about 1.5 MB, far below the 100 MB response buffer of the client
     * (GOU-1134, GOU-1203, GOU-2249). Never raise the buffer instead.
     */
    private int exportPageSize = 2000;

    /**
     * Pause after each page, to leave Elasticsearch to the site search. Zero disables it. Must stay well below the
     * scroll keep-alive.
     */
    private Duration pagePause = Duration.ofMillis(50);

    /**
     * Free space required before a pass, as a multiple of the size of the files currently published (GOU-823).
     */
    private double minFreeSpaceRatio = 1.5;

    /**
     * Minimum ratio of rows of a new dataset compared with the previous manifest and with the index count taken at the
     * start of the pass. Below it, the pass fails and the published files stay (as {@code min-rows-ratio} in
     * {@code IcecatIndexVersionManager}).
     */
    private double minRowsRatio = 0.9;

    /**
     * Publication of the zips and manifests on Cloudflare R2 after a successful pass (GOU-2289). Disabled by default.
     */
    private Publication publication = new Publication();

    public OpenDataConfig() {
		super();
	}

	/**
     * Base URLs used to build product links in the export.
     */
    private Localisable<String, String> baseUrls = new Localisable<>();

    public int getDownloadSpeedKb() {
        return downloadSpeedKb;
    }

    public void setDownloadSpeedKb(int downloadSpeedKb) {
        this.downloadSpeedKb = downloadSpeedKb;
    }

    public int getConcurrentDownloads() {
        return concurrentDownloads;
    }

    public void setConcurrentDownloads(int concurrentDownloads) {
        this.concurrentDownloads = concurrentDownloads;
    }



    public Boolean getGenerationEnabled() {
		return generationEnabled;
	}

	public void setGenerationEnabled(Boolean generationEnabled) {
		this.generationEnabled = generationEnabled;
	}

	public boolean isGenerationEnabled() {
        return Boolean.TRUE.equals(generationEnabled);
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getRedirectBaseUrl() {
        return redirectBaseUrl;
    }

    public void setRedirectBaseUrl(String redirectBaseUrl) {
        this.redirectBaseUrl = redirectBaseUrl;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public String getOpenDataFolder() {
        return openDataFolder;
    }

    public void setOpenDataFolder(String openDataFolder) {
        this.openDataFolder = openDataFolder;
    }

    public String getOpenDataArchiveFilename() {
        return openDataArchiveFilename;
    }

    public void setOpenDataArchiveFilename(String openDataArchiveFilename) {
        this.openDataArchiveFilename = openDataArchiveFilename;
    }

    public String getIsbnFilename() {
        return isbnFilename;
    }

    public void setIsbnFilename(String isbnFilename) {
        this.isbnFilename = isbnFilename;
    }

    public String getTmpIsbnFilename() {
        return tmpIsbnFilename;
    }

    public void setTmpIsbnFilename(String tmpIsbnFilename) {
        this.tmpIsbnFilename = tmpIsbnFilename;
    }

    public String getGtinFilename() {
        return gtinFilename;
    }

    public void setGtinFilename(String gtinFilename) {
        this.gtinFilename = gtinFilename;
    }

    public String getTmpGtinFilename() {
        return tmpGtinFilename;
    }

    public void setTmpGtinFilename(String tmpGtinFilename) {
        this.tmpGtinFilename = tmpGtinFilename;
    }

    public String getTmpFolder() {
        return tmpFolder;
    }

    public void setTmpFolder(String tmpFolder) {
        this.tmpFolder = tmpFolder;
    }

    public String getManifestFilename() {
        return manifestFilename;
    }

    public void setManifestFilename(String manifestFilename) {
        this.manifestFilename = manifestFilename;
    }

    public String getGtinManifestFilename() {
        return gtinManifestFilename;
    }

    public void setGtinManifestFilename(String gtinManifestFilename) {
        this.gtinManifestFilename = gtinManifestFilename;
    }

    public String getIsbnManifestFilename() {
        return isbnManifestFilename;
    }

    public void setIsbnManifestFilename(String isbnManifestFilename) {
        this.isbnManifestFilename = isbnManifestFilename;
    }

    public String getGtinPublicFilename() {
        return gtinPublicFilename;
    }

    public void setGtinPublicFilename(String gtinPublicFilename) {
        this.gtinPublicFilename = gtinPublicFilename;
    }

    public String getIsbnPublicFilename() {
        return isbnPublicFilename;
    }

    public void setIsbnPublicFilename(String isbnPublicFilename) {
        this.isbnPublicFilename = isbnPublicFilename;
    }

    public String getGenerationCron() {
        return generationCron;
    }

    public void setGenerationCron(String generationCron) {
        this.generationCron = generationCron;
    }

    public String getGenerationZone() {
        return generationZone;
    }

    public void setGenerationZone(String generationZone) {
        this.generationZone = generationZone;
    }

    public int getExportPageSize() {
        return exportPageSize;
    }

    public void setExportPageSize(int exportPageSize) {
        this.exportPageSize = exportPageSize;
    }

    public Duration getPagePause() {
        return pagePause;
    }

    public void setPagePause(Duration pagePause) {
        this.pagePause = pagePause;
    }

    public double getMinFreeSpaceRatio() {
        return minFreeSpaceRatio;
    }

    public void setMinFreeSpaceRatio(double minFreeSpaceRatio) {
        this.minFreeSpaceRatio = minFreeSpaceRatio;
    }

    public double getMinRowsRatio() {
        return minRowsRatio;
    }

    public void setMinRowsRatio(double minRowsRatio) {
        this.minRowsRatio = minRowsRatio;
    }

    public Localisable<String, String> getBaseUrls() {
        return baseUrls;
    }

    public void setBaseUrls(Localisable<String, String> baseUrls) {
        this.baseUrls = baseUrls;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public Path openDataDirectory() {
        return Paths.get(rootFolder).resolve(openDataFolder);
    }

    public File openDataFile() {
        return openDataDirectory().resolve(openDataArchiveFilename).toFile();
    }

    public File isbnZipFile() {
        return openDataDirectory().resolve(isbnFilename).toFile();
    }

    /**
     * @return the folder where a pass writes its files before the atomic rename
     */
    public Path tmpDirectory() {
        return openDataDirectory().resolve(tmpFolder);
    }

    public File tmpIsbnZipFile() {
        return tmpDirectory().resolve(tmpIsbnFilename).toFile();
    }

    public File gtinZipFile() {
        return openDataDirectory().resolve(gtinFilename).toFile();
    }

    public File tmpGtinZipFile() {
        return tmpDirectory().resolve(tmpGtinFilename).toFile();
    }

    public File manifestFile() {
        return openDataDirectory().resolve(manifestFilename).toFile();
    }

    /**
     * @param vertical vertical identifier
     * @return published zip of the impact dataset of this vertical
     */
    public File impactZipFile(String vertical) {
        return openDataDirectory().resolve(impactPublicFilename(vertical)).toFile();
    }

    /**
     * @param vertical vertical identifier
     * @return zip of the impact dataset of this vertical while a pass writes it
     */
    public File tmpImpactZipFile(String vertical) {
        return tmpDirectory().resolve(impactPublicFilename(vertical)).toFile();
    }

    /**
     * @param vertical vertical identifier
     * @return published manifest of the impact dataset of this vertical
     */
    public File impactManifestFile(String vertical) {
        return openDataDirectory().resolve("impact-" + vertical + "-manifest.json").toFile();
    }

    /**
     * @param vertical vertical identifier
     * @return public object name of the impact zip of this vertical ({@code impact-{vertical}.zip})
     */
    public String impactPublicFilename(String vertical) {
        return "impact-" + vertical + ".zip";
    }

    public String getSiteBaseUrl() {
        return siteBaseUrl;
    }

    public void setSiteBaseUrl(String siteBaseUrl) {
        this.siteBaseUrl = siteBaseUrl;
    }

    public File gtinManifestFile() {
        return openDataDirectory().resolve(gtinManifestFilename).toFile();
    }

    public File isbnManifestFile() {
        return openDataDirectory().resolve(isbnManifestFilename).toFile();
    }

    /**
     * Resolve the base URL for a given locale, falling back to the default entry when available.
     *
     * @param locale the requested locale
     * @return the base URL or {@code null} when none configured
     */
    public String getBaseUrl(Locale locale) {
        if (baseUrls == null || baseUrls.isEmpty()) {
            return null;
        }
        String languageKey = locale.getLanguage();
        String url = baseUrls.get(languageKey);
        if (StringUtils.isBlank(url)) {
            url = baseUrls.get("default");
        }
        return url;
    }

    /**
     * Publication on the R2 bucket through its S3 API (GOU-2289, {@code open-data-config.publication}). The account id
     * and the key pair are rendered by {@code scripts/deploy/render-service-secrets.sh} from the {@code prod} GitHub
     * Environment ({@code R2_ACCOUNT_ID}, {@code R2_ACCESS_KEY_ID}, {@code R2_SECRET_ACCESS_KEY}); never in a YAML
     * file.
     */
    public static class Publication {

        /**
         * Publishes after each successful generation pass. False by default and in beta; enabled in prod by a
         * dedicated change.
         */
        private boolean enabled = false;

        /**
         * Cloudflare account id: the endpoint is {@code https://{account-id}.r2.cloudflarestorage.com}.
         */
        private String accountId = "";

        /**
         * Access key id of the R2 token, limited to the bucket.
         */
        private String accessKeyId = "";

        /**
         * Secret access key of the R2 token.
         */
        private String secretAccessKey = "";

        /**
         * S3 endpoint, overriding the one built from {@link #accountId} (tests, another bucket provider).
         */
        private String endpoint = "";

        /**
         * Bucket served by {@code data.nudger.fr}; its object names are the {@code file} of the manifests.
         */
        private String bucket = "nudger-opendata";

        /**
         * Prefix of the temporary keys: a zip is sent there, read back and checked, then copied to its public key
         * (as {@code scripts/deploy/opendata-r2-upload.sh}, GOU-2285). Must end with {@code /}.
         */
        private String stagingPrefix = ".staging/";

        /**
         * Budget guard: the publication is refused, before any upload, when the bucket would exceed this size after
         * it (R2 is free under 10 GB-month). Binary units: 9GB is 9 x 1024^3 bytes.
         */
        private DataSize maxBucketSize = DataSize.ofGigabytes(9);

        /**
         * Size of a part of the multipart upload. R2: 5 MB to 5 GB, every part but the last of the same size, 10 000
         * parts at most.
         */
        private DataSize partSize = DataSize.ofMegabytes(64);

        /**
         * Size of a part of the server-side copy from the temporary key; an object up to this size is copied in one
         * request (a single copy is limited to 5 GB).
         */
        private DataSize copyPartSize = DataSize.ofGigabytes(1);

        /**
         * Upload bandwidth per second, so that the upload does not take the outbound bandwidth of the site (as the
         * {@code upload_limit} of the R2 upload workflow, 2 MB/s). Zero disables the limit. The read back of the
         * temporary object is not limited.
         */
        private DataSize uploadBandwidth = DataSize.ofMegabytes(2);

        /**
         * {@code Cache-Control} of the zips.
         */
        private String zipCacheControl = "public, max-age=86400";

        /**
         * {@code Cache-Control} of the manifests, short so that a new manifest is seen soon after the new zips.
         */
        private String manifestCacheControl = "public, max-age=300";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getStagingPrefix() {
            return stagingPrefix;
        }

        public void setStagingPrefix(String stagingPrefix) {
            this.stagingPrefix = stagingPrefix;
        }

        public DataSize getMaxBucketSize() {
            return maxBucketSize;
        }

        public void setMaxBucketSize(DataSize maxBucketSize) {
            this.maxBucketSize = maxBucketSize;
        }

        public DataSize getPartSize() {
            return partSize;
        }

        public void setPartSize(DataSize partSize) {
            this.partSize = partSize;
        }

        public DataSize getCopyPartSize() {
            return copyPartSize;
        }

        public void setCopyPartSize(DataSize copyPartSize) {
            this.copyPartSize = copyPartSize;
        }

        public DataSize getUploadBandwidth() {
            return uploadBandwidth;
        }

        public void setUploadBandwidth(DataSize uploadBandwidth) {
            this.uploadBandwidth = uploadBandwidth;
        }

        public String getZipCacheControl() {
            return zipCacheControl;
        }

        public void setZipCacheControl(String zipCacheControl) {
            this.zipCacheControl = zipCacheControl;
        }

        public String getManifestCacheControl() {
            return manifestCacheControl;
        }

        public void setManifestCacheControl(String manifestCacheControl) {
            this.manifestCacheControl = manifestCacheControl;
        }

        /**
         * @return the configured endpoint, or the R2 endpoint of the account; empty when neither is set
         */
        public String resolvedEndpoint() {
            if (StringUtils.isNotBlank(endpoint)) {
                return endpoint.trim();
            }
            if (StringUtils.isBlank(accountId)) {
                return "";
            }
            return "https://" + accountId.trim() + ".r2.cloudflarestorage.com";
        }

        /**
         * Never prints the credentials.
         */
        @Override
        public String toString() {
            return "Publication[enabled=" + enabled + ", bucket=" + bucket + ", endpoint=" + resolvedEndpoint() + "]";
        }
    }
}
