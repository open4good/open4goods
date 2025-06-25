package org.open4goods.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.model.dto.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {RequestContextConfig.class, RequestContextInterceptorTest.TestController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RequestContextInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void headerStoredInContext() throws Exception {
        mockMvc.perform(get("/test").header("X-Test", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string("abc"));
    }

    @RestController
    static class TestController {
        private final RequestContext requestContext;

        TestController(RequestContext requestContext) {
            this.requestContext = requestContext;
        }

        @GetMapping("/test")
        String value() {
            return requestContext.getHeaders().get("X-Test");
        }
    }
}
