package org.open4goods.commons.config.yml.datasource;

import java.util.ArrayList;
import java.util.List;

public class QuestionsConfig {
	private String root;
	private String questionTitle;
	private String questionDate;
	private String questionAuthor;

	private String answerTitle;

	private String answerAuthor;
	private String answerDate;

	private String answerUseless;
	private String answerUsefull;

	/**
	 * Removals that will be used for answerUseless / useFull
	 */
	private List<String> useRemovals = new ArrayList<>();


	public String getQuestionTitle() {
		return questionTitle;
	}

	public void setQuestionTitle(final String questionTitle) {
		this.questionTitle = questionTitle;
	}

	public String getQuestionDate() {
		return questionDate;
	}

	public void setQuestionDate(final String questionDate) {
		this.questionDate = questionDate;
	}

	public String getQuestionAuthor() {
		return questionAuthor;
	}

	public void setQuestionAuthor(final String questionAuthor) {
		this.questionAuthor = questionAuthor;
	}

	public String getAnswerTitle() {
		return answerTitle;
	}

	public void setAnswerTitle(final String answerTitle) {
		this.answerTitle = answerTitle;
	}

	public String getAnswerAuthor() {
		return answerAuthor;
	}

	public void setAnswerAuthor(final String answerAuthor) {
		this.answerAuthor = answerAuthor;
	}

	public String getAnswerDate() {
		return answerDate;
	}

	public void setAnswerDate(final String answerDate) {
		this.answerDate = answerDate;
	}

	public String getRoot() {
		return root;
	}

	public void setRoot(final String root) {
		this.root = root;
	}

	public String getAnswerUseless() {
		return answerUseless;
	}

	public void setAnswerUseless(final String answerUseless) {
		this.answerUseless = answerUseless;
	}

	public String getAnswerUsefull() {
		return answerUsefull;
	}

	public void setAnswerUsefull(final String answerUsefull) {
		this.answerUsefull = answerUsefull;
	}

	public List<String> getUseRemovals() {
		return useRemovals;
	}

	public void setUseRemovals(final List<String> useRemovals) {
		this.useRemovals = useRemovals;
	}




}
