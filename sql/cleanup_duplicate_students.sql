USE online_exam;

-- 可选脚本：保留每个学号最早创建的学生记录，删除重复学生记录。
-- 执行前建议先备份数据库。
DELETE u FROM users u
JOIN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY student_number ORDER BY id) AS rn
        FROM users
        WHERE role = 'STUDENT' AND student_number IS NOT NULL
    ) ranked
    WHERE ranked.rn > 1
) duplicates ON duplicates.id = u.id;
