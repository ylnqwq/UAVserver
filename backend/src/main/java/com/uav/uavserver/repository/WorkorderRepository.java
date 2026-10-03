package com.uav.uavserver.repository;

import com.uav.uavserver.domain.Workorder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkorderRepository extends JpaRepository<Workorder, Long> {

    boolean existsByWorkorderNo(String workorderNo);

    List<Workorder> findByStatusOrderByCreatedAtDesc(String status);

    List<Workorder> findByType(String type);

    List<Workorder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
