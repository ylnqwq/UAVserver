package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "airspace")
@Data
public class Airspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String airspaceCode; // 空域编码

    @Column(nullable = false, length = 200)
    private String name; // 空域名称

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AirspaceType type;

    @Column(columnDefinition = "TEXT")
    private String boundaries; // 边界坐标(JSON格式，多边形)

    private Double minAltitude; // 最低高度(m)

    private Double maxAltitude; // 最高高度(m)

    private Boolean isActive = true; // 是否启用

    private LocalDateTime effectiveFrom; // 生效时间

    private LocalDateTime effectiveTo; // 失效时间

    @Column(columnDefinition = "TEXT")
    private String restrictions; // 限制说明

    @Column(length = 200)
    private String managingAuthority; // 管理机构

    @Column(length = 20)
    private String contactPhone; // 联系电话

    @Column(columnDefinition = "TEXT")
    private String description; // 描述

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
