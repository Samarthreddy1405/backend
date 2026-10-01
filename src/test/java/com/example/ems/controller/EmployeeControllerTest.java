package com.example.ems.controller;

import com.example.ems.dto.request.EmployeeCreateRequest;
import com.example.ems.dto.request.EmployeeUpdateRequest;
import com.example.ems.dto.response.EmployeeResponse;
import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer unit tests for EmployeeController.
 * Uses @WebMvcTest — no real Tomcat, no database.
 * EmployeeService is mocked via @MockBean.
 * GlobalExceptionHandler is loaded automatically as a @RestControllerAdvice.
 */
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    // ── shared fixtures ───────────────────────────────────────────────────────

    private EmployeeResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new EmployeeResponse(
                1L,
                "Alice Smith",
                "alice@example.com",
                "Engineering",
                "Senior Developer",
                true
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /employees
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /employees: valid request returns 201 with full employee body")
    void createEmployee_validRequest_returns201() throws Exception {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Alice Smith", "alice@example.com",
                "Engineering", "Senior Developer");

        when(employeeService.createEmployee(any(EmployeeCreateRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",          is(1)))
                .andExpect(jsonPath("$.name",         is("Alice Smith")))
                .andExpect(jsonPath("$.email",        is("alice@example.com")))
                .andExpect(jsonPath("$.department",   is("Engineering")))
                .andExpect(jsonPath("$.designation",  is("Senior Developer")))
                .andExpect(jsonPath("$.isActive",     is(true)));

        verify(employeeService).createEmployee(any(EmployeeCreateRequest.class));
    }

    @Test
    @DisplayName("POST /employees: blank name returns 400 BAD_REQUEST")
    void createEmployee_blankName_returns400() throws Exception {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "", "alice@example.com", "Engineering", "Senior Developer");

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path",    is("/employees")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("POST /employees: invalid email returns 400 BAD_REQUEST")
    void createEmployee_invalidEmail_returns400() throws Exception {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Alice Smith", "not-an-email", "Engineering", "Senior Developer");

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path",    is("/employees")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("POST /employees: blank department returns 400 BAD_REQUEST")
    void createEmployee_blankDepartment_returns400() throws Exception {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Alice Smith", "alice@example.com", "", "Senior Developer");

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path",    is("/employees")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("POST /employees: blank designation returns 400 BAD_REQUEST")
    void createEmployee_blankDesignation_returns400() throws Exception {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Alice Smith", "alice@example.com", "Engineering", "");

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path",    is("/employees")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /employees
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /employees: returns 200 with JSON array of active employees")
    void getAllEmployees_returns200WithList() throws Exception {
        EmployeeResponse second = new EmployeeResponse(
                2L, "Bob Jones", "bob@example.com",
                "HR", "Manager", true);

        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse, second));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",           hasSize(2)))
                .andExpect(jsonPath("$[0].id",     is(1)))
                .andExpect(jsonPath("$[0].name",   is("Alice Smith")))
                .andExpect(jsonPath("$[1].id",     is(2)))
                .andExpect(jsonPath("$[1].name",   is("Bob Jones")));

        verify(employeeService).getAllEmployees();
    }

    @Test
    @DisplayName("GET /employees: returns 200 with empty array when no active employees")
    void getAllEmployees_returnsEmptyList() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(employeeService).getAllEmployees();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /employees/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /employees/{id}: returns 200 with employee JSON when found")
    void getEmployeeById_found_returns200() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",         is(1)))
                .andExpect(jsonPath("$.name",        is("Alice Smith")))
                .andExpect(jsonPath("$.email",       is("alice@example.com")))
                .andExpect(jsonPath("$.department",  is("Engineering")))
                .andExpect(jsonPath("$.designation", is("Senior Developer")))
                .andExpect(jsonPath("$.isActive",    is(true)));

        verify(employeeService).getEmployeeById(1L);
    }

    @Test
    @DisplayName("GET /employees/999: returns 404 with ErrorResponse when employee not found")
    void getEmployeeById_notFound_returns404() throws Exception {
        when(employeeService.getEmployeeById(999L))
                .thenThrow(new EmployeeNotFoundException(
                        "Employee not found with id: 999"));

        mockMvc.perform(get("/employees/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status",    is(404)))
                .andExpect(jsonPath("$.message",   is("Employee not found with id: 999")))
                .andExpect(jsonPath("$.path",      is("/employees/999")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(employeeService).getEmployeeById(999L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /employees/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PATCH /employees/{id}: valid request returns 200 with updated employee")
    void updateEmployee_validRequest_returns200() throws Exception {
        EmployeeUpdateRequest updateRequest =
                new EmployeeUpdateRequest("Alice Updated", null, null, "Lead Developer");

        EmployeeResponse updatedResponse = new EmployeeResponse(
                1L, "Alice Updated", "alice@example.com",
                "Engineering", "Lead Developer", true);

        when(employeeService.updateEmployee(eq(1L), any(EmployeeUpdateRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(patch("/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",          is(1)))
                .andExpect(jsonPath("$.name",         is("Alice Updated")))
                .andExpect(jsonPath("$.designation",  is("Lead Developer")))
                .andExpect(jsonPath("$.isActive",     is(true)));

        verify(employeeService).updateEmployee(eq(1L), any(EmployeeUpdateRequest.class));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /employees/{id}/deactivate
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PATCH /employees/{id}/deactivate: returns 204 NO_CONTENT on success")
    void deactivateEmployee_returns204() throws Exception {
        doNothing().when(employeeService).deactivateEmployee(1L);

        mockMvc.perform(patch("/employees/1/deactivate"))
                .andExpect(status().isNoContent());

        verify(employeeService).deactivateEmployee(1L);
    }

    @Test
    @DisplayName("PATCH /employees/{id}/deactivate: returns 404 when employee not found")
    void deactivateEmployee_notFound_returns404() throws Exception {
        doThrow(new EmployeeNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deactivateEmployee(99L);

        mockMvc.perform(patch("/employees/99/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status",  is(404)))
                .andExpect(jsonPath("$.message", is("Employee not found with id: 99")))
                .andExpect(jsonPath("$.path",    is("/employees/99/deactivate")));

        verify(employeeService).deactivateEmployee(99L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /employees/{id}/activate
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PATCH /employees/{id}/activate: returns 204 NO_CONTENT on success")
    void activateEmployee_returns204() throws Exception {
        doNothing().when(employeeService).activateEmployee(1L);

        mockMvc.perform(patch("/employees/1/activate"))
                .andExpect(status().isNoContent());

        verify(employeeService).activateEmployee(1L);
    }

    @Test
    @DisplayName("PATCH /employees/{id}/activate: returns 404 when employee not found")
    void activateEmployee_notFound_returns404() throws Exception {
        doThrow(new EmployeeNotFoundException("Employee not found with id: 99"))
                .when(employeeService).activateEmployee(99L);

        mockMvc.perform(patch("/employees/99/activate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status",  is(404)))
                .andExpect(jsonPath("$.message", is("Employee not found with id: 99")))
                .andExpect(jsonPath("$.path",    is("/employees/99/activate")));

        verify(employeeService).activateEmployee(99L);
    }
}
