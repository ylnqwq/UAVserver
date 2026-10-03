package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应急告警（应急告警页面）。
 * level: 1(一般) / 2(严重) / 3(紧急)
 * type: device_fault / airspace_conflict / route_deviation / battery_low / order_abnormal / other
 * status: pending(未处理) / processing(处理中) / handled(已处理) / closed(已关闭)
 */
@Entity
@Table(name = "emergency_alerts")
@Data
public class EmergencyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_no", nullable = false, unique = true, length = 50)
    private String alertNo;

    @Column(length = 20)
    private Integer level;

    @Column(length = 30)
    private String type;

    @Column(length = 255)
    private String content;

    @Column(name = "related_object", length = 100)
    private String relatedObject; // 涉及对象：飞行器/空域/订单

    @Column(length = 20)
    private String status = "pending";

    @Column(length = 255)
    private String location;

    private Double latitude;

    private Double longitude;

    @Column(name = "aircraft_id")
    private Long aircraftId;

    @Column(name = "occur_time")
    private LocalDateTime occurTime;

    @Column(length = 50)
    private String handler;

    @Column(name = "handle_method", length = 100)
    private String handleMethod; // 处理方式：远程接管/返航/迫降/人工介入

    @Column(name = "handle_measure", columnDefinition = "TEXT")
    private String handleMeasure; // 处置措施

    @Column(name = "expected_time")
    private LocalDateTime expectedTime; // 预计完成时间

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "close_time")
    private LocalDateTime closeTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
