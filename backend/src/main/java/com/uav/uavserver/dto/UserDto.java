package com.uav.uavserver.dto;

import com.uav.uavserver.domain.UserRole;
import com.uav.uavserver.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户相关 DTO。
 * 注意：这里刻意不包含 password 字段，避免把密码哈希返回给前端。
 */
public class UserDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
        private String password;

        @NotBlank(message = "真实姓名不能为空")
        private String realName;

        @Email(message = "邮箱格式不正确")
        private String email;

        private String phone;

        @NotNull(message = "角色不能为空")
        private UserRole role;

        private UserStatus status = UserStatus.ACTIVE;

        private String organization;

        // 人员档案（人员管理页面使用）
        private String idCard;
        private String certNo;
        private LocalDate certExpiry;
        private LocalDate joinDate;
        private Double flightHours;
        private Integer todayTasks;
        private Integer totalTasks;
        private String training;
        private String remark;
    }

    @Data
    public static class UpdateRequest {
        private String realName;
        @Email(message = "邮箱格式不正确")
        private String email;
        private String phone;
        private UserRole role;
        private UserStatus status;
        private String organization;

        // 人员档案（人员管理页面使用）
        private String idCard;
        private String certNo;
        private LocalDate certExpiry;
        private LocalDate joinDate;
        private Double flightHours;
        private Integer todayTasks;
        private Integer totalTasks;
        private String training;
        private String remark;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
        private String newPassword;
    }

    @Data
    public static class Response {
        private Long id;
        private String username;
        private String email;
        private String phone;
        private String realName;
        private UserRole role;
        private UserStatus status;
        private String organization;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;

        // 人员档案（人员管理页面使用）
        private String idCard;
        private String certNo;
        private LocalDate certExpiry;
        private LocalDate joinDate;
        private Double flightHours;
        private Integer todayTasks;
        private Integer totalTasks;
        private String training;
        private String remark;
    }
}
