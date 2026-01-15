package org.open4goods.services.prompt.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-agnostic prompt options used by the GenAI providers.
 */
public class PromptOptions {

    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer seed;
    private Boolean jsonSchemaMode;
    private Long timeoutMs;

    public static PromptOptions fromMap(Map<String, Object> optionsMap, Map<String, Object> providerOptions) {
        PromptOptions options = new PromptOptions();
        if (optionsMap == null) {
            return options;
        }
        Map<String, Object> unknownOptions = new HashMap<>(optionsMap);
        for (Map.Entry<String, Object> entry : optionsMap.entrySet()) {
            String key = normalizeKey(entry.getKey());
            Object value = entry.getValue();
            switch (key) {
                case "model" -> options.setModel(asString(value));
                case "temperature" -> options.setTemperature(asDouble(value));
                case "maxtokens" -> options.setMaxTokens(asInteger(value));
                case "topp" -> options.setTopP(asDouble(value));
                case "seed" -> options.setSeed(asInteger(value));
                case "jsonschemamode" -> options.setJsonSchemaMode(asBoolean(value));
                case "timeoutms" -> options.setTimeoutMs(asLong(value));
                default -> {
                    continue;
                }
            }
            unknownOptions.remove(entry.getKey());
        }
        if (providerOptions != null) {
            providerOptions.clear();
            providerOptions.putAll(unknownOptions);
        }
        return options;
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.replace("-", "").replace("_", "").toLowerCase();
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.valueOf(text);
        }
        return null;
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.valueOf(text);
        }
        return null;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.valueOf(text);
        }
        return null;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public Boolean getJsonSchemaMode() {
        return jsonSchemaMode;
    }

    public void setJsonSchemaMode(Boolean jsonSchemaMode) {
        this.jsonSchemaMode = jsonSchemaMode;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String toString() {
        return "PromptOptions{" +
                "model='" + model + '\'' +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                ", topP=" + topP +
                ", seed=" + seed +
                ", jsonSchemaMode=" + jsonSchemaMode +
                ", timeoutMs=" + timeoutMs +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, temperature, maxTokens, topP, seed, jsonSchemaMode, timeoutMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromptOptions)) return false;
        PromptOptions that = (PromptOptions) o;
        return Objects.equals(model, that.model) &&
                Objects.equals(temperature, that.temperature) &&
                Objects.equals(maxTokens, that.maxTokens) &&
                Objects.equals(topP, that.topP) &&
                Objects.equals(seed, that.seed) &&
                Objects.equals(jsonSchemaMode, that.jsonSchemaMode) &&
                Objects.equals(timeoutMs, that.timeoutMs);
    }
}
