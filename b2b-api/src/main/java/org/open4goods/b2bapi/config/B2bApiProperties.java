package org.open4goods.b2bapi.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime settings for the Product Data API.
 */
@Validated
@ConfigurationProperties(prefix = "b2b")
public class B2bApiProperties {

    @NotNull
    private URI publicBaseUrl = URI.create("https://product-data-api.com");

    @NotBlank
    private String apiBasePath = "/api/v1";

    @Valid
    private Credits credits = new Credits();

    @Valid
    private Price price = new Price();

    @Valid
    private Security security = new Security();

    @Valid
    private RequestIds requestIds = new RequestIds();

    @Valid
    private Redis redis = new Redis();

    @Valid
    private RateLimit ratelimit = new RateLimit();

    @Valid
    private Hardener hardener = new Hardener();

    public URI getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(final URI publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getApiBasePath() {
        return apiBasePath;
    }

    public void setApiBasePath(final String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    public Credits getCredits() {
        return credits;
    }

    public void setCredits(final Credits credits) {
        this.credits = credits;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(final Price price) {
        this.price = price;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(final Security security) {
        this.security = security;
    }

    public RequestIds getRequestIds() {
        return requestIds;
    }

    public void setRequestIds(final RequestIds requestIds) {
        this.requestIds = requestIds;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(final Redis redis) {
        this.redis = redis;
    }

    public RateLimit getRatelimit() {
        return ratelimit;
    }

    public void setRatelimit(final RateLimit ratelimit) {
        this.ratelimit = ratelimit;
    }

    public Hardener getHardener() {
        return hardener;
    }

    public void setHardener(final Hardener hardener) {
        this.hardener = hardener;
    }

    public static class Credits {

        @Positive
        private double unitEur = 0.002D;

        @Min(0)
        private int freeGrantCredits = 2_500;

        public double getUnitEur() {
            return unitEur;
        }

        public void setUnitEur(final double unitEur) {
            this.unitEur = unitEur;
        }

        public int getFreeGrantCredits() {
            return freeGrantCredits;
        }

        public void setFreeGrantCredits(final int freeGrantCredits) {
            this.freeGrantCredits = freeGrantCredits;
        }
    }

    public static class Price {

        @Min(1)
        private int freshnessDays = 30;

        public int getFreshnessDays() {
            return freshnessDays;
        }

        public void setFreshnessDays(final int freshnessDays) {
            this.freshnessDays = freshnessDays;
        }
    }

    public static class Security {

        private List<String> allowedOrigins = new ArrayList<>();

        private List<String> adminEmails = new ArrayList<>();

        @NotBlank
        private String cookieDomain = ".product-data-api.com";

        private boolean cookieSecure = true;

        @NotBlank
        private String cookieSameSite = "Lax";

        @NotBlank
        private String jwtSecret = "change-me-dev-only-change-me-dev-only";

        @NotNull
        private Duration accessTokenTtl = Duration.ofMinutes(15);

        @NotNull
        private Duration refreshTokenTtl = Duration.ofDays(30);

        @Valid
        private Oidc oidc = new Oidc();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(final List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAdminEmails() {
            return adminEmails;
        }

        public void setAdminEmails(final List<String> adminEmails) {
            this.adminEmails = adminEmails;
        }

        public String getCookieDomain() {
            return cookieDomain;
        }

        public void setCookieDomain(final String cookieDomain) {
            this.cookieDomain = cookieDomain;
        }

        public boolean isCookieSecure() {
            return cookieSecure;
        }

        public void setCookieSecure(final boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }

        public String getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(final String cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(final String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(final Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(final Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }

        public Oidc getOidc() {
            return oidc;
        }

        public void setOidc(final Oidc oidc) {
            this.oidc = oidc;
        }
    }

    public static class Oidc {

        @Valid
        private OidcProvider google = new OidcProvider(
                "https://accounts.google.com",
                "https://www.googleapis.com/oauth2/v3/certs");

        @Valid
        private OidcProvider microsoft = new OidcProvider(
                "https://login.microsoftonline.com/common/v2.0",
                "https://login.microsoftonline.com/common/discovery/v2.0/keys");

        @Valid
        private Github github = new Github();

        @Valid
        private OidcProvider apple = new OidcProvider(
                "https://appleid.apple.com",
                "https://appleid.apple.com/auth/keys");

        public OidcProvider getGoogle() {
            return google;
        }

        public void setGoogle(final OidcProvider google) {
            this.google = google;
        }

        public OidcProvider getMicrosoft() {
            return microsoft;
        }

        public void setMicrosoft(final OidcProvider microsoft) {
            this.microsoft = microsoft;
        }

        public Github getGithub() {
            return github;
        }

        public void setGithub(final Github github) {
            this.github = github;
        }

        public OidcProvider getApple() {
            return apple;
        }

        public void setApple(final OidcProvider apple) {
            this.apple = apple;
        }
    }

    public static class OidcProvider {

        private String clientId = "";

        private String clientSecret = "";

        @NotBlank
        private String issuer;

        @NotBlank
        private String jwksUri;

        public OidcProvider() {
        }

        public OidcProvider(final String issuer, final String jwksUri) {
            this.issuer = issuer;
            this.jwksUri = jwksUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(final String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(final String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(final String issuer) {
            this.issuer = issuer;
        }

        public String getJwksUri() {
            return jwksUri;
        }

        public void setJwksUri(final String jwksUri) {
            this.jwksUri = jwksUri;
        }
    }

    public static class Github {

        private String clientId = "";

        private String clientSecret = "";

        @NotBlank
        private String apiBaseUrl = "https://api.github.com";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(final String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(final String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(final String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }
    }

    public static class RequestIds {

        @NotBlank
        private String prefix = "pdreq_";

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }
    }

    public static class Redis {

        @NotNull
        private Duration balanceTtl = Duration.ofHours(24);

        @NotNull
        private Duration apikeyCacheTtl = Duration.ofMinutes(10);

        @Min(1)
        private long usageStreamMaxlen = 1_000_000L;

        @NotNull
        private Duration reconcileInterval = Duration.ofMinutes(5);

        private boolean failClosed = true;

        public Duration getBalanceTtl() {
            return balanceTtl;
        }

        public void setBalanceTtl(final Duration balanceTtl) {
            this.balanceTtl = balanceTtl;
        }

        public Duration getApikeyCacheTtl() {
            return apikeyCacheTtl;
        }

        public void setApikeyCacheTtl(final Duration apikeyCacheTtl) {
            this.apikeyCacheTtl = apikeyCacheTtl;
        }

        public long getUsageStreamMaxlen() {
            return usageStreamMaxlen;
        }

        public void setUsageStreamMaxlen(final long usageStreamMaxlen) {
            this.usageStreamMaxlen = usageStreamMaxlen;
        }

        public Duration getReconcileInterval() {
            return reconcileInterval;
        }

        public void setReconcileInterval(final Duration reconcileInterval) {
            this.reconcileInterval = reconcileInterval;
        }

        public boolean isFailClosed() {
            return failClosed;
        }

        public void setFailClosed(final boolean failClosed) {
            this.failClosed = failClosed;
        }
    }

    public static class RateLimit {

        @Min(1)
        private int requestsPerMinute = 600;

        @NotNull
        private Duration window = Duration.ofMinutes(1);

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(final int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(final Duration window) {
            this.window = window;
        }
    }

    public static class Hardener {

        @NotBlank
        private String cron = "0 */5 * * * *";

        @Min(1)
        private int streamBatchSize = 100;

        @NotNull
        private Duration lockAtLeastFor = Duration.ofMinutes(1);

        @NotNull
        private Duration lockAtMostFor = Duration.ofMinutes(10);

        public String getCron() {
            return cron;
        }

        public void setCron(final String cron) {
            this.cron = cron;
        }

        public int getStreamBatchSize() {
            return streamBatchSize;
        }

        public void setStreamBatchSize(final int streamBatchSize) {
            this.streamBatchSize = streamBatchSize;
        }

        public Duration getLockAtLeastFor() {
            return lockAtLeastFor;
        }

        public void setLockAtLeastFor(final Duration lockAtLeastFor) {
            this.lockAtLeastFor = lockAtLeastFor;
        }

        public Duration getLockAtMostFor() {
            return lockAtMostFor;
        }

        public void setLockAtMostFor(final Duration lockAtMostFor) {
            this.lockAtMostFor = lockAtMostFor;
        }
    }
}
