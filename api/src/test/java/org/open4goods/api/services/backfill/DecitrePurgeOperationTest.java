package org.open4goods.api.services.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Planning of the Decitre purge (GOU-2266): which keys are removed and saved, and the link with the ingestion
 * exclusions of GOU-2264.
 */
class DecitrePurgeOperationTest {

	private final DecitrePurgeOperation operation = new DecitrePurgeOperation();

	@Test
	void defaultKeysAreTheIngestionExclusionsOfGou2264() {
		VerticalsConfigService configService = new VerticalsConfigService(new SerialisationService(),
				mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());

		assertThat(configService.getConfigByIdOrDefault(null).getAttributesConfig().getExclusions())
				.containsAll(DecitrePurgeOperation.DEFAULT_KEYS);
	}

	@Test
	void readsOnlyTheTargetedKeysOfBooks() {
		assertThat(operation.sourceFields()).hasSize(7).allMatch(field -> field.startsWith("attributes.all."))
				.contains("attributes.all.CLASSIFICATION DECITRE 1", "attributes.all.PROPRIÉTAIRE");
		assertThat(operation.query().term().field()).isEqualTo("gtinInfos.upcType");
		assertThat(operation.query().term().value().stringValue()).isEqualTo("ISBN_13");
	}

	@Test
	void plansTheRemovalOfThePresentKeysOnlyAndSavesThemWhole() {
		Map<String, Object> decitre1 = Map.of("name", "CLASSIFICATION DECITRE 1", "value", "Littérature",
				"sources", List.of(Map.of("datasourceName", "rakuten.com", "value", "Littérature")));
		Map<String, Object> origin = Map.of("name", "LIVRES ORIGINE", "value", "Decitre");
		Map<String, Object> source = Map.of("attributes", Map.of("all",
				Map.of("CLASSIFICATION DECITRE 1", decitre1, "LIVRES ORIGINE", origin)));

		BackfillChange change = operation.plan("9782017148654", source).orElseThrow();

		assertThat(change.script()).isEqualTo(DecitrePurgeOperation.PURGE_SCRIPT);
		assertThat(change.params()).containsEntry("keys", List.of("CLASSIFICATION DECITRE 1", "LIVRES ORIGINE"));
		assertThat(change.backup()).isEqualTo(
				Map.of("CLASSIFICATION DECITRE 1", decitre1, "LIVRES ORIGINE", origin));
		assertThat(change.labels()).containsExactly("CLASSIFICATION DECITRE 1", "LIVRES ORIGINE");
	}

	@Test
	void productWithoutTheKeysOrWithoutAttributesNeedsNoChange() {
		assertThat(operation.plan("1", Map.of())).isEmpty();
		assertThat(operation.plan("2", Map.of("attributes", Map.of()))).isEmpty();
		assertThat(operation.plan("3", Map.of("attributes", Map.of("all", Map.of())))).isEmpty();
	}

	@Test
	void customKeysAreValidated() {
		assertThat(new DecitrePurgeOperation(List.of("ATTRIBUTS", " ", "ATTRIBUTS")).keys()).containsExactly("ATTRIBUTS");
		assertThatThrownBy(() -> new DecitrePurgeOperation(List.of(" "))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new DecitrePurgeOperation(List.of("A.B"))).isInstanceOf(IllegalArgumentException.class);
	}
}
