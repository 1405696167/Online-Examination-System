package com.exam.dto;

import com.exam.model.*;
import java.time.LocalDateTime;
import java.util.List;

public class Responses {
    public record LoginResponse(Long id, String username, String name, UserRole role, String className, String studentNumber, String token) {
        public static LoginResponse from(User user, String token) {
            return new LoginResponse(user.getId(), user.getUsername(), user.getName(), user.getRole(), user.getClassName(), user.getStudentNumber(), token);
        }
    }

    public record QuestionView(Long id, Long subjectId, String subjectName, QuestionType questionType, String content, String optionA, String optionB,
                               String optionC, String optionD, Integer score) {
        public static QuestionView from(Question q) {
            return new QuestionView(q.getId(), q.getSubject().getId(), q.getSubject().getName(), q.getQuestionType(), q.getContent(),
                    q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getScore());
        }
    }

    public record ExamView(Long id, String title, Long subjectId, String subjectName, String teacherName,
                           String className, Integer questionCount, Integer durationMinutes, boolean active, LocalDateTime deadline) {
        public static ExamView from(Exam exam) {
            return new ExamView(exam.getId(), exam.getTitle(), exam.getSubject().getId(), exam.getSubject().getName(),
                    exam.getTeacher().getName(), exam.getClassName(), exam.getQuestionCount(), exam.getDurationMinutes(), exam.isActive(), exam.getDeadline());
        }
    }

    public record PaperView(Long studentExamId, ExamView exam, List<QuestionView> questions) {}

    public record TeacherSubjectView(Long teacherId, String teacherName, String username, String className, List<String> subjects) {}

    public record CourseClassView(Long id, Long teacherId, String teacherName, Long subjectId, String subjectName, String name) {
        public static CourseClassView from(CourseClass courseClass) {
            return new CourseClassView(courseClass.getId(), courseClass.getTeacher().getId(), courseClass.getTeacher().getName(),
                    courseClass.getSubject().getId(), courseClass.getSubject().getName(), courseClass.getName());
        }
    }

    public record EnrollmentView(Long id, String studentName, String studentNumber, String adminClassName,
                                 String subjectName, String courseClassName, String teacherName) {
        public static EnrollmentView from(CourseEnrollment enrollment) {
            User student = enrollment.getStudent();
            String subjectName = enrollment.getCourseClass() != null ? enrollment.getCourseClass().getSubject().getName() : enrollment.getSubject().getName();
            String className = enrollment.getCourseClass() != null ? enrollment.getCourseClass().getName() : enrollment.getCourseClassName();
            String teacherName = enrollment.getCourseClass() != null ? enrollment.getCourseClass().getTeacher().getName() : enrollment.getTeacher().getName();
            return new EnrollmentView(enrollment.getId(), student.getName(), student.getStudentNumber(), student.getClassName(),
                    subjectName, className, teacherName);
        }
    }

    public record ScoreView(Long id, String studentName, String studentNumber, String className, String examTitle,
                            String subjectName, Integer score, Integer switchCount, boolean graded, LocalDateTime submittedAt) {
        public static ScoreView from(StudentExam se) {
            User s = se.getStudent();
            Exam e = se.getExam();
            return new ScoreView(se.getId(), s.getName(), s.getStudentNumber(), s.getClassName(), e.getTitle(),
                    e.getSubject().getName(), se.getScore(), se.getSwitchCount(), se.isGraded(), se.getSubmittedAt());
        }
    }

    public record ReviewQuestionView(Long id, QuestionType questionType, String content, String studentAnswer,
                                     String referenceAnswer, Integer maxScore, Integer manualScore) {}

    public record ReviewView(Long studentExamId, String studentName, String studentNumber, String examTitle,
                             String subjectName, Integer currentScore, boolean graded, List<ReviewQuestionView> questions) {}
}
