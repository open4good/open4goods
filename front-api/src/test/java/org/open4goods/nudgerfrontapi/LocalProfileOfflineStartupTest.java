package org.open4goods.nudgerfrontapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kohsuke.github.GHRepository;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.services.feedback.service.GitHubIssueService;
import org.open4goods.services.geocode.service.GeoNamesIndexService;
import org.open4goods.services.geocode.service.IpGeolocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Guards front-api's "local" profile offline startup contract (see README,
 * "Running Locally (Offline Mode)"): the context must start with no network, no token and no
 * pre-existing cache file, and {@code /actuator/health} must respond.
 *
 * <p>Deliberately named with the {@code Test} suffix rather than the {@code IT} suffix the
 * design plan suggested: this repository's {@code maven-failsafe-plugin} declaration has no
 * {@code <executions>} bound to the {@code integration-test}/{@code verify} phases, so
 * {@code *IT.java} classes (see {@code SharedTokenFilterIT}, {@code ProductControllerIT}, ...)
 * are never picked up by {@code mvn install} / CI (confirmed by running {@code mvn --offline
 * -pl front-api test} and observing they do not appear in the Surefire run). Naming this class
 * with the {@code Test} suffix is what actually makes it an enforced regression guard.
 *
 * <p>No {@code @MockitoBean} is used: every bean below is the real one the "local" profile
 * wires. The GeoNames/MaxMind dataset URLs are pointed at the IPv4 "TEST-NET-3" documentation
 * range (RFC 5737, 203.0.113.0/24): those addresses are reserved and never routed, so the
 * download deterministically fails without depending on the test host's actual network
 * reachability or DNS resolution - exercising the exact fail-soft path
 * {@link GeoNamesIndexService#initialize()} must take instead of failing startup. This is the
 * documented fallback for the plan's preferred mechanism (a loopback-only
 * {@code InetAddressResolverProvider}): that SPI is a JVM-wide resolver, and this module's
 * Surefire configuration reuses one forked JVM for all ~115 other tests (no per-class fork), so
 * installing it here would silently change DNS behaviour for every other test in the module.
 */
@SpringBootTest(properties = {
        "geocode.geonames.url=http://203.0.113.1:9/cities5000.zip",
        "geocode.maxmind.url=http://203.0.113.1:9/maxmind.tar.gz",
        "remote-file-caching.connection-timeout=1000",
        "remote-file-caching.read-timeout=1000",
        "o4g.actuator.monitor.username=monitor",
        "o4g.actuator.monitor.password=monitor-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalProfileOfflineStartupTest
{
    @TempDir
    static File tempDir;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GeoNamesIndexService geoNamesIndexService;

    @Autowired
    private IpGeolocationService ipGeolocationService;

    @Autowired
    private IcecatCategoryRepository icecatCategoryRepository;

    @Autowired
    private IcecatFeatureRepository icecatFeatureRepository;

    @Autowired
    private IcecatFeatureGroupRepository icecatFeatureGroupRepository;

    @Autowired
    private IcecatSupplierRepository icecatSupplierRepository;

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.test.context.DynamicPropertySource
    static void offlineCachePaths(org.springframework.test.context.DynamicPropertyRegistry registry)
    {
        registry.add("front.cache.path", () -> new File(tempDir, "front-cache").getAbsolutePath());
        registry.add("geocode.cache.path", () -> new File(tempDir, "geocode-cache").getAbsolutePath());
    }

    /**
     * The context must start despite having no network, no GitHub token and no pre-existing
     * dataset cache, and the Icecat Elasticsearch repository proxies must be real, usable beans
     * (not {@code @MockitoBean} stand-ins, not {@code null} from a reflective proxy stub).
     */
    @Test
    void contextStartsOffline()
    {
        assertThat(icecatCategoryRepository).isNotNull();
        assertThat(icecatFeatureRepository).isNotNull();
        assertThat(icecatFeatureGroupRepository).isNotNull();
        assertThat(icecatSupplierRepository).isNotNull();
    }

    /**
     * B1: {@code feedback.github.enabled=false} in {@code application-local.yml} must keep the
     * GitHub-backed beans out of the context, so nothing calls the GitHub API at startup. The
     * {@code IssueService} contract itself stays satisfied by {@code LocalMockConfig}'s
     * no-op {@code mockIssueService}, which is exactly what activates once the flag is false.
     */
    @Test
    void githubFeedbackIsDisabled()
    {
        assertThat(applicationContext.getBeanNamesForType(GHRepository.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(GitHubIssueService.class)).isEmpty();
    }

    /**
     * B2: a dataset download that cannot reach the network must leave the GeoNames index
     * unloaded (fail-soft) rather than aborting startup.
     */
    @Test
    void geoNamesIndexFailsSoftWithoutNetwork()
    {
        assertThat(geoNamesIndexService.isLoaded()).isFalse();
    }

    /**
     * B2's MaxMind twin, already fail-soft before this issue: kept here as a regression guard
     * for the pair.
     */
    @Test
    void maxMindGeolocationFailsSoftWithoutNetwork()
    {
        assertThat(ipGeolocationService.isLoaded()).isFalse();
    }

    /**
     * {@code /actuator/health} must respond even though GeoNames/MaxMind are degraded.
     * Spring Boot reports a {@code 503} (not {@code 200}) once any indicator is {@code DOWN};
     * that is expected local-profile behaviour here (not just for geoNames/maxMind, but also
     * for indicators this profile does not attempt to simulate, like outbound mail or a real
     * Elasticsearch cluster) and must not be mistaken for the endpoint failing to respond.
     */
    @Test
    void actuatorHealthResponds() throws Exception
    {
        mockMvc.perform(get("/actuator/health").with(httpBasic("monitor", "monitor-secret")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 503))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"geoNames\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"maxMind\"")));
    }
}
