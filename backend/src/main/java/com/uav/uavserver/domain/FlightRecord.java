package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "flight_records")
@Data
public class FlightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "flight_plan_id", nullable = false)
    private FlightPlan flightPlan; // 关联飞行计划

    @Column(nullable = false, unique = true, length = 50)
    private String flightNumber; // 飞行编号

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FlightStatus status = FlightStatus.SCHEDULED;

    private LocalDateTime actualStartTime; // 实际起飞时间

    private LocalDateTime actualEndTime; // 实际降落时间

    private Integer actualDuration; // 实际飞行时长(分钟)

    private Double actualMaxAltitude; // 实际最大高度(m)

    private Double actualMaxSpeed; // 实际最大速度(m/s)

    private Double actualDistance; // 实际飞行距离(km)

    @Column(columnDefinition = "TEXT")
    private String actualRoute; // 实际航线(JSON格式)

    @Column(columnDefinition = "TEXT")
    private String flightLog; // 飞行日志

    @Column(columnDefinition = "TEXT")
    private String telemetryData; // 遥测数据

    @Column(columnDefinition = "TEXT")
    private String incidents; // 事件记录

    private Boolean hasAbnormalEvents = false; // 是否有异常事件

    @Column(columnDefinition = "TEXT")
    private String abnormalDescription; // 异常描述

    @Column(length = 100)
    private String weatherActual; // 实际天气

    private Double fuelConsumption; // 燃料消耗

    private Integer batteryUsage; // 电池使用量(%)

    @Column(columnDefinition = "TEXT")
    private String postFlightComments; // 飞行后评论

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
