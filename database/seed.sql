-- Roles
INSERT INTO ROLES (role_name) VALUES ('admin');
INSERT INTO ROLES (role_name) VALUES ('teacher');
INSERT INTO ROLES (role_name) VALUES ('student');

-- Users
INSERT INTO USERS (name, email, password, role_id)
VALUES ('Admin Demo', 'admin@demo.com', 'admin123', (SELECT role_id FROM ROLES WHERE role_name='admin'));

INSERT INTO USERS (name, email, password, role_id)
VALUES ('Teacher Demo', 'teacher@demo.com', 'teacher123', (SELECT role_id FROM ROLES WHERE role_name='teacher'));

INSERT INTO USERS (name, email, password, role_id)
VALUES ('Student Demo', 'student@demo.com', 'student123', (SELECT role_id FROM ROLES WHERE role_name='student'));

-- Teacher mapping
INSERT INTO TEACHERS (teacher_id, user_id)
VALUES (1, (SELECT user_id FROM USERS WHERE email='teacher@demo.com'));

-- Department and batch
INSERT INTO DEPARTMENTS (dept_id, dept_name) VALUES (1, 'Computer Science and Engineering');
INSERT INTO BATCHES (batch_id, dept_id, year) VALUES (1, 1, 2025);

-- Sections A-D only
INSERT INTO SECTIONS (section_id, batch_id, section_name) VALUES (1, 1, 'A');
INSERT INTO SECTIONS (section_id, batch_id, section_name) VALUES (2, 1, 'B');
INSERT INTO SECTIONS (section_id, batch_id, section_name) VALUES (3, 1, 'C');
INSERT INTO SECTIONS (section_id, batch_id, section_name) VALUES (4, 1, 'D');

-- BSc in CSE subjects
INSERT INTO SUBJECTS (subject_id, subject_code, name, dept_id) VALUES (1, 'CSE-201', 'Data Structures', 1);
INSERT INTO SUBJECTS (subject_id, subject_code, name, dept_id) VALUES (2, 'CSE-203', 'Discrete Mathematics', 1);
INSERT INTO SUBJECTS (subject_id, subject_code, name, dept_id) VALUES (3, 'CSE-205', 'Object Oriented Programming', 1);
INSERT INTO SUBJECTS (subject_id, subject_code, name, dept_id) VALUES (4, 'CSE-207', 'Digital Logic Design', 1);
INSERT INTO SUBJECTS (subject_id, subject_code, name, dept_id) VALUES (5, 'CSE-209', 'Database Systems', 1);
INSERT INTO SUBJECTS (subject_id, subject_code, name, dept_id) VALUES (6, 'CSE-211', 'Algorithms', 1);

-- Fixed rooms
INSERT INTO CLASSROOMS (room_id, room_name, capacity) VALUES (101, 'CSE Lab 1', 45);
INSERT INTO CLASSROOMS (room_id, room_name, capacity) VALUES (102, 'CSE Lab 2', 45);
INSERT INTO CLASSROOMS (room_id, room_name, capacity) VALUES (201, 'CSE Room 201', 60);
INSERT INTO CLASSROOMS (room_id, room_name, capacity) VALUES (202, 'CSE Room 202', 60);
INSERT INTO CLASSROOMS (room_id, room_name, capacity) VALUES (301, 'CSE Seminar Hall', 100);

-- Demo schedule
INSERT INTO SCHEDULE (
  dept_id, batch_id, section_id, subject_id, teacher_id, room_id,
  day, time_start, time_end, status
)
VALUES (
  1, 1, 1, 1, 1, 201,
  'Monday',
  TO_DATE('09:00','HH24:MI'),
  TO_DATE('10:30','HH24:MI'),
  'APPROVED'
);

INSERT INTO SCHEDULE (
  dept_id, batch_id, section_id, subject_id, teacher_id, room_id,
  day, time_start, time_end, status
)
VALUES (
  1, 1, 2, 2, 1, 202,
  'Tuesday',
  TO_DATE('10:30','HH24:MI'),
  TO_DATE('12:00','HH24:MI'),
  'APPROVED'
);

-- Announcement
INSERT INTO ANNOUNCEMENTS (message) VALUES ('Welcome to EduFlow demo environment.');

-- Default weekend policy
INSERT INTO DAY_SETTINGS (day_name, day_type, updated_by)
VALUES ('Saturday', 'WEEKEND', (SELECT user_id FROM USERS WHERE email='admin@demo.com'));
INSERT INTO DAY_SETTINGS (day_name, day_type, updated_by)
VALUES ('Sunday', 'WEEKEND', (SELECT user_id FROM USERS WHERE email='admin@demo.com'));
