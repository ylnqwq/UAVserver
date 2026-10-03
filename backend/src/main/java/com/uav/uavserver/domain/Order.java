package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体（对应 orders 表）。
 *
 * 说明：已有的 orders 表最初是脚本建的，没有 JPA 实体；现在把它纳入 JPA 管理，
 * 并按「下单/调度/跟踪」页面的需要补了几个列（重量、体积、收件人、时效要求、特殊要求、规划高度），
 * ddl-auto=update 会自动补列。
 */
@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber; // 订单编号

    @Column(name = "customer_id")
    private Long customerId; // 下单客户

    @Column(name = "accepted_by", length = 50)
    private String acceptedBy; // 接单人（平台运营）

    @Column(name = "aircraft_id")
    private Long aircraftId; // 执行飞行器

    @Column(name = "pilot_id")
    private Long pilotId; // 执行机长（调度时指派）

    @Column(name = "service_type", length = 50)
    private String serviceType; // 服务/物品类型

    @Column(name = "pickup_location", length = 255)
    private String pickupLocation; // 寄件地址

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @Column(name = "delivery_location", length = 255)
    private String deliveryLocation; // 收件地址

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    private Double distance; // 距离(km)

    @Column(name = "estimated_duration")
    private Integer estimatedDuration; // 预计时长(分钟)

    @Column(length = 20)
    private String status = "PENDING"; // PENDING/ACCEPTED/DISPATCHED/IN_PROGRESS/COMPLETED/CANCELLED/FAILED

    @Column(length = 20)
    private String priority = "NORMAL"; // NORMAL/HIGH/URGENT

    private Double amount; // 金额

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime; // 预约配送时间

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt; // 接单时间

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt; // 调度完成时间

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(columnDefinition = "TEXT")
    private String notes; // 备注

    // ================= 下单/调度页面需要的扩展字段 =================

    private Double weight; // 重量(kg)

    private Double volume; // 体积(cm³)

    @Column(name = "recv_name", length = 50)
    private String receiverName; // 收件人

    @Column(name = "recv_phone", length = 20)
    private String receiverPhone; // 收件人电话

    @Column(name = "time_requirement", length = 20)
    private String timeRequirement; // normal / urgent

    @Column(name = "special_requirements", length = 255)
    private String specialRequirements; // 特殊要求(逗号分隔)

    @Column(name = "route_altitude")
    private Double routeAltitude; // 规划飞行高度(米)

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
