package com.exam.repository;

import com.exam.model.AdminClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminClassRepository extends JpaRepository<AdminClass, Long> {
    Optional<AdminClass> findByName(String name);
}
