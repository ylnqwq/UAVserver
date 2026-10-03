package com.uav.uavserver.repository;

import com.uav.uavserver.domain.FlightRecord;
import com.uav.uavserver.domain.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightRecordRepository extends JpaRepository<FlightRecord, Long> {

    List<FlightRecord> findByStatus(FlightStatus status);

    List<FlightRecord> findByActualStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<FlightRecord> findByHasAbnormalEventsTrue();
}
