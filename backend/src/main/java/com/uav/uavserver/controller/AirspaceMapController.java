package com.uav.uavserver.controller;

import com.uav.uavserver.domain.AirspaceZone;
import com.uav.uavserver.domain.Station;
import com.uav.uavserver.service.AirspaceZoneService;
import com.uav.uavserver.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 空域地图相关接口：地理围栏（禁飞/限飞/临时管制）、场站、地图聚合数据。
 * 供前端「空域地图」页面（front/src/views/Airspace/AirspaceMap.vue）使用。
 */
@RestController
@RequestMapping("/api/airspace")
@RequiredArgsConstructor
public class AirspaceMapController {
    private final AirspaceZoneService airspaceZoneService;
    private final StationService stationService;

    @GetMapping("/zones")
    public ResponseEntity<List<AirspaceZone>> getAllZones() {
        return ResponseEntity.ok(airspaceZoneService.getAllActiveZones());
    }

    /** 空域管理页面用：包含已停用的空域 */
    @GetMapping("/zones/all")
    public ResponseEntity<List<AirspaceZone>> getAllZonesForManage() {
        return ResponseEntity.ok(airspaceZoneService.getAllZones());
    }

    @GetMapping("/zones/type/{type}")
    public ResponseEntity<List<AirspaceZone>> getZonesByType(@PathVariable String type) {
        return ResponseEntity.ok(airspaceZoneService.getZonesByType(type));
    }

    @GetMapping("/zones/{id:\\d+}")
    public ResponseEntity<AirspaceZone> getZoneById(@PathVariable Long id) {
        return ResponseEntity.ok(airspaceZoneService.getZoneById(id));
    }

    @PostMapping("/zones")
    public ResponseEntity<AirspaceZone> createZone(@RequestBody AirspaceZone zone) {
        return ResponseEntity.ok(airspaceZoneService.createZone(zone));
    }

    @PutMapping("/zones/{id:\\d+}")
    public ResponseEntity<AirspaceZone> updateZone(@PathVariable Long id, @RequestBody AirspaceZone zone) {
        return ResponseEntity.ok(airspaceZoneService.updateZone(id, zone));
    }

    @DeleteMapping("/zones/{id:\\d+}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        airspaceZoneService.deleteZone(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stations")
    public ResponseEntity<List<Station>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllActiveStations());
    }

    /** 场站管理页面用：包含维护中/停用的场站 */
    @GetMapping("/stations/all")
    public ResponseEntity<List<Station>> getAllStationsForManage() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/stations/type/{type}")
    public ResponseEntity<List<Station>> getStationsByType(@PathVariable String type) {
        return ResponseEntity.ok(stationService.getStationsByType(type));
    }

    @GetMapping("/stations/{id:\\d+}")
    public ResponseEntity<Station> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    @PostMapping("/stations")
    public ResponseEntity<Station> createStation(@RequestBody Station station) {
        return ResponseEntity.ok(stationService.createStation(station));
    }

    @PutMapping("/stations/{id:\\d+}")
    public ResponseEntity<Station> updateStation(@PathVariable Long id, @RequestBody Station station) {
        return ResponseEntity.ok(stationService.updateStation(id, station));
    }

    @DeleteMapping("/stations/{id:\\d+}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.ok().build();
    }

    /** 地图一次性拉取：空域区域 + 场站 */
    @GetMapping("/map-data")
    public ResponseEntity<Map<String, Object>> getMapData() {
        Map<String, Object> data = new HashMap<>();
        data.put("zones", airspaceZoneService.getAllActiveZones());
        data.put("stations", stationService.getAllActiveStations());
        return ResponseEntity.ok(data);
    }
}
