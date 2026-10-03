package org.open4goods.datareference.port;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.grouping.GroupConfirmation;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupIndexEntry;
import org.open4goods.datareference.model.grouping.GroupMember;
import org.open4goods.datareference.service.GroupIndexer;

/**
 * Test-only reference implementation of the GOU-199 query ports, built once
 * from a surface's full membership.
 *
 * <p>Backs {@link GroupLookupPort}, {@link GtinGroupLookupPort} and {@link
 * GroupSearchPort} from the same {@link GroupIndexer#index} output used in
 * production, plus a per-GTIN reverse map kept outside the compact entries:
 * {@link GroupIndexEntry#representativeGtins()} is a capped sample and must
 * never be used to answer a GTIN-to-groups query. This type lives under
 * {@code src/test} because no persistence layer exists in this source-neutral
 * module (GOU-199); a consuming service backs these same ports with a real
 * store.
 */
public final class InMemoryGroupIndex implements GroupLookupPort, GtinGroupLookupPort, GroupSearchPort {

    private final Map<ProjectionSurface, Map<GroupId, GroupIndexEntry>> entriesBySurface = new EnumMap<>(ProjectionSurface.class);
    private final Map<ProjectionSurface, Map<Gtin, List<GroupId>>> membershipBySurface = new EnumMap<>(ProjectionSurface.class);

    /**
     * Indexes one surface's full confirmed membership.
     *
     * @param surface surface this membership was resolved for
     * @param members every GTIN leaf's confirmed membership on that surface
     * @param evidenceVersion rule or registry version that produced the membership
     */
    public void index(ProjectionSurface surface, List<GroupMember> members, RuleVersion evidenceVersion) {
        Objects.requireNonNull(surface, "surface must not be null");
        List<GroupIndexEntry> entries = new GroupIndexer().index(members, evidenceVersion);

        Map<GroupId, GroupIndexEntry> byId = new LinkedHashMap<>();
        entries.forEach(entry -> byId.put(entry.groupId(), entry));
        entriesBySurface.put(surface, byId);

        Map<Gtin, List<GroupId>> byGtin = new LinkedHashMap<>();
        for (GroupMember member : members) {
            byGtin.computeIfAbsent(member.gtin(), gtin -> new ArrayList<>()).add(member.groupId());
        }
        membershipBySurface.put(surface, byGtin);
    }

    /**
     * Adds unreviewed candidate entries to one surface's index, for prefix
     * search only: a candidate never appears in {@link #findByGtin}, since it is
     * not a confirmed membership.
     *
     * @param surface surface to add the candidates to
     * @param candidates entries that must all carry {@link GroupConfirmation#CANDIDATE}
     */
    public void indexCandidates(ProjectionSurface surface, List<GroupIndexEntry> candidates) {
        Objects.requireNonNull(surface, "surface must not be null");
        Map<GroupId, GroupIndexEntry> byId =
                entriesBySurface.computeIfAbsent(surface, ignored -> new LinkedHashMap<>());
        for (GroupIndexEntry candidate : candidates) {
            if (candidate.confirmation() != GroupConfirmation.CANDIDATE) {
                throw new IllegalArgumentException("indexCandidates requires CANDIDATE entries: " + candidate.groupId());
            }
            byId.put(candidate.groupId(), candidate);
        }
    }

    @Override
    public Optional<GroupIndexEntry> find(GroupId groupId, ProjectionSurface surface) {
        return Optional.ofNullable(entriesBySurface.getOrDefault(surface, Map.of()).get(groupId));
    }

    @Override
    public List<GroupIndexEntry> findAll(List<GroupId> groupIds, ProjectionSurface surface) {
        Map<GroupId, GroupIndexEntry> byId = entriesBySurface.getOrDefault(surface, Map.of());
        List<GroupIndexEntry> found = new ArrayList<>();
        for (GroupId groupId : groupIds) {
            GroupIndexEntry entry = byId.get(groupId);
            if (entry != null) {
                found.add(entry);
            }
        }
        return List.copyOf(found);
    }

    @Override
    public List<GroupIndexEntry> findByGtin(Gtin gtin, ProjectionSurface surface) {
        Map<GroupId, GroupIndexEntry> byId = entriesBySurface.getOrDefault(surface, Map.of());
        List<GroupId> groupIds = membershipBySurface.getOrDefault(surface, Map.of()).getOrDefault(gtin, List.of());
        List<GroupIndexEntry> found = new ArrayList<>();
        for (GroupId groupId : groupIds) {
            GroupIndexEntry entry = byId.get(groupId);
            if (entry != null) {
                found.add(entry);
            }
        }
        return List.copyOf(found);
    }

    @Override
    public ScanPage<GroupIndexEntry> searchByPrefix(GroupSearchRequest request, ProjectionSurface surface) {
        Objects.requireNonNull(request, "request must not be null");
        List<GroupIndexEntry> matches = entriesBySurface.getOrDefault(surface, Map.of()).values().stream()
                .filter(entry -> entry.groupId().type() == request.type())
                .filter(entry -> entry.searchTokens().stream().anyMatch(token -> token.startsWith(request.prefix())))
                .sorted(Comparator.comparing(entry -> entry.groupId().externalForm()))
                .toList();

        String resumeAfter = request.cursor().map(ScanCursor::token).orElse(null);
        List<GroupIndexEntry> page = new ArrayList<>();
        boolean resumed = resumeAfter == null;
        for (GroupIndexEntry entry : matches) {
            if (!resumed) {
                if (entry.groupId().externalForm().equals(resumeAfter)) {
                    resumed = true;
                }
                continue;
            }
            if (page.size() == request.pageSize()) {
                return new ScanPage<>(page, Optional.of(new ScanCursor(page.get(page.size() - 1).groupId().externalForm())),
                        List.of());
            }
            page.add(entry);
        }
        return ScanPage.last(page);
    }
}
