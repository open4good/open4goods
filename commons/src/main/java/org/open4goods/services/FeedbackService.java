package org.open4goods.services;


import java.io.IOException;
import java.util.Collection;

import org.kohsuke.github.GHEventPayload.Issue;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHubBuilder;
import org.open4goods.config.yml.FeedbackConfiguration;
import org.open4goods.config.yml.GithubConfiguration;
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
		} catch (IOException e) {
			logger.error("Error while setuping github access",e);
			
		}
	}


	
	
	
	public GHIssue createIssue (String title, String description, String urlSource, String publisherName, Collection<String> labels) throws IOException {
		
		GHIssueBuilder issue = github.createIssue(IdHelper.sanitize(title));
		
		StringBuilder messageMarkdown = new StringBuilder();
		messageMarkdown.append("## Feedback\n\n");
		messageMarkdown.append("### URL : " ).append(urlSource).append("\n\n");
		messageMarkdown.append("### Author : " ).append(publisherName).append("\n\n");
		messageMarkdown.append("### Message : \n" ).append(description).append("\n\n");
		
		
		
		
		
		// TODO : Templatize the description
		issue = issue.body(messageMarkdown.toString());
		
		for (String label : labels) {
			issue = issue.label(label);
		}
		
		GHIssue ret = issue.create();
		
		return ret;
	
	}
	
}