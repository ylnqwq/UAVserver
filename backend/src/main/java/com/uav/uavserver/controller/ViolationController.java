package com.uav.uavserver.controller;

import com.uav.uavserver.domain.Violation;
import com.uav.uavserver.repository.ViolationRepository;
import com.uav.uavserver.service.ViolationTrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 空域违规处理接口。
 *   GET    /api/violations              列表（可按 ?status= / ?type= / ?severity= 过滤）
 *   GET    /api/violations/{id}         详情
 *   GET    /api/violations/{id}/track   违规航迹（地图用：起点/航迹/终点/周围禁飞区）
 *   POST   /api/violations              登记违规
 *   PUT    /api/violations/{id}         修改
 *   DELETE /api/violations/{id}         删除
 *   POST   /api/violations/{id}/process 处理（处理人/处理方式/罚款/暂停天数/是否上报）
 *   POST   /api/violations/{id}/close   关闭
 *
 * 说明：本模块是简单 CRUD + 状态流转，逻辑直接放在控制器里（与其它模块的 service 分层保持一致的写法见 AirspaceService 等）。
 */
@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationRepository violationRepository;
    private final ViolationTrackService violationTrackService;
    private static final Random RANDOM = new Random();

    @GetMapping
    public ResponseEntity<List<Violation>> getViolations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer severity) {
        List<Violation> all = violationRepository.findAll();
        return ResponseEntity.ok(all.stream()
                .filter(v -> status == null || status.isBlank() || status.equalsIgnoreCase(v.getStatus()))
                .filter(v -> type == null || type.isBlank() || type.equalsIgnoreCase(v.getType()))
                .filter(v -> severity == null || severity.equals(v.getSeverity()))
                .sorted((a, b) -> {
                    if (a.getOccurTime() == null || b.getOccurTime() == null) return 0;
                    return b.getOccurTime().compareTo(a.getOccurTime());
                })
                .toList());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Violation> getViolation(@PathVariable Long id) {
        return ResponseEntity.ok(find(id));
    }

    /** 违规航迹：详情弹窗地图用（真实锚点推演，返回 source/sourceNote 标明来源） */
    @GetMapping("/{id:\\d+}/track")
    public ResponseEntity<Map<String, Object>> getViolationTrack(@PathVariable Long id) {
        return ResponseEntity.ok(violationTrackService.buildTrack(id));
    }

    @PostMapping
    public ResponseEntity<Violation> createViolation(@RequestBody Violation violation) {
        if (violation.getViolationNo() == null || violation.getViolationNo().isBlank()) {
            violation.setViolationNo(generateNo());
        }
        if (violation.getStatus() == null) violation.setStatus("pending");
        if (violation.getOccurTime() == null) violation.setOccurTime(LocalDateTime.now());
        return ResponseEntity.ok(violationRepository.save(violation));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Violation> updateViolation(@PathVariable Long id, @RequestBody Violation body) {
        Violation violation = find(id);
        if (body.getType() != null) violation.setType(body.getType());
        if (body.getSeverity() != null) violation.setSeverity(body.getSeverity());
        if (body.getStatus() != null) violation.setStatus(body.getStatus());
        if (body.getDeviceNo() != null) violation.setDeviceNo(body.getDeviceNo());
        if (body.getPilotName() != null) violation.setPilotName(body.getPilotName());
        if (body.getOrderNo() != null) violation.setOrderNo(body.getOrderNo());
        if (body.getLocation() != null) violation.setLocation(body.getLocation());
        if (body.getOccurTime() != null) violation.setOccurTime(body.getOccurTime());
        if (body.getDetectMethod() != null) violation.setDetectMethod(body.getDetectMethod());
        if (body.getDetail() != null) violation.setDetail(body.getDetail());
        return ResponseEntity.ok(violationRepository.save(violation));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteViolation(@PathVariable Long id) {
        violationRepository.delete(find(id));
        return ResponseEntity.ok().build();
    }

    /** 处理违规 */
    @PostMapping("/{id:\\d+}/process")
    public ResponseEntity<Violation> processViolation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Violation violation = find(id);
        violation.setHandler((String) body.getOrDefault("handler", violation.getHandler()));
        violation.setHandleResult((String) body.getOrDefault("handleResult", violation.getHandleResult()));
        violation.setHandleRemark((String) body.getOrDefault("description", violation.getHandleRemark()));
        violation.setDetail((String) body.getOrDefault("detail", violation.getDetail()));
        Object fine = body.get("fineAmount");
        if (fine instanceof Number number) violation.setFineAmount(number.doubleValue());
        Object suspend = body.get("suspendDays");
        if (suspend instanceof Number number) violation.setSuspendDays(number.intValue());
        Object reported = body.get("reported");
        if (reported instanceof Boolean bool) violation.setReported(bool);
        violation.setStatus("handled");
        violation.setHandleTime(LocalDateTime.now());
        return ResponseEntity.ok(violationRepository.save(violation));
    }

    /** 上报上级监管 */
    @PostMapping("/{id:\\d+}/report")
    public ResponseEntity<Violation> reportViolation(@PathVariable Long id) {
        Violation violation = find(id);
        violation.setReported(true);
        violation.setStatus("reported");
        return ResponseEntity.ok(violationRepository.save(violation));
    }

    @PostMapping("/{id:\\d+}/close")
    public ResponseEntity<Violation> closeViolation(@PathVariable Long id) {
        Violation violation = find(id);
        violation.setStatus("closed");
        if (violation.getHandleTime() == null) violation.setHandleTime(LocalDateTime.now());
        return ResponseEntity.ok(violationRepository.save(violation));
    }

    private Violation find(Long id) {
        return violationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("违规记录不存在: id=" + id));
    }

    private String generateNo() {
        String base = "VIO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 5; i++) {
            String candidate = base + (1000 + RANDOM.nextInt(9000));
            if (!violationRepository.existsByViolationNo(candidate)) return candidate;
        }
        return base + RANDOM.nextInt(10000);
    }
}
