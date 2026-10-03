package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服工单（客服工单页面）。
 * type: consult(咨询) / complaint(投诉) / repair(报修) / suggest(建议) / other
 * priority: 1(普通) / 2(高) / 3(紧急)
 * status: pending(待处理) / processing(处理中) / completed(已完成) / closed(已关闭)
 */
@Entity
@Table(name = "work_orders")
@Data
public class Workorder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workorder_no", nullable = false, unique = true, length = 50)
    private String workorderNo;

    @Column(length = 30)
    private String type;

    private Integer priority = 1;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 50)
    private String customerName;

    @Column(length = 50)
    private String contact;

    @Column(length = 20)
    private String status = "pending";

    @Column(length = 50)
    private String handler;

    @Column(columnDefinition = "TEXT")
    private String reply; // 处理回复

    @Column(name = "order_no", length = 50)
    private String orderNo; // 关联订单号

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
