package com.example.ems.service;

import com.example.ems.dto.request.EmployeeCreateRequest;
import com.example.ems.dto.request.EmployeeUpdateRequest;
import com.example.ems.dto.response.EmployeeResponse;
import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.jooq.tables.records.EmployeeRecord;
import com.example.ems.mapper.EmployeeMapper;
import com.example.ems.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper     employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper     = employeeMapper;
    }

    /**
     * Creates a new employee.
     * The mapper converts the request to a record (id and isActive are excluded
     * by the mapper). The repository sets isActive = true and persists the record,
     * returning it with the database-generated id populated.
     */
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        EmployeeRecord record      = employeeMapper.toRecord(request);
        EmployeeRecord savedRecord = employeeRepository.create(record);
        return employeeMapper.toResponse(savedRecord);
    }

    /**
     * Returns all active employees.
     * The repository filters by is_active = true; inactive employees are never exposed.
     */
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAllActive()
                                 .stream()
                                 .map(employeeMapper::toResponse)
                                 .toList();
    }

    /**
     * Returns a single employee by id regardless of active status.
     * Throws RuntimeException when no employee exists with the given id.
     */
    public EmployeeResponse getEmployeeById(Long id) {
        EmployeeRecord record = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found with id: " + id));
        return employeeMapper.toResponse(record);
    }

    /**
     * Applies a partial update to an active employee.
     * Only active employees are updatable — an inactive employee must be
     * re-activated before their data can be changed.
     * The mapper applies only non-null fields from the request onto the
     * existing record, preserving untouched fields (PATCH semantics).
     * id and isActive are never altered by this operation.
     */
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        EmployeeRecord existingRecord = employeeRepository.findActiveById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Active employee not found with id: " + id));

        employeeMapper.updateRecord(request, existingRecord);
        EmployeeRecord updatedRecord = employeeRepository.update(existingRecord);
        return employeeMapper.toResponse(updatedRecord);
    }

    /**
     * Deactivates an employee by setting is_active = false.
     * The employee must exist (active or inactive) for this to succeed.
     */
    public void deactivateEmployee(Long id) {
        employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found with id: " + id));

        employeeRepository.updateActiveStatus(id, false);
    }

    /**
     * Re-activates a previously deactivated employee by setting is_active = true.
     * The employee must exist (active or inactive) for this to succeed.
     */
    public void activateEmployee(Long id) {
        employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found with id: " + id));

        employeeRepository.updateActiveStatus(id, true);
    }
}
