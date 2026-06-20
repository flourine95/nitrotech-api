package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.exception.CannotUnsetDefaultAddressException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UpdateAddressUseCaseTest {

    @Autowired
    private UpdateAddressUseCase updateAddressUseCase;

    @Autowired
    private AddressRepository addressRepository;

    @Test
    void keepsAnExistingDefaultAddressDefaultWhenDefaultAddressIsNotChanged() {
        AddressData address = addressRepository.create(1L, createCommand(true, "Phi Long"));

        updateAddressUseCase.execute(1L, address.id(), updateCommand(null, "Phi Long Nguyen"));

        AddressData reloaded = addressRepository.findById(address.id()).orElseThrow();
        assertThat(reloaded.receiver()).isEqualTo("Phi Long Nguyen");
        assertThat(reloaded.defaultAddress()).isTrue();
    }

    @Test
    void rejectsUnsettingTheCurrentDefaultAddress() {
        AddressData address = addressRepository.create(1L, createCommand(true, "Phi Long"));

        assertThatThrownBy(() -> updateAddressUseCase.execute(1L, address.id(), updateCommand(false, "Phi Long")))
                .isInstanceOf(CannotUnsetDefaultAddressException.class);

        assertThat(addressRepository.findById(address.id()).orElseThrow().defaultAddress()).isTrue();
    }

    @Test
    void makesTheUpdatedAddressDefaultWhenRequested() {
        AddressData defaultAddress = addressRepository.create(1L, createCommand(true, "Phi Long"));
        AddressData secondaryAddress = addressRepository.create(1L, createCommand(false, "Mai Anh"));

        updateAddressUseCase.execute(1L, secondaryAddress.id(), updateCommand(true, "Mai Anh Nguyen"));

        assertThat(addressRepository.findById(defaultAddress.id()).orElseThrow().defaultAddress()).isFalse();
        assertThat(addressRepository.findById(secondaryAddress.id()).orElseThrow().defaultAddress()).isTrue();
    }

    private CreateAddressCommand createCommand(boolean defaultAddress, String receiver) {
        return new CreateAddressCommand(
                receiver, "0901000000", "Ho Chi Minh", "79", "District 1", "760",
                "Ben Nghe", "26734", "1 Nguyen Hue", defaultAddress);
    }

    private UpdateAddressCommand updateCommand(Boolean defaultAddress, String receiver) {
        return new UpdateAddressCommand(
                receiver, "0901000000", "Ho Chi Minh", "79", "District 1", "760",
                "Ben Nghe", "26734", "1 Nguyen Hue", defaultAddress);
    }
}
