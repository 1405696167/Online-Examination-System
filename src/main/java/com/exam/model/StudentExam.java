package com.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "exam_id"}))
public class StudentExam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User student;

    @ManyToOne(optional = false)
    private Exam exam;

    @Column(length = 4000)
    private String paperQuestionIds;

    @Column(length = 8000)
    private String answersJson;

    @Column(length = 4000)
    private String manualScoresJson;

    private Integer score = 0;
    private Integer switchCount = 0;
    private boolean graded = false;
    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime submittedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }
    public String getPaperQuestionIds() { return paperQuestionIds; }
    public void setPaperQuestionIds(String paperQuestionIds) { this.paperQuestionIds = paperQuestionIds; }
    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }
    public String getManualScoresJson() { return manualScoresJson; }
    public void setManualScoresJson(String manualScoresJson) { this.manualScoresJson = manualScoresJson; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getSwitchCount() { return switchCount; }
    public void setSwitchCount(Integer switchCount) { this.switchCount = switchCount; }
    public boolean isGraded() { return graded; }
    public void setGraded(boolean graded) { this.graded = graded; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
