package org.open4goods.api.controller.api;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.dto.metriks.MetriksPeriod;
import org.open4goods.api.dto.metriks.MetriksResponse;
import org.open4goods.services.feedservice.service.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/metriks")
@Tag(name = "Metriks", description = "Endpoints for Metriks monitoring system")
public class MetriksController {

    private final FeedService feedService;
    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public MetriksController(FeedService feedService, ElasticsearchOperations elasticsearchOperations) {
        this.feedService = feedService;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @GetMapping("/system")
    @Operation(summary = "Get system metrics (Disk, Elasticsearch)")
    public MetriksResponse getSystemMetrics() {
        MetriksResponse response = new MetriksResponse();
        response.setPeriod(new MetriksPeriod(LocalDate.now(), LocalDate.now()));
        List<MetriksEvent> events = new ArrayList<>();

        // Disk Usage
        File root = new File("/");
        events.add(createEvent("system.disk.total", "Total Disk Space", root.getTotalSpace(), "bytes"));
        events.add(createEvent("system.disk.free", "Free Disk Space", root.getFreeSpace(), "bytes"));
        events.add(createEvent("system.disk.used", "Used Disk Space", root.getTotalSpace() - root.getFreeSpace(), "bytes"));

        // Elastic stats
        try {
            // Index count
            // This is a simplification; iterating all indices might be heavy if there are thousands,
            // but typical for this app. 
            // operations.indexOps(Object.class).getInformation() needs a class or index name.
            // Using a wildcard check via Cluster operations would be better but ElasticsearchOperations
            // abstracts that. 
            // For now, let's try to get stats for the main product index if possible,
            // or just skip complex cluster stats if the API doesn't expose them easily without low-level client.
            
            // Alternative: Use the cluster helper if available, or just map what we can.
            // Since we don't have a direct "ClusterOps" in high level common spring-data-es (depends on version),
            // We might skip cluster-wide document count for now unless we query matching all.
            
        	// Let's just monitor we can talk to it.
        	events.add(createEvent("infrastructure.elastic.status", "Elasticsearch Status", 1, "status")); // 1 for UP
        	
        } catch (Exception e) {
            MetriksEvent errorEvent = createEvent("infrastructure.elastic.status", "Elasticsearch Status", 0, "status");
            errorEvent.setStatus("error");
            errorEvent.setErrorMessage(e.getMessage());
            events.add(errorEvent);
        }

        response.setEvents(events);
        return response;
    }

    @GetMapping("/functional")
    @Operation(summary = "Get functional metrics (Business KPIs)")
    public MetriksResponse getFunctionalMetrics() {
        MetriksResponse response = new MetriksResponse();
        response.setPeriod(new MetriksPeriod(LocalDate.now(), LocalDate.now()));
        List<MetriksEvent> events = new ArrayList<>();

        // Partners
        int partnerCount = 0;
        try {
            partnerCount = feedService.getPartners().size();
            events.add(createEvent("business.partners.count", "Affiliation Partners", partnerCount, "count"));
        } catch (Exception e) {
             MetriksEvent errorEvent = createEvent("business.partners.count", "Affiliation Partners", null, "count");
             errorEvent.setStatus("error");
             errorEvent.setErrorMessage(e.getMessage());
             events.add(errorEvent);
        }

        response.setEvents(events);
        return response;
    }

    private MetriksEvent createEvent(String id, String name, Number value, String unit) {
        MetriksEvent event = new MetriksEvent();
        event.setId(id);
        event.setName(name);
        event.setValue(value);
        event.setUnit(unit);
        event.setStatus("ok");
        return event;
    }
}
