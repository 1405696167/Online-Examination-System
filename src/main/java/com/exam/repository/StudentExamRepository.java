package com.exam.repository;

import com.exam.model.StudentExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentExamRepository extends JpaRepository<StudentExam, Long> {
    Optional<StudentExam> findByStudentIdAndExamId(Long studentId, Long examId);
    List<StudentExam> findByExamTeacherId(Long teacherId);
    List<StudentExam> findByStudentId(Long studentId);

    @Modifying
    @Query("update StudentExam se set se.switchCount = se.switchCount + 1 where se.id = :id")
    void incrementSwitchCount(@Param("id") Long id);
}
