package org.open4goods.nudgerfrontapi.controller.api;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.model.Localisable;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.vertical.NudgeToolConfig;
import org.open4goods.model.vertical.NudgeToolScore;
import org.open4goods.model.vertical.NudgeToolSubsetGroup;
import org.open4goods.model.vertical.SubsetCriteria;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.assistant.AssistantConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.NudgeToolConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.NudgeToolScoreDto;
import org.open4goods.nudgerfrontapi.dto.category.NudgeToolSubsetGroupDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalSubsetDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing assistant configurations derived from YAML resources.
 */
@RestController
@RequestMapping("/assistant-configs")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Assistants", description = "Expose YAML-based assistant configurations for the frontend.")
public class AssistantConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssistantConfigController.class);

    private final VerticalsConfigService verticalsConfigService;

    public AssistantConfigController(VerticalsConfigService verticalsConfigService) {
        this.verticalsConfigService = verticalsConfigService;
    }

    /**
     * List all available assistant configurations.
     *
     * @param domainLanguage language used to localise the returned fields
     * @return the assistant configurations with their identifiers
     */
    @GetMapping
    @Operation(
            summary = "List assistant configurations",
            description = "Return YAML-based assistant configurations available for the nudge tool wizard.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Assistant configurations returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for the response"),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AssistantConfigDto.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<AssistantConfigDto>> listAssistants(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Listing assistant configurations for {}", domainLanguage);
        List<AssistantConfigDto> body = verticalsConfigService.getAssistantConfigs().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> new AssistantConfigDto(entry.getKey(), mapNudgeToolConfig(entry.getValue(), domainLanguage)))
                .filter(dto -> dto.config() != null)
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(body);
    }

    /**
     * Retrieve an assistant configuration by identifier.
     *
     * @param assistantId identifier of the assistant to retrieve
     * @param domainLanguage language used to localise the returned fields
     * @return the assistant configuration, or 404 when missing
     */
    @GetMapping("/{assistantId}")
    @Operation(
            summary = "Get assistant configuration",
            description = "Return the assistant configuration identified by its id.",
            parameters = {
                    @Parameter(name = "assistantId", in = ParameterIn.PATH, required = true,
                            description = "Identifier of the assistant to retrieve.",
                            schema = @Schema(type = "string", example = "tv")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Assistant configuration returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for the response"),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = NudgeToolConfigDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid assistant id"),
                    @ApiResponse(responseCode = "404", description = "Assistant not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<NudgeToolConfigDto> getAssistant(
            @PathVariable("assistantId") String assistantId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Fetching assistant configuration {} for {}", assistantId, domainLanguage);
        if (!StringUtils.hasText(assistantId)) {
            LOGGER.warn("Assistant id is required");
            return ResponseEntity.badRequest().build();
        }
        NudgeToolConfig config = verticalsConfigService.getAssistantConfigById(assistantId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(mapNudgeToolConfig(config, domainLanguage));
    }

    /**
     * Map the backend nudge tool configuration to its DTO representation.
     *
     * @param nudgeToolConfig the configuration to map
     * @param domainLanguage language used for localisation
     * @return mapped DTO or {@code null} if the input is null
     */
    private NudgeToolConfigDto mapNudgeToolConfig(NudgeToolConfig nudgeToolConfig, DomainLanguage domainLanguage) {
        if (nudgeToolConfig == null) {
            return null;
        }

        List<NudgeToolScoreDto> scores = defaultList(nudgeToolConfig.getScores()).stream()
                .map(score -> mapNudgeToolScore(score, domainLanguage))
                .filter(Objects::nonNull)
                .toList();

        return new NudgeToolConfigDto(scores,
                mapVerticalSubsets(nudgeToolConfig.getSubsets(), domainLanguage),
                mapNudgeToolSubsetGroups(nudgeToolConfig.getSubsetGroups(), domainLanguage));
    }

    /**
     * Map a score entry to the DTO used by the frontend.
     *
     * @param score the score configuration
     * @param domainLanguage language used for localisation
     * @return mapped score DTO or {@code null} if the input is null
     */
    private NudgeToolScoreDto mapNudgeToolScore(NudgeToolScore score, DomainLanguage domainLanguage) {
        if (score == null) {
            return null;
        }
        return new NudgeToolScoreDto(
                score.getScoreName(),
                score.getScoreMinValue(),
                score.getFromPercent(),
                score.getToPercent(),
                score.getMdiIcon(),
                score.getDisabled(),
                localise(score.getTitle(), domainLanguage),
                localise(score.getDescription(), domainLanguage));
    }

    /**
     * Map subset group definitions to DTOs.
     *
     * @param subsetGroups configured groups
     * @param domainLanguage language used for localisation
     * @return list of mapped group DTOs
     */
    private List<NudgeToolSubsetGroupDto> mapNudgeToolSubsetGroups(List<NudgeToolSubsetGroup> subsetGroups,
            DomainLanguage domainLanguage) {
        return defaultList(subsetGroups).stream()
                .map(group -> mapNudgeToolSubsetGroup(group, domainLanguage))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Map a subset group to the DTO used by the frontend.
     *
     * @param group group configuration
     * @param domainLanguage language used for localisation
     * @return mapped group DTO or {@code null} if the input is null
     */
    private NudgeToolSubsetGroupDto mapNudgeToolSubsetGroup(NudgeToolSubsetGroup group, DomainLanguage domainLanguage) {
        if (group == null) {
            return null;
        }
        return new NudgeToolSubsetGroupDto(
                group.getId(),
                localise(group.getTitle(), domainLanguage),
                localise(group.getDescription(), domainLanguage),
                group.getMdiIcon(),
                group.getLayout(),
                localise(group.getCtaLabel(), domainLanguage));
    }

    /**
     * Map vertical subsets to DTOs.
     *
     * @param subsets configured subsets
     * @param domainLanguage language used for localisation
     * @return list of mapped subset DTOs
     */
    private List<VerticalSubsetDto> mapVerticalSubsets(List<VerticalSubset> subsets, DomainLanguage domainLanguage) {
        return defaultList(subsets).stream()
                .map(subset -> mapVerticalSubset(subset, domainLanguage))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Map a single subset to its DTO, ensuring percentile operators are applied.
     *
     * @param subset the subset configuration
     * @param domainLanguage language used for localisation
     * @return mapped subset DTO or {@code null} if the input is null
     */
    private VerticalSubsetDto mapVerticalSubset(VerticalSubset subset, DomainLanguage domainLanguage) {
        if (subset == null) {
            return null;
        }

        List<SubsetCriteria> criterias = defaultList(subset.getCriterias()).stream()
                .peek(criteria -> {
                    if ((criteria.getFromPercent() != null || criteria.getToPercent() != null)
                            && criteria.getOperator() == null) {
                        criteria.setOperator(SubsetCriteriaOperator.RANKING_PERCENTILE);
                    }
                })
                .toList();

        return new VerticalSubsetDto(
                subset.getId(),
                subset.getGroup(),
                criterias,
                subset.getImage(),
                localise(subset.getUrl(), domainLanguage),
                localise(subset.getCaption(), domainLanguage),
                localise(subset.getTitle(), domainLanguage),
                localise(subset.getDescription(), domainLanguage));
    }

    /**
     * Localise a localisable map into the requested language tag.
     *
     * @param localisable map of language keys to values
     * @param domainLanguage language requested by the caller
     * @return localised value or {@code null} if the map is null
     */
    private String localise(Localisable<String, String> localisable, DomainLanguage domainLanguage) {
        if (localisable == null) {
            return null;
        }
        String languageTag = domainLanguage == null ? null : domainLanguage.languageTag();
        return localisable.i18n(languageTag);
    }

    /**
     * Ensure list values are never null.
     *
     * @param list input list
     * @param <T> list element type
     * @return a non-null list
     */
    private <T> List<T> defaultList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
