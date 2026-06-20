package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.exception.DefaultAddressConflictException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAddressUseCaseTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private CreateAddressUseCase createAddressUseCase;

    @Test
    void translatesDefaultAddressUniqueIndexViolationToConflict() {
        when(addressRepository.countByUserId(1L)).thenReturn(1L);
        when(addressRepository.create(eq(1L), any())).thenReturn(address());
        doThrow(new DataIntegrityViolationException("duplicate key violates uq_addresses_one_default_per_user"))
                .when(addressRepository).setAsDefault(1L, 2L);

        assertThatThrownBy(() -> createAddressUseCase.execute(1L, command()))
                .isInstanceOf(DefaultAddressConflictException.class)
                .hasMessage("Another address was set as default. Please try again.");
    }

    private CreateAddressCommand command() {
        return new CreateAddressCommand(
                "Phi Long", "0901000000", "Ho Chi Minh", "79", "District 1", "760",
                "Ben Nghe", "26734", "1 Nguyen Hue", true);
    }

    private AddressData address() {
        return new AddressData(
                2L, 1L, "Phi Long", "0901000000", "Ho Chi Minh", "79", "District 1", "760",
                "Ben Nghe", "26734", "1 Nguyen Hue", false, null, null);
    }
}
