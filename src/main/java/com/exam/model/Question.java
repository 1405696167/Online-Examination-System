package com.exam.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User teacher;

    @ManyToOne(optional = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType = QuestionType.SINGLE_CHOICE;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String content;

    @Column(length = 500)
    private String optionA;
    @Column(length = 500)
    private String optionB;
    @Column(length = 500)
    private String optionC;
    @Column(length = 500)
    private String optionD;

    @Column(nullable = false, length = 1000)
    private String answer;

    @Column(nullable = false)
    private Integer score = 5;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public QuestionType getQuestionType() { return questionType == null ? QuestionType.SINGLE_CHOICE : questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
