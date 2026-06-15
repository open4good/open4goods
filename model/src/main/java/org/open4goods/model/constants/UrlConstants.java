package org.open4goods.model.constants;

/**
 * All shared API URL constants are stored here, in the Java String Format template
 * syntax : https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html
 * @author goulven
 *
 */
public class UrlConstants {

	/**
	 * The header or parameter name for the security token
	 */
	public static final String APIKEY_PARAMETER = "Authorization";
	/** Indicates a siteName parameter**/
	public static final String SITENAME_PARAMETER = "siteName";
	public static final String URL_PARAMETER = "url";

	// API Endpoints for datasource informations

	public static final String MASTER_API_DATASOURCES_CONFIG = "/datasources";
	public static final String MASTER_API_DATASOURCE_CONFIG_PREFIX = "/datasource/";
	public static final String MASTER_API_DATASOURCE_CONFIG_NEXT_SCHEDULE_SUFFIX = "/nextFetchingDate";

	public static final String MASTER_API_RELATION_DATA_PREFIX = "/api/relations";

	// Api download endpoint
	public static final String API_DOWNLOAD_ENDPOINT = "/api/download";

	// The path for DataFragments indexation
	public static final String API_INDEXATION_ENDPOINT = "/index";


	public static final String API_STORE_ICO = "/api/images/icon/store/";
	public static final String API_INFO_ENDPOINT = "/api/info";
	public static final String MASTER_API_ASSOCIATION_IMPORT = "/api/associations/fetch";
	public static final String MASTER_API_ASSOCIATION_GET_DEFAULT = "/api/associations/get";
	public static final String MASTER_API_ASSOCIATION_GET_SPECIFIC_IP = "/api/associations/ip";
	public static final String MASTER_API_CAUSE_ROOT = "/api/cause/";
	public static final String MASTER_API_ASSO_SEARCH = "/api/cause/search/";
	public static final String MASTER_API_PRODUCT_AGGREGATE = "api/product/search";
	public static final String MASTER_API_PRODUCT_SEARCH = "api/product/aggregate";
	public static final String API_REVERSEMENT_PROMISE = "/api/promise/";
	public static final String API_TOP_PRODUCTS = "/api/top";



}
