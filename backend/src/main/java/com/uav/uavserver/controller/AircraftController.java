package com.uav.uavserver.controller;

import com.uav.uavserver.domain.AircraftStatus;
import com.uav.uavserver.dto.AircraftDto;
import com.uav.uavserver.service.AircraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 飞行器接口。
 * 路径与前端 front/src/api/index.js 中的 aircraftAPI 保持一致：
 *   GET    /api/aircraft                 列表
 *   GET    /api/aircraft/{id}            详情
 *   POST   /api/aircraft                 新增
 *   PUT    /api/aircraft/{id}            修改
 *   DELETE /api/aircraft/{id}            删除
 *   GET    /api/aircraft/status/{status} 按状态查询
 *   GET    /api/aircraft/flying          地图用：当前有实时位置的飞行器（匿名可访问）
 *   GET    /api/aircraft/{id}/route      地图用：飞行器当前/最近航线（含航点、进度、预计到达）
 */
@RestController
@RequestMapping("/api/aircraft")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    /** 地图用：返回带经纬度的飞行器实时数据（SecurityConfig 中已放行） */
    @GetMapping("/flying")
    public ResponseEntity<List<Map<String, Object>>> getFlyingAircraft() {
        return ResponseEntity.ok(aircraftService.getFlyingAircraft());
    }

    @GetMapping
    public ResponseEntity<List<AircraftDto.Response>> getAllAircraft() {
        return ResponseEntity.ok(aircraftService.getAllAircraft());
    }

    /** 用正则限定为数字，避免与 /flying、/status/{status} 产生歧义 */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<AircraftDto.Response> getAircraft(@PathVariable Long id) {
        return ResponseEntity.ok(aircraftService.getAircraft(id));
    }

    /** 地图用：飞行器航线（点击飞行器时展示） */
    @GetMapping("/{id:\\d+}/route")
    public ResponseEntity<Map<String, Object>> getAircraftRoute(@PathVariable Long id) {
        return ResponseEntity.ok(aircraftService.getAircraftRoute(id));
    }

    @PostMapping
    public ResponseEntity<AircraftDto.Response> createAircraft(@Valid @RequestBody AircraftDto.CreateRequest request) {
        return ResponseEntity.ok(aircraftService.createAircraft(request));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<AircraftDto.Response> updateAircraft(
            @PathVariable Long id,
            @Valid @RequestBody AircraftDto.UpdateRequest request) {
        return ResponseEntity.ok(aircraftService.updateAircraft(id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteAircraft(@PathVariable Long id) {
        aircraftService.deleteAircraft(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AircraftDto.Response>> getAircraftByStatus(@PathVariable String status) {
        AircraftStatus aircraftStatus;
        try {
            aircraftStatus = AircraftStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的飞行器状态: " + status
                    + "，可选值: AVAILABLE, IN_FLIGHT, MAINTENANCE, RETIRED, DAMAGED");
        }
        return ResponseEntity.ok(aircraftService.getAircraftByStatus(aircraftStatus));
    }
}
