package com.uav.uavserver.dto;

import com.uav.uavserver.domain.FlightStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class FlightRecordDto {

    @Data
    public static class CreateRequest {
        @NotNull(message = "飞行计划ID不能为空")
        private Long flightPlanId;
    }

    @Data
    public static class UpdateRequest {
        private FlightStatus status;
        private LocalDateTime actualStartTime;
        private LocalDateTime actualEndTime;
        private Double actualMaxAltitude;
        private Double actualMaxSpeed;
        private Double actualDistance;
        private String actualRoute;
        private String flightLog;
        private String telemetryData;
        private String incidents;
        private Boolean hasAbnormalEvents;
        private String abnormalDescription;
        private String weatherActual;
        private Double fuelConsumption;
        private Integer batteryUsage;
        private String postFlightComments;
    }

    @Data
    public static class Response {
        private Long id;
        private String flightNumber;
        private Long flightPlanId;
        private String planNumber;
        private FlightStatus status;
        private LocalDateTime actualStartTime;
        private LocalDateTime actualEndTime;
        private Integer actualDuration;
        private Double actualMaxAltitude;
        private Double actualMaxSpeed;
        private Double actualDistance;
        private Boolean hasAbnormalEvents;
        private String abnormalDescription;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
