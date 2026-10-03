package com.uav.uavserver.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsDto {

    /**
     * 平台总览统计（工作台/数据大屏使用）。
     * 所有数字都来自数据库真实记录。
     */
    @Data
    public static class Overview {
        private long userCount;
        private long aircraftCount;
        private long flyingAircraftCount;
        private long zoneCount;
        private long stationCount;
        private long flightPlanCount;
        private long orderCount;

        private Map<String, Long> userByRole = new LinkedHashMap<>();
        private Map<String, Long> aircraftByStatus = new LinkedHashMap<>();
        private Map<String, Long> aircraftByType = new LinkedHashMap<>();
        private Map<String, Long> flightPlanByStatus = new LinkedHashMap<>();
        private Map<String, Long> zoneByType = new LinkedHashMap<>();
        private Map<String, Long> stationByType = new LinkedHashMap<>();

        /** 场站总容量 / 当前占用（用于利用率展示） */
        private int stationCapacity;
        private int stationCurrentUsage;
    }

    /** 工作台数据（全部由数据库推导，不含任何写死的数字） */
    @Data
    public static class Dashboard {
        private long todayOrders;
        private double completionRate;
        private long flyingCount;
        private long alertCount;

        /** 近7天订单趋势 */
        private List<String> trendDates = new ArrayList<>();
        private List<Long> trendOrders = new ArrayList<>();
        private List<Long> trendCompleted = new ArrayList<>();

        /** 订单状态分布 */
        private Map<String, Long> orderStatusDistribution = new LinkedHashMap<>();

        /** 待处理事项 */
        private List<TodoItem> todos = new ArrayList<>();

        /** 实时告警（根据现有数据推导） */
        private List<AlertItem> alerts = new ArrayList<>();
    }

    @Data
    public static class TodoItem {
        private String type;
        private String title;
        private String time;
        private String businessType;
        private Long businessId;
    }

    @Data
    public static class AlertItem {
        private String level;
        private String content;
        private String time;
    }
}
