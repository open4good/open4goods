package org.open4goods.commons.services;
//
//package org.open4goods.services;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.Charset;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.http.HttpHost;
//import org.apache.http.client.fluent.Executor;
//import org.apache.http.client.fluent.Request;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import org.open4goods.api.config.yml.ApiConfig;
//import org.open4goods.model.consts.Currency;
//import org.open4goods.model.data.Price;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import io.micrometer.core.annotation.Timed;
//
///**
// * This service will retrieves currency conversions rate in a scheduled way and
// * offer cached access to it. It also provides a fallback if API is not
// * availlable. We are based on a EUR conversion
// *
// * @author goulven
// * TODO(gof) : A specific currencyservice
// */
//@EnableScheduling
//public class CurrencyStandardiserService extends StandardiserService {
//
//	private static final String EXCHANGERATES_FILENAME = "exchangerates.json";
//
//	private final static Logger log = LoggerFactory.getLogger(CurrencyStandardiserService.class);
//
////	private static ObjectMapper mapper = new ObjectMapper();
//
//	@Autowired
//	private ApiConfig config;
//
//	/** The map containing the 1 EUR conversions to other currencies **/
//	private final Map<Currency, Double> values = new ConcurrentHashMap<>();
//
//	@Override
//	public void standarise(final Price price, final Currency currency) {
//		if (price.getCurrency() != currency) {
//			price.setPrice(convert(price.getPrice(), price.getCurrency(), currency));
//			price.setCurrency(currency);
//		}
//
//	}
//
//	/**
//	 * Convert one price into another currency
//	 *
//	 * @param price
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public Double convert(final Double price, final Currency from, final Currency to) {
//
//		Double value = price;
//
//		if (from != StandardiserService.DEFAULT_CURRENCY) {
//			// Convert to base
//			value = convert(price, StandardiserService.DEFAULT_CURRENCY, from);
//		}
//
//		// Now convert
//		if (!values.containsKey(to)) {
//			updateCurrencies();
//		}
//
//		return value * values.get(to);
//
//	}
//
//	@Scheduled(initialDelay = 0, fixedDelay = 3600 * 1000 * 6)
//	@Timed(value="service.CurrencyStandardiserService.updateCurrencies()",description="Informations about currencies update against openexchangerates.com")
//	public void updateCurrencies() {
//
//		log.info("Will update currencies");
//		String response = null;
//		try {
//			// Requesting the WS
//			if (!StringUtils.isEmpty(config.getProxyhost())) {
//
//				// If proxy
//				response = Executor.newInstance()
//						.auth(new HttpHost(config.getProxyhost(), config.getProxyport()), config.getProxyusername(),
//								config.getProxypassword())
//						.authPreemptiveProxy(new HttpHost(config.getProxyhost(), config.getProxyport()))
//						.execute(Request
//								.Get("https://openexchangerates.org/api/latest.json?app_id="
//										+ config.getOpenexchangeratesid())
//								.viaProxy(new HttpHost(config.getProxyhost(), config.getProxyport())))
//						.returnContent().asString();
//			} else {
//				response = Executor.newInstance()
//
//						.execute(Request.Get("https://openexchangerates.org/api/latest.json?app_id="
//								+ config.getOpenexchangeratesid()))
//						.returnContent().asString();
//			}
//
//			// Writing the response on disk for further callbacks
//
//			FileUtils.write(new File(config.getSnapShotsFolder() + "/" + EXCHANGERATES_FILENAME), response,
//					Charset.defaultCharset());
//
//		} catch (final Exception e) {
//			log.error("Unable to retrieve currency exchange rates : ({}). Trying to fallback on local copy",
//					e.getMessage());
//
//			try {
//				response = FileUtils.readFileToString(
//						new File(config.getSnapShotsFolder() + "/" + EXCHANGERATES_FILENAME), Charset.defaultCharset());
//			} catch (final IOException e1) {
//				log.error("Unable to retrieve currency from local copy  ! ({}).", e1.getMessage());
//			}
//		}
//
//		if (StringUtils.isEmpty(response)) {
//			return;
//		}
//
//		try {
//			final JsonNode node = mapper.readTree(response);
//
//			// The API provides USD based
//
//			final Double eurRate = 1 / Double.valueOf(node.at("/rates/" + StandardiserService.DEFAULT_CURRENCY).asText());
//
//			for (final Currency c : Currency.values()) {
//
//				try {
//					values.put(c, eurRate * Double.valueOf(node.at("/rates/" + c.toString()).asText()));
//				} catch (final Exception e) {
//					log.error("Error handling currency : {} > {}", c, e.getMessage());
//				}
//			}
//
//		} catch (final IOException e) {
//			log.error("Unable to parse json : {}", e.getMessage());
//			return;
//		}
//	}
//
//}
