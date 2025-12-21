package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.agent.AgentRequestDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentRequestResponseDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentTemplateDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AgentController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        // Exclude security config if possible or mock it
    })
)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller test
@org.junit.jupiter.api.Disabled("Context loading issues to be resolved")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private org.open4goods.nudgerfrontapi.config.properties.SecurityProperties securityProperties;

    @MockBean
    private org.open4goods.nudgerfrontapi.config.properties.ApiProperties apiProperties;

    @MockBean
    private AgentService agentService;

    @MockBean
    private org.open4goods.nudgerfrontapi.interceptor.XLocaleHeaderInterceptor xLocaleHeaderInterceptor;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void listTemplates_ShouldReturnTemplates() throws Exception {
        AgentTemplateDto template = new AgentTemplateDto(
                "agent-id", "Agent Name", "Desc", "icon", "prompt", 
                List.of("tag"), List.of("ROLE_USER"), true, null
        );
        when(agentService.listTemplates(any(DomainLanguage.class))).thenReturn(List.of(template));

        mockMvc.perform(get("/agents/templates")
                .param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(jsonPath("$[0].id").value("agent-id"));
    }

    @Test
    @WithMockUser
    void submitRequest_ShouldReturnCreated() throws Exception {
        AgentRequestDto request = new AgentRequestDto(
                AgentRequestDto.AgentRequestType.FEATURE,
                "Request content",
                "agent-id",
                AgentRequestDto.PromptVisibility.PUBLIC,
                null
        );
        
        AgentRequestResponseDto response = new AgentRequestResponseDto(
                "id", 123, "url", "CREATED", null, AgentRequestDto.PromptVisibility.PUBLIC
        );

        when(agentService.submitRequest(any(AgentRequestDto.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/agents")
                .param("domainLanguage", "fr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueNumber").value(123));
    }
}
