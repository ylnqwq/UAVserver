package com.uav.uavserver.dto;

import com.uav.uavserver.domain.AircraftStatus;
import com.uav.uavserver.domain.AircraftType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class AircraftDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "注册编号不能为空")
        private String registrationNumber;

        @NotBlank(message = "飞行器名称不能为空")
        private String name;

        private String model;
        private String manufacturer;

        @NotNull(message = "飞行器类型不能为空")
        private AircraftType type;

        private String serialNumber;
        private Double maxTakeoffWeight;
        private Double maxSpeed;
        private Double maxFlightTime;
        private Double maxFlightDistance;
        private Double maxAltitude;
        private String owner;
        private String operator;
        private String description;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private String model;
        private String manufacturer;
        private AircraftType type;
        private Double maxTakeoffWeight;
        private Double maxSpeed;
        private Double maxFlightTime;
        private Double maxFlightDistance;
        private Double maxAltitude;
        private AircraftStatus status;
        private String owner;
        private String operator;
        private String description;
    }

    @Data
    public static class Response {
        private Long id;
        private String registrationNumber;
        private String name;
        private String model;
        private String manufacturer;
        private AircraftType type;
        private String serialNumber;
        private Double maxTakeoffWeight;
        private Double maxSpeed;
        private Double maxFlightTime;
        private Double maxFlightDistance;
        private Double maxAltitude;
        private AircraftStatus status;
        private String owner;
        private String operator;
        private String description;
        private Double latitude;
        private Double longitude;
        private Double currentAltitude;
        private Double currentSpeed;
        private String flightStatus;
        private Integer batteryLevel;
        private LocalDateTime lastMaintenanceDate;
        private LocalDateTime nextMaintenanceDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
