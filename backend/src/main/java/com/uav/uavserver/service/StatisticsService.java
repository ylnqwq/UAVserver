package com.uav.uavserver.service;

import com.uav.uavserver.domain.Aircraft;
import com.uav.uavserver.domain.AircraftStatus;
import com.uav.uavserver.domain.AirspaceZone;
import com.uav.uavserver.domain.EmergencyAlert;
import com.uav.uavserver.domain.FlightPlan;
import com.uav.uavserver.domain.FlightPlanStatus;
import com.uav.uavserver.domain.Order;
import com.uav.uavserver.domain.Station;
import com.uav.uavserver.domain.User;
import com.uav.uavserver.domain.Violation;
import com.uav.uavserver.dto.StatisticsDto;
import com.uav.uavserver.repository.AircraftRepository;
import com.uav.uavserver.repository.AirspaceZoneRepository;
import com.uav.uavserver.repository.EmergencyAlertRepository;
import com.uav.uavserver.repository.FlightPlanRepository;
import com.uav.uavserver.repository.OrderRepository;
import com.uav.uavserver.repository.StationRepository;
import com.uav.uavserver.repository.UserRepository;
import com.uav.uavserver.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统计服务：把数据库里的真实记录聚合给前端（工作台、数据大屏）。
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private final UserRepository userRepository;
    private final AircraftRepository aircraftRepository;
    private final AirspaceZoneRepository airspaceZoneRepository;
    private final StationRepository stationRepository;
    private final FlightPlanRepository flightPlanRepository;
    private final ViolationRepository violationRepository;
    private final EmergencyAlertRepository alertRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    public StatisticsDto.Overview getOverview() {
        StatisticsDto.Overview overview = new StatisticsDto.Overview();

        List<User> users = userRepository.findAll();
        List<Aircraft> aircraft = aircraftRepository.findAll();
        List<AirspaceZone> zones = airspaceZoneRepository.findAll();
        List<Station> stations = stationRepository.findAll();
        List<FlightPlan> plans = flightPlanRepository.findAll();

        overview.setUserCount(users.size());
        overview.setAircraftCount(aircraft.size());
        overview.setFlyingAircraftCount(aircraft.stream()
                .filter(a -> "flying".equals(a.getFlightStatus()))
                .count());
        overview.setZoneCount(zones.size());
        overview.setStationCount(stations.size());
        overview.setFlightPlanCount(plans.size());
        overview.setOrderCount(countOrders());

        overview.setUserByRole(groupBy(users, u -> u.getRole() != null ? u.getRole().name() : "UNKNOWN"));
        overview.setAircraftByStatus(groupBy(aircraft, a -> a.getStatus() != null ? a.getStatus().name() : "UNKNOWN"));
        overview.setAircraftByType(groupBy(aircraft, a -> a.getType() != null ? a.getType().name() : "UNKNOWN"));
        overview.setFlightPlanByStatus(groupBy(plans, p -> p.getStatus() != null ? p.getStatus().name() : "UNKNOWN"));
        overview.setZoneByType(groupBy(zones, z -> z.getType() != null ? z.getType() : "unknown"));
        overview.setStationByType(groupBy(stations, s -> s.getType() != null ? s.getType() : "unknown"));

        overview.setStationCapacity(stations.stream()
                .map(Station::getCapacity)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
        overview.setStationCurrentUsage(stations.stream()
                .map(Station::getCurrentUsage)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());

        return overview;
    }

    /**
     * 管理员大屏：指标 + 订单趋势 + 违规统计 + 告警 + 审批分布 + 场站占用率。
     * 全部由数据库真实数据聚合，前端不再有写死的数字。
     */
    public Map<String, Object> getAdminReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        long today = count("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE()");
        long yesterday = count("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = ?",
                Date.valueOf(LocalDate.now().minusDays(1)));
        long totalOrders = count("SELECT COUNT(*) FROM orders");
        long completedOrders = count("SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED'");

        List<User> users = userRepository.findAll();
        List<Aircraft> aircraft = aircraftRepository.findAll();
        List<Station> stations = stationRepository.findAll();
        List<FlightPlan> plans = flightPlanRepository.findAll();
        List<Violation> violations = violationRepository.findAll();
        List<EmergencyAlert> alerts = alertRepository.findAll();

        long customers = users.stream().filter(u -> u.getRole() != null && "CUSTOMER".equals(u.getRole().name())).count();
        long activeCustomers = users.stream()
                .filter(u -> u.getRole() != null && "CUSTOMER".equals(u.getRole().name()))
                .filter(u -> u.getStatus() != null && "ACTIVE".equals(u.getStatus().name()))
                .count();
        long flying = aircraft.stream().filter(a -> "flying".equals(a.getFlightStatus())).count();

        report.put("todayOrders", today);
        report.put("orderTrend", yesterday == 0 ? (today > 0 ? 100.0 : 0.0)
                : Math.round((today - yesterday) * 1000.0 / yesterday) / 10.0);
        report.put("completionRate", totalOrders == 0 ? 0.0 : Math.round(completedOrders * 1000.0 / totalOrders) / 10.0);
        report.put("activeCustomers", activeCustomers);
        report.put("totalCustomers", customers);
        report.put("currentFlights", flying);
        report.put("airspaceUtilization", aircraft.isEmpty() ? 0.0
                : Math.round(flying * 1000.0 / aircraft.size()) / 10.0);

        // 违规统计
        Map<String, Long> violationTypes = new LinkedHashMap<>();
        for (String type : List.of("routeDeviation", "airspaceViolation", "heightViolation", "noFlyZone", "other")) {
            violationTypes.put(type, violations.stream().filter(v -> type.equals(v.getType())).count());
        }
        report.put("violationTotal", (long) violations.size());
        report.put("violationTypes", violationTypes);

        // 实时告警：未关闭的应急告警（最多 8 条）
        List<Map<String, Object>> alertItems = alerts.stream()
                .filter(a -> a.getStatus() == null || !"closed".equalsIgnoreCase(a.getStatus()))
                .sorted(Comparator.comparing(EmergencyAlert::getOccurTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(a -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", a.getId());
                    item.put("level", levelText(a.getLevel()));
                    item.put("content", a.getContent());
                    item.put("time", a.getOccurTime() != null ? a.getOccurTime().format(TIME_FMT) : "");
                    return item;
                })
                .collect(Collectors.toList());
        report.put("alerts", alertItems);

        // 近7天订单趋势（总数/完成/取消）
        List<String> dates = new ArrayList<>();
        List<Long> ordersSeries = new ArrayList<>();
        List<Long> completedSeries = new ArrayList<>();
        List<Long> cancelledSeries = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            Date d = Date.valueOf(day);
            dates.add(day.format(fmt));
            ordersSeries.add(count("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = ?", d));
            completedSeries.add(count("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = ? AND status = 'COMPLETED'", d));
            cancelledSeries.add(count("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = ? AND status = 'CANCELLED'", d));
        }
        report.put("trendDates", dates);
        report.put("trendOrders", ordersSeries);
        report.put("trendCompleted", completedSeries);
        report.put("trendCancelled", cancelledSeries);

        // 近8周订单量
        List<String> weekLabels = new ArrayList<>();
        List<Long> weekOrders = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            LocalDate start = LocalDate.now().minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate end = start.plusDays(6);
            weekLabels.add("第" + (8 - i) + "周");
            weekOrders.add(count("SELECT COUNT(*) FROM orders WHERE DATE(created_at) BETWEEN ? AND ?",
                    Date.valueOf(start), Date.valueOf(end)));
        }
        report.put("weekLabels", weekLabels);
        report.put("weekOrders", weekOrders);

        // 订单类型分布
        Map<String, Long> orderTypes = new LinkedHashMap<>();
        try {
            jdbcTemplate.query("SELECT service_type AS t, COUNT(*) AS c FROM orders GROUP BY service_type ORDER BY c DESC",
                    rs -> {
                        orderTypes.put(rs.getString("t") != null ? rs.getString("t") : "未分类", rs.getLong("c"));
                    });
        } catch (Exception e) {
            log.warn("统计订单类型失败: {}", e.getMessage());
        }
        report.put("orderTypeDistribution", orderTypes);

        // 违规趋势（近7天 × 类型）
        List<String> violationDates = new ArrayList<>();
        Map<String, List<Long>> violationTrend = new LinkedHashMap<>();
        for (String type : List.of("routeDeviation", "airspaceViolation", "heightViolation")) {
            violationTrend.put(type, new ArrayList<>());
        }
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            violationDates.add(day.format(fmt));
            for (String type : violationTrend.keySet()) {
                long c = violations.stream()
                        .filter(v -> type.equals(v.getType()))
                        .filter(v -> v.getOccurTime() != null && v.getOccurTime().toLocalDate().equals(day))
                        .count();
                violationTrend.get(type).add(c);
            }
        }
        report.put("violationDates", violationDates);
        report.put("violationTrend", violationTrend);

        // 飞行计划审批分布
        report.put("approvalApproved", plans.stream().filter(p -> p.getStatus() == FlightPlanStatus.APPROVED).count());
        report.put("approvalRejected", plans.stream().filter(p -> p.getStatus() == FlightPlanStatus.REJECTED).count());
        report.put("approvalPending", plans.stream()
                .filter(p -> p.getStatus() == FlightPlanStatus.SUBMITTED || p.getStatus() == FlightPlanStatus.UNDER_REVIEW)
                .count());

        // 场站占用率（雷达图）
        List<String> densityLabels = new ArrayList<>();
        List<Integer> densityValues = new ArrayList<>();
        stations.stream().limit(6).forEach(s -> {
            densityLabels.add(s.getName());
            int capacity = s.getCapacity() != null ? s.getCapacity() : 0;
            int used = s.getCurrentUsage() != null ? s.getCurrentUsage() : 0;
            densityValues.add(capacity == 0 ? 0 : Math.min(100, (int) Math.round(used * 100.0 / capacity)));
        });
        report.put("densityLabels", densityLabels);
        report.put("densityValues", densityValues);

        return report;
    }

    /**
     * 工作台数据：统计 + 近7天趋势 + 订单状态分布 + 待处理事项 + 实时告警。
     * 待办/告警都由数据库现有数据推导，不是写死的假数据。
     */
    public StatisticsDto.Dashboard getDashboard() {
        StatisticsDto.Dashboard dashboard = new StatisticsDto.Dashboard();

        // ---- 指标卡 ----
        dashboard.setTodayOrders(count("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE()"));
        long totalOrders = count("SELECT COUNT(*) FROM orders");
        long completedOrders = count("SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED'");
        dashboard.setCompletionRate(totalOrders == 0
                ? 0.0
                : Math.round(completedOrders * 1000.0 / totalOrders) / 10.0);

        List<Aircraft> aircraft = aircraftRepository.findAll();
        long flyingCount = aircraft.stream().filter(a -> "flying".equals(a.getFlightStatus())).count();
        dashboard.setFlyingCount(flyingCount);

        // ---- 近7天订单趋势 ----
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            dashboard.getTrendDates().add(day.format(fmt));
            dashboard.getTrendOrders().add(count(
                    "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = ?", Date.valueOf(day)));
            dashboard.getTrendCompleted().add(count(
                    "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = ? AND status = 'COMPLETED'",
                    Date.valueOf(day)));
        }

        // ---- 订单状态分布 ----
        try {
            jdbcTemplate.query("SELECT status, COUNT(*) AS c FROM orders GROUP BY status", rs -> {
                dashboard.getOrderStatusDistribution().put(rs.getString("status"), rs.getLong("c"));
            });
        } catch (Exception e) {
            log.warn("统计订单状态分布失败: {}", e.getMessage());
        }

        // ---- 待处理事项：待审批的飞行计划 / 待接单的订单 ----
        flightPlanRepository.findAll().stream()
                .filter(p -> p.getStatus() == FlightPlanStatus.SUBMITTED
                        || p.getStatus() == FlightPlanStatus.UNDER_REVIEW)
                .sorted(Comparator.comparing(FlightPlan::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .forEach(p -> {
                    StatisticsDto.TodoItem item = new StatisticsDto.TodoItem();
                    item.setType("计划审批");
                    item.setTitle("飞行计划 " + p.getPlanNumber() + " 待审批");
                    item.setTime(formatTime(p.getCreatedAt() != null ? p.getCreatedAt() : p.getSubmittedAt()));
                    item.setBusinessType("flightPlan");
                    item.setBusinessId(p.getId());
                    dashboard.getTodos().add(item);
                });

        List<Map<String, Object>> pendingOrders = queryRows(
                "SELECT id, order_number, created_at FROM orders WHERE status = 'PENDING' ORDER BY created_at DESC LIMIT 10");
        for (Map<String, Object> row : pendingOrders) {
            StatisticsDto.TodoItem item = new StatisticsDto.TodoItem();
            item.setType("订单处理");
            item.setTitle("订单 " + row.get("order_number") + " 待接单");
            item.setTime(formatTime(row.get("created_at")));
            item.setBusinessType("order");
            item.setBusinessId(((Number) row.get("id")).longValue());
            dashboard.getTodos().add(item);
        }

        // ---- 实时告警：低电量 / 维护中的飞行器 ----
        aircraft.stream()
                .filter(a -> a.getBatteryLevel() != null && a.getBatteryLevel() <= 30)
                .forEach(a -> dashboard.getAlerts().add(alert("紧急",
                        "飞行器 " + a.getRegistrationNumber() + " 电量低于 " + a.getBatteryLevel() + "%")));
        aircraft.stream()
                .filter(a -> a.getStatus() == AircraftStatus.MAINTENANCE)
                .forEach(a -> dashboard.getAlerts().add(alert("一般",
                        "设备 " + a.getRegistrationNumber() + " 处于维护状态")));
        dashboard.setAlertCount(dashboard.getAlerts().size());

        return dashboard;
    }

    private StatisticsDto.AlertItem alert(String level, String content) {
        StatisticsDto.AlertItem item = new StatisticsDto.AlertItem();
        item.setLevel(level);
        item.setContent(content);
        item.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        return item;
    }

    private String formatTime(Object time) {
        if (time == null) return "";
        if (time instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().format(TIME_FMT);
        }
        if (time instanceof LocalDateTime ldt) {
            return ldt.format(TIME_FMT);
        }
        if (time instanceof java.util.Date date) {
            return new java.sql.Timestamp(date.getTime()).toLocalDateTime().format(TIME_FMT);
        }
        return time.toString();
    }

    private long count(String sql, Object... args) {
        try {
            Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.warn("统计查询失败 [{}]: {}", sql, e.getMessage());
            return 0L;
        }
    }

    private List<Map<String, Object>> queryRows(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForList(sql, args);
        } catch (Exception e) {
            log.warn("查询失败 [{}]: {}", sql, e.getMessage());
            return List.of();
        }
    }

    /**
     * 订单暂时还没有对应的 JPA 实体（只有 orders 表），这里用 JDBC 直接计数。
     */
    private long countOrders() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("统计订单数量失败（orders 表可能不存在）: {}", e.getMessage());
            return 0L;
        }
    }

    private String levelText(Integer level) {
        if (level == null) return "一般";
        return switch (level) {
            case 3 -> "紧急";
            case 2 -> "严重";
            default -> "一般";
        };
    }

    private <T, K> Map<String, Long> groupBy(List<T> list, Function<T, String> keyFn) {
        Map<String, Long> result = list.stream()
                .collect(Collectors.groupingBy(keyFn, LinkedHashMap::new, Collectors.counting()));
        return result;
    }
}
