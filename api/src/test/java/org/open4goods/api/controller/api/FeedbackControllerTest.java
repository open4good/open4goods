package org.open4goods.api.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssue;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.open4goods.api.dto.CreateIssueRequest;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.open4goods.services.feedback.dto.IssueDTO;
import org.open4goods.services.feedback.service.IssueService;
import org.open4goods.services.feedback.service.VoteResponse;
import org.open4goods.services.feedback.service.VoteService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

class FeedbackControllerTest {

    private MockMvc mvc;

    @Mock
    private IssueService issueService;
    @Mock
    private VoteService voteService;
    @Mock
    private HcaptchaService hcaptchaService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        issueService = Mockito.mock(IssueService.class);
        voteService = Mockito.mock(VoteService.class);
        hcaptchaService = Mockito.mock(HcaptchaService.class);
        FeedbackController controller = new FeedbackController(issueService, voteService, hcaptchaService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createIssueReturnsLocation() throws Exception {
        GHIssue issue = Mockito.mock(GHIssue.class);
        when(issue.getHtmlUrl()).thenReturn(new URL("https://example.com/1"));
        when(issueService.createBug(any(), any(), any(), any(), any())).thenReturn(issue);

        CreateIssueRequest req = new CreateIssueRequest("bug", "t", "m", "u", "a", Set.of());
        mvc.perform(MockMvcRequestBuilders.post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hcaptcha-Response", "token")
                .content(mapper.writeValueAsString(req)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(201))
                .andExpect(result -> assertThat(result.getResponse().getHeader("Location")).isEqualTo("https://example.com/1"));

        verify(hcaptchaService).verifyRecaptcha(any(), Mockito.eq("token"));
    }

    @Test
    void listIssuesReturnsData() throws Exception {
        GHIssue issue = Mockito.mock(GHIssue.class, Mockito.RETURNS_DEEP_STUBS);
        when(issue.getNumber()).thenReturn(1);
        when(issue.getTitle()).thenReturn("A");
        when(issue.getHtmlUrl()).thenReturn(new URL("https://example.com/1"));
        when(issueService.listIssues()).thenReturn(List.of(issue));
        when(voteService.getTotalVotes("1")).thenReturn(2);

        mvc.perform(MockMvcRequestBuilders.get("/issues"))
                .andExpect(result -> {
                    assertThat(result.getResponse().getStatus()).isEqualTo(200);
                    String json = result.getResponse().getContentAsString();
                    IssueDTO[] dtos = mapper.readValue(json, IssueDTO[].class);
                    assertThat(dtos).hasSize(1);
                    assertThat(dtos[0].votes()).isEqualTo(2);
                });
    }

    @Test
    void voteEndpointReturnsTotals() throws Exception {
        when(voteService.vote("1", "127.0.0.1")).thenReturn(new VoteResponse(4, 10));

        mvc.perform(MockMvcRequestBuilders.post("/issues/1/votes")
                .header("X-Hcaptcha-Response", "tok")
                .with(req -> { req.setRemoteAddr("127.0.0.1"); return req; }))
                .andExpect(result -> {
                    assertThat(result.getResponse().getStatus()).isEqualTo(200);
                    String json = result.getResponse().getContentAsString();
                    VoteResponse resp = mapper.readValue(json, VoteResponse.class);
                    assertThat(resp.totalVotes()).isEqualTo(10);
                });

        verify(hcaptchaService).verifyRecaptcha("127.0.0.1", "tok");
    }
}
