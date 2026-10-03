package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "aircraft")
@Data
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String registrationNumber; // 注册编号

    @Column(nullable = false, length = 100)
    private String name; // 飞行器名称

    @Column(length = 100)
    private String model; // 型号

    @Column(length = 100)
    private String manufacturer; // 制造商

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AircraftType type; // 飞行器类型

    @Column(length = 50)
    private String serialNumber; // 序列号

    private Double maxTakeoffWeight; // 最大起飞重量(kg)

    private Double maxSpeed; // 最大速度(m/s)

    private Double maxFlightTime; // 最大飞行时间(分钟)

    private Double maxFlightDistance; // 最大飞行距离(km)

    private Double maxAltitude; // 最大飞行高度(m)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AircraftStatus status = AircraftStatus.AVAILABLE;

    @Column(length = 200)
    private String owner; // 所有者

    @Column(length = 200)
    private String operator; // 运营者

    @Column(columnDefinition = "TEXT")
    private String description; // 描述

    // 实时位置和状态信息
    private Double latitude; // 当前纬度

    private Double longitude; // 当前经度

    private Double currentAltitude; // 当前高度(米)

    private Double currentSpeed; // 当前速度(m/s)

    @Column(length = 20)
    private String flightStatus = "grounded"; // 飞行状态: grounded, flying, returning

    private Integer batteryLevel = 100; // 电池电量(%)

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime lastMaintenanceDate; // 上次维护日期

    private LocalDateTime nextMaintenanceDate; // 下次维护日期

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
