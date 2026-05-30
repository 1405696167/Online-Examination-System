INSERT INTO users (username, password, name, role, class_name, student_number)
SELECT 'admin', 'admin123', '系统管理员', 'ADMIN', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin' AND role = 'ADMIN');

INSERT INTO admin_class (name)
SELECT DISTINCT class_name FROM users
WHERE role = 'STUDENT' AND class_name IS NOT NULL AND class_name <> ''
  AND NOT EXISTS (SELECT 1 FROM admin_class WHERE admin_class.name = users.class_name);
