package com.uav.uavserver.repository;

import com.uav.uavserver.domain.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    boolean existsByViolationNo(String violationNo);

    List<Violation> findByStatusOrderByOccurTimeDesc(String status);

    List<Violation> findBySeverity(Integer severity);

    List<Violation> findByType(String type);

    List<Violation> findByDeviceNo(String deviceNo);
}
