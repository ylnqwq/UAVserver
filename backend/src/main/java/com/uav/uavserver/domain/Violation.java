package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 违规记录（空域违规处理页面）。
 * type: route_deviation(偏离航线) / no_fly_zone(闯入禁飞区) / height_violation(超高飞行) / unauthorized(未经许可飞行) / other
 * severity: 1(一般) / 2(较重) / 3(严重)
 * status: pending(待处理) / processing(处理中) / handled(已处理) / reported(已上报) / closed(已关闭)
 */
@Entity
@Table(name = "violations")
@Data
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "violation_no", nullable = false, unique = true, length = 50)
    private String violationNo;

    @Column(length = 20)
    private String type;

    private Integer severity;

    @Column(length = 20)
    private String status = "pending";

    @Column(name = "aircraft_id")
    private Long aircraftId;

    @Column(name = "device_no", length = 50)
    private String deviceNo;

    @Column(name = "pilot_id")
    private Long pilotId;

    @Column(name = "pilot_name", length = 50)
    private String pilotName;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_no", length = 50)
    private String orderNo;

    @Column(length = 255)
    private String location;

    private Double latitude;

    private Double longitude;

    @Column(name = "occur_time")
    private LocalDateTime occurTime;

    @Column(name = "detect_method", length = 50)
    private String detectMethod; // 发现方式：系统自动检测 / 人工上报

    @Column(columnDefinition = "TEXT")
    private String detail; // 违规详情

    @Column(length = 50)
    private String handler; // 处理人

    @Column(name = "handle_result", length = 100)
    private String handleResult; // 处理方式：warning/fine/suspend/revoke

    @Column(name = "handle_remark", columnDefinition = "TEXT")
    private String handleRemark; // 处理说明

    @Column(name = "fine_amount")
    private Double fineAmount; // 罚款金额

    @Column(name = "suspend_days")
    private Integer suspendDays; // 暂停天数

    private Boolean reported = false; // 是否上报上级监管

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
