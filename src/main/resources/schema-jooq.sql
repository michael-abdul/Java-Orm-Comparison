CREATE SCHEMA IF NOT EXISTS demo;
SET SCHEMA demo;

DROP TABLE IF EXISTS salary;
CREATE TABLE salary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    work_year INT NOT NULL,
    experience_level CHAR(2) NOT NULL,
    employment_type CHAR(2) NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    salary DECIMAL(12, 2) NOT NULL,
    salary_currency CHAR(3) NOT NULL,
    salary_in_usd DECIMAL(12, 2) NOT NULL,
    employee_residence CHAR(2) NOT NULL,
    remote_ratio INT NOT NULL,
    company_location CHAR(2) NOT NULL,
    company_size CHAR(1) NOT NULL
);
