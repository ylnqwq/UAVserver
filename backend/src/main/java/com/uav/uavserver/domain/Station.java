package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stations")
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // landing, charging, maintenance, emergency, transfer

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "current_usage")
    private Integer currentUsage = 0;

    private String address;

    private String facilities;

    @Column(nullable = false)
    private String status = "active"; // active, maintenance, closed

    @Column(length = 50)
    private String manager; // 负责人

    @Column(length = 20)
    private String phone; // 联系方式

    @Column(name = "operation_time", length = 50)
    private String operationTime; // 运营时间，如 06:00-22:00 / 全天

    @Column(name = "today_usage")
    private Integer todayUsage = 0; // 今日使用次数

    @Column(columnDefinition = "TEXT")
    private String remark; // 备注

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
