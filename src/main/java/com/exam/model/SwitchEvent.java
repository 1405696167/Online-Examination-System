package com.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class SwitchEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private StudentExam studentExam;

    @Column(nullable = false)
    private String eventType;

    private String visibilityState;
    private LocalDateTime happenedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StudentExam getStudentExam() { return studentExam; }
    public void setStudentExam(StudentExam studentExam) { this.studentExam = studentExam; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getVisibilityState() { return visibilityState; }
    public void setVisibilityState(String visibilityState) { this.visibilityState = visibilityState; }
    public LocalDateTime getHappenedAt() { return happenedAt; }
    public void setHappenedAt(LocalDateTime happenedAt) { this.happenedAt = happenedAt; }
}
