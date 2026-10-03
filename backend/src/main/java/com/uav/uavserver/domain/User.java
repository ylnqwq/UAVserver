package com.uav.uavserver.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String realName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(length = 200)
    private String organization;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime lastLoginAt;

    @Column(length = 50)
    private String lastLoginIp;

    // ================= 人员档案扩展字段（人员管理页面使用） =================

    @Column(name = "id_card", length = 30)
    private String idCard; // 身份证号

    @Column(name = "cert_no", length = 50)
    private String certNo; // 执照/资格证号

    @Column(name = "cert_expiry")
    private LocalDate certExpiry; // 执照到期日期

    @Column(name = "join_date")
    private LocalDate joinDate; // 入职日期

    @Column(name = "flight_hours")
    private Double flightHours; // 累计飞行时长(小时)

    @Column(name = "today_tasks")
    private Integer todayTasks; // 今日任务数

    @Column(name = "total_tasks")
    private Integer totalTasks; // 累计任务数

    @Column(columnDefinition = "TEXT")
    private String training; // 培训记录

    @Column(columnDefinition = "TEXT")
    private String remark; // 备注

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
