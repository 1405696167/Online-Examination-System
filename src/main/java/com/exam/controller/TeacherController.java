package com.exam.controller;

import com.exam.dto.Requests.*;
import com.exam.dto.Responses.*;
import com.exam.model.*;
import com.exam.repository.*;
import com.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private final UserRepository users;
    private final SubjectRepository subjects;
    private final TeacherSubjectRepository teacherSubjects;
    private final CourseEnrollmentRepository courseEnrollments;
    private final CourseClassRepository courseClasses;
    private final QuestionRepository questions;
    private final ExamRepository exams;
    private final StudentExamRepository studentExams;
    private final ExamService examService;

    public TeacherController(UserRepository users, SubjectRepository subjects, TeacherSubjectRepository teacherSubjects,
                             CourseEnrollmentRepository courseEnrollments, CourseClassRepository courseClasses, QuestionRepository questions,
                             ExamRepository exams, StudentExamRepository studentExams, ExamService examService) {
        this.users = users;
        this.subjects = subjects;
        this.teacherSubjects = teacherSubjects;
        this.courseEnrollments = courseEnrollments;
        this.courseClasses = courseClasses;
        this.questions = questions;
        this.exams = exams;
        this.studentExams = studentExams;
        this.examService = examService;
    }

    @GetMapping("/{teacherId}/subjects")
    public List<Subject> mySubjects(@PathVariable Long teacherId) {
        return teacherSubjects.findByTeacherId(teacherId).stream().map(TeacherSubject::getSubject).toList();
    }

    @GetMapping("/{teacherId}/students")
    public List<EnrollmentView> myStudents(@PathVariable Long teacherId) {
        return courseEnrollments.findByTeacherId(teacherId).stream().map(EnrollmentView::from).toList();
    }

    @GetMapping("/{teacherId}/course-classes")
    public List<CourseClassView> courseClasses(@PathVariable Long teacherId) {
        return courseClasses.findByTeacherId(teacherId).stream().map(CourseClassView::from).toList();
    }

    @PostMapping("/course-classes")
    public CourseClassView createCourseClass(@Valid @RequestBody CourseClassRequest request) {
        User teacher = users.findById(request.teacherId()).orElseThrow();
        Subject subject = subjects.findById(request.subjectId()).orElseThrow();
        teacherSubjects.findByTeacherIdAndSubjectId(request.teacherId(), request.subjectId())
                .orElseThrow(() -> new IllegalArgumentException("教师未被分配该科目"));
        CourseClass courseClass = courseClasses
                .findByTeacherIdAndSubjectIdAndName(request.teacherId(), request.subjectId(), request.name())
                .orElseGet(CourseClass::new);
        courseClass.setTeacher(teacher);
        courseClass.setSubject(subject);
        courseClass.setName(request.name());
        return CourseClassView.from(courseClasses.save(courseClass));
    }

    @DeleteMapping("/course-classes/{id}")
    @Transactional
    public void deleteCourseClass(@PathVariable Long id) {
        CourseClass courseClass = courseClasses.findById(id).orElseThrow();
        courseEnrollments.deleteByCourseClass(courseClass);
        courseClasses.delete(courseClass);
    }

    @PostMapping("/students/import-class")
    public List<EnrollmentView> importAdminClassStudents(@Valid @RequestBody BatchImportClassRequest request) {
        CourseClass courseClass = courseClasses.findById(request.courseClassId()).orElseThrow();
        if (!courseClass.getTeacher().getId().equals(request.teacherId())) {
            throw new IllegalArgumentException("课程班不属于当前教师");
        }
        List<User> students = users.findByRoleAndClassName(UserRole.STUDENT, request.adminClassName());
        if (students.isEmpty()) {
            throw new IllegalArgumentException("该行政班级暂无学生");
        }
        return students.stream().map(student -> {
            return EnrollmentView.from(enrollStudent(student, courseClass));
        }).toList();
    }

    @PostMapping("/students/single")
    public EnrollmentView addSingleStudent(@Valid @RequestBody SingleCourseStudentRequest request) {
        CourseClass courseClass = courseClasses.findById(request.courseClassId()).orElseThrow();
        if (!courseClass.getTeacher().getId().equals(request.teacherId())) {
            throw new IllegalArgumentException("课程班不属于当前教师");
        }
        User student = users.findFirstByStudentNumberOrderByIdAsc(request.studentNumber()).orElseGet(User::new);
        student.setUsername(request.studentNumber());
        student.setStudentNumber(request.studentNumber());
        student.setName(request.name());
        if (student.getPassword() == null || student.getPassword().isBlank()) {
            student.setPassword("123456");
        }
        if (student.getClassName() == null || student.getClassName().isBlank()) {
            student.setClassName("未分配行政班");
        }
        student.setRole(UserRole.STUDENT);
        return EnrollmentView.from(enrollStudent(users.save(student), courseClass));
    }

    @PostMapping("/students/import")
    public List<EnrollmentView> importStudents(@Valid @RequestBody ImportStudentsRequest request) {
        User teacher = users.findById(request.teacherId()).orElseThrow();
        Subject subject = subjects.findById(request.subjectId()).orElseThrow();
        teacherSubjects.findByTeacherIdAndSubjectId(request.teacherId(), request.subjectId())
                .orElseThrow(() -> new IllegalArgumentException("教师未被分配该科目"));
        return request.students().stream().map(s -> {
            if (s.studentNumber() == null || s.studentNumber().isBlank() || s.name() == null || s.name().isBlank()) {
                throw new IllegalArgumentException("每行必须包含姓名和学号");
            }
            User user = users.findFirstByStudentNumberOrderByIdAsc(s.studentNumber()).orElseGet(User::new);
            user.setUsername(s.studentNumber());
            user.setStudentNumber(s.studentNumber());
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword("123456");
            }
            user.setName(s.name());
            if (user.getClassName() == null || user.getClassName().isBlank()) {
                user.setClassName("未分配行政班");
            }
            user.setRole(UserRole.STUDENT);
            User savedStudent = users.save(user);
            CourseEnrollment enrollment = courseEnrollments
                    .findByStudentIdAndTeacherIdAndSubjectIdAndCourseClassName(savedStudent.getId(), teacher.getId(), subject.getId(), request.courseClassName())
                    .orElseGet(CourseEnrollment::new);
            enrollment.setStudent(savedStudent);
            enrollment.setTeacher(teacher);
            enrollment.setSubject(subject);
            enrollment.setCourseClassName(request.courseClassName());
            return EnrollmentView.from(courseEnrollments.save(enrollment));
        }).toList();
    }

    @DeleteMapping("/students/{enrollmentId}")
    public void removeStudentFromCourse(@PathVariable Long enrollmentId) {
        courseEnrollments.deleteById(enrollmentId);
    }

    @GetMapping("/{teacherId}/questions")
    public List<Question> questions(@PathVariable Long teacherId) {
        return questions.findByTeacherId(teacherId);
    }

    @PostMapping("/questions")
    public Question createQuestion(@Valid @RequestBody QuestionRequest request) {
        return saveQuestion(new Question(), request);
    }

    @PutMapping("/questions/{id}")
    public Question updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return saveQuestion(questions.findById(id).orElseThrow(), request);
    }

    @DeleteMapping("/questions/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        questions.deleteById(id);
    }

    @GetMapping("/{teacherId}/exams")
    public List<ExamView> exams(@PathVariable Long teacherId) {
        return exams.findByTeacherId(teacherId).stream().map(ExamView::from).toList();
    }

    @PostMapping("/exams")
    public ExamView createExam(@Valid @RequestBody ExamRequest request) {
        Exam exam = new Exam();
        exam.setTeacher(users.findById(request.teacherId()).orElseThrow());
        exam.setSubject(subjects.findById(request.subjectId()).orElseThrow());
        exam.setTitle(request.title());
        exam.setClassName(request.className());
        exam.setQuestionCount(request.questionCount());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setActive(request.active());
        exam.setDeadline(parseDeadline(request.deadline()));
        return ExamView.from(exams.save(exam));
    }

    @PutMapping("/exams/{id}")
    public ExamView updateExam(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        Exam exam = exams.findById(id).orElseThrow();
        exam.setSubject(subjects.findById(request.subjectId()).orElseThrow());
        exam.setTitle(request.title());
        exam.setClassName(request.className());
        exam.setQuestionCount(request.questionCount());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setActive(request.active());
        exam.setDeadline(parseDeadline(request.deadline()));
        return ExamView.from(exams.save(exam));
    }

    @DeleteMapping("/exams/{id}")
    public void deleteExam(@PathVariable Long id) {
        exams.deleteById(id);
    }

    @GetMapping("/{teacherId}/scores")
    public List<ScoreView> scores(@PathVariable Long teacherId) {
        return studentExams.findByExamTeacherId(teacherId).stream().map(ScoreView::from).toList();
    }

    @GetMapping("/{teacherId}/reviews")
    public List<ReviewView> reviews(@PathVariable Long teacherId) {
        return examService.reviews(teacherId);
    }

    @PostMapping("/reviews/{studentExamId}")
    public ReviewView grade(@PathVariable Long studentExamId, @Valid @RequestBody GradeRequest request) {
        return examService.grade(studentExamId, request.manualScores());
    }

    @PutMapping("/scores/{studentExamId}")
    public ScoreView updateScore(@PathVariable Long studentExamId, @Valid @RequestBody ScoreRequest request) {
        StudentExam se = studentExams.findById(studentExamId).orElseThrow();
        se.setScore(request.score());
        se.setGraded(true);
        return ScoreView.from(studentExams.save(se));
    }

    @DeleteMapping("/scores/{studentExamId}")
    public void deleteScore(@PathVariable Long studentExamId) {
        studentExams.deleteById(studentExamId);
    }

    private Question saveQuestion(Question question, QuestionRequest request) {
        teacherSubjects.findByTeacherIdAndSubjectId(request.teacherId(), request.subjectId())
                .orElseThrow(() -> new IllegalArgumentException("教师未被分配该科目"));
        question.setTeacher(users.findById(request.teacherId()).orElseThrow());
        question.setSubject(subjects.findById(request.subjectId()).orElseThrow());
        question.setQuestionType(request.questionType());
        question.setContent(request.content());
        question.setOptionA(request.optionA() == null ? "" : request.optionA());
        question.setOptionB(request.optionB() == null ? "" : request.optionB());
        question.setOptionC(request.optionC() == null ? "" : request.optionC());
        question.setOptionD(request.optionD() == null ? "" : request.optionD());
        question.setAnswer(request.answer());
        question.setScore(request.score());
        return questions.save(question);
    }

    private LocalDateTime parseDeadline(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value);
    }

    private CourseEnrollment enrollStudent(User student, CourseClass courseClass) {
        CourseEnrollment enrollment = courseEnrollments.findByStudentIdAndCourseClassId(student.getId(), courseClass.getId())
                .orElseGet(CourseEnrollment::new);
        enrollment.setStudent(student);
        enrollment.setTeacher(courseClass.getTeacher());
        enrollment.setSubject(courseClass.getSubject());
        enrollment.setCourseClass(courseClass);
        enrollment.setCourseClassName(courseClass.getName());
        return courseEnrollments.save(enrollment);
    }
}
