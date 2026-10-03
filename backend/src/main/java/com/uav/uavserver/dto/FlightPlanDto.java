package com.uav.uavserver.dto;

import com.uav.uavserver.domain.FlightPlanStatus;
import com.uav.uavserver.domain.FlightPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class FlightPlanDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "计划名称不能为空")
        private String planName;

        @NotNull(message = "飞行器ID不能为空")
        private Long aircraftId;

        @NotNull(message = "飞行员ID不能为空")
        private Long pilotId;

        @NotNull(message = "飞行目的不能为空")
        private FlightPurpose purpose;

        @NotNull(message = "计划起飞时间不能为空")
        private LocalDateTime plannedStartTime;

        @NotNull(message = "计划降落时间不能为空")
        private LocalDateTime plannedEndTime;

        @NotBlank(message = "起飞地点不能为空")
        private String departureLocation;

        @NotBlank(message = "降落地点不能为空")
        private String arrivalLocation;

        private Double plannedAltitude;
        private String routeDescription;
        private String routeCoordinates;
        private String remarks;
        private String weatherConditions;
        private String emergencyContact;
        private String emergencyPhone;
    }

    @Data
    public static class UpdateRequest {
        private String planName;
        private LocalDateTime plannedStartTime;
        private LocalDateTime plannedEndTime;
        private String departureLocation;
        private String arrivalLocation;
        private Double plannedAltitude;
        private String routeDescription;
        private String routeCoordinates;
        private String remarks;
        private String weatherConditions;
    }

    @Data
    public static class ApprovalRequest {
        @NotNull(message = "审批决定不能为空")
        private Boolean approved;

        private String comments;
    }

    @Data
    public static class Response {
        private Long id;
        private String planNumber;
        private String planName;
        private String aircraftName;
        private String aircraftRegistration;
        private String pilotName;
        private String applicantName;
        private FlightPurpose purpose;
        private LocalDateTime plannedStartTime;
        private LocalDateTime plannedEndTime;
        private Integer plannedDuration;
        private String departureLocation;
        private String arrivalLocation;
        private Double plannedAltitude;
        private FlightPlanStatus status;
        private LocalDateTime submittedAt;
        private LocalDateTime approvedAt;
        private String approvedByName;
        private String approvalComments;
        private LocalDateTime createdAt;
    }
}
