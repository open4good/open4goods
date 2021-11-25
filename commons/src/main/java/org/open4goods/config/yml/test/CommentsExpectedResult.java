package org.open4goods.config.yml.test;

import java.util.Set;

import org.open4goods.model.data.Comment;

public class CommentsExpectedResult extends NumericExpectedResult {

	private boolean withUtility = false;

	public void test(final Set<Comment> comments, final TestResultReport ret) {
		testCollection(comments, "comments", ret);

		if (withUtility) {
			boolean withUti = false;
			for (final Comment comment : comments) {

				if (comment.getUsefull() != null || comment.getUseless() != null) {
					withUti = true;
					break;
				}

			}

			if (!withUti) {
				ret.addMessage("No comments with utility != null found");
			}

		}

	}

	public boolean isWithUtility() {
		return withUtility;
	}

	public void setWithUtility(final boolean withUtility) {
		this.withUtility = withUtility;
	}



}
