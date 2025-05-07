// src/main/java/org/open4goods/services/feedback/exception/VotingNotAllowedException.java
package org.open4goods.services.feedback.exception;

/**
 * Thrown when attempting to vote on an issue that does not have the required votable label.
 */
public class VotingNotAllowedException extends RuntimeException {
    public VotingNotAllowedException(String message) {
        super(message);
    }
}
