package com.uav.uavserver.controller;

import com.uav.uavserver.domain.User;
import com.uav.uavserver.dto.StatisticsDto;
import com.uav.uavserver.repository.UserRepository;
import com.uav.uavserver.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统计接口（工作台 / 数据大屏使用），数据全部来自数据库。
 *   GET /api/statistics/overview   平台总览
 *   GET /api/statistics/dashboard  工作台（指标卡+趋势+待办+告警）
 *   GET /api/statistics/admin      管理员大屏
 *   GET /api/statistics/admin      管理员大屏（平台总览）
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final UserRepository userRepository;

    @GetMapping("/overview")
    public ResponseEntity<StatisticsDto.Overview> getOverview() {
        return ResponseEntity.ok(statisticsService.getOverview());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<StatisticsDto.Dashboard> getDashboard() {
        return ResponseEntity.ok(statisticsService.getDashboard());
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAdminReport() {
        return ResponseEntity.ok(statisticsService.getAdminReport());
    }

}
