package com.exam.dto;

import com.exam.model.UserRole;
import com.exam.model.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class Requests {
    public record LoginRequest(@NotBlank String username, @NotBlank String password, @NotNull UserRole role) {}
    public record TeacherRequest(@NotBlank String username, @NotBlank String password, @NotBlank String name, String className) {}
    public record StudentRequest(@NotBlank String studentNumber, @NotBlank String password, @NotBlank String name, @NotBlank String className) {}
    public record ImportStudentRequest(@NotBlank String studentNumber, @NotBlank String name) {}
    public record AdminClassRequest(@NotBlank String name) {}
    public record CourseClassRequest(@NotNull Long teacherId, @NotNull Long subjectId, @NotBlank String name) {}
    public record SingleCourseStudentRequest(@NotNull Long teacherId, @NotNull Long courseClassId,
                                             @NotBlank String name, @NotBlank String studentNumber) {}
    public record SubjectRequest(@NotBlank String name) {}
    public record AssignSubjectRequest(@NotNull Long teacherId, @NotNull List<Long> subjectIds) {}
    public record QuestionRequest(@NotNull Long teacherId, @NotNull Long subjectId, @NotBlank String content,
                                  @NotNull QuestionType questionType,
                                  String optionA, String optionB,
                                  String optionC, String optionD,
                                  @NotBlank String answer, @NotNull Integer score) {}
    public record ExamRequest(@NotNull Long teacherId, @NotNull Long subjectId, @NotBlank String title,
                              @NotBlank String className, @NotNull Integer questionCount,
                              @NotNull Integer durationMinutes, boolean active, String deadline) {}
    public record ImportStudentsRequest(@NotNull Long teacherId, @NotNull Long subjectId, @NotBlank String courseClassName,
                                        @NotNull List<ImportStudentRequest> students) {}
    public record BatchImportClassRequest(@NotNull Long teacherId, @NotNull Long courseClassId, @NotBlank String adminClassName) {}
    public record SubmitRequest(@NotNull Long studentExamId, @NotNull Map<Long, String> answers) {}
    public record SwitchEventRequest(@NotNull Long studentExamId, @NotBlank String eventType, String visibilityState) {}
    public record ScoreRequest(@NotNull Integer score) {}
    public record GradeRequest(@NotNull Map<Long, Integer> manualScores) {}
}
