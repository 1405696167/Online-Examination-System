package com.exam.repository;

import com.exam.model.CourseClass;
import com.exam.model.Subject;
import com.exam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseClassRepository extends JpaRepository<CourseClass, Long> {
    List<CourseClass> findByTeacherId(Long teacherId);
    Optional<CourseClass> findByTeacherIdAndSubjectIdAndName(Long teacherId, Long subjectId, String name);
    void deleteByTeacher(User teacher);
    void deleteBySubject(Subject subject);
}
