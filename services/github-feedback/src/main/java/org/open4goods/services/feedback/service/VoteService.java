// src/main/java/org/open4goods/services/feedback/service/VoteService.java
package org.open4goods.services.feedback.service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.open4goods.services.feedback.config.FeedbackConfiguration;
import org.open4goods.services.feedback.exception.VotingLimitExceededException;
import org.open4goods.services.feedback.exception.VotingNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Handles per‑IP voting quotas, vote‐count caching, and a single “Nudgers vote : N”
 * comment per issue (rather than one 👍 per vote).
 */
@Service
public class VoteService {

    private static final Logger logger = LoggerFactory.getLogger(VoteService.class);

    /** Prefix used for our single voting comment on each issue */
    private static final String VOTE_COMMENT_PREFIX = "👍🚀👍 Nudgers vote 👍🚀👍 : ";

    private final FeedbackConfiguration config;
    private final GHRepository repository;
    private final MeterRegistry meterRegistry;

    /** Cache of total votes per issue, initialized by reading the single vote comment. */
    private final ConcurrentMap<String, AtomicInteger> voteCountCache = new ConcurrentHashMap<>();

    /** Cache of the single vote‑comment ID per issue. */
    private final ConcurrentMap<String, Long> voteCommentIdCache = new ConcurrentHashMap<>();

    /** The GitHub login of our bot user, used to filter the vote comment. */
    private final String botLogin;

    private final org.open4goods.commons.services.IpQuotaService ipQuotaService;

    private static final String QUOTA_ACTION_VOTE = "VOTE";

    public VoteService(FeedbackConfiguration config,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) GHRepository repository,
                       MeterRegistry meterRegistry,
                       org.open4goods.commons.services.IpQuotaService ipQuotaService) {
        this.config = config;
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.ipQuotaService = ipQuotaService;
        this.botLogin = config.getGithub().getUser();
    }

    /**
     * @return whether the given IP still has votes left today.
     */
    public boolean userCanVote(String ip) {
        int max = config.getVoting().getMaxVotesPerIpPerDay();
        return ipQuotaService.isAllowed(QUOTA_ACTION_VOTE, ip, max);
    }

    /**
     * Casts a vote from the given IP on the specified issue.
     * First verifies the issue has the required label.
     *
     * @throws VotingNotAllowedException if the issue isn't votable
     * @throws VotingLimitExceededException if the IP has no votes left
     */
    public VoteResponse vote(String issueId, String ip) {
        // 0) Ensure the issue is votable (skip check if repository is null/mocked)
        if (repository != null) {
            String required = config.getVoting().getRequiredLabel();
            try {
                GHIssue issue = repository.getIssue(Integer.parseInt(issueId));
                boolean has = issue.getLabels().stream()
                                   .anyMatch(l -> required.equals(l.getName()));
                if (!has) {
                    logger.warn("Issue {} missing required label '{}' for voting", issueId, required);
                    throw new VotingNotAllowedException("Voting not allowed on this issue");
                }
            } catch (IOException e) {
                logger.error("Could not verify votable label on issue {}: {}", issueId, e.getMessage());
                throw new RuntimeException("Internal error verifying votable issue", e);
            }
        }

        // 1) Check usage (quota)
        int max = config.getVoting().getMaxVotesPerIpPerDay();
        if (!ipQuotaService.isAllowed(QUOTA_ACTION_VOTE, ip, max)) {
            logger.warn("IP {} reached daily vote limit ({})", ip, max);
            throw new VotingLimitExceededException("Maximum " + max + " votes reached today");
        }
        
        // Increment usage
        int newUsage = ipQuotaService.increment(QUOTA_ACTION_VOTE, ip);

        // 2) Increment our local total
        AtomicInteger totalCounter = voteCountCache
            .computeIfAbsent(issueId, this::loadInitialVotes);
        int newTotal = totalCounter.incrementAndGet();

        // 3) Record metric
        meterRegistry.counter("feedback.votes.total").increment();

        // 4) Update (or create) the single vote comment on GitHub
        updateVoteCommentOnGitHub(issueId, newTotal);

        int remaining = Math.max(0, max - newUsage);
        logger.info("IP {} voted on issue {} (remaining: {}, total: {})",
                    ip, issueId, remaining, newTotal);

        return new VoteResponse(remaining, newTotal);
    }

    /**
     * @param ip client IP address
     * @return how many votes this IP may still cast today
     */
    public int getRemainingVotes(String ip) {
        int max = config.getVoting().getMaxVotesPerIpPerDay();
        return ipQuotaService.getRemaining(QUOTA_ACTION_VOTE, ip, max);
    }

    /**
     * @param issueId GitHub issue number as String
     * @return the total number of votes recorded locally (initializing if needed)
     */
    public int getTotalVotes(String issueId) {
        return voteCountCache
            .computeIfAbsent(issueId, this::loadInitialVotes)
            .get();
    }

    //---- Internals -------------------------------------------------------------------

    /**
     * Loads the initial vote count by scanning for our single vote comment.
     */
    private AtomicInteger loadInitialVotes(String issueId) {
        if (repository == null) {
            return new AtomicInteger(0);
        }
        int count = 0;
        try {
            GHIssue issue = repository.getIssue(Integer.parseInt(issueId));
            for (GHIssueComment c : issue.getComments()) {
                if (c.getUser().getLogin().equals(botLogin)
                        && c.getBody().startsWith(VOTE_COMMENT_PREFIX)) {
                    // parse “Nudgers vote : N”
                    String num = c.getBody().substring(VOTE_COMMENT_PREFIX.length()).trim();
                    count = Integer.parseInt(num);
                    voteCommentIdCache.put(issueId, c.getId());
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load initial votes for issue {}: {}", issueId, e.getMessage());
        }
        return new AtomicInteger(count);
    }

    /**
     * Finds (or creates) our single vote comment on GitHub, then updates it.
     */
    private void updateVoteCommentOnGitHub(String issueId, int newTotal) {
        if (repository == null) {
            return;
        }
        Integer issueNum = Integer.valueOf(issueId);
        try {
            GHIssue issue = repository.getIssue(issueNum);
            long commentId = voteCommentIdCache.computeIfAbsent(issueId, id -> {
                try {
                    for (GHIssueComment c : issue.getComments()) {
                        if (c.getUser().getLogin().equals(botLogin)
                                && c.getBody().startsWith(VOTE_COMMENT_PREFIX)) {
                            return c.getId();
                        }
                    }
                    GHIssueComment created = issue.comment(VOTE_COMMENT_PREFIX + "0");
                    return created.getId();
                } catch (IOException ex) {
                    logger.error("Could not initialize vote comment for issue {}: {}", issueId, ex.getMessage());
                    return -1L;
                }
            });

            if (commentId > 0) {
                GHIssueComment voteComment = issue.getComments()
                                                  .stream()
                                                  .filter(e -> e.getId() == commentId)
                                                  .findAny()
                                                  .orElse(null);
                if (voteComment != null) {
                    voteComment.update(VOTE_COMMENT_PREFIX + newTotal);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to update vote comment for issue {}: {}", issueId, e.getMessage());
        }
    }
}
