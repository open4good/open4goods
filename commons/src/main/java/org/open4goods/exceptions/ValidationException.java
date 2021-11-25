package org.open4goods.exceptions;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.Validable.ValidationMessage;

public class ValidationException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 5609156048858109249L;
	private Set<ValidationMessage> result = new HashSet<>();

	public ValidationException(final String string, final Set<ValidationMessage> ret) {
		super(string);
		result = ret;
	}

	public ValidationException(final String string) {
		super(string);
	}

	public Set<ValidationMessage> getResult() {
		return result;
	}

	public void setResult(final Set<ValidationMessage> result) {
		this.result = result;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " : " + StringUtils.join(result, ", ");
	}

}
