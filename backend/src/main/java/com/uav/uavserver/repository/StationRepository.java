package com.uav.uavserver.repository;

import com.uav.uavserver.domain.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
    List<Station> findByStatus(String status);
    List<Station> findByType(String type);
    List<Station> findByTypeAndStatus(String type, String status);
}
