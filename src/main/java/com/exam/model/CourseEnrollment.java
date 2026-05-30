package com.exam.model;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_class_id"}))
public class CourseEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User student;

    @ManyToOne(optional = false)
    private User teacher;

    @ManyToOne(optional = false)
    private Subject subject;

    @ManyToOne
    private CourseClass courseClass;

    @Column(name = "course_class_name", nullable = false)
    private String courseClassName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public CourseClass getCourseClass() { return courseClass; }
    public void setCourseClass(CourseClass courseClass) { this.courseClass = courseClass; }
    public String getCourseClassName() { return courseClassName; }
    public void setCourseClassName(String courseClassName) { this.courseClassName = courseClassName; }
}
