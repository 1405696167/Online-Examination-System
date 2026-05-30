package com.exam.repository;

import com.exam.model.SwitchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwitchEventRepository extends JpaRepository<SwitchEvent, Long> {
}
