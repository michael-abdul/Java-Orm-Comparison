CREATE SCHEMA IF NOT EXISTS demo;

SET search_path TO demo;

DROP TABLE IF EXISTS salaries1;
CREATE TABLE salaries1 (
    id          SERIAL PRIMARY KEY,
    work_year   INT NOT NULL,
    experience_level CHAR(2) NOT NULL,
    employment_type  CHAR(2) NOT NULL,
    job_title        VARCHAR(100) NOT NULL,
    salary           DECIMAL(12, 2) NOT NULL,
    salary_currency  CHAR(3) NOT NULL,
    salary_in_usd    DECIMAL(12, 2) NOT NULL,
    employee_residence CHAR(2) NOT NULL,
    remote_ratio     INT NOT NULL,
    company_location CHAR(2) NOT NULL,
    company_size     CHAR(1) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_salaries1_usd_residence ON demo.salaries1 (salary_in_usd, employee_residence);
CREATE INDEX IF NOT EXISTS idx_salaries1_year_usd ON demo.salaries1 (work_year, salary_in_usd);
CREATE INDEX IF NOT EXISTS idx_salaries1_title_usd ON demo.salaries1 (job_title, salary_in_usd);

-- Sample data
INSERT INTO salaries1 (work_year, experience_level, employment_type, job_title, salary, salary_currency, salary_in_usd, employee_residence, remote_ratio, company_location, company_size) VALUES
(2025, 'MI', 'FT', 'Data Scientist', 132600.00, 'USD', 132600.00, 'US', 100, 'US', 'M'),
(2025, 'MI', 'FT', 'Data Scientist', 102000.00, 'USD', 102000.00, 'US', 100, 'US', 'M'),
(2025, 'SE', 'FT', 'Data Product Manager', 260520.00, 'USD', 260520.00, 'US', 0, 'US', 'M'),
(2025, 'SE', 'FT', 'Data Product Manager', 140280.00, 'USD', 140280.00, 'US', 0, 'US', 'M'),
(2025, 'SE', 'FT', 'Researcher', 250000.00, 'USD', 250000.00, 'US', 100, 'US', 'L'),
(2025, 'MI', 'FT', 'Researcher', 80000.00, 'USD', 80000.00, 'US', 50, 'US', 'M'),
(2025, 'SE', 'FT', 'Manager', 350000.00, 'USD', 350000.00, 'GB', 20, 'GB', 'L'),
(2025, 'EN', 'CT', 'Analyst', 45000.00, 'USD', 45000.00, 'IN', 0, 'IN', 'S'),
(2025, 'MI', 'FT', 'Engineer', 150000.00, 'USD', 150000.00, 'CA', 80, 'CA', 'M'),
(2025, 'SE', 'FT', 'Director', 500000.00, 'USD', 500000.00, 'US', 10, 'US', 'L');
