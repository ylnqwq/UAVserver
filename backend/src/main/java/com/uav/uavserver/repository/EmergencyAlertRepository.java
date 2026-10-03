package com.uav.uavserver.repository;

import com.uav.uavserver.domain.EmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {

    boolean existsByAlertNo(String alertNo);

    List<EmergencyAlert> findByStatusOrderByOccurTimeDesc(String status);

    List<EmergencyAlert> findByLevel(Integer level);

    List<EmergencyAlert> findByType(String type);
}
