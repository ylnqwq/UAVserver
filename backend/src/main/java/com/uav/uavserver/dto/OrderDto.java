package com.uav.uavserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单 DTO。
 *
 * Response 刻意使用前端页面已经使用的字段名（orderNo / sendAddr / recvAddr ...），
 * 这样页面模板几乎不用改，由后端做字段适配。
 */
public class OrderDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "寄件地址不能为空")
        private String sendAddr;
        private Double sendLat;
        private Double sendLng;

        @NotBlank(message = "收件地址不能为空")
        private String recvAddr;
        private Double recvLat;
        private Double recvLng;

        @NotBlank(message = "物品类型不能为空")
        private String itemType;

        private Double weight;
        private Double volume;
        private String recvName;
        private String recvPhone;
        private String timeRequirement; // normal / urgent
        private String specialRequirements;
        private String notes;
        private String priority;
        private LocalDateTime scheduledTime;
        private Long customerId;
    }

    @Data
    public static class UpdateRequest {
        private String sendAddr;
        private String recvAddr;
        private String itemType;
        private Double weight;
        private Double volume;
        private String recvName;
        private String recvPhone;
        private String timeRequirement;
        private String specialRequirements;
        private String notes;
        private String priority;
        private LocalDateTime scheduledTime;
    }

    /** 调度：指派飞行器/机长/高度 */
    @Data
    public static class DispatchRequest {
        private Long aircraftId;
        private Long pilotId;
        private Double routeAltitude;
        private Integer estimatedDuration;
    }

    @Data
    public static class Response {
        private Long id;
        private String orderNo;
        private String status;
        private String priority;
        private Boolean urgencyFlag;

        private String sendAddr;
        private Double sendLat;
        private Double sendLng;
        private String recvAddr;
        private Double recvLat;
        private Double recvLng;

        private String itemType;
        private Double weight;
        private Double volume;
        private String recvName;
        private String recvPhone;
        private String timeRequirement;
        private String specialRequirements;
        private String notes;

        private Double distance;
        private Integer estimatedDuration;
        private Double amount;
        private LocalDateTime scheduledTime;
        private LocalDateTime actualStartTime;
        private LocalDateTime actualEndTime;
        private LocalDateTime acceptedTime;
        private LocalDateTime dispatchedTime;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        private Long customerId;
        private String customerName;
        private String acceptedBy;

        private Long aircraftId;
        private String deviceNo;
        private String model;
        private Long pilotId;
        private String pilotName;

        private Double routeAltitude;
    }

    /** 航线预检结果（距离/时长/高度/途经禁飞区数量） */
    @Data
    public static class RouteInfo {
        private Double distance;
        private Integer duration;
        private Double altitude;
        private Integer noFlyZones;
    }
}
