//package org.open4goods.api.services.aggregation.services.realtime.parser;
//
//import org.open4goods.api.services.BatchService;
//import org.open4goods.model.attribute.ProductAttribute;
//import org.open4goods.model.attribute.SourcedAttribute;
//import org.open4goods.model.exceptions.ParseException;
//import org.open4goods.model.vertical.AttributeConfig;
//import org.open4goods.model.vertical.AttributeParser;
//import org.open4goods.model.vertical.VerticalConfig;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * A specific parser for weight, that operates best effort matching to return weight in kilograms
// */
//public class WeightParser extends AttributeParser {
//
//	protected static final Logger logger = LoggerFactory.getLogger(WeightParser.class);
//
//	@Override
//	public String parse(ProductAttribute attribute, AttributeConfig attributeConfig, VerticalConfig verticalConfig) throws ParseException {
//
//		Double weightInGrams = null;
//
//		for (SourcedAttribute e : attribute.getSource()){
//			Double actualWeightInGrams = getWeightInGrams(e.getValue(), attributeConfig,verticalConfig);
//
//			if (null == actualWeightInGrams) {
//				continue;
//			}
//
//			if (null == weightInGrams) {
//				weightInGrams = actualWeightInGrams;
//			}
//
//			// Comparing
//			if (weightInGrams.doubleValue() != actualWeightInGrams.doubleValue()) {
//				// TODO(P1, design) : exclude, alarm, get an election mechanism...
//				logger.warn("Conflict : {} <> {}", weightInGrams, e );
//			}
//
//
//		}
//		return String.valueOf(weightInGrams);
//	}
//
//	public String parse(String value, AttributeConfig attributeConfig, VerticalConfig verticalConfig) throws ParseException {
//		return String.valueOf(getWeightInGrams(value, attributeConfig, verticalConfig));
//	}
//
//	private Double getWeightInGrams(String rawValue, AttributeConfig attributeConfig, VerticalConfig verticalConfig) {
//
//		Double value = Double.valueOf(rawValue.replace(",", "."));
//
//		if (rawValue.contains(".")) {
//			// There is a coma / dot, this is so expressed in kg
//			return Double.valueOf(rawValue);
//		}
//
//
//		// Applying upper born
//		// TODO : from conf, could depend on the vertical
//		if (value > 200.0) {
//			return value / 1000;
//		}
//
//		// Applying lower born
//		// TODO : from conf, could depend on the vertical
//		if (value < 50) {
//			return value ;
//		}
//
//
//		// TODO : Should discard ?
//		 logger.info("Uncertain weight type for {}", e);
//
//		return value;
//	}
//
//}
