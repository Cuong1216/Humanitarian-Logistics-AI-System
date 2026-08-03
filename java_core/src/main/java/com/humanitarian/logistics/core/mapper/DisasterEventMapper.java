package com.humanitarian.logistics.core.mapper;

import com.humanitarian.logistics.core.dto.DisasterEventDTO;
import com.humanitarian.logistics.core.entity.DisasterEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DisasterEventMapper {

    DisasterEventDTO toDTO(DisasterEvent entity);

    DisasterEvent toEntity(DisasterEventDTO dto);
}
