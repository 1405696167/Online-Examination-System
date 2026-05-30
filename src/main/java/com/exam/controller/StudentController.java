package com.exam.controller;

import com.exam.dto.Requests.SubmitRequest;
import com.exam.dto.Requests.SwitchEventRequest;
import com.exam.dto.Responses.*;
import com.exam.model.Exam;
import com.exam.model.User;
import com.exam.repository.ExamRepository;
import com.exam.repository.CourseEnrollmentRepository;
import com.exam.repository.StudentExamRepository;
import com.exam.repository.UserRepository;
import com.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    private final UserRepository users;
    private final ExamRepository exams;
    private final CourseEnrollmentRepository courseEnrollments;
    private final StudentExamRepository studentExams;
    private final ExamService examService;

    public StudentController(UserRepository users, ExamRepository exams, CourseEnrollmentRepository courseEnrollments,
                             StudentExamRepository studentExams, ExamService examService) {
        this.users = users;
        this.exams = exams;
        this.courseEnrollments = courseEnrollments;
        this.studentExams = studentExams;
        this.examService = examService;
    }

    @GetMapping("/{studentId}/exams")
    public List<ExamView> exams(@PathVariable Long studentId) {
        User student = users.findById(studentId).orElseThrow();
        List<Long> submitted = studentExams.findByStudentId(studentId).stream()
                .filter(se -> se.getSubmittedAt() != null)
                .map(se -> se.getExam().getId())
                .toList();
        return courseEnrollments.findByStudentId(student.getId()).stream()
                .flatMap(enrollment -> exams.findByTeacherIdAndSubjectIdAndClassNameAndActiveTrue(
                        enrollment.getCourseClass() != null ? enrollment.getCourseClass().getTeacher().getId() : enrollment.getTeacher().getId(),
                        enrollment.getCourseClass() != null ? enrollment.getCourseClass().getSubject().getId() : enrollment.getSubject().getId(),
                        enrollment.getCourseClass() != null ? enrollment.getCourseClass().getName() : enrollment.getCourseClassName()).stream())
                .filter(exam -> exam.getDeadline() == null || LocalDateTime.now().isBefore(exam.getDeadline()))
                .filter(exam -> !submitted.contains(exam.getId()))
                .distinct()
                .map(ExamView::from)
                .toList();
    }

    @PostMapping("/{studentId}/exams/{examId}/start")
    public PaperView start(@PathVariable Long studentId, @PathVariable Long examId) {
        return examService.startExam(studentId, examId);
    }

    @PostMapping("/submit")
    public ScoreView submit(@Valid @RequestBody SubmitRequest request) {
        return examService.submit(request.studentExamId(), request.answers());
    }

    @PostMapping("/switch-events")
    public void switchEvent(@Valid @RequestBody SwitchEventRequest request) {
        examService.recordSwitch(request.studentExamId(), request.eventType(), request.visibilityState());
    }
}
