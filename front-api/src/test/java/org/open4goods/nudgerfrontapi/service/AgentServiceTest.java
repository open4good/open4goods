package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.nudgerfrontapi.config.AgentProperties;
import org.open4goods.nudgerfrontapi.config.AgentProperties.AgentConfig;
import org.open4goods.nudgerfrontapi.dto.agent.AgentRequestDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentRequestResponseDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentTemplateDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.feedback.dto.IssueDto;
import org.open4goods.services.feedback.service.IssueService;
import org.open4goods.services.captcha.service.HcaptchaService;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentProperties agentProperties;
    @Mock
    private IssueService issueService;
    @Mock
    private HcaptchaService hcaptchaService;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(agentProperties, issueService, hcaptchaService);
    }

    @Test
    void listTemplates_ShouldReturnMappedTemplates() {
        AgentConfig config = new AgentConfig();
        config.setId("test-agent");
        config.setName(Map.of("en", "Test Agent", "fr", "Agent Test"));
        config.setDescription(Map.of("en", "Description"));
        config.setTags(List.of("tag1"));

        AgentProperties.AgentAttribute attr = new AgentProperties.AgentAttribute();
        attr.setId("attr1");
        attr.setType("TEXT");
        attr.setLabel(Map.of("en", "Attr Label"));
        config.setAttributes(List.of(attr));
        
        when(agentProperties.getAgents()).thenReturn(List.of(config));

        List<AgentTemplateDto> templates = agentService.listTemplates(DomainLanguage.fr);

        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).id()).isEqualTo("test-agent");
        assertThat(templates.get(0).name()).isEqualTo("Agent Test");
        assertThat(templates.get(0).attributes()).hasSize(1);
        assertThat(templates.get(0).attributes().get(0).id()).isEqualTo("attr1");
    }

    @Test
    void submitRequest_ShouldCreateIssue_WithAttributesAndCaptcha() throws IOException {
        AgentConfig config = new AgentConfig();
        config.setId("test-agent");
        config.setPromptTemplate("System Prompt");
        config.setPublicPromptHistory(true);
        
        when(agentProperties.getAgents()).thenReturn(List.of(config));

        IssueDto mockIssue = new IssueDto("123", 123, "https://github.com/org/repo/issues/123", "OPEN", "Title", null);

        when(issueService.createIssue(any(), any(), any(), anySet())).thenReturn(mockIssue);

        AgentRequestDto request = new AgentRequestDto(
                AgentRequestDto.AgentRequestType.FEATURE,
                "User Prompt",
                "test-agent",
                null,
                "user-handle",
                Map.of("attr1", "Value1"),
                "valid-token"
        );

        AgentRequestResponseDto response = agentService.submitRequest(request, "127.0.0.1");

        assertThat(response.issueNumber()).isEqualTo(123);
        assertThat(response.promptVisibility()).isEqualTo(AgentRequestDto.PromptVisibility.PUBLIC);

        verify(hcaptchaService).verifyRecaptcha("127.0.0.1", "valid-token");
    }

    @Test
    void submitRequest_ShouldFail_WhenCaptchaInvalid() {
        AgentRequestDto request = new AgentRequestDto(
                AgentRequestDto.AgentRequestType.FEATURE,
                "User Prompt",
                "test-agent",
                null,
                null,
                null,
                "bad-token"
        );
        
        doThrow(new SecurityException("Invalid token")).when(hcaptchaService).verifyRecaptcha(any(), eq("bad-token"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            agentService.submitRequest(request, "127.0.0.1");
        });
    }
}
