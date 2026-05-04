package com.nitrotech.api.shared.config;

import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.domain.address.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AddressConfig {

    @Bean
    public GetAddressesUseCase getAddressesUseCase(AddressRepository addressRepository) {
        return new GetAddressesUseCase(addressRepository);
    }

    @Bean
    public CreateAddressUseCase createAddressUseCase(AddressRepository addressRepository) {
        return new CreateAddressUseCase(addressRepository);
    }

    @Bean
    public UpdateAddressUseCase updateAddressUseCase(AddressRepository addressRepository) {
        return new UpdateAddressUseCase(addressRepository);
    }

    @Bean
    public DeleteAddressUseCase deleteAddressUseCase(AddressRepository addressRepository) {
        return new DeleteAddressUseCase(addressRepository);
    }

    @Bean
    public SetDefaultAddressUseCase setDefaultAddressUseCase(AddressRepository addressRepository) {
        return new SetDefaultAddressUseCase(addressRepository);
    }
}
