# Employee Management System - Database Schema

## Overview

The Employee Management System uses PostgreSQL with Liquibase for database schema management.

## Table: employee

| Column | PostgreSQL Type | Constraints | Default |
|---|---|---|---|
| id | BIGINT | Primary Key, NOT NULL, Auto Increment | — |
| name | VARCHAR(100) | NOT NULL | — |
| email | VARCHAR(150) | NOT NULL, UNIQUE | — |
| department | VARCHAR(100) | NOT NULL | — |
| designation | VARCHAR(100) | NOT NULL | — |
| is_active | BOOLEAN | NOT NULL | TRUE |

## Constraints

- Primary key: `pk_employee`
- Unique constraint: `uq_employee_email`

## Soft Delete

Employees are not physically deleted. The `is_active` column is used to deactivate and reactivate employees.

- `true` — active employee
- `false` — deactivated employee

## Liquibase

The schema is managed through:

`src/main/resources/db/changelog/db.changelog-master.yaml`

which includes:

`src/main/resources/db/changelog/db-changelog-202609301630.xml`

## jOOQ

jOOQ generation targets the PostgreSQL `employee` table and generates the corresponding database classes used by the backend repository layer.
