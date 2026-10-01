package com.example.ems.controller;

import com.example.ems.dto.request.EmployeeCreateRequest;
import com.example.ems.dto.request.EmployeeUpdateRequest;
import com.example.ems.dto.response.EmployeeResponse;
import com.example.ems.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * POST /employees
     * Creates a new employee. Validates the request body before passing it to
     * the service. Returns 201 CREATED with the persisted employee in the body.
     */
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {

        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /employees
     * Returns all active employees. Inactive employees are excluded by the
     * service and repository layers. Returns 200 OK.
     */
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        List<EmployeeResponse> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    /**
     * GET /employees/{id}
     * Returns a single employee by id regardless of active status.
     * Returns 200 OK, or the global exception handler will return 404
     * when the employee does not exist.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            @PathVariable Long id) {

        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /employees/{id}
     * Partially updates an active employee. Only non-null fields in the request
     * body are applied. Validates the request body before passing it to the
     * service. Returns 200 OK with the updated employee in the body.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {

        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /employees/{id}/deactivate
     * Sets is_active = false for the specified employee.
     * Returns 204 NO CONTENT on success (no body).
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /employees/{id}/activate
     * Sets is_active = true for the specified employee.
     * Returns 204 NO CONTENT on success (no body).
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateEmployee(@PathVariable Long id) {
        employeeService.activateEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
