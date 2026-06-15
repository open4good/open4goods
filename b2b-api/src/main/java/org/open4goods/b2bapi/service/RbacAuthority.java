package org.open4goods.b2bapi.service;

/**
 * Spring Security authority names used by Product Data API dashboard sessions.
 */
public final class RbacAuthority {

    public static final String DASHBOARD = "PDAPI_DASHBOARD";
    public static final String PLATFORM_ADMIN = "PDAPI_PLATFORM_ADMIN";
    public static final String ORG_OWNER = "PDAPI_ORG_OWNER";
    public static final String ORG_ADMIN = "PDAPI_ORG_ADMIN";
    public static final String ORG_DEVELOPER = "PDAPI_ORG_DEVELOPER";
    public static final String ORG_BILLING = "PDAPI_ORG_BILLING";

    private RbacAuthority() {
    }
}
