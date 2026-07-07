package com.nitrotech.api.shared.exception;

import com.nitrotech.api.domain.address.exception.DefaultAddressConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsConflictForDefaultAddressConflict() {
        var response = handler.handleConflict(new DefaultAddressConflictException());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("DEFAULT_ADDRESS_CONFLICT");
    }

    @Test
    void genericHandlerDoesNotWriteJsonBodyForSseResponses() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        servletResponse.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        var response = handler.handleGeneric(new RuntimeException("boom"), servletResponse);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNull();
    }
}
