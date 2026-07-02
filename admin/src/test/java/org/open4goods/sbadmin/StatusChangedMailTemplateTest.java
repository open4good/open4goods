package org.open4goods.sbadmin;

import static org.assertj.core.api.Assertions.assertThat;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class StatusChangedMailTemplateTest {

    private static final String TEMPLATE = "META-INF/spring-boot-admin-server/mail/status-changed";

    @Test
    void rendersNestedStatusDetails() {
        Map<String, Object> backupDetails = Map.of(
                "xwiki_backup_too_old", 1782780302359L,
                "product_backup_file_count", "5 <> 4");
        StatusInfo statusInfo = StatusInfo.ofDown(Map.of(
                "backupService", Map.of(
                        "status", "DOWN",
                        "details", backupDetails),
                "promptService", Map.of("status", "UP")));
        InstanceId instanceId = InstanceId.of("777de8a5e899");
        Registration registration = Registration.create("open4goods-admin", "http://localhost/actuator/health")
                .serviceUrl("http://localhost")
                .managementUrl("http://localhost/actuator")
                .build();
        Instance instance = Instance.create(instanceId).register(registration).withStatusInfo(statusInfo);
        InstanceStatusChangedEvent event = new InstanceStatusChangedEvent(
                instanceId, 49L, Instant.parse("2026-06-30T14:56:27.980322602Z"), statusInfo);

        Context context = new Context();
        context.setVariable("instance", instance);
        context.setVariable("event", event);
        context.setVariable("lastStatus", "UP");
        context.setVariable("baseUrl", "http://localhost:8080");

        String rendered = templateEngine().process(TEMPLATE, context);

        assertThat(rendered)
                .contains("backupService")
                .contains("xwiki_backup_too_old")
                .contains("product_backup_file_count")
                .contains("5 &lt;&gt; 4");
    }

    private static SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setSuffix(".html");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }
}
