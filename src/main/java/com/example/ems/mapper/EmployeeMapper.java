package com.example.ems.mapper;

import com.example.ems.dto.request.EmployeeCreateRequest;
import com.example.ems.dto.request.EmployeeUpdateRequest;
import com.example.ems.dto.response.EmployeeResponse;
import com.example.ems.jooq.tables.records.EmployeeRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EmployeeMapper {

    /**
     * Maps a jOOQ EmployeeRecord to the outbound EmployeeResponse DTO.
     * All fields (id, name, email, department, designation, isActive) match by name.
     */
    EmployeeResponse toResponse(EmployeeRecord record);

    /**
     * Maps an EmployeeCreateRequest to a new EmployeeRecord.
     * id and isActive are not present on the request and must be excluded —
     * the database assigns id via auto-increment, and the service sets isActive = true.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    EmployeeRecord toRecord(EmployeeCreateRequest request);

    /**
     * Applies non-null fields from an EmployeeUpdateRequest onto an existing EmployeeRecord.
     * Null fields are skipped due to NullValuePropertyMappingStrategy.IGNORE, making this
     * safe for PATCH semantics where the caller may send only a subset of fields.
     * id and isActive must not be touched during a data update.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateRecord(EmployeeUpdateRequest request, @MappingTarget EmployeeRecord record);
}
