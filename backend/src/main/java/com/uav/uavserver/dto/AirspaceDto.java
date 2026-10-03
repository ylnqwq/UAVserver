package com.uav.uavserver.dto;

import com.uav.uavserver.domain.AirspaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class AirspaceDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "空域编码不能为空")
        private String airspaceCode;

        @NotBlank(message = "空域名称不能为空")
        private String name;

        @NotNull(message = "空域类型不能为空")
        private AirspaceType type;

        @NotBlank(message = "边界坐标不能为空")
        private String boundaries;

        private Double minAltitude;
        private Double maxAltitude;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private String restrictions;
        private String managingAuthority;
        private String contactPhone;
        private String description;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private AirspaceType type;
        private String boundaries;
        private Double minAltitude;
        private Double maxAltitude;
        private Boolean isActive;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private String restrictions;
        private String managingAuthority;
        private String contactPhone;
        private String description;
    }

    @Data
    public static class Response {
        private Long id;
        private String airspaceCode;
        private String name;
        private AirspaceType type;
        private String boundaries;
        private Double minAltitude;
        private Double maxAltitude;
        private Boolean isActive;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private String restrictions;
        private String managingAuthority;
        private String contactPhone;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
