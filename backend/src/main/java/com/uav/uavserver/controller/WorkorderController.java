package com.uav.uavserver.controller;

import com.uav.uavserver.domain.Workorder;
import com.uav.uavserver.repository.WorkorderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 客服工单接口。
 *   GET    /api/workorders               列表（可按 ?status= / ?type= / ?customerId= / ?priority= 过滤）
 *   GET    /api/workorders/{id}          详情
 *   POST   /api/workorders               创建工单
 *   PUT    /api/workorders/{id}          修改
 *   DELETE /api/workorders/{id}          删除
 *   POST   /api/workorders/{id}/assign   分派（指定处理人 → 处理中）
 *   POST   /api/workorders/{id}/reply    回复并解决
 *   POST   /api/workorders/{id}/close    关闭
 */
@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
public class WorkorderController {

    private final WorkorderRepository workorderRepository;
    private static final Random RANDOM = new Random();

    @GetMapping
    public ResponseEntity<List<Workorder>> getWorkorders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer priority) {
        return ResponseEntity.ok(workorderRepository.findAll().stream()
                .filter(w -> status == null || status.isBlank() || status.equalsIgnoreCase(w.getStatus()))
                .filter(w -> type == null || type.isBlank() || type.equalsIgnoreCase(w.getType()))
                .filter(w -> customerId == null || customerId.equals(w.getCustomerId()))
                .filter(w -> priority == null || priority.equals(w.getPriority()))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Workorder> getWorkorder(@PathVariable Long id) {
        return ResponseEntity.ok(find(id));
    }

    @PostMapping
    public ResponseEntity<Workorder> createWorkorder(@RequestBody Workorder workorder) {
        if (workorder.getWorkorderNo() == null || workorder.getWorkorderNo().isBlank()) {
            workorder.setWorkorderNo(generateNo());
        }
        if (workorder.getStatus() == null) workorder.setStatus("pending");
        if (workorder.getPriority() == null) workorder.setPriority(1);
        return ResponseEntity.ok(workorderRepository.save(workorder));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Workorder> updateWorkorder(@PathVariable Long id, @RequestBody Workorder body) {
        Workorder workorder = find(id);
        if (body.getType() != null) workorder.setType(body.getType());
        if (body.getPriority() != null) workorder.setPriority(body.getPriority());
        if (body.getTitle() != null) workorder.setTitle(body.getTitle());
        if (body.getContent() != null) workorder.setContent(body.getContent());
        if (body.getContact() != null) workorder.setContact(body.getContact());
        if (body.getStatus() != null) workorder.setStatus(body.getStatus());
        if (body.getReply() != null) workorder.setReply(body.getReply());
        if (body.getHandler() != null) workorder.setHandler(body.getHandler());
        if (body.getOrderNo() != null) workorder.setOrderNo(body.getOrderNo());
        return ResponseEntity.ok(workorderRepository.save(workorder));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteWorkorder(@PathVariable Long id) {
        workorderRepository.delete(find(id));
        return ResponseEntity.ok().build();
    }

    /** 分派处理人 */
    @PostMapping("/{id:\\d+}/assign")
    public ResponseEntity<Workorder> assignWorkorder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Workorder workorder = find(id);
        workorder.setHandler((String) body.getOrDefault("handler", workorder.getHandler()));
        workorder.setStatus("processing");
        return ResponseEntity.ok(workorderRepository.save(workorder));
    }

    /** 回复并解决 */
    @PostMapping("/{id:\\d+}/reply")
    public ResponseEntity<Workorder> replyWorkorder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Workorder workorder = find(id);
        workorder.setReply((String) body.getOrDefault("reply", workorder.getReply()));
        Object handler = body.get("handler");
        if (handler instanceof String text && !text.isBlank()) workorder.setHandler(text);
        workorder.setStatus("completed");
        workorder.setResolvedAt(LocalDateTime.now());
        return ResponseEntity.ok(workorderRepository.save(workorder));
    }

    @PostMapping("/{id:\\d+}/close")
    public ResponseEntity<Workorder> closeWorkorder(@PathVariable Long id) {
        Workorder workorder = find(id);
        workorder.setStatus("closed");
        if (workorder.getResolvedAt() == null) workorder.setResolvedAt(LocalDateTime.now());
        return ResponseEntity.ok(workorderRepository.save(workorder));
    }

    private Workorder find(Long id) {
        return workorderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工单不存在: id=" + id));
    }

    private String generateNo() {
        String base = "WO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 5; i++) {
            String candidate = base + (1000 + RANDOM.nextInt(9000));
            if (!workorderRepository.existsByWorkorderNo(candidate)) return candidate;
        }
        return base + RANDOM.nextInt(10000);
    }
}
