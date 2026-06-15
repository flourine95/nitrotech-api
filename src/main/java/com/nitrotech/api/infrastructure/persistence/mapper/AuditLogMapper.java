package com.nitrotech.api.infrastructure.persistence.mapper;

import com.nitrotech.api.domain.audit.dto.AuditLogData;
import com.nitrotech.api.infrastructure.persistence.entity.AuditLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuditLogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    AuditLogEntity toEntity(AuditLogData data);
}
