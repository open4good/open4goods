package org.open4goods.metriks.providers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.open4goods.metriks.core.MetricDefinition;
import org.open4goods.metriks.core.MetrikPayload;
import org.open4goods.metriks.core.MetrikPayload.EventData;
import org.open4goods.metriks.core.MetrikPayload.PeriodPayload;
import org.open4goods.metriks.core.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provider collecting GitHub repository metrics through the GraphQL API.
 */
public class GithubProvider implements MetriksProvider {

    private static final Logger logger = LoggerFactory.getLogger(GithubProvider.class);
    private static final URI GRAPHQL_ENDPOINT = URI.create("https://api.github.com/graphql");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String repository;
    private final String token;

    private GithubMetrics cachedMetrics;
    private Period cachedPeriod;

    public GithubProvider(ObjectMapper objectMapper, String repository, String token) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.token = token;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Optional<MetrikPayload> collect(MetricDefinition definition, Period period) {
        if (!"github".equalsIgnoreCase(definition.event_provider())) {
            return Optional.empty();
        }
        if (repository == null || repository.isBlank() || token == null || token.isBlank()) {
            return Optional.of(errorPayload(definition, period, "Missing GITHUB_TOKEN or GITHUB_REPOSITORY"));
        }
        try {
            GithubMetrics metrics = resolveMetrics(period);
            BigDecimal value = metrics.valueFor(definition.event_id());
            if (value == null) {
                return Optional.of(errorPayload(definition, period, "Unsupported github metric id"));
            }
            String eventUrl = metrics.eventUrlFor(definition.event_id());
            return Optional.of(buildPayload(definition, period, value, "count", eventUrl));
        } catch (Exception ex) {
            logger.error("Failed to compute GitHub metrics", ex);
            return Optional.of(errorPayload(definition, period, ex.getMessage()));
        }
    }

    private GithubMetrics resolveMetrics(Period period) throws IOException, InterruptedException {
        if (cachedMetrics != null && period.equals(cachedPeriod)) {
            return cachedMetrics;
        }
        String[] parts = repository.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid GITHUB_REPOSITORY value: " + repository);
        }
        String owner = parts[0];
        String name = parts[1];

        GithubMetrics metrics = new GithubMetrics(owner, name);
        metrics.populatePullRequestStats(fetchPullRequests(owner, name, period));
        metrics.openIssuesTotal = fetchOpenIssueTotal(owner, name);
        metrics.openedIssuesCount = fetchIssuesCount(owner, name, period, "CREATED_AT", "createdAt");
        metrics.closedIssuesCount = fetchIssuesCount(owner, name, period, "UPDATED_AT", "closedAt");
        cachedMetrics = metrics;
        cachedPeriod = period;
        return metrics;
    }

    private List<PullRequestStats> fetchPullRequests(String owner, String name, Period period)
            throws IOException, InterruptedException {
        String query = "query($owner:String!, $name:String!, $cursor:String) {"
                + "repository(owner:$owner, name:$name) {"
                + "pullRequests(states:MERGED, first:100, after:$cursor, orderBy:{field:UPDATED_AT, direction:DESC}) {"
                + "pageInfo { hasNextPage endCursor }"
                + "nodes { mergedAt additions deletions }"
                + "}" + "}" + "}";

        LocalDate startDate = period.startDate();
        LocalDate endDate = period.endDate();
        boolean hasNext = true;
        String cursor = null;
        List<PullRequestStats> results = new java.util.ArrayList<>();

        while (hasNext) {
            JsonNode response = graphqlRequest(query, owner, name, cursor);
            JsonNode pulls = response.at("/data/repository/pullRequests");
            JsonNode nodes = pulls.path("nodes");
            for (JsonNode node : nodes) {
                String mergedAt = node.path("mergedAt").asText(null);
                if (mergedAt == null) {
                    continue;
                }
                LocalDate mergedDate = Instant.parse(mergedAt).atZone(ZoneOffset.UTC).toLocalDate();
                if (mergedDate.isBefore(startDate)) {
                    hasNext = false;
                    break;
                }
                if (!mergedDate.isAfter(endDate)) {
                    results.add(new PullRequestStats(
                            node.path("additions").asInt(),
                            node.path("deletions").asInt()
                    ));
                }
            }
            if (hasNext) {
                JsonNode pageInfo = pulls.path("pageInfo");
                hasNext = pageInfo.path("hasNextPage").asBoolean(false);
                cursor = pageInfo.path("endCursor").asText(null);
            }
        }
        return results;
    }

    private int fetchOpenIssueTotal(String owner, String name) throws IOException, InterruptedException {
        String query = "query($owner:String!, $name:String!) {"
                + "repository(owner:$owner, name:$name) {"
                + "issues(states:OPEN) { totalCount }"
                + "}" + "}";
        JsonNode response = graphqlRequest(query, owner, name, null);
        return response.at("/data/repository/issues/totalCount").asInt(0);
    }

    private int fetchIssuesCount(String owner, String name, Period period, String orderField, String dateField)
            throws IOException, InterruptedException {
        String query = "query($owner:String!, $name:String!, $cursor:String) {"
                + "repository(owner:$owner, name:$name) {"
                + "issues(states:[OPEN, CLOSED], first:100, after:$cursor, orderBy:{field:"
                + orderField + ", direction:DESC}) {"
                + "pageInfo { hasNextPage endCursor }"
                + "nodes { createdAt closedAt }"
                + "}" + "}" + "}";

        LocalDate startDate = period.startDate();
        LocalDate endDate = period.endDate();
        boolean hasNext = true;
        String cursor = null;
        int total = 0;

        while (hasNext) {
            JsonNode response = graphqlRequest(query, owner, name, cursor);
            JsonNode issues = response.at("/data/repository/issues");
            JsonNode nodes = issues.path("nodes");
            for (JsonNode node : nodes) {
                String dateValue = node.path(dateField).asText(null);
                if (dateValue == null) {
                    continue;
                }
                LocalDate eventDate = Instant.parse(dateValue).atZone(ZoneOffset.UTC).toLocalDate();
                if (eventDate.isBefore(startDate)) {
                    hasNext = false;
                    break;
                }
                if (!eventDate.isAfter(endDate)) {
                    total++;
                }
            }
            if (hasNext) {
                JsonNode pageInfo = issues.path("pageInfo");
                hasNext = pageInfo.path("hasNextPage").asBoolean(false);
                cursor = pageInfo.path("endCursor").asText(null);
            }
        }
        return total;
    }

    private JsonNode graphqlRequest(String query, String owner, String name, String cursor)
            throws IOException, InterruptedException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("owner", owner);
        variables.put("name", name);
        variables.put("cursor", cursor);
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("variables", variables);

        HttpRequest request = HttpRequest.newBuilder(GRAPHQL_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("GitHub GraphQL request failed with status " + response.statusCode());
        }
        JsonNode body = objectMapper.readTree(response.body());
        if (body.has("errors")) {
            throw new IOException("GitHub GraphQL error: " + body.get("errors").toString());
        }
        return body;
    }

    private MetrikPayload buildPayload(MetricDefinition definition, Period period, BigDecimal value, String unit, String url) {
        return new MetrikPayload(
                "1.0",
                definition.event_id(),
                definition.event_provider(),
                definition.eventName(),
                definition.eventDescription(),
                new PeriodPayload(period.lastPeriodInDays(), period.startDate().toString(), period.endDate().toString()),
                Instant.now(),
                "ok",
                null,
                new EventData(value, unit, null),
                null,
                url
        );
    }

    private MetrikPayload errorPayload(MetricDefinition definition, Period period, String message) {
        return new MetrikPayload(
                "1.0",
                definition.event_id(),
                definition.event_provider(),
                definition.eventName(),
                definition.eventDescription(),
                new PeriodPayload(period.lastPeriodInDays(), period.startDate().toString(), period.endDate().toString()),
                Instant.now(),
                "error",
                message,
                new EventData(null, "count", null),
                null,
                null
        );
    }

    private record PullRequestStats(int additions, int deletions) {
    }

    private static final class GithubMetrics {

        private final String owner;
        private final String name;
        private int prsMergedCount;
        private int prsMergedAdditions;
        private int prsMergedDeletions;
        private int openIssuesTotal;
        private int openedIssuesCount;
        private int closedIssuesCount;

        private GithubMetrics(String owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        private void populatePullRequestStats(List<PullRequestStats> stats) {
            prsMergedCount = stats.size();
            prsMergedAdditions = stats.stream().mapToInt(PullRequestStats::additions).sum();
            prsMergedDeletions = stats.stream().mapToInt(PullRequestStats::deletions).sum();
        }

        private BigDecimal valueFor(String eventId) {
            return switch (eventId) {
                case "github_prs_merged_count" -> BigDecimal.valueOf(prsMergedCount);
                case "github_prs_merged_additions" -> BigDecimal.valueOf(prsMergedAdditions);
                case "github_prs_merged_deletions" -> BigDecimal.valueOf(prsMergedDeletions);
                case "github_issues_open_total" -> BigDecimal.valueOf(openIssuesTotal);
                case "github_issues_opened_count" -> BigDecimal.valueOf(openedIssuesCount);
                case "github_issues_closed_count" -> BigDecimal.valueOf(closedIssuesCount);
                default -> null;
            };
        }

        private String eventUrlFor(String eventId) {
            String base = "https://github.com/" + owner + "/" + name;
            if (eventId.startsWith("github_prs_")) {
                return base + "/pulls";
            }
            if (eventId.startsWith("github_issues_")) {
                return base + "/issues";
            }
            return base;
        }
    }
}
