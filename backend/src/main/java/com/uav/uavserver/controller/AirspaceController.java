package com.uav.uavserver.controller;

import com.uav.uavserver.domain.AirspaceType;
import com.uav.uavserver.dto.AirspaceDto;
import com.uav.uavserver.service.AirspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 空域合规信息（airspace 表）接口。
 * 路径与前端 front/src/api/index.js 中的 airspaceAPI 保持一致：
 *   GET    /api/airspace                 列表
 *   GET    /api/airspace/{id}            详情
 *   POST   /api/airspace                 新增
 *   PUT    /api/airspace/{id}            修改
 *   DELETE /api/airspace/{id}            删除
 *   GET    /api/airspace/active          启用的空域
 *   GET    /api/airspace/type/{type}     按类型查询
 *
 * 说明：地理围栏（禁飞/限飞区 + 场站 + 地图数据）在 AirspaceMapController 中，
 * 两边路径不冲突（/api/airspace/zones、/api/airspace/stations、/api/airspace/map-data 均为固定路径）。
 */
@RestController
@RequestMapping("/api/airspace")
@RequiredArgsConstructor
public class AirspaceController {

    private final AirspaceService airspaceService;

    @GetMapping
    public ResponseEntity<List<AirspaceDto.Response>> getAllAirspaces() {
        return ResponseEntity.ok(airspaceService.getAllAirspaces());
    }

    @GetMapping("/active")
    public ResponseEntity<List<AirspaceDto.Response>> getActiveAirspaces() {
        return ResponseEntity.ok(airspaceService.getActiveAirspaces());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AirspaceDto.Response>> getAirspacesByType(@PathVariable String type) {
        AirspaceType airspaceType;
        try {
            airspaceType = AirspaceType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的空域类型: " + type
                    + "，可选值: CONTROLLED, RESTRICTED, PROHIBITED, DANGEROUS, TEMPORARY, UNRESTRICTED");
        }
        return ResponseEntity.ok(airspaceService.getAirspacesByType(airspaceType));
    }

    /** 用正则限定为数字，避免与 /active、/map-data 等固定路径冲突 */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<AirspaceDto.Response> getAirspace(@PathVariable Long id) {
        return ResponseEntity.ok(airspaceService.getAirspace(id));
    }

    @PostMapping
    public ResponseEntity<AirspaceDto.Response> createAirspace(@Valid @RequestBody AirspaceDto.CreateRequest request) {
        return ResponseEntity.ok(airspaceService.createAirspace(request));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<AirspaceDto.Response> updateAirspace(
            @PathVariable Long id,
            @Valid @RequestBody AirspaceDto.UpdateRequest request) {
        return ResponseEntity.ok(airspaceService.updateAirspace(id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteAirspace(@PathVariable Long id) {
        airspaceService.deleteAirspace(id);
        return ResponseEntity.ok().build();
    }
}
