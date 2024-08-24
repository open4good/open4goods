package org.open4goods.commons.config;
import org.open4goods.commons.model.constants.CacheConstants;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;

@Component(CacheConstants.KEY_GENERATOR)
public class CacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // Get the target class name
        String className = target.getClass().getSimpleName();
        // Get the method name
        String methodName = method.getName();
        
        // Use StringJoiner to create the key with colon as delimiters
        StringJoiner key = new StringJoiner(":");
        key.add(className);
        key.add(methodName);
        
        // Add method parameters to the key
        for (Object param : params) {
            key.add(param == null ? "null" : param.toString());
        }
        
        return key.toString();
    }
}