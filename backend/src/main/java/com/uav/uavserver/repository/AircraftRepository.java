package com.uav.uavserver.repository;

import com.uav.uavserver.domain.Aircraft;
import com.uav.uavserver.domain.AircraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {

    Optional<Aircraft> findByRegistrationNumber(String registrationNumber);

    List<Aircraft> findByStatus(AircraftStatus status);

    boolean existsByRegistrationNumber(String registrationNumber);
}
