package com.example.ems.repository;

import com.example.ems.jooq.tables.records.EmployeeRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.ems.jooq.Tables.EMPLOYEE;

@Repository
@RequiredArgsConstructor
public class EmployeeRepository {

    private final DSLContext dsl;

    /**
     * Inserts a new employee and returns the persisted record with the
     * database-generated id populated.
     *
     * The record is attached to the DSLContext so that store() issues an INSERT.
     * jOOQ recognises the IDENTITY column and excludes id from the INSERT
     * statement automatically, then fetches the generated key back.
     * isActive is set to true here so the caller never has to remember to do it.
     */
    public EmployeeRecord create(EmployeeRecord record) {
        EmployeeRecord attached = dsl.newRecord(EMPLOYEE);
        attached.setName(record.getName());
        attached.setEmail(record.getEmail());
        attached.setDepartment(record.getDepartment());
        attached.setDesignation(record.getDesignation());
        attached.setIsActive(true);
        attached.store();
        return attached;
    }

    /**
     * Returns all employees whose is_active column is true.
     */
    public List<EmployeeRecord> findAllActive() {
        return dsl.selectFrom(EMPLOYEE)
                  .where(EMPLOYEE.IS_ACTIVE.isTrue())
                  .fetchInto(EmployeeRecord.class);
    }

    /**
     * Finds an employee by id regardless of active status.
     * Returns Optional.empty() when no row exists.
     */
    public Optional<EmployeeRecord> findById(Long id) {
        return dsl.selectFrom(EMPLOYEE)
                  .where(EMPLOYEE.ID.eq(id))
                  .fetchOptionalInto(EmployeeRecord.class);
    }

    /**
     * Finds an employee by id only when they are active.
     * Returns Optional.empty() when the employee does not exist or is inactive.
     */
    public Optional<EmployeeRecord> findActiveById(Long id) {
        return dsl.selectFrom(EMPLOYEE)
                  .where(EMPLOYEE.ID.eq(id)
                     .and(EMPLOYEE.IS_ACTIVE.isTrue()))
                  .fetchOptionalInto(EmployeeRecord.class);
    }

    /**
     * Updates the mutable fields of an existing employee record identified by id.
     * id and isActive are intentionally excluded — callers must not alter them here.
     * Returns the updated record.
     */
    public EmployeeRecord update(EmployeeRecord record) {
        dsl.update(EMPLOYEE)
           .set(EMPLOYEE.NAME,        record.getName())
           .set(EMPLOYEE.EMAIL,       record.getEmail())
           .set(EMPLOYEE.DEPARTMENT,  record.getDepartment())
           .set(EMPLOYEE.DESIGNATION, record.getDesignation())
           .where(EMPLOYEE.ID.eq(record.getId()))
           .execute();
        return record;
    }

    /**
     * Flips the is_active flag for a single employee.
     * Returns true when exactly one row was updated, false when no employee
     * with that id was found.
     */
    public boolean updateActiveStatus(Long id, boolean isActive) {
        int rowsAffected = dsl.update(EMPLOYEE)
                              .set(EMPLOYEE.IS_ACTIVE, isActive)
                              .where(EMPLOYEE.ID.eq(id))
                              .execute();
        return rowsAffected > 0;
    }
}
