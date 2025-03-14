package org.open4goods.ui.config.yml;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
@Validated
public class ReversementConfig {

	/**
	 * The list of reversements (cashback) to ecological organisations that have
	 * already been operated
	 */
	private List<Reversement> reversements = new ArrayList<>();

	/**
	 * The list of available (and authorised) organisations, by keys
	 * TODO : I18n
	 */

	Map<String, ContributedOrganisation> contributedOrganisations = new HashMap<>();
	
	/**
	 * get the total amount of reversements
	 * @return
	 */
	public Double getTotalReversements() {
		return reversements.stream().mapToDouble(Reversement::getAmount).sum();
	}
	
	/**
	 * 	* get the total amount of reversed organisations
	 * @return
	 */
	public int getDistingReversementsOrganisation() {
		return reversements.stream().map(Reversement::getOrgKey).distinct().toArray().length;
	}
	
	/**
	 * 
	 * @return LocalDate the LocalDate corresponding to the latest reversement 
	 */
	public LocalDate getLastReversementDate() {
		return getReversements().stream()
				.map(e->e.getDate())
				.max(LocalDate::compareTo)
				// If no reversements defined, set it late
				.orElse(LocalDate.MIN);
	}

	/**
	 * 
	 * @return The epoch corresponding to the latest reversement
	 */
	public long getLastReversementEpoch() {
		return getLastReversementDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
	}
	
	public List<Reversement> getReversements() {
		return reversements;
	}

	public void setReversements(List<Reversement> reversements) {
		this.reversements = reversements;
	}

	public Map<String, ContributedOrganisation> getContributedOrganisations() {
		return contributedOrganisations;
	}

	public void setContributedOrganisations(Map<String, ContributedOrganisation> contributedOrganisations) {
		this.contributedOrganisations = contributedOrganisations;
	}

	
}
