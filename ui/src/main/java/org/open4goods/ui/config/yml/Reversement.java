package org.open4goods.ui.config.yml;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * POJO representing a reversement to an organization
 */

@Validated
public class Reversement {
	
	@DateTimeFormat(pattern = "yyyy:MM:dd")
	@NotNull
	private LocalDate date;

	@NotEmpty
	private String orgKey;
	
	@Min(1)
	private double amount;

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getOrgKey() {
		return orgKey;
	}

	public void setOrgKey(String orgName) {
		this.orgKey = orgName;
	}

	

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	

}
