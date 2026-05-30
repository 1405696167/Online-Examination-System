package com.exam.service;

import com.exam.dto.Responses.*;
import com.exam.model.*;
import com.exam.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamService {
    private final UserRepository users;
    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final CourseEnrollmentRepository courseEnrollments;
    private final StudentExamRepository studentExams;
    private final SwitchEventRepository switchEvents;
    private final ObjectMapper mapper;

    public ExamService(UserRepository users, ExamRepository exams, QuestionRepository questions,
                       CourseEnrollmentRepository courseEnrollments,
                       StudentExamRepository studentExams, SwitchEventRepository switchEvents, ObjectMapper mapper) {
        this.users = users;
        this.exams = exams;
        this.questions = questions;
        this.courseEnrollments = courseEnrollments;
        this.studentExams = studentExams;
        this.switchEvents = switchEvents;
        this.mapper = mapper;
    }

    @Transactional
    public PaperView startExam(Long studentId, Long examId) {
        User student = users.findById(studentId).orElseThrow();
        Exam exam = exams.findById(examId).orElseThrow();
        if (exam.getDeadline() != null && LocalDateTime.now().isAfter(exam.getDeadline())) {
            throw new IllegalArgumentException("考试已超过截止时间");
        }
        boolean enrolled = courseEnrollments.findByStudentId(studentId).stream().anyMatch(enrollment -> {
            Long teacherId = enrollment.getCourseClass() != null ? enrollment.getCourseClass().getTeacher().getId() : enrollment.getTeacher().getId();
            Long subjectId = enrollment.getCourseClass() != null ? enrollment.getCourseClass().getSubject().getId() : enrollment.getSubject().getId();
            String className = enrollment.getCourseClass() != null ? enrollment.getCourseClass().getName() : enrollment.getCourseClassName();
            return teacherId.equals(exam.getTeacher().getId())
                    && subjectId.equals(exam.getSubject().getId())
                    && className.equals(exam.getClassName());
        });
        if (student.getRole() != UserRole.STUDENT || !enrolled || !exam.isActive()) {
            throw new IllegalArgumentException("学生未加入该课程班或考试未开放");
        }
        StudentExam se = studentExams.findByStudentIdAndExamId(studentId, examId).orElseGet(() -> {
            List<Question> randomQuestions = questions.randomPaper(exam.getTeacher().getId(), exam.getSubject().getId(), exam.getQuestionCount());
            if (randomQuestions.size() < exam.getQuestionCount()) {
                throw new IllegalArgumentException("题库数量不足，无法生成试卷");
            }
            StudentExam created = new StudentExam();
            created.setStudent(student);
            created.setExam(exam);
            created.setPaperQuestionIds(randomQuestions.stream().map(q -> q.getId().toString()).collect(Collectors.joining(",")));
            return studentExams.save(created);
        });
        List<Long> ids = Arrays.stream(se.getPaperQuestionIds().split(",")).filter(s -> !s.isBlank()).map(Long::valueOf).toList();
        List<Question> paper = ids.stream().map(id -> questions.findById(id).orElseThrow()).toList();
        return new PaperView(se.getId(), ExamView.from(exam), paper.stream().map(QuestionView::from).toList());
    }

    @Transactional
    public ScoreView submit(Long studentExamId, Map<Long, String> answers) {
        StudentExam se = studentExams.findById(studentExamId).orElseThrow();
        if (se.getExam().getDeadline() != null && LocalDateTime.now().isAfter(se.getExam().getDeadline())) {
            throw new IllegalArgumentException("考试已超过截止时间，不能提交");
        }
        List<Long> ids = Arrays.stream(se.getPaperQuestionIds().split(",")).filter(s -> !s.isBlank()).map(Long::valueOf).toList();
        List<Question> paper = ids.stream().map(questions::findById).filter(Optional::isPresent).map(Optional::get).toList();
        int total = paper.stream()
                .filter(q -> q.getQuestionType() != QuestionType.ESSAY)
                .filter(q -> Objects.equals(normalize(q.getAnswer()), normalize(answers.getOrDefault(q.getId(), ""))))
                .mapToInt(Question::getScore).sum();
        boolean needsManualGrading = paper.stream().anyMatch(q -> q.getQuestionType() == QuestionType.ESSAY);
        se.setScore(total);
        se.setGraded(!needsManualGrading);
        se.setSubmittedAt(LocalDateTime.now());
        try {
            se.setAnswersJson(mapper.writeValueAsString(answers));
        } catch (JsonProcessingException e) {
            se.setAnswersJson("{}");
        }
        return ScoreView.from(studentExams.save(se));
    }

    @Transactional
    public ReviewView grade(Long studentExamId, Map<Long, Integer> manualScores) {
        StudentExam se = studentExams.findById(studentExamId).orElseThrow();
        if (se.getSubmittedAt() == null) {
            throw new IllegalArgumentException("学生尚未交卷，不能阅卷");
        }
        List<Question> paper = paperQuestions(se);
        Map<Long, String> answers = readAnswers(se.getAnswersJson());
        int autoScore = paper.stream()
                .filter(q -> q.getQuestionType() != QuestionType.ESSAY)
                .filter(q -> Objects.equals(normalize(q.getAnswer()), normalize(answers.getOrDefault(q.getId(), ""))))
                .mapToInt(Question::getScore).sum();
        int manualScore = paper.stream()
                .filter(q -> q.getQuestionType() == QuestionType.ESSAY)
                .mapToInt(q -> Math.max(0, Math.min(q.getScore(), manualScores.getOrDefault(q.getId(), 0))))
                .sum();
        se.setManualScoresJson(writeJson(manualScores));
        se.setScore(autoScore + manualScore);
        se.setGraded(true);
        studentExams.save(se);
        return review(se);
    }

    public List<ReviewView> reviews(Long teacherId) {
        return studentExams.findByExamTeacherId(teacherId).stream()
                .filter(se -> se.getSubmittedAt() != null)
                .filter(se -> paperQuestions(se).stream().anyMatch(q -> q.getQuestionType() == QuestionType.ESSAY))
                .map(this::review)
                .toList();
    }

    private ReviewView review(StudentExam se) {
        Map<Long, String> answers = readAnswers(se.getAnswersJson());
        Map<Long, Integer> manualScores = readManualScores(se.getManualScoresJson());
        List<ReviewQuestionView> reviewQuestions = paperQuestions(se).stream()
                .map(q -> new ReviewQuestionView(q.getId(), q.getQuestionType(), q.getContent(),
                        answers.getOrDefault(q.getId(), ""), q.getAnswer(), q.getScore(), manualScores.get(q.getId())))
                .toList();
        return new ReviewView(se.getId(), se.getStudent().getName(), se.getStudent().getStudentNumber(),
                se.getExam().getTitle(), se.getExam().getSubject().getName(), se.getScore(), se.isGraded(), reviewQuestions);
    }

    private List<Question> paperQuestions(StudentExam se) {
        return Arrays.stream(se.getPaperQuestionIds().split(","))
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .map(questions::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Map<Long, String> readAnswers(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> raw = mapper.readValue(json, new TypeReference<Map<String, String>>() {});
            return raw.entrySet().stream().collect(Collectors.toMap(e -> Long.valueOf(e.getKey()), Map.Entry::getValue));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<Long, Integer> readManualScores(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Integer> raw = mapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
            return raw.entrySet().stream().collect(Collectors.toMap(e -> Long.valueOf(e.getKey()), Map.Entry::getValue));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toUpperCase();
    }

    @Transactional
    public void recordSwitch(Long studentExamId, String eventType, String visibilityState) {
        try {
            StudentExam se = studentExams.getReferenceById(studentExamId);
            SwitchEvent event = new SwitchEvent();
            event.setStudentExam(se);
            event.setEventType(eventType);
            event.setVisibilityState(visibilityState);
            switchEvents.save(event);
            studentExams.incrementSwitchCount(studentExamId);
        } catch (CannotAcquireLockException ignored) {
            // 高频切屏事件可能同时到达，监控记录失败不应中断学生考试。
        }
    }
}
