package com.uav.uavserver.repository;

import com.uav.uavserver.domain.AirspaceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirspaceZoneRepository extends JpaRepository<AirspaceZone, Long> {
    List<AirspaceZone> findByActiveTrue();
    List<AirspaceZone> findByType(String type);
    List<AirspaceZone> findByTypeAndActive(String type, Boolean active);
}
