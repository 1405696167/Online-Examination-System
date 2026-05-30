package com.exam.config;

import com.exam.model.*;
import com.exam.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final AdminClassRepository adminClasses;
    private final SubjectRepository subjects;
    private final TeacherSubjectRepository teacherSubjects;
    private final CourseClassRepository courseClasses;
    private final QuestionRepository questions;

    public DataSeeder(UserRepository users, AdminClassRepository adminClasses, SubjectRepository subjects,
                      TeacherSubjectRepository teacherSubjects, CourseClassRepository courseClasses,
                      QuestionRepository questions) {
        this.users = users;
        this.adminClasses = adminClasses;
        this.subjects = subjects;
        this.teacherSubjects = teacherSubjects;
        this.courseClasses = courseClasses;
        this.questions = questions;
    }

    @Override
    public void run(String... args) {
        AdminClass adminClass = adminClasses.findByName("计算机1班").orElseGet(() -> {
            AdminClass created = new AdminClass();
            created.setName("计算机1班");
            return adminClasses.save(created);
        });

        User teacher = users.findFirstByUsernameAndRoleOrderByIdAsc("teacher01", UserRole.TEACHER).orElseGet(User::new);
        teacher.setUsername("teacher01");
        teacher.setPassword("123456");
        teacher.setName("测试教师");
        teacher.setRole(UserRole.TEACHER);
        User savedTeacher = users.save(teacher);

        User student = users.findFirstByStudentNumberOrderByIdAsc("20260001").orElseGet(User::new);
        student.setUsername("20260001");
        student.setStudentNumber("20260001");
        student.setPassword("123456");
        student.setName("测试学生");
        student.setClassName(adminClass.getName());
        student.setRole(UserRole.STUDENT);
        users.save(student);

        Subject subject = subjects.findByName("Java程序设计").orElseGet(() -> {
            Subject created = new Subject();
            created.setName("Java程序设计");
            return subjects.save(created);
        });

        teacherSubjects.findByTeacherIdAndSubjectId(savedTeacher.getId(), subject.getId()).orElseGet(() -> {
            TeacherSubject created = new TeacherSubject();
            created.setTeacher(savedTeacher);
            created.setSubject(subject);
            return teacherSubjects.save(created);
        });

        courseClasses.findByTeacherIdAndSubjectIdAndName(savedTeacher.getId(), subject.getId(), "Java1班").orElseGet(() -> {
            CourseClass created = new CourseClass();
            created.setTeacher(savedTeacher);
            created.setSubject(subject);
            created.setName("Java1班");
            return courseClasses.save(created);
        });

        if (questions.findByTeacherIdAndSubjectId(savedTeacher.getId(), subject.getId()).isEmpty()) {
            saveQuestion(savedTeacher, subject, QuestionType.SINGLE_CHOICE, "Java 中用于定义类的关键字是？", "class", "def", "function", "struct", "A", 5);
            saveQuestion(savedTeacher, subject, QuestionType.SINGLE_CHOICE, "JVM 的中文含义是？", "Java变量模型", "Java虚拟机", "Java视图模块", "Java版本管理", "B", 5);
            saveQuestion(savedTeacher, subject, QuestionType.BLANK, "Java 程序入口方法名是 ______。", "", "", "", "", "main", 5);
            saveQuestion(savedTeacher, subject, QuestionType.BLANK, "面向对象三大特性包括封装、继承和 ______。", "", "", "", "", "多态", 5);
            saveQuestion(savedTeacher, subject, QuestionType.ESSAY, "简述 Java 中接口和抽象类的主要区别。", "", "", "", "", "接口强调能力约束，抽象类强调共同父类抽象；接口可多实现，抽象类只能单继承。", 15);
            saveQuestion(savedTeacher, subject, QuestionType.ESSAY, "请说明异常处理 try-catch-finally 的执行流程。", "", "", "", "", "try 执行业务代码，catch 捕获并处理异常，finally 通常执行资源释放。", 15);
        }
    }

    private void saveQuestion(User teacher, Subject subject, QuestionType type, String content,
                              String optionA, String optionB, String optionC, String optionD,
                              String answer, int score) {
        Question question = new Question();
        question.setTeacher(teacher);
        question.setSubject(subject);
        question.setQuestionType(type);
        question.setContent(content);
        question.setOptionA(optionA);
        question.setOptionB(optionB);
        question.setOptionC(optionC);
        question.setOptionD(optionD);
        question.setAnswer(answer);
        question.setScore(score);
        questions.save(question);
    }
}
