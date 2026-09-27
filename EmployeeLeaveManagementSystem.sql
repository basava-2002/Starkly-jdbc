CREATE DATABASE employee_leave_db;
USE employee_leave_db;

CREATE TABLE employees (
    employee_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    department VARCHAR(50)
);

CREATE TABLE leave_balance (
    balance_id INT PRIMARY KEY AUTO_INCREMENT,
    employee_id INT,
    total_leave INT DEFAULT 20,
    used_leave INT DEFAULT 0,
    remaining_leave INT DEFAULT 20,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
);

CREATE TABLE leave_requests (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    employee_id INT,
    leave_type VARCHAR(30),
    start_date DATE,
    end_date DATE,
    days INT,
    reason VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
);