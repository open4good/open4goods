//package org.open4goods.filter;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Properties;
//import java.util.Set;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.concurrent.ConcurrentHashMap;
//
//import javax.servlet.Filter;
//import javax.servlet.FilterChain;
//import javax.servlet.FilterConfig;
//import javax.servlet.ServletException;
//import javax.servlet.ServletRequest;
//import javax.servlet.ServletResponse;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.http.HttpStatus;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.subtitle.commons.exceptions.FetchException;
//import com.subtitle.commons.helper.UrlHelper;
//
///**
// * This filter limits the requests per IP or per token<br/>
// * -> A global Fixed number of hits for arbitraries IP (public calls) <br/>
// * -> Whitelist (for bots) on IP list basis -> Specific quotas on a token basis
// *
// * @author goulven
// *
// */
//public class QuotasFilter implements Filter {
//
//    /*
//     * The QUOTA properties
//     */
//    private static final Properties config = new Properties();
//    private static final String QUOTA_PROPERTY_FILE = "/quotas.properties";
//    private static final String DEFAULT_ALLOWED_REQUEST_ATTR = "default.request.limit";
//
//    /**
//     * Header or parameter name that contains the identifying token
//     **/
//    public static final String TOKEN_ATTRIBUTE = "AUTH_TOKEN";
//
//    /** The duration of the quota limit **/
//    private static final int QUOTAS_DELAY_IN_HOURS = 24;
//
//    /** The default allowed request. Redefined by conf) */
//    private static Long DEFAULT_QUOTA = 500L;
//
//    /** The Logger, log in quotas.log **/
//    private static final Logger LOGGER = LoggerFactory.getLogger(QuotasFilter.class);
//
//    /**
//     * The map that stores IP / access count
//     */
//    private static Map<String, Long> accessed = new HashMap<>();
//
//    /**
//     * The allowed quotas map, per tokens
//     */
//    private static Map<String, Long> tokenQuotas = new ConcurrentHashMap<String, Long>();
//
//    /**
//     * The set that contains all whitelist IP's (bots)
//     */
//    private static Set<String> ipWhiteList = new HashSet<>();
//
//    /**
//     * * Loads http://www.iplists.com/ as white list
//     *
//     * @param url
//     * @throws FetchException
//     */
//    private void addIps(final String url) throws FetchException {
//        final String content = UrlHelper.readUrl(url);
//
//        final String[] lines = content.split("\n");
//        for (String line : lines) {
//            line = line.trim();
//            if (!StringUtils.isEmpty("line") && !line.startsWith("#")) {
//                // Adding this ip
//                ipWhiteList.add(line);
//            }
//        }
//    }
//
//    @Override
//    public void destroy() {
//
//    }
//
//    @Override
//    public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain) throws ServletException, IOException {
//
//        // ////////////////////////////////////////////////////////////////
//        // Retrieving commons used infos, parameters,
//        // ////////////////////////////////////////////////////////////////
//
//        // Casting for conveniency
//        final HttpServletRequest request = (HttpServletRequest) req;
//        final HttpServletResponse response = (HttpServletResponse) resp;
//
//        final String ip = request.getRemoteAddr();
//
//        // /////////////////////////////////////////////
//        // Checking if the requester is in a trusted
//        // ip range
//        // /////////////////////////////////////////////
//        if (isFullAccess(ip)) {
//            chain.doFilter(req, response);
//            LOGGER.info("{} GRANTED ON {} WITH UNLIMITED QUOTAS.", ip, request.getRequestURL());
//            return;
//        }
//
//        // /////////////////////////////////////////////
//        // Retrieves the TOKEN from header
//        // or attribute if any
//        // /////////////////////////////////////////////
//
//        String token = request.getParameter(TOKEN_ATTRIBUTE);
//        if (StringUtils.isEmpty(token)) {
//            token = request.getHeader(token);
//        }
//
//        Long maxHit = 0L;
//        // Getting and incrementing the hit count for this ip
//        final Long actualHit = accessed.getOrDefault(ip, 0L) + 1;
//        accessed.put(ip, actualHit);
//
//        if (!StringUtils.isEmpty(token)) {
//            // ////////////////////////////////////////////////////////////////
//            // A TOKEN IS PROVIDED
//            // ////////////////////////////////////////////////////////////////
//
//            // Get the allowed quotas for this host
//            maxHit = tokenQuotas.get(token);
//
//            if (null == maxHit) {
//                maxHit = DEFAULT_QUOTA;
//                LOGGER.warn("{}  ON {} TRYED AN UNEXPECTED TOKEN : {}", ip, request.getRequestURL(), token);
//            }
//        } else {
//            // ////////////////////////////////////////////////////////////////
//            // A token is not provided.
//            // ////////////////////////////////////////////////////////////////
//            maxHit = DEFAULT_QUOTA;
//        }
//
//        // //////////////////////////////////////////
//        // Filling informational headers
//        // //////////////////////////////////////////
//        response.addHeader("hits-total", actualHit.toString());
//        Long remaining = maxHit - actualHit;
//        if (remaining < 0) {
//            remaining = 0L;
//        }
//        response.addHeader("hits-remaining", String.valueOf(remaining));
//
//        if (actualHit > maxHit) {
//            // QUOTA IS REACHED
//            response.sendError(HttpStatus.SC_FORBIDDEN, ip + " did " + actualHit + " requests. Only " + maxHit + " are allowed every " + QUOTAS_DELAY_IN_HOURS + " hours. Please contact us if you need a higher limit");
//            LOGGER.info("{} (token : {}) DENIED ON {} : {} > {} ", ip, token, request.getRequestURL(), actualHit, maxHit);
//            return;
//        } else {
//            chain.doFilter(req, response);
//            LOGGER.info("{} (token : {}) GRANTED ON {} : {} > {} ", ip, token, request.getRequestURL(), actualHit, maxHit);
//        }
//    }
//
//    @Override
//    public void init(final FilterConfig arg0) throws ServletException {
//        // //////////////////////////////////////////////////////////
//        // Loading the core configuration
//        // //////////////////////////////////////////////////////////
//
//        try {
//            LOGGER.info("Loading QUOTA properties");
//            config.load(QuotasFilter.class.getResourceAsStream(QUOTA_PROPERTY_FILE));
//            DEFAULT_QUOTA = Long.valueOf(config.get(DEFAULT_ALLOWED_REQUEST_ATTR).toString());
//
//            // Getting all the keys, except the previous one. They are the tokens / allowed hits
//            for (final Object key : config.keySet()) {
//                if (key.equals(DEFAULT_ALLOWED_REQUEST_ATTR)) {
//                    continue;
//                }
//                LOGGER.info("Setting quota to {} for {}", key.toString(), config.get(key).toString());
//                tokenQuotas.put(key.toString(), Long.valueOf(config.get(key).toString()));
//            }
//
//        } catch (final Exception e) {
//            LOGGER.error("Error while reading quotas.properties", e);
//        }
//
//        // //////////////////////////////////////////////////////////
//        // Load the to be trusted IP's from http://www.iplists.com/
//        // //////////////////////////////////////////////////////////
//        try {
//            LOGGER.info("Loading IP white list");
//            addIps("http://www.iplists.com/google.txt");
//            addIps("http://www.iplists.com/inktomi.txt");
//            addIps("http://www.iplists.com/lycos.txt");
//            addIps("http://www.iplists.com/infoseek.txt");
//            addIps("http://www.iplists.com/altavista.txt");
//            addIps("http://www.iplists.com/excite.txt");
//            addIps("http://www.iplists.com/northernlight.txt");
//            addIps("http://www.iplists.com/misc.txt");
//
//            LOGGER.info("BOT IP White list loaded, {} have unlimited access to the API", ipWhiteList.size());
//        } catch (final Exception e) {
//            LOGGER.error("Cannot retrieve IP's whitelist ", e);
//        }
//
//        // //////////////////////////////////////////////////////////
//        // Scheduling the IP map cleaning every 24 hours
//        // //////////////////////////////////////////////////////////
//        LOGGER.info("Scheduling the job cleaning");
//        new Timer().scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                LOGGER.info("Clearing the IP / HITS map. {} entries are removed", accessed.size());
//                accessed.clear();
//            }
//        }, 0L, 3600 * 10000 * QUOTAS_DELAY_IN_HOURS);
//    }
//
//    /**
//     * Check if the IP can access with no quotas limit
//     *
//     * @param ip
//     * @return
//     */
//    private Boolean isFullAccess(final String ip) {
//        return ipWhiteList.contains(ip);
//    }
//
//}