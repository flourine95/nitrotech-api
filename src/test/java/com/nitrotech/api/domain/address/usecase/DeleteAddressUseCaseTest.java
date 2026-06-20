package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.exception.CannotDeleteDefaultAddressException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAddressUseCaseTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private DeleteAddressUseCase deleteAddressUseCase;

    @Test
    void deletesANonDefaultAddress() {
        when(addressRepository.findById(2L)).thenReturn(Optional.of(address(false)));
        when(addressRepository.deleteNonDefault(2L)).thenReturn(true);

        deleteAddressUseCase.execute(1L, 2L);

        verify(addressRepository).deleteNonDefault(2L);
    }

    @Test
    void rejectsDeletionWhenAddressBecomesDefaultBeforeDelete() {
        when(addressRepository.findById(2L)).thenReturn(Optional.of(address(false)));
        when(addressRepository.deleteNonDefault(2L)).thenReturn(false);

        assertThatThrownBy(() -> deleteAddressUseCase.execute(1L, 2L))
                .isInstanceOf(CannotDeleteDefaultAddressException.class);
    }

    @Test
    void rejectsDeletionOfCurrentDefaultWithoutDeleting() {
        when(addressRepository.findById(2L)).thenReturn(Optional.of(address(true)));

        assertThatThrownBy(() -> deleteAddressUseCase.execute(1L, 2L))
                .isInstanceOf(CannotDeleteDefaultAddressException.class);

        verify(addressRepository, never()).deleteNonDefault(2L);
    }

    private AddressData address(boolean defaultAddress) {
        return new AddressData(
                2L, 1L, "Phi Long", "0901000000", "Ho Chi Minh", "79", "District 1", "760",
                "Ben Nghe", "26734", "1 Nguyen Hue", defaultAddress, null, null);
    }
}
