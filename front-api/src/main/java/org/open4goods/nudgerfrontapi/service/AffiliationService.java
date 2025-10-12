package org.open4goods.nudgerfrontapi.service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.UUID;

import org.open4goods.services.contribution.model.ContributionVote;
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
 * instances through {@link SerialisationService} and storing redirects to Elasticsearch using
 * {@link ContributionVoteRepository}.
 * </p>
 */
@Service
public class AffiliationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AffiliationService.class);
    private static final int MAX_USER_AGENT_LENGTH = 100;

    private final SerialisationService serialisationService;
    private final ContributionVoteRepository contributionVoteRepository;

    public AffiliationService(SerialisationService serialisationService,
            ContributionVoteRepository contributionVoteRepository) {
        this.serialisationService = serialisationService;
        this.contributionVoteRepository = contributionVoteRepository;
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
        try {
            ContributionVote vote = new ContributionVote(datasourceName, url);
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
     * Tracks the redirect represented by the given token, recording IP and user agent information in Elasticsearch.
     *
     * @param token     the encrypted contribution vote token
     * @param ip        the originating IP of the client triggering the redirect
     * @param userAgent the user agent header supplied by the client
     * @return the destination URL extracted from the token
     */
    public String trackRedirect(String token, String ip, String userAgent) {
        ContributionVote vote = decryptAffiliationLink(token);
        vote.setId(UUID.randomUUID().toString());
        vote.setTs(System.currentTimeMillis());
        vote.setIp(ip);
        vote.setUa(sanitiseUserAgent(userAgent));
        vote.setVote(ContributionService.DEFAULT_VOTE);

        try {
            contributionVoteRepository.save(vote);
            LOGGER.info("Tracked affiliation redirect for datasource '{}' towards '{}'", vote.getDatasourceName(),
                    vote.getUrl());
            return vote.getUrl();
        }
        catch (Exception exception) {
            throw new AffiliationTrackingException("Failed to persist affiliation redirect", exception);
        }
    }

    /**
     * Trim and normalise the user agent while preventing Elasticsearch field explosion.
     *
     * @param userAgent raw header value provided by the client
     * @return sanitised user agent or {@code null} when blank
     */
    private String sanitiseUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        String trimmed = userAgent.trim();
        if (trimmed.length() > MAX_USER_AGENT_LENGTH) {
            return trimmed.substring(0, MAX_USER_AGENT_LENGTH);
        }
        return trimmed;
    }
}
