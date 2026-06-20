package com.nitrotech.api.shared.exception;

import com.nitrotech.api.domain.address.exception.DefaultAddressConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsConflictForDefaultAddressConflict() {
        var response = handler.handleConflict(new DefaultAddressConflictException());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("DEFAULT_ADDRESS_CONFLICT");
    }
}
