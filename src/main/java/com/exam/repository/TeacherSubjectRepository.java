package com.exam.repository;

import com.exam.model.TeacherSubject;
import com.exam.model.Subject;
import com.exam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {
    List<TeacherSubject> findByTeacherId(Long teacherId);
    Optional<TeacherSubject> findByTeacherIdAndSubjectId(Long teacherId, Long subjectId);
    void deleteByTeacher(User teacher);
    void deleteBySubject(Subject subject);
}
