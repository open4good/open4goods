package org.open4goods.datareference.model;

/**
 * A use of source content that reviewed terms may forbid.
 *
 * <p>A policy's {@code prohibitedUses} is a denylist: absent from a policy, it
 * reads as every value in this enum, the most restrictive reading, since
 * nothing was reviewed to clear it.
 */
public enum ProhibitedUse {
    /**
     * Using the content to train, fine-tune or otherwise build a machine-learning
     * or statistical model, as forbidden by Icecat Open Content License v1.4
     * clause 10.
     */
    AI_TRAINING,
    /**
     * Using the content as input to automated synthetic content creation, as
     * forbidden by Icecat Open Content License v1.4 clause 10.
     */
    SYNTHETIC_CONTENT_GENERATION
}
