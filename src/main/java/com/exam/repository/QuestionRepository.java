package com.exam.repository;

import com.exam.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTeacherId(Long teacherId);
    List<Question> findByTeacherIdAndSubjectId(Long teacherId, Long subjectId);

    @Query(value = """
            SELECT * FROM question
            WHERE teacher_id = :teacherId AND subject_id = :subjectId
            ORDER BY RAND()
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> randomPaper(@Param("teacherId") Long teacherId,
                               @Param("subjectId") Long subjectId,
                               @Param("limit") int limit);
}
