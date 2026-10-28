package org.open4goods.nudgerfrontapi.service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.UUID;

import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.ContributionVote;
import org.open4goods.services.contribution.repository.AffiliationClickRepository;
import org.open4goods.services.contribution.repository.ContributionVoteRepository;
import org.open4goods.services.contribution.service.ContributionService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.nudgerfrontapi.service.exception.AffiliationTrackingException;
import org.open4goods.nudgerfrontapi.service.exception.InvalidAffiliationTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service handling the encoding, decoding and tracking of affiliation redirect tokens.
 * <p>
 * The implementation mirrors the legacy UI controller by serialising {@link ContributionVote}
 * instances through {@link SerialisationService}. Each redirect is stored in Elasticsearch as an
 * {@link AffiliationClick} (no personal data) and, for compatibility, as a {@link ContributionVote}.
 * </p>
 */
@Service
public class AffiliationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AffiliationService.class);
    /** Maximum length of the originating path sent in {@code X-Affiliation-Origin}. */
    public static final int MAX_ORIGIN_PATH_LENGTH = 200;

    private final SerialisationService serialisationService;
    private final ContributionVoteRepository contributionVoteRepository;
    private final AffiliationClickRepository affiliationClickRepository;

    public AffiliationService(SerialisationService serialisationService,
            ContributionVoteRepository contributionVoteRepository,
            AffiliationClickRepository affiliationClickRepository) {
        this.serialisationService = serialisationService;
        this.contributionVoteRepository = contributionVoteRepository;
        this.affiliationClickRepository = affiliationClickRepository;
    }

    /**
     * Serialises the provided datasource name and URL into a redirect token that can later be consumed
     * by the affiliation controller.
     *
     * @param datasourceName the datasource generating the affiliation link
     * @param url            the destination URL to reach after decoding the token
     * @return a URL-safe token encapsulating a {@link ContributionVote}
     */
    public String encryptAffiliationLink(String datasourceName, String url) {
        return encryptAffiliationLink(datasourceName, url, null);
    }

    /**
     * Serialises the provided datasource name, URL and product GTIN into a redirect token. The GTIN is recorded
     * with the click when the token is consumed; tokens without GTIN remain valid.
     *
     * @param datasourceName the datasource generating the affiliation link
     * @param url            the destination URL to reach after decoding the token
     * @param gtin           GTIN of the product the offer belongs to, or {@code null}
     * @return a URL-safe token encapsulating a {@link ContributionVote}
     */
    public String encryptAffiliationLink(String datasourceName, String url, String gtin) {
        try {
            ContributionVote vote = new ContributionVote(datasourceName, url);
            vote.setGtin(StringUtils.hasText(gtin) ? gtin : null);
            String json = serialisationService.toJson(vote);
            String compressed = serialisationService.compressString(json);
            return URLEncoder.encode(compressed, Charset.defaultCharset());
        }
        catch (SerialisationException exception) {
            throw new AffiliationTrackingException("Unable to serialise affiliation link", exception);
        }
    }

    /**
     * Decodes and deserialises a redirect token into the original {@link ContributionVote} payload.
     *
     * @param token the encrypted token received from the client
     * @return the contribution vote payload stored within the token
     */
    public ContributionVote decryptAffiliationLink(String token) {
        try {
            String decoded = URLDecoder.decode(token, Charset.defaultCharset());
            String json = SerialisationService.uncompressString(decoded);
            ContributionVote vote = serialisationService.fromJson(json, ContributionVote.class);
            if (!StringUtils.hasText(vote.getUrl())) {
                throw new InvalidAffiliationTokenException("Decoded token does not contain a target URL.");
            }
            return vote;
        }
        catch (InvalidAffiliationTokenException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new InvalidAffiliationTokenException("Unable to decode affiliation token.", exception);
        }
    }

    /**
     * Tracks the redirect represented by the given token and returns its destination.
     * <p>
     * Two documents are written, each fail-open (a persistence failure never prevents the redirect):
     * <ul>
     * <li>an {@link AffiliationClick} ({@code affiliation-clicks} index), aggregated by the Metriks endpoint: network,
     * merchant, product GTIN, originating path and target host, without any personal data;</li>
     * <li>the legacy {@link ContributionVote} ({@code contribution-votes} index), kept for compatibility, without IP
     * nor user agent.</li>
     * </ul>
     *
     * @param token      the encrypted contribution vote token
     * @param originPath raw value of the {@code X-Affiliation-Origin} header, validated by
     *                   {@link #sanitiseOriginPath(String)}; {@code null} when absent
     * @return the destination URL extracted from the token
     */
    public String trackRedirect(String token, String originPath) {
        ContributionVote vote = decryptAffiliationLink(token);
        long now = System.currentTimeMillis();
        vote.setId(UUID.randomUUID().toString());
        vote.setTs(now);
        vote.setIp(null);
        vote.setUa(null);
        vote.setVote(ContributionService.DEFAULT_VOTE);

        AffiliationClick click = AffiliationClick.of(vote.getDatasourceName(), vote.getUrl(), vote.getGtin(),
                sanitiseOriginPath(originPath), now);
        try {
            affiliationClickRepository.save(click);
            LOGGER.info("Tracked affiliation click for datasource '{}' through network '{}' towards host '{}'",
                    click.getMerchant(), click.getNetwork(), click.getTargetHost());
        }
        catch (Exception exception) {
            // Fail-open: a tracking/persistence failure must never prevent the user from
            // reaching the merchant. The redirect always proceeds; only the click log is lost.
            LOGGER.error("Failed to persist affiliation click for datasource '{}' towards host '{}'",
                    click.getMerchant(), click.getTargetHost(), exception);
        }

        try {
            contributionVoteRepository.save(vote);
        }
        catch (Exception exception) {
            LOGGER.error("Failed to persist affiliation redirect vote for datasource '{}' towards host '{}'",
                    vote.getDatasourceName(), click.getTargetHost(), exception);
        }
        return vote.getUrl();
    }

    /**
     * Validates the originating page sent by the frontend in the {@code X-Affiliation-Origin} header.
     *
     * @param originPath raw header value, possibly {@code null}
     * @return the value when it is a bare path (starts with a single {@code /}, no query nor fragment, no
     *         whitespace or control character, at most {@value #MAX_ORIGIN_PATH_LENGTH} characters), {@code null}
     *         otherwise
     */
    public static String sanitiseOriginPath(String originPath) {
        if (originPath == null || originPath.isEmpty() || originPath.length() > MAX_ORIGIN_PATH_LENGTH
                || originPath.charAt(0) != '/' || originPath.startsWith("//")) {
            return null;
        }
        for (int i = 0; i < originPath.length(); i++) {
            char c = originPath.charAt(i);
            if (c == '?' || c == '#' || Character.isWhitespace(c) || Character.isISOControl(c)) {
                return null;
            }
        }
        return originPath;
    }
}
