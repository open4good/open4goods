package org.open4goods.nudgerfrontapi.controller.advice;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    @Test
    void handleResponseStatusException_404_LogsWithoutStackTrace(CapturedOutput output) {
        io.micrometer.core.instrument.simple.SimpleMeterRegistry simpleRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler(simpleRegistry);

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        
        ProblemDetail pd = globalExceptionHandler.handleResponseStatusException(ex);
        
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(output.getOut()).contains("Resource not found");
        // Ensure stack trace is NOT logged (checking for a line typical in stack trace)
        assertThat(output.getOut()).doesNotContain("at org.open4goods.nudgerfrontapi.controller.advice.GlobalExceptionHandlerTest");
    }

    @Test
    void handleResponseStatusException_500_LogsWithStackTrace(CapturedOutput output) {
         io.micrometer.core.instrument.simple.SimpleMeterRegistry simpleRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler(simpleRegistry);

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");

        ProblemDetail pd = globalExceptionHandler.handleResponseStatusException(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(output.getOut()).contains("Internal error");
        // Ensure stack trace IS logged
        assertThat(output.getOut()).contains("at org.open4goods.nudgerfrontapi.controller.advice.GlobalExceptionHandlerTest");
    }
}
