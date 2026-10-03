package com.uav.uavserver.repository;

import com.uav.uavserver.domain.FlightPlan;
import com.uav.uavserver.domain.FlightPlanStatus;
import com.uav.uavserver.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightPlanRepository extends JpaRepository<FlightPlan, Long> {

    List<FlightPlan> findByStatus(FlightPlanStatus status);

    List<FlightPlan> findByApplicant(User applicant);

    List<FlightPlan> findByPilot(User pilot);

    List<FlightPlan> findByPlannedStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
