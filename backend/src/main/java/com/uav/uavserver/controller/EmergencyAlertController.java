package com.uav.uavserver.controller;

import com.uav.uavserver.domain.EmergencyAlert;
import com.uav.uavserver.repository.EmergencyAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 应急告警接口。
 *   GET    /api/alerts              列表（可按 ?status= / ?level= / ?type= 过滤）
 *   GET    /api/alerts/{id}         详情
 *   POST   /api/alerts              新增告警
 *   PUT    /api/alerts/{id}         修改
 *   DELETE /api/alerts/{id}         删除
 *   POST   /api/alerts/{id}/process 处置（处理人/处理方式/处置措施/预计完成时间）
 *   POST   /api/alerts/{id}/close   关闭
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class EmergencyAlertController {

    private final EmergencyAlertRepository alertRepository;
    private static final Random RANDOM = new Random();

    @GetMapping
    public ResponseEntity<List<EmergencyAlert>> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(alertRepository.findAll().stream()
                .filter(a -> status == null || status.isBlank() || status.equalsIgnoreCase(a.getStatus()))
                .filter(a -> level == null || level.isBlank() || level.equals(a.getLevel()))
                .filter(a -> type == null || type.isBlank() || type.equalsIgnoreCase(a.getType()))
                .sorted((a, b) -> {
                    if (a.getOccurTime() == null || b.getOccurTime() == null) return 0;
                    return b.getOccurTime().compareTo(a.getOccurTime());
                })
                .toList());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<EmergencyAlert> getAlert(@PathVariable Long id) {
        return ResponseEntity.ok(find(id));
    }

    @PostMapping
    public ResponseEntity<EmergencyAlert> createAlert(@RequestBody EmergencyAlert alert) {
        if (alert.getAlertNo() == null || alert.getAlertNo().isBlank()) {
            alert.setAlertNo(generateNo());
        }
        if (alert.getStatus() == null) alert.setStatus("pending");
        if (alert.getOccurTime() == null) alert.setOccurTime(LocalDateTime.now());
        return ResponseEntity.ok(alertRepository.save(alert));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<EmergencyAlert> updateAlert(@PathVariable Long id, @RequestBody EmergencyAlert body) {
        EmergencyAlert alert = find(id);
        if (body.getLevel() != null) alert.setLevel(body.getLevel());
        if (body.getType() != null) alert.setType(body.getType());
        if (body.getContent() != null) alert.setContent(body.getContent());
        if (body.getRelatedObject() != null) alert.setRelatedObject(body.getRelatedObject());
        if (body.getStatus() != null) alert.setStatus(body.getStatus());
        if (body.getLocation() != null) alert.setLocation(body.getLocation());
        if (body.getOccurTime() != null) alert.setOccurTime(body.getOccurTime());
        return ResponseEntity.ok(alertRepository.save(alert));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertRepository.delete(find(id));
        return ResponseEntity.ok().build();
    }

    /** 处置告警 */
    @PostMapping("/{id:\\d+}/process")
    public ResponseEntity<EmergencyAlert> processAlert(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EmergencyAlert alert = find(id);
        alert.setHandler((String) body.getOrDefault("handler", alert.getHandler()));
        alert.setHandleMethod((String) body.getOrDefault("handleMethod", alert.getHandleMethod()));
        alert.setHandleMeasure((String) body.getOrDefault("handleMeasure", alert.getHandleMeasure()));
        Object expected = body.get("expectedTime");
        if (expected instanceof String text && !text.isBlank()) {
            alert.setExpectedTime(LocalDateTime.parse(text));
        }
        Object level = body.get("level");
        if (level instanceof Number number) alert.setLevel(number.intValue());
        alert.setStatus("handled");
        alert.setHandleTime(LocalDateTime.now());
        return ResponseEntity.ok(alertRepository.save(alert));
    }

    @PostMapping("/{id:\\d+}/close")
    public ResponseEntity<EmergencyAlert> closeAlert(@PathVariable Long id) {
        EmergencyAlert alert = find(id);
        alert.setStatus("closed");
        alert.setCloseTime(LocalDateTime.now());
        if (alert.getHandleTime() == null) alert.setHandleTime(LocalDateTime.now());
        return ResponseEntity.ok(alertRepository.save(alert));
    }

    private EmergencyAlert find(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("告警记录不存在: id=" + id));
    }

    private String generateNo() {
        String base = "ALT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 5; i++) {
            String candidate = base + (1000 + RANDOM.nextInt(9000));
            if (!alertRepository.existsByAlertNo(candidate)) return candidate;
        }
        return base + RANDOM.nextInt(10000);
    }
}
