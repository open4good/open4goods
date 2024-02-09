package org.open4goods.services;


import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHubBuilder;
import org.open4goods.config.yml.FeedbackConfiguration;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedbackService {

	private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class);

	
	
	private GHRepository github;
	private FeedbackConfiguration feedbackConfiguration;

	
	public FeedbackService(FeedbackConfiguration feedbackConfiguration) {
		super();
		this.feedbackConfiguration = feedbackConfiguration;
		try {
			this.github = new GitHubBuilder().withOAuthToken(feedbackConfiguration.getGithubConfig().getAccessToken()).build()
					.getRepository(feedbackConfiguration.getGithubConfig().getOrganization() +  "/" + feedbackConfiguration.getGithubConfig().getRepo());
		} catch (Exception e) {
			logger.error("Error while setuping github access : " + e.getMessage());			
		}
	}


	/**
	 * Get the template corresponding to an issue
	 * @param title
	 * @param description
	 * @param urlSource
	 * @param publisherName
	 * @param labels
	 * @return
	 */
	public String getIssueTemplate (String title, String description, String urlSource, String publisherName, Collection<String> labels) {
		
		StringBuilder messageMarkdown = new StringBuilder();
		/**
		## Issue
		This feedback has been submited by *AUTHOR* on [nudger.fr](https://nudger.fr/), in order to declare an issue on [this page](PAGE).
		#### User message : 
		> MESSAGE
		**/
		messageMarkdown.append("## Bug ! \n\n");
		messageMarkdown.append("This issue has been submited by *").append(publisherName).append("* on [nudger.fr](").append(urlSource).append("), in order to declare a bug");
		
		if (!StringUtils.isEmpty(urlSource)) {
			messageMarkdown.append("\n\nThis bug is relative to [this page](").append(urlSource).append(").\n\n");
		}
		
		messageMarkdown.append("\n\n");
		messageMarkdown.append("#### User message : \n\n");
		messageMarkdown.append("> ").append(description).append("\n\n");
		
		return messageMarkdown.toString();
		}
	
	
	/**
	 * Get the template corresponding to an idea
	 * @param title
	 * @param description
	 * @param urlSource
	 * @param publisherName
	 * @param labels
	 * @return
	 */
	public String getIdeaTemplate (String title, String description, String urlSource, String publisherName, Collection<String> labels) {
		
		StringBuilder messageMarkdown = new StringBuilder();
		/**
		## Issue
		This feedback has been submited by *AUTHOR* on [nudger.fr](https://nudger.fr/), in order to declare an issue on [this page](PAGE).
		#### User message : 
		> MESSAGE
		**/
		messageMarkdown.append("## Idea \n\n");
		messageMarkdown.append("This feedback has been submited by *").append(publisherName).append("* on [nudger.fr](").append(urlSource).append("), in order to declare a brillant idea.");
		
		if (!urlSource.isEmpty()) {
			messageMarkdown.append("\n\nThis idea could be related to [this page](").append(urlSource).append(").\n\n");
		}
		
		messageMarkdown.append("\n\n");
		messageMarkdown.append("#### User message : \n\n");
		messageMarkdown.append("> ").append(description).append("\n\n");
		
		return messageMarkdown.toString();
		}
	
	/**
	 * Create an issue on github
	 * @param title
	 * @param description
	 * @param labels
	 * @return
	 * @throws IOException
	 * @throws InvalidParameterException
	 */
	
	public GHIssue createIssue (String title, String description, Collection<String> labels) throws IOException, InvalidParameterException {
		
		GHIssueBuilder issue = github.createIssue(IdHelper.sanitize(title));

		issue = issue.body(description);
		
		for (String label : labels) {
			issue = issue.label(label);
		}
		
		GHIssue ret = issue.create();		
		return ret;
	
	}

	/**
	 * Create a bug issue on github
	 * @param title
	 * @param description
	 * @param url
	 * @param author
	 * @param labels
	 * @throws IOException
	 * @throws InvalidParameterException
	 */
	public void createBug(String title, String description, String url, String author, Set<String> labels) throws IOException, InvalidParameterException {
		String message = getIssueTemplate(title, description, url, author, labels);
		createIssue(title, message, labels);
	}
	
	
	/**
	 * Create an ideaissue on github
	 * @param title
	 * @param description
	 * @param url
	 * @param author
	 * @param labels
	 * @throws IOException
	 * @throws InvalidParameterException
	 */
	public void createIdea(String title, String description, String url, String author, Set<String> labels) throws IOException, InvalidParameterException {
		String message = getIssueTemplate(title, description, url, author, labels);
		createIssue(title, message, labels);

	}
}