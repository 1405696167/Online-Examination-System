package com.exam.repository;

import com.exam.model.User;
import com.exam.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstByUsernameAndRoleOrderByIdAsc(String username, UserRole role);
    Optional<User> findFirstByStudentNumberOrderByIdAsc(String studentNumber);
    List<User> findByStudentNumberOrderByIdAsc(String studentNumber);
    List<User> findByRole(UserRole role);
    List<User> findByRoleAndClassName(UserRole role, String className);
}
