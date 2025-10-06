package org.open4goods.model;

import java.lang.reflect.Method;

import org.open4goods.model.constants.CacheConstants;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

@Component(CacheConstants.KEY_GENERATOR)
public class CacheKeyGenerator implements KeyGenerator {
	@Override
	public Object generate(Object target, Method method, Object... params) {
		return (params == null || params.length == 0) ? method : new MethodParamsKey(method, params);
	}

	// Only used when there are parameters
	static final class MethodParamsKey {
		private final Method method;
		private final Object[] params;
		private final int hash;

		MethodParamsKey(Method method, Object[] params) {
			this.method = method;
			// clone top-level array to avoid accidental external mutation of the varargs
			// array itself
			this.params = params.clone();
			this.hash = 31 * method.hashCode() + java.util.Arrays.deepHashCode(this.params);
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof MethodParamsKey other))
				return false;
			return method.equals(other.method) && java.util.Arrays.deepEquals(this.params, other.params);
		}

		@Override
		public String toString() {
			return method.toGenericString() + java.util.Arrays.deepToString(params);
		}
	}

}
