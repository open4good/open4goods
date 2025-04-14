package org.open4goods.model.ai;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AiFieldScanner {

    public static Map<String, String> getGenAiInstruction(Class<?> clazz) {
        return getGenAiInstruction(clazz,  "", new HashSet<>());
    }

    private static Map<String, String> getGenAiInstruction(Class<?> clazz, String prefix, Set<Class<?>> visited) {
        Map<String, String> result = new LinkedHashMap<>();
        if (clazz == null || visited.contains(clazz) || clazz.getName().startsWith("java.")) {
            return result;
        }
        visited.add(clazz);

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            AiGeneratedField annotation = field.getAnnotation(AiGeneratedField.class);
            String fieldName = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();

            if (annotation != null) {
                String value = getFacetValue(annotation);
                result.put(fieldName, value);
            }

            Class<?> fieldType = field.getType();

            // Handle collection with generics
            if (Collection.class.isAssignableFrom(fieldType)) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    Type actualType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                    if (actualType instanceof Class<?>) {
                        result.putAll(getGenAiInstruction((Class<?>) actualType,  fieldName, visited));
                    }
                }
            }
            // Handle nested POJOs
            else if (!fieldType.isPrimitive() && !fieldType.isEnum() && !fieldType.getName().startsWith("java.")) {
                result.putAll(getGenAiInstruction(fieldType,  fieldName, visited));
            }
        }

        return result;
    }

    private static String getFacetValue(AiGeneratedField annotation) {
    	return annotation.instruction();
    	
    }
}
