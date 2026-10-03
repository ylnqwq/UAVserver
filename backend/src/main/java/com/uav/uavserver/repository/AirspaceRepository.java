package com.uav.uavserver.repository;

import com.uav.uavserver.domain.Airspace;
import com.uav.uavserver.domain.AirspaceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirspaceRepository extends JpaRepository<Airspace, Long> {

    Optional<Airspace> findByAirspaceCode(String airspaceCode);

    List<Airspace> findByType(AirspaceType type);

    List<Airspace> findByIsActiveTrue();
}
