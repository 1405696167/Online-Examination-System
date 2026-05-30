package com.exam.repository;

import com.exam.model.CourseEnrollment;
import com.exam.model.CourseClass;
import com.exam.model.Subject;
import com.exam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    List<CourseEnrollment> findByTeacherId(Long teacherId);
    List<CourseEnrollment> findByStudentId(Long studentId);
    Optional<CourseEnrollment> findByStudentIdAndCourseClassId(Long studentId, Long courseClassId);
    Optional<CourseEnrollment> findByStudentIdAndTeacherIdAndSubjectIdAndCourseClassName(Long studentId, Long teacherId, Long subjectId, String courseClassName);
    boolean existsByStudentIdAndTeacherIdAndSubjectIdAndCourseClassName(Long studentId, Long teacherId, Long subjectId, String courseClassName);
    void deleteByStudent(User student);
    void deleteByTeacher(User teacher);
    void deleteBySubject(Subject subject);
    void deleteByCourseClass(CourseClass courseClass);
}
