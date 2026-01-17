package org.open4goods.services.prompt.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.open4goods.services.prompt.model.BatchJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * File-based persistence for batch jobs in the batch folder.
 */
public class BatchJobStore {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobStore.class);
    private static final String JOB_PREFIX = "batch-job-";
    private static final String JOB_SUFFIX = ".json";

    private final ObjectMapper objectMapper;
    private final File baseFolder;

    public BatchJobStore(File baseFolder, ObjectMapper objectMapper) {
        this.baseFolder = baseFolder;
        this.objectMapper = objectMapper;
    }

    public void save(BatchJob job) {
        job.setUpdatedAt(Instant.now());
        File file = resolveFile(job.getId());
        try {
            Files.writeString(file.toPath(), objectMapper.writeValueAsString(job), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to persist batch job {}: {}", job.getId(), e.getMessage(), e);
        }
    }

    public Optional<BatchJob> load(String jobId) {
        File file = resolveFile(jobId);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return Optional.of(objectMapper.readValue(content, BatchJob.class));
        } catch (Exception e) {
            logger.error("Failed to load batch job {}: {}", jobId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public List<BatchJob> loadAll() {
        List<BatchJob> jobs = new ArrayList<>();
        File[] files = baseFolder.listFiles((dir, name) -> name.startsWith(JOB_PREFIX) && name.endsWith(JOB_SUFFIX));
        if (files == null) {
            return jobs;
        }
        for (File file : files) {
            String name = file.getName();
            String jobId = name.substring(JOB_PREFIX.length(), name.length() - JOB_SUFFIX.length());
            load(jobId).ifPresent(jobs::add);
        }
        return jobs;
    }

    private File resolveFile(String jobId) {
        return new File(baseFolder, JOB_PREFIX + jobId + JOB_SUFFIX);
    }
}
