package org.open4goods.config.yml.test;

import java.util.Set;

import org.open4goods.model.data.Answer;
import org.open4goods.model.data.Question;

public class QuestionsExpectedResult extends NumericExpectedResult {

	private boolean withUtility = false;

	public void test(final Set<Question> questions, final TestResultReport ret) {
		testCollection(questions, "questions", ret);

		if (withUtility) {
			boolean withUti = false;
			for (final Question q : questions) {
				for (final Answer a : q.getAnswers()) {
					if (a.getUsefull() != null || a.getUseless() != null) {
						withUti = true;
						break;
					}
				}
			}

			if (!withUti) {
				ret.addMessage("No answer with utility != null found");
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
