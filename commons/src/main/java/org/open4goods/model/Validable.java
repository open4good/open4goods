package org.open4goods.model;

import org.open4goods.exceptions.ValidationException;

public interface Validable {

	class ValidationMessage {
		/** Reference of the validated object **/
		private String ref;
		private String msg;
		private long date;

		public static ValidationMessage newValidationMessage(final String ref) {
			final ValidationMessage ret = new ValidationMessage();
			ret.setDate(System.currentTimeMillis());
			ret.setRef(ref);
			ret.setMsg(ref);
			return ret;
		}

		@Override
		public String toString() {

			return msg;
		}

		public static ValidationMessage newValidationMessage(final String ref, final String msg) {
			final ValidationMessage ret = newValidationMessage(ref);
			ret.setMsg(msg);
			return ret;
		}

		public String getRef() {
			return ref;
		}

		public void setRef(final String ref) {
			this.ref = ref;
		}

		public String getMsg() {
			return msg;
		}

		public void setMsg(final String msg) {
			this.msg = msg;
		}

		public long getDate() {
			return date;
		}

		public void setDate(final long date) {
			this.date = date;
		}

	}

	void validate() throws ValidationException;

}
