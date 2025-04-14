package org.open4goods.services.prompt.dto.openai;

import java.util.List;

/**
 * DTO used to expose the status of batch jobs.
 *
 * @param activeJobs the number of active batch jobs
 * @param jobIds the list of job IDs still pending a response
 */
public record BatchStatus(int activeJobs, List<String> jobIds) { }
