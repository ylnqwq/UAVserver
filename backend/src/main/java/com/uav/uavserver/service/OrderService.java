package com.uav.uavserver.service;

import com.uav.uavserver.domain.Aircraft;
import com.uav.uavserver.domain.AirspaceZone;
import com.uav.uavserver.domain.Order;
import com.uav.uavserver.domain.User;
import com.uav.uavserver.dto.OrderDto;
import com.uav.uavserver.repository.AircraftRepository;
import com.uav.uavserver.repository.AirspaceZoneRepository;
import com.uav.uavserver.repository.OrderRepository;
import com.uav.uavserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 订单服务：下单 → 平台接单 → 调度（指派飞行器/机长）→ 起飞 → 完成 / 取消。
 * 距离、预计时长、途经禁飞区数量都是按坐标实时计算，不是写死的。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AircraftRepository aircraftRepository;
    private final AirspaceZoneRepository airspaceZoneRepository;

    /** 巡航速度(m/s)，用于估算配送时长 */
    private static final double CRUISE_SPEED_MPS = 15.0;
    private static final Random RANDOM = new Random();

    // ==================== 查询 ====================

    public List<OrderDto.Response> getAllOrders() {
        return orderRepository.findAll().stream()
                .sorted((a, b) -> compareCreatedAt(b, a))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderDto.Response> getOrdersByStatus(String status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderDto.Response> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public OrderDto.Response getOrder(Long id) {
        return toResponse(findOrder(id));
    }

    public OrderDto.Response getOrderByNumber(String orderNumber) {
        return toResponse(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNumber)));
    }

    // ==================== 下单 ====================

    @Transactional
    public OrderDto.Response createOrder(OrderDto.CreateRequest request, String username) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerId(request.getCustomerId() != null
                ? request.getCustomerId()
                : userRepository.findByUsername(username).map(User::getId).orElse(null));

        order.setPickupLocation(request.getSendAddr());
        order.setPickupLatitude(request.getSendLat());
        order.setPickupLongitude(request.getSendLng());
        order.setDeliveryLocation(request.getRecvAddr());
        order.setDeliveryLatitude(request.getRecvLat());
        order.setDeliveryLongitude(request.getRecvLng());

        order.setServiceType(request.getItemType());
        order.setWeight(request.getWeight());
        order.setVolume(request.getVolume());
        order.setReceiverName(request.getRecvName());
        order.setReceiverPhone(request.getRecvPhone());
        order.setTimeRequirement(request.getTimeRequirement());
        order.setSpecialRequirements(request.getSpecialRequirements());
        order.setNotes(request.getNotes());
        order.setScheduledTime(request.getScheduledTime());

        boolean urgent = "urgent".equalsIgnoreCase(request.getTimeRequirement());
        order.setPriority(request.getPriority() != null
                ? request.getPriority()
                : (urgent ? "URGENT" : "NORMAL"));
        order.setStatus("PENDING");

        // 按坐标实时算距离/时长
        Double distance = calcDistanceKm(order.getPickupLatitude(), order.getPickupLongitude(),
                order.getDeliveryLatitude(), order.getDeliveryLongitude());
        order.setDistance(distance);
        order.setEstimatedDuration(estimateDuration(distance));
        // 基础运费：起步价 5 元 + 距离单价 12 元/km（演示用规则）
        if (distance != null) {
            order.setAmount(Math.round((5 + distance * 12) * 100) / 100.0);
        }

        return toResponse(orderRepository.save(order));
    }

    // ==================== 修改 / 流转 ====================

    @Transactional
    public OrderDto.Response updateOrder(Long id, OrderDto.UpdateRequest request) {
        Order order = findOrder(id);

        if (request.getSendAddr() != null) order.setPickupLocation(request.getSendAddr());
        if (request.getRecvAddr() != null) order.setDeliveryLocation(request.getRecvAddr());
        if (request.getItemType() != null) order.setServiceType(request.getItemType());
        if (request.getWeight() != null) order.setWeight(request.getWeight());
        if (request.getVolume() != null) order.setVolume(request.getVolume());
        if (request.getRecvName() != null) order.setReceiverName(request.getRecvName());
        if (request.getRecvPhone() != null) order.setReceiverPhone(request.getRecvPhone());
        if (request.getTimeRequirement() != null) order.setTimeRequirement(request.getTimeRequirement());
        if (request.getSpecialRequirements() != null) order.setSpecialRequirements(request.getSpecialRequirements());
        if (request.getNotes() != null) order.setNotes(request.getNotes());
        if (request.getPriority() != null) order.setPriority(request.getPriority());
        if (request.getScheduledTime() != null) order.setScheduledTime(request.getScheduledTime());

        return toResponse(orderRepository.save(order));
    }

    /** 平台接单：由运营/管理员确认接单（本平台自营，不再设商家角色） */
    @Transactional
    public OrderDto.Response acceptOrder(Long id, String username) {
        Order order = findOrder(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("只有待接单的订单可以接单，当前状态: " + order.getStatus());
        }
        order.setAcceptedBy(userName(userRepository.findByUsername(username).map(User::getId).orElse(null)));
        order.setStatus("ACCEPTED");
        order.setAcceptedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    /** 调度：指派飞行器 / 机长 / 飞行高度 */
    @Transactional
    public OrderDto.Response dispatchOrder(Long id, OrderDto.DispatchRequest request) {
        Order order = findOrder(id);
        if (request.getAircraftId() != null) {
            Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                    .orElseThrow(() -> new RuntimeException("飞行器不存在: id=" + request.getAircraftId()));
            order.setAircraftId(aircraft.getId());
        }
        if (request.getPilotId() != null) order.setPilotId(request.getPilotId());
        if (request.getRouteAltitude() != null) order.setRouteAltitude(request.getRouteAltitude());
        if (request.getEstimatedDuration() != null) order.setEstimatedDuration(request.getEstimatedDuration());
        order.setStatus("DISPATCHED");
        order.setDispatchedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderDto.Response startOrder(Long id) {
        Order order = findOrder(id);
        if (order.getAircraftId() == null) {
            throw new RuntimeException("请先调度飞行器再开始配送");
        }
        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderDto.Response completeOrder(Long id) {
        Order order = findOrder(id);
        order.setStatus("COMPLETED");
        order.setActualEndTime(LocalDateTime.now());
        Aircraft aircraft = order.getAircraftId() != null
                ? aircraftRepository.findById(order.getAircraftId()).orElse(null)
                : null;
        if (aircraft != null) {
            aircraft.setFlightStatus("grounded");
            aircraftRepository.save(aircraft);
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderDto.Response cancelOrder(Long id) {
        Order order = findOrder(id);
        if ("COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("已完成的订单不能取消");
        }
        order.setStatus("CANCELLED");
        return toResponse(orderRepository.save(order));
    }

    // ==================== 航线预检 ====================

    /**
     * 计算订单航线信息：距离、预计时长、规划高度、途经禁飞区数量。
     * 禁飞区判定：禁飞区圆心到航线线段的距离 ≤ 禁飞区半径 + 300m 安全余量。
     */
    public OrderDto.RouteInfo getRouteInfo(Long id) {
        Order order = findOrder(id);
        OrderDto.RouteInfo info = new OrderDto.RouteInfo();

        Double distance = order.getDistance() != null
                ? order.getDistance()
                : calcDistanceKm(order.getPickupLatitude(), order.getPickupLongitude(),
                        order.getDeliveryLatitude(), order.getDeliveryLongitude());
        info.setDistance(distance);
        info.setDuration(order.getEstimatedDuration() != null
                ? order.getEstimatedDuration()
                : estimateDuration(distance));
        info.setAltitude(order.getRouteAltitude() != null ? order.getRouteAltitude() : 120.0);

        int conflicts = 0;
        if (order.getPickupLatitude() != null && order.getDeliveryLatitude() != null) {
            List<AirspaceZone> noFlyZones = airspaceZoneRepository.findByTypeAndActive("nofly", true);
            for (AirspaceZone zone : noFlyZones) {
                if (zone.getLatitude() == null || zone.getLongitude() == null) continue;
                double distanceToRoute = distanceToSegmentMeters(
                        zone.getLatitude(), zone.getLongitude(),
                        order.getPickupLatitude(), order.getPickupLongitude(),
                        order.getDeliveryLatitude(), order.getDeliveryLongitude());
                double safeRadius = (zone.getRadius() != null ? zone.getRadius() : 0) + 300;
                if (distanceToRoute <= safeRadius) conflicts++;
            }
        }
        info.setNoFlyZones(conflicts);
        return info;
    }

    // ==================== 内部工具 ====================

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在: id=" + id));
    }

    private String generateOrderNumber() {
        String base = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        for (int i = 0; i < 5; i++) {
            String candidate = base + (100 + RANDOM.nextInt(900));
            if (!orderRepository.existsByOrderNumber(candidate)) return candidate;
        }
        return base + RANDOM.nextInt(10000);
    }

    /** 球面距离（km） */
    private Double calcDistanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return null;
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(earthRadius * c * 100) / 100.0;
    }

    private Integer estimateDuration(Double distanceKm) {
        if (distanceKm == null) return null;
        // 距离(m) / 巡航速度 = 秒 → 分钟，向上取整
        return (int) Math.max(1, Math.ceil(distanceKm * 1000 / CRUISE_SPEED_MPS / 60));
    }

    /** 点(纬度,经度)到线段(1-2)的距离，单位米（小范围平面近似） */
    private double distanceToSegmentMeters(double lat, double lng,
                                           double lat1, double lng1,
                                           double lat2, double lng2) {
        double metersPerDegLat = 111320.0;
        double metersPerDegLng = 111320.0 * Math.cos(Math.toRadians(lat1));
        double x = (lng - lng1) * metersPerDegLng;
        double y = (lat - lat1) * metersPerDegLat;
        double dx = (lng2 - lng1) * metersPerDegLng;
        double dy = (lat2 - lat1) * metersPerDegLat;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, (x * dx + y * dy) / len2));
        return Math.hypot(x - t * dx, y - t * dy);
    }

    private int compareCreatedAt(Order a, Order b) {
        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
        return a.getCreatedAt().compareTo(b.getCreatedAt());
    }

    /** 把订单实体适配成前端字段（大屏/列表共用） */
    public OrderDto.Response toResponse(Order order) {        OrderDto.Response response = new OrderDto.Response();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setPriority(order.getPriority());
        response.setUrgencyFlag("URGENT".equals(order.getPriority()) || "HIGH".equals(order.getPriority())
                || "urgent".equalsIgnoreCase(order.getTimeRequirement()));

        response.setSendAddr(order.getPickupLocation());
        response.setSendLat(order.getPickupLatitude());
        response.setSendLng(order.getPickupLongitude());
        response.setRecvAddr(order.getDeliveryLocation());
        response.setRecvLat(order.getDeliveryLatitude());
        response.setRecvLng(order.getDeliveryLongitude());

        response.setItemType(order.getServiceType());
        response.setWeight(order.getWeight());
        response.setVolume(order.getVolume());
        response.setRecvName(order.getReceiverName());
        response.setRecvPhone(order.getReceiverPhone());
        response.setTimeRequirement(order.getTimeRequirement());
        response.setSpecialRequirements(order.getSpecialRequirements());
        response.setNotes(order.getNotes());

        response.setDistance(order.getDistance());
        response.setEstimatedDuration(order.getEstimatedDuration());
        response.setAmount(order.getAmount());
        response.setScheduledTime(order.getScheduledTime());
        response.setActualStartTime(order.getActualStartTime());
        response.setActualEndTime(order.getActualEndTime());
        response.setAcceptedTime(order.getAcceptedAt());
        response.setDispatchedTime(order.getDispatchedAt());
        response.setCreateTime(order.getCreatedAt());
        response.setUpdateTime(order.getUpdatedAt());
        response.setRouteAltitude(order.getRouteAltitude());

        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(userName(order.getCustomerId()));
        response.setAcceptedBy(order.getAcceptedBy());

        response.setAircraftId(order.getAircraftId());
        if (order.getAircraftId() != null) {
            Optional<Aircraft> aircraft = aircraftRepository.findById(order.getAircraftId());
            aircraft.ifPresent(a -> {
                response.setDeviceNo(a.getRegistrationNumber());
                response.setModel(a.getModel());
            });
        }
        response.setPilotId(order.getPilotId());
        response.setPilotName(userName(order.getPilotId()));

        return response;
    }

    private String userName(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> u.getRealName() != null && !u.getRealName().isBlank() ? u.getRealName() : u.getUsername())
                .orElse(null);
    }
}
