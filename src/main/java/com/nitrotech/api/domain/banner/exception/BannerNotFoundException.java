package com.nitrotech.api.domain.banner.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class BannerNotFoundException extends NotFoundException {

    public BannerNotFoundException() {
        super("BANNER_NOT_FOUND", "Banner not found");
    }
}
