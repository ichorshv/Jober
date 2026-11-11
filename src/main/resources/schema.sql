DROP TABLE IF EXISTS interviews;
DROP TABLE IF EXISTS applications;
DROP TABLE IF EXISTS job_offers;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS companies;

CREATE TABLE companies (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL UNIQUE,
    website       VARCHAR(255),
    industry      VARCHAR(100),
    location      VARCHAR(150),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE students (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    university      VARCHAR(150),
    major           VARCHAR(100),
    graduation_year INT CHECK (graduation_year BETWEEN 2000 AND 2100),
    role            VARCHAR(20)  NOT NULL DEFAULT 'STUDENT'
                    CHECK (role IN ('STUDENT', 'ADMIN')),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE job_offers (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT       NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    job_type    VARCHAR(20)  NOT NULL
                CHECK (job_type IN ('INTERNSHIP', 'PART_TIME', 'FULL_TIME')),
    location    VARCHAR(150),
    salary_min  NUMERIC(10, 2) CHECK (salary_min >= 0),
    salary_max  NUMERIC(10, 2) CHECK (salary_max >= 0),
    deadline    DATE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min)
);
CREATE INDEX idx_job_offers_company ON job_offers(company_id);
CREATE INDEX idx_job_offers_active_deadline ON job_offers(active, deadline);

CREATE TABLE applications (
    id           BIGSERIAL PRIMARY KEY,
    student_id   BIGINT      NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    job_offer_id BIGINT      NOT NULL REFERENCES job_offers(id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'SAVED'
                 CHECK (status IN ('SAVED', 'APPLIED', 'INTERVIEWING',
                                   'OFFER', 'REJECTED', 'WITHDRAWN')),
    applied_date DATE,
    notes        TEXT,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_student_job_offer UNIQUE (student_id, job_offer_id)
);
CREATE INDEX idx_applications_student_status ON applications(student_id, status);
CREATE INDEX idx_applications_job_offer ON applications(job_offer_id);

CREATE TABLE interviews (
    id               BIGSERIAL PRIMARY KEY,
    application_id   BIGINT      NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    type             VARCHAR(20) NOT NULL
                     CHECK (type IN ('PHONE', 'TECHNICAL', 'HR', 'ONSITE')),
    scheduled_at     TIMESTAMP   NOT NULL,
    location_or_link VARCHAR(255),
    outcome          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (outcome IN ('PENDING', 'PASSED', 'FAILED')),
    notes            TEXT
);
CREATE INDEX idx_interviews_application ON interviews(application_id);
CREATE INDEX idx_interviews_scheduled ON interviews(scheduled_at);

-- Seed data. 'CHANGE_ME' hashes are replaced on startup by DataSeeder
-- with a BCrypt hash of app.seed.default-password (password123).

INSERT INTO companies (name, website, industry, location, email, password_hash) VALUES
('Acme Software', 'https://acme.example', 'Software', 'Sofia', 'hr@acme.example', 'CHANGE_ME'),
('BlueBank', 'https://bluebank.example', 'Finance', 'Plovdiv', 'jobs@bluebank.example', 'CHANGE_ME');

INSERT INTO students (email, password_hash, full_name, university, major, graduation_year, role) VALUES
('admin@jobtracker.example', 'CHANGE_ME', 'Admin User', NULL, NULL, NULL, 'ADMIN'),
('ivan@student.example', 'CHANGE_ME', 'Ivan Petrov', 'Sofia University', 'Computer Science', 2027, 'STUDENT'),
('maria@student.example', 'CHANGE_ME', 'Maria Georgieva', 'TU Sofia', 'Software Engineering', 2026, 'STUDENT'),
('georgi@student.example', 'CHANGE_ME', 'Georgi Ivanov', 'Sofia University', 'Informatics', 2026, 'STUDENT'),
('elena@student.example', 'CHANGE_ME', 'Elena Dimitrova', 'UNWE', 'Business Informatics', 2027, 'STUDENT');

INSERT INTO job_offers (company_id, title, description, job_type, location, salary_min, salary_max, deadline) VALUES
(1, 'Java Intern', 'Backend internship with Spring Boot', 'INTERNSHIP', 'Sofia', 800, 1200, CURRENT_DATE + 30),
(1, 'Junior Backend Developer', 'Full-time junior role', 'FULL_TIME', 'Sofia', 2000, 3000, CURRENT_DATE + 45),
(2, 'QA Part-Time', 'Part-time testing role', 'PART_TIME', 'Plovdiv', 600, 900, CURRENT_DATE + 20);

INSERT INTO applications (student_id, job_offer_id, status, applied_date, updated_at) VALUES
(2, 1, 'APPLIED',      CURRENT_DATE - 5,  NOW()),
(2, 2, 'INTERVIEWING', CURRENT_DATE - 10, NOW()),
(3, 1, 'APPLIED',      CURRENT_DATE - 3,  NOW()),
(3, 3, 'SAVED',        NULL,              NOW()),
-- extra rows so the ranking (6.1), popular offers (6.2) and stale applications (7) show something
(4, 1, 'INTERVIEWING', CURRENT_DATE - 12, NOW() - INTERVAL '8 days'),
(4, 3, 'REJECTED',     CURRENT_DATE - 15, NOW() - INTERVAL '10 days'),
(5, 2, 'OFFER',        CURRENT_DATE - 20, NOW() - INTERVAL '9 days'),
(5, 1, 'APPLIED',      CURRENT_DATE - 25, NOW() - INTERVAL '20 days'),
(3, 2, 'APPLIED',      CURRENT_DATE - 18, NOW() - INTERVAL '16 days');

INSERT INTO interviews (application_id, type, scheduled_at, location_or_link, outcome) VALUES
(2, 'PHONE',     NOW() + INTERVAL '2 days',  'https://meet.example/abc', 'PENDING'),
(5, 'PHONE',     NOW() - INTERVAL '8 days',  'https://meet.example/def', 'PASSED'),
(6, 'TECHNICAL', NOW() - INTERVAL '11 days', 'BlueBank office, Plovdiv', 'FAILED'),
(7, 'TECHNICAL', NOW() - INTERVAL '14 days', 'https://meet.example/ghi', 'PASSED'),
(7, 'HR',        NOW() - INTERVAL '10 days', 'Acme office, Sofia',       'PASSED');
