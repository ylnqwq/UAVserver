package com.uav.uavserver.service;

import com.uav.uavserver.domain.Station;
import com.uav.uavserver.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stationRepository;

    public List<Station> getAllActiveStations() {
        return stationRepository.findByStatus("active");
    }

    /** 管理页面用：不分状态，全部返回 */
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public List<Station> getStationsByType(String type) {
        return stationRepository.findByTypeAndStatus(type, "active");
    }

    public Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("场站不存在"));
    }

    @Transactional
    public Station createStation(Station station) {
        return stationRepository.save(station);
    }

    @Transactional
    public Station updateStation(Long id, Station station) {
        Station existing = getStationById(id);
        existing.setName(station.getName());
        existing.setType(station.getType());
        existing.setLatitude(station.getLatitude());
        existing.setLongitude(station.getLongitude());
        existing.setCapacity(station.getCapacity());
        existing.setCurrentUsage(station.getCurrentUsage());
        existing.setAddress(station.getAddress());
        existing.setFacilities(station.getFacilities());
        existing.setStatus(station.getStatus());
        existing.setManager(station.getManager());
        existing.setPhone(station.getPhone());
        existing.setOperationTime(station.getOperationTime());
        return stationRepository.save(existing);
    }

    @Transactional
    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }
}
