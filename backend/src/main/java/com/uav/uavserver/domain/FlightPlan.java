package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "flight_plans")
@Data
public class FlightPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String planNumber; // 计划编号

    @Column(nullable = false, length = 200)
    private String planName; // 计划名称

    @ManyToOne
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft; // 关联飞行器

    @ManyToOne
    @JoinColumn(name = "pilot_id", nullable = false)
    private User pilot; // 飞行员

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant; // 申请人

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FlightPurpose purpose; // 飞行目的

    @Column(nullable = false)
    private LocalDateTime plannedStartTime; // 计划起飞时间

    @Column(nullable = false)
    private LocalDateTime plannedEndTime; // 计划降落时间

    private Integer plannedDuration; // 计划飞行时长(分钟)

    @Column(nullable = false, length = 200)
    private String departureLocation; // 起飞地点

    @Column(nullable = false, length = 200)
    private String arrivalLocation; // 降落地点

    private Double plannedAltitude; // 计划飞行高度(m)

    @Column(columnDefinition = "TEXT")
    private String routeDescription; // 航线描述

    @Column(columnDefinition = "TEXT")
    private String routeCoordinates; // 航线坐标(JSON格式)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FlightPlanStatus status = FlightPlanStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String remarks; // 备注

    @Column(length = 100)
    private String weatherConditions; // 天气条件

    @Column(length = 200)
    private String emergencyContact; // 紧急联系人

    @Column(length = 20)
    private String emergencyPhone; // 紧急联系电话

    private LocalDateTime submittedAt; // 提交时间

    private LocalDateTime approvedAt; // 审批时间

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy; // 审批人

    @Column(columnDefinition = "TEXT")
    private String approvalComments; // 审批意见

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
