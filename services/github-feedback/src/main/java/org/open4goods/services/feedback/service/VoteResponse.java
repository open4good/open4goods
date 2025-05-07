// src/main/java/org/open4goods/services/feedback/service/VoteResponse.java
package org.open4goods.services.feedback.service;

/**
 * Response DTO returned after a vote is cast.
 *
 * @param remainingVotes how many votes the current IP may still cast
 * @param totalVotes     the updated total votes for the issue
 */
public record VoteResponse(int remainingVotes, int totalVotes) { }
