package com.example.ems.service;

import com.example.ems.dto.request.EmployeeCreateRequest;
import com.example.ems.dto.request.EmployeeUpdateRequest;
import com.example.ems.dto.response.EmployeeResponse;
import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.jooq.tables.records.EmployeeRecord;
import com.example.ems.mapper.EmployeeMapper;
import com.example.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for EmployeeService.
 * No Spring context, no database — everything is mocked via Mockito.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeService employeeService;

    // ── shared test fixtures ──────────────────────────────────────────────────

    private EmployeeRecord        sampleRecord;
    private EmployeeResponse      sampleResponse;
    private EmployeeCreateRequest createRequest;
    private EmployeeUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        sampleRecord = new EmployeeRecord(
                1L, "Alice Smith", "alice@example.com",
                "Engineering", "Senior Developer", true);

        sampleResponse = new EmployeeResponse(
                1L, "Alice Smith", "alice@example.com",
                "Engineering", "Senior Developer", true);

        createRequest = new EmployeeCreateRequest(
                "Alice Smith", "alice@example.com",
                "Engineering", "Senior Developer");

        updateRequest = new EmployeeUpdateRequest(
                "Alice Updated", null, null, "Lead Developer");
    }

    // ── createEmployee ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createEmployee: maps request → saves → maps saved record → returns response")
    void createEmployee_success() {
        // Arrange
        EmployeeRecord mappedRecord = new EmployeeRecord();
        when(employeeMapper.toRecord(createRequest)).thenReturn(mappedRecord);
        when(employeeRepository.create(mappedRecord)).thenReturn(sampleRecord);
        when(employeeMapper.toResponse(sampleRecord)).thenReturn(sampleResponse);

        // Act
        EmployeeResponse result = employeeService.createEmployee(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L,                 result.getId());
        assertEquals("Alice Smith",      result.getName());
        assertEquals("alice@example.com",result.getEmail());
        assertEquals("Engineering",      result.getDepartment());
        assertEquals("Senior Developer", result.getDesignation());
        assertEquals(true,               result.getIsActive());

        verify(employeeMapper).toRecord(createRequest);
        verify(employeeRepository).create(mappedRecord);
        verify(employeeMapper).toResponse(sampleRecord);
        verifyNoMoreInteractions(employeeRepository, employeeMapper);
    }

    // ── getAllEmployees ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllEmployees: calls findAllActive and maps each record to response")
    void getAllEmployees_returnsActiveEmployees() {
        // Arrange
        EmployeeRecord record2 = new EmployeeRecord(
                2L, "Bob Jones", "bob@example.com",
                "HR", "Manager", true);
        EmployeeResponse response2 = new EmployeeResponse(
                2L, "Bob Jones", "bob@example.com",
                "HR", "Manager", true);

        when(employeeRepository.findAllActive()).thenReturn(List.of(sampleRecord, record2));
        when(employeeMapper.toResponse(sampleRecord)).thenReturn(sampleResponse);
        when(employeeMapper.toResponse(record2)).thenReturn(response2);

        // Act
        List<EmployeeResponse> result = employeeService.getAllEmployees();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Alice Smith", result.get(0).getName());
        assertEquals("Bob Jones",   result.get(1).getName());

        // must use the active-only query — never findById here
        verify(employeeRepository).findAllActive();
        verifyNoMoreInteractions(employeeRepository);
    }

    @Test
    @DisplayName("getAllEmployees: returns empty list when no active employees exist")
    void getAllEmployees_noActiveEmployees_returnsEmptyList() {
        when(employeeRepository.findAllActive()).thenReturn(List.of());

        List<EmployeeResponse> result = employeeService.getAllEmployees();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(employeeRepository).findAllActive();
        verifyNoMoreInteractions(employeeRepository);
    }

    // ── getEmployeeById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getEmployeeById: returns response when employee exists")
    void getEmployeeById_found() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleRecord));
        when(employeeMapper.toResponse(sampleRecord)).thenReturn(sampleResponse);

        EmployeeResponse result = employeeService.getEmployeeById(1L);

        assertNotNull(result);
        assertEquals(1L,           result.getId());
        assertEquals("Alice Smith", result.getName());
        verify(employeeRepository).findById(1L);
        verify(employeeMapper).toResponse(sampleRecord);
    }

    @Test
    @DisplayName("getEmployeeById: throws EmployeeNotFoundException when id does not exist")
    void getEmployeeById_notFound_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        EmployeeNotFoundException ex = assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.getEmployeeById(99L));

        assertEquals("Employee not found with id: 99", ex.getMessage());
        verify(employeeRepository).findById(99L);
        verifyNoMoreInteractions(employeeRepository, employeeMapper);
    }

    // ── updateEmployee ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateEmployee: applies partial update on active employee and returns updated response")
    void updateEmployee_success() {
        EmployeeRecord updatedRecord = new EmployeeRecord(
                1L, "Alice Updated", "alice@example.com",
                "Engineering", "Lead Developer", true);
        EmployeeResponse updatedResponse = new EmployeeResponse(
                1L, "Alice Updated", "alice@example.com",
                "Engineering", "Lead Developer", true);

        when(employeeRepository.findActiveById(1L)).thenReturn(Optional.of(sampleRecord));
        // mapper.updateRecord mutates sampleRecord in-place — no return value
        doNothing().when(employeeMapper).updateRecord(eq(updateRequest), eq(sampleRecord));
        when(employeeRepository.update(sampleRecord)).thenReturn(updatedRecord);
        when(employeeMapper.toResponse(updatedRecord)).thenReturn(updatedResponse);

        EmployeeResponse result = employeeService.updateEmployee(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Alice Updated",  result.getName());
        assertEquals("Lead Developer", result.getDesignation());

        verify(employeeRepository).findActiveById(1L);
        verify(employeeMapper).updateRecord(updateRequest, sampleRecord);
        verify(employeeRepository).update(sampleRecord);
        verify(employeeMapper).toResponse(updatedRecord);
        verifyNoMoreInteractions(employeeRepository, employeeMapper);
    }

    @Test
    @DisplayName("updateEmployee: throws EmployeeNotFoundException when active employee does not exist")
    void updateEmployee_notFound_throwsException() {
        when(employeeRepository.findActiveById(99L)).thenReturn(Optional.empty());

        EmployeeNotFoundException ex = assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.updateEmployee(99L, updateRequest));

        assertEquals("Active employee not found with id: 99", ex.getMessage());
        verify(employeeRepository).findActiveById(99L);
        verifyNoMoreInteractions(employeeRepository, employeeMapper);
    }

    // ── deactivateEmployee ────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateEmployee: verifies employee exists then calls updateActiveStatus with false")
    void deactivateEmployee_success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleRecord));
        when(employeeRepository.updateActiveStatus(1L, false)).thenReturn(true);

        employeeService.deactivateEmployee(1L);

        verify(employeeRepository).findById(1L);
        verify(employeeRepository).updateActiveStatus(1L, false);
        verifyNoMoreInteractions(employeeRepository, employeeMapper);
    }

    @Test
    @DisplayName("deactivateEmployee: throws EmployeeNotFoundException when employee does not exist")
    void deactivateEmployee_notFound_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        EmployeeNotFoundException ex = assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.deactivateEmployee(99L));

        assertEquals("Employee not found with id: 99", ex.getMessage());
        verify(employeeRepository).findById(99L);
        verifyNoMoreInteractions(employeeRepository, employeeMapper);
    }

    // ── activateEmployee ──────────────────────────────────────────────────────

    @Test
    @DisplayName("activateEmployee: verifies employee exists then calls updateActiveStatus with true")
    void activateEmployee_success() {
        EmployeeRecord inactiveRecord = new EmployeeRecord(
                1L, "Alice Smith", "alice@example.com",
                "Engineering", "Senior Developer", false);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(inactiveRecord));
        when(employeeRepository.updateActiveStatus(1L, true)).thenReturn(true);

        employeeService.activateEmployee(1L);

        verify(employeeRepository).findById(1L);
        verify(employeeRepository).updateActiveStatus(1L, true);
        verifyNoMoreInteractions(employeeRepository, employeeMapper);
    }

    @Test
    @DisplayName("activateEmployee: throws EmployeeNotFoundException when employee does not exist")
    void activateEmployee_notFound_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        EmployeeNotFoundException ex = assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.activateEmployee(99L));

        assertEquals("Employee not found with id: 99", ex.getMessage());
        verify(employeeRepository).findById(99L);
        verifyNoMoreInteractions(employeeRepository, employeeMapper);
    }
}
