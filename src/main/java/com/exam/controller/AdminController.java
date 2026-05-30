package com.exam.controller;

import com.exam.dto.Requests.*;
import com.exam.dto.Responses.ScoreView;
import com.exam.dto.Responses.TeacherSubjectView;
import com.exam.model.*;
import com.exam.repository.*;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final AdminClassRepository adminClasses;
    private final SubjectRepository subjects;
    private final TeacherSubjectRepository teacherSubjects;
    private final CourseEnrollmentRepository courseEnrollments;
    private final CourseClassRepository courseClasses;
    private final StudentExamRepository studentExams;

    public AdminController(UserRepository users, AdminClassRepository adminClasses, SubjectRepository subjects,
                           TeacherSubjectRepository teacherSubjects, CourseEnrollmentRepository courseEnrollments,
                           CourseClassRepository courseClasses,
                           StudentExamRepository studentExams) {
        this.users = users;
        this.adminClasses = adminClasses;
        this.subjects = subjects;
        this.teacherSubjects = teacherSubjects;
        this.courseEnrollments = courseEnrollments;
        this.courseClasses = courseClasses;
        this.studentExams = studentExams;
    }

    @GetMapping("/teachers")
    public List<User> teachers() {
        return users.findByRole(UserRole.TEACHER).stream()
                .collect(Collectors.toMap(User::getUsername, Function.identity(), (first, ignored) -> first, LinkedHashMap::new))
                .values().stream().toList();
    }

    @PostMapping("/teachers")
    public User createTeacher(@Valid @RequestBody TeacherRequest request) {
        User user = users.findFirstByUsernameAndRoleOrderByIdAsc(request.username(), UserRole.TEACHER).orElseGet(User::new);
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setName(request.name());
        user.setClassName(null);
        user.setRole(UserRole.TEACHER);
        return users.save(user);
    }

    @PutMapping("/teachers/{id}")
    public User updateTeacher(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        User user = users.findById(id).orElseThrow();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setName(request.name());
        user.setClassName(null);
        user.setRole(UserRole.TEACHER);
        return users.save(user);
    }

    @DeleteMapping("/teachers/{id}")
    @Transactional
    public void deleteTeacher(@PathVariable Long id) {
        User teacher = users.findById(id).orElseThrow();
        courseEnrollments.deleteByTeacher(teacher);
        courseClasses.deleteByTeacher(teacher);
        teacherSubjects.deleteByTeacher(teacher);
        users.delete(teacher);
    }

    @GetMapping("/classes")
    public List<AdminClass> classes() {
        return adminClasses.findAll();
    }

    @PostMapping("/classes")
    public AdminClass createClass(@Valid @RequestBody AdminClassRequest request) {
        return adminClasses.findByName(request.name()).orElseGet(() -> {
            AdminClass adminClass = new AdminClass();
            adminClass.setName(request.name());
            return adminClasses.save(adminClass);
        });
    }

    @PutMapping("/classes/{id}")
    public AdminClass updateClass(@PathVariable Long id, @Valid @RequestBody AdminClassRequest request) {
        AdminClass adminClass = adminClasses.findById(id).orElseThrow();
        String oldName = adminClass.getName();
        adminClass.setName(request.name());
        users.findByRoleAndClassName(UserRole.STUDENT, oldName).forEach(student -> {
            student.setClassName(request.name());
            users.save(student);
        });
        return adminClasses.save(adminClass);
    }

    @DeleteMapping("/classes/{id}")
    public void deleteClass(@PathVariable Long id) {
        AdminClass adminClass = adminClasses.findById(id).orElseThrow();
        if (!users.findByRoleAndClassName(UserRole.STUDENT, adminClass.getName()).isEmpty()) {
            throw new IllegalArgumentException("该行政班级已有学生，不能删除");
        }
        adminClasses.delete(adminClass);
    }

    @GetMapping("/students")
    public List<User> students() {
        return users.findByRole(UserRole.STUDENT).stream()
                .collect(Collectors.toMap(User::getStudentNumber, Function.identity(), (first, ignored) -> first, LinkedHashMap::new))
                .values().stream().toList();
    }

    @PostMapping("/students")
    public User createStudent(@Valid @RequestBody StudentRequest request) {
        adminClasses.findByName(request.className()).orElseThrow(() -> new IllegalArgumentException("请先创建行政班级"));
        User user = users.findFirstByStudentNumberOrderByIdAsc(request.studentNumber()).orElseGet(User::new);
        user.setUsername(request.studentNumber());
        user.setStudentNumber(request.studentNumber());
        user.setPassword(request.password());
        user.setName(request.name());
        user.setClassName(request.className());
        user.setRole(UserRole.STUDENT);
        return users.save(user);
    }

    @PutMapping("/students/{id}")
    public User updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        adminClasses.findByName(request.className()).orElseThrow(() -> new IllegalArgumentException("请先创建行政班级"));
        User user = users.findById(id).orElseThrow();
        user.setUsername(request.studentNumber());
        user.setStudentNumber(request.studentNumber());
        user.setPassword(request.password());
        user.setName(request.name());
        user.setClassName(request.className());
        user.setRole(UserRole.STUDENT);
        return users.save(user);
    }

    @DeleteMapping("/students/{id}")
    @Transactional
    public void deleteStudent(@PathVariable Long id) {
        User student = users.findById(id).orElseThrow();
        courseEnrollments.deleteByStudent(student);
        users.delete(student);
    }

    @GetMapping("/subjects")
    public List<Subject> subjects() {
        return subjects.findAll();
    }

    @PostMapping("/subjects")
    public Subject createSubject(@Valid @RequestBody SubjectRequest request) {
        return subjects.findByName(request.name()).orElseGet(() -> {
            Subject subject = new Subject();
            subject.setName(request.name());
            return subjects.save(subject);
        });
    }

    @PutMapping("/subjects/{id}")
    public Subject updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        Subject subject = subjects.findById(id).orElseThrow();
        subject.setName(request.name());
        return subjects.save(subject);
    }

    @DeleteMapping("/subjects/{id}")
    @Transactional
    public void deleteSubject(@PathVariable Long id) {
        Subject subject = subjects.findById(id).orElseThrow();
        courseEnrollments.deleteBySubject(subject);
        courseClasses.deleteBySubject(subject);
        teacherSubjects.deleteBySubject(subject);
        subjects.delete(subject);
    }

    @PostMapping("/teacher-subjects")
    @Transactional
    public List<TeacherSubject> assignSubjects(@Valid @RequestBody AssignSubjectRequest request) {
        User teacher = users.findById(request.teacherId()).orElseThrow();
        teacherSubjects.deleteByTeacher(teacher);
        return request.subjectIds().stream().map(subjectId -> {
            TeacherSubject ts = new TeacherSubject();
            ts.setTeacher(teacher);
            ts.setSubject(subjects.findById(subjectId).orElseThrow());
            return teacherSubjects.save(ts);
        }).toList();
    }

    @GetMapping("/teacher-subjects")
    public List<TeacherSubjectView> teacherSubjectViews() {
        return users.findByRole(UserRole.TEACHER).stream().map(teacher -> {
            List<String> subjectNames = teacherSubjects.findByTeacherId(teacher.getId()).stream()
                    .map(teacherSubject -> teacherSubject.getSubject().getName())
                    .toList();
            return new TeacherSubjectView(teacher.getId(), teacher.getName(), teacher.getUsername(), teacher.getClassName(), subjectNames);
        }).toList();
    }

    @GetMapping("/scores")
    public List<ScoreView> scores() {
        return studentExams.findAll().stream().map(ScoreView::from).toList();
    }

    @PutMapping("/scores/{studentExamId}")
    public ScoreView updateScore(@PathVariable Long studentExamId, @Valid @RequestBody ScoreRequest request) {
        StudentExam se = studentExams.findById(studentExamId).orElseThrow();
        se.setScore(request.score());
        se.setGraded(true);
        return ScoreView.from(studentExams.save(se));
    }
}
