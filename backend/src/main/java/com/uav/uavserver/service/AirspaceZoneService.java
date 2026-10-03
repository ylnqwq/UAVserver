package com.uav.uavserver.service;

import com.uav.uavserver.domain.AirspaceZone;
import com.uav.uavserver.repository.AirspaceZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirspaceZoneService {
    private final AirspaceZoneRepository airspaceZoneRepository;

    public List<AirspaceZone> getAllActiveZones() {
        return airspaceZoneRepository.findByActiveTrue();
    }

    /** 管理页面用：不分启用/停用，全部返回 */
    public List<AirspaceZone> getAllZones() {
        return airspaceZoneRepository.findAll();
    }

    public List<AirspaceZone> getZonesByType(String type) {
        return airspaceZoneRepository.findByTypeAndActive(type, true);
    }

    public AirspaceZone getZoneById(Long id) {
        return airspaceZoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("空域不存在"));
    }

    @Transactional
    public AirspaceZone createZone(AirspaceZone zone) {
        return airspaceZoneRepository.save(zone);
    }

    @Transactional
    public AirspaceZone updateZone(Long id, AirspaceZone zone) {
        AirspaceZone existing = getZoneById(id);
        existing.setName(zone.getName());
        existing.setType(zone.getType());
        existing.setLatitude(zone.getLatitude());
        existing.setLongitude(zone.getLongitude());
        existing.setRadius(zone.getRadius());
        existing.setDescription(zone.getDescription());
        existing.setStartTime(zone.getStartTime());
        existing.setEndTime(zone.getEndTime());
        existing.setActive(zone.getActive());
        return airspaceZoneRepository.save(existing);
    }

    @Transactional
    public void deleteZone(Long id) {
        airspaceZoneRepository.deleteById(id);
    }
}
