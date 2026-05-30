package com.exam.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "subject_id", "name"}))
public class CourseClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User teacher;

    @ManyToOne(optional = false)
    private Subject subject;

    @NotBlank
    @Column(nullable = false)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
