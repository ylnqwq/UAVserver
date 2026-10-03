package com.uav.uavserver.service;

import com.uav.uavserver.domain.AirspaceZone;
import com.uav.uavserver.domain.Order;
import com.uav.uavserver.domain.Station;
import com.uav.uavserver.domain.Violation;
import com.uav.uavserver.repository.AirspaceZoneRepository;
import com.uav.uavserver.repository.OrderRepository;
import com.uav.uavserver.repository.StationRepository;
import com.uav.uavserver.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 违规轨迹服务：为「违规详情」提供地图上要画的航迹数据。
 *
 * <p>数据来源说明（重要）：库中并没有实测遥测/航迹数据（flight_records 为空表、
 * flight_plans.route_coordinates 全为 NULL），因此航迹是用【真实锚点】推演出来的：
 * <ol>
 *   <li>优先用违规记录关联订单（order_no）的「取件地 → 送达地」；</li>
 *   <li>关联订单缺失时，退回该飞行器（aircraft_id）最近一次有坐标的订单；</li>
 *   <li>再退回离违规点最近的场站 → 违规点；</li>
 *   <li>最终兜底：以违规点几何偏移出一个推演起点。</li>
 * </ol>
 * 航迹线强制经过违规点，并按违规 id 施加固定方向的横向弧度，使线条像真实航迹而非直线；
 * 同一记录每次请求结果完全一致（不使用随机数）。
 * 返回值里的 source / sourceNote 会明确标注"推演"，前端需据此提示用户。
 */
@Service
@RequiredArgsConstructor
public class ViolationTrackService {

    private final ViolationRepository violationRepository;
    private final OrderRepository orderRepository;
    private final StationRepository stationRepository;
    private final AirspaceZoneRepository airspaceZoneRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double METERS_PER_DEG_LAT = 111320.0;
    /** 航迹弧度上限（米），避免长航线甩出夸张的弯 */
    private static final double MAX_BULGE_METERS = 600.0;
    /** 判定"点在区域内/贴近边界"的容差（米） */
    private static final double ZONE_TOLERANCE_METERS = 300.0;
    /** 无关联订单时，飞行器最近一次订单的航线距违规点超过该距离就不采用（米） */
    private static final double RELATED_ROUTE_TOLERANCE_METERS = 3000.0;

    @Transactional(readOnly = true)
    public Map<String, Object> buildTrack(Long violationId) {
        Violation v = violationRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("违规记录不存在: id=" + violationId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("violationId", v.getId());
        result.put("violationNo", v.getViolationNo());
        result.put("type", v.getType());
        result.put("severity", v.getSeverity());
        result.put("status", v.getStatus());
        result.put("deviceNo", v.getDeviceNo());
        result.put("pilotName", v.getPilotName());
        result.put("orderNo", v.getOrderNo());
        result.put("location", v.getLocation());
        result.put("occurTime", v.getOccurTime() == null ? null : v.getOccurTime().format(TIME_FMT));
        result.put("detectMethod", v.getDetectMethod());

        Double lat = v.getLatitude();
        Double lng = v.getLongitude();
        if (lat == null || lng == null) {
            result.put("hasTrack", false);
            result.put("message", "该违规记录没有经纬度，无法生成轨迹");
            return result;
        }

        Anchor anchor = resolveAnchor(v, lat, lng);
        result.put("hasTrack", true);
        result.put("source", anchor.source());
        result.put("sourceNote", anchor.note());
        result.put("origin", point(anchor.originName(), anchor.originLat(), anchor.originLng(), "起"));
        result.put("destination", point(anchor.destName(), anchor.destLat(), anchor.destLng(), "终"));
        result.put("violationPoint", point(v.getLocation(), lat, lng, "违"));

        // 申报/计划航线：起点 → 终点 直线
        result.put("plannedRoute", curve(anchor.originLat(), anchor.originLng(),
                anchor.destLat(), anchor.destLng(), 16, 0, true));

        // 实际航迹：起点 → 违规点 → 终点，两段各带一个方向的弧度
        double sign = (v.getId() != null && v.getId() % 2 == 0) ? 1 : -1;
        double bulge = Math.min(Math.max(distance(anchor.originLat(), anchor.originLng(), lat, lng) * 0.08, 60), MAX_BULGE_METERS);
        List<Map<String, Object>> track = new ArrayList<>(
                curve(anchor.originLat(), anchor.originLng(), lat, lng, 12, sign * bulge, true));
        track.addAll(curve(lat, lng, anchor.destLat(), anchor.destLng(), 12, -sign * bulge, false));
        result.put("trackPoints", track);

        // 违规点附近的禁飞/限飞/临时管制区
        result.put("zones", relatedZones(lat, lng));
        return result;
    }

    // ===================== 锚点解析 =====================

    private record Anchor(String source, String note,
                          String originName, double originLat, double originLng,
                          String destName, double destLat, double destLng) {
    }

    private Anchor resolveAnchor(Violation v, double vLat, double vLng) {
        String note = null;

        Order order = null;
        if (notBlank(v.getOrderNo())) {
            order = orderRepository.findByOrderNumber(v.getOrderNo()).orElse(null);
            if (order == null) {
                note = "关联订单 " + v.getOrderNo() + " 在库中不存在，";
            }
        }
        if (order == null && v.getAircraftId() != null) {
            Order candidate = orderRepository.findByAircraftId(v.getAircraftId()).stream()
                    .filter(o -> o.getPickupLatitude() != null && o.getPickupLongitude() != null
                            && o.getDeliveryLatitude() != null && o.getDeliveryLongitude() != null)
                    .max(Comparator.comparing(o -> o.getCreatedAt() == null ? LocalDateTime.MIN : o.getCreatedAt()))
                    .orElse(null);
            // 该订单航线得跟违规点相关（距航线 3km 内）才采用，否则画出来是一条无关的跨城绕行
            if (candidate != null) {
                double off = distanceToSegment(vLat, vLng,
                        candidate.getPickupLatitude(), candidate.getPickupLongitude(),
                        candidate.getDeliveryLatitude(), candidate.getDeliveryLongitude());
                if (off <= RELATED_ROUTE_TOLERANCE_METERS) {
                    order = candidate;
                    note = (note == null ? "" : note) + "改用该飞行器最近一次订单 " + order.getOrderNumber() + "，";
                } else {
                    note = (note == null ? "" : note) + "该飞行器最近一次订单（" + candidate.getOrderNumber() + "）距违规点约 "
                            + Math.round(off / 1000) + " km，与本次违规无关，";
                }
            }
        }
        if (order != null && order.getPickupLatitude() != null && order.getDeliveryLatitude() != null) {
            return new Anchor("ORDER_DERIVED",
                    (note == null ? "" : note) + "航迹由订单 " + order.getOrderNumber()
                            + " 的取件地 → 送达地结合违规点推演（库中无实测遥测数据）",
                    defaultValue(order.getPickupLocation(), "取件地"), order.getPickupLatitude(), order.getPickupLongitude(),
                    defaultValue(order.getDeliveryLocation(), "送达地"), order.getDeliveryLatitude(), order.getDeliveryLongitude());
        }

        Station nearest = nearestStation(vLat, vLng);
        if (nearest != null) {
            double d = distance(vLat, vLng, nearest.getLatitude(), nearest.getLongitude());
            return new Anchor("STATION_DERIVED",
                    (note == null ? "无可用关联订单，" : note) + "航迹由最近场站「" + nearest.getName() + "」（直线 "
                            + Math.round(d) + " m）→ 违规点推演（库中无实测遥测数据）",
                    nearest.getName(), nearest.getLatitude(), nearest.getLongitude(),
                    defaultValue(v.getLocation(), "违规点"), vLat, vLng);
        }

        // 兜底：向北约 2km 生成一个推演起点
        return new Anchor("GEOMETRY_DERIVED",
                "无可用订单与场站，航迹按违规点几何推演（库中无实测遥测数据）",
                "推演起点", vLat + 0.018, vLng,
                defaultValue(v.getLocation(), "违规点"), vLat, vLng);
    }

    private Station nearestStation(double lat, double lng) {
        return stationRepository.findAll().stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .filter(s -> !"closed".equalsIgnoreCase(s.getStatus()))
                .min(Comparator.comparingDouble(s -> distance(lat, lng, s.getLatitude(), s.getLongitude())))
                .orElse(null);
    }

    // ===================== 区域 =====================

    private List<Map<String, Object>> relatedZones(double lat, double lng) {
        List<AirspaceZone> all = airspaceZoneRepository.findAll().stream()
                .filter(z -> z.getLatitude() != null && z.getLongitude() != null && z.getRadius() != null)
                .filter(z -> !Boolean.FALSE.equals(z.getActive()))
                .toList();

        // 违规点落在区域内，或距边界 300m 以内
        List<AirspaceZone> hits = all.stream()
                .filter(z -> distance(lat, lng, z.getLatitude(), z.getLongitude()) - z.getRadius() <= ZONE_TOLERANCE_METERS)
                .sorted(Comparator.comparingDouble(z -> distance(lat, lng, z.getLatitude(), z.getLongitude())))
                .limit(5)
                .toList();

        // 没有任何命中时，给出最近的禁飞区作为背景参考
        List<AirspaceZone> used = hits.isEmpty()
                ? all.stream()
                    .sorted(Comparator.comparingDouble(z -> distance(lat, lng, z.getLatitude(), z.getLongitude())))
                    .limit(1)
                    .toList()
                : hits;

        List<Map<String, Object>> out = new ArrayList<>();
        for (AirspaceZone z : used) {
            double d = distance(lat, lng, z.getLatitude(), z.getLongitude());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", z.getId());
            item.put("name", z.getName());
            item.put("type", z.getType());
            item.put("latitude", z.getLatitude());
            item.put("longitude", z.getLongitude());
            item.put("radius", z.getRadius());
            item.put("description", z.getDescription());
            // 违规点是否落在该区域内
            item.put("contains", d <= z.getRadius());
            item.put("distanceToCenter", Math.round(d));
            out.add(item);
        }
        return out;
    }

    // ===================== 几何工具 =====================

    private List<Map<String, Object>> curve(double lat1, double lng1, double lat2, double lng2,
                                            int segments, double offsetMeters, boolean includeStart) {
        List<Map<String, Object>> pts = new ArrayList<>();
        double dLat = lat2 - lat1;
        double dLng = lng2 - lng1;
        double metersPerDegLng = METERS_PER_DEG_LAT * Math.cos(Math.toRadians((lat1 + lat2) / 2));
        if (metersPerDegLng == 0) metersPerDegLng = METERS_PER_DEG_LAT;

        // 米制平面上的位移向量与其法向量（用于施加横向弧度）
        double dx = dLng * metersPerDegLng;
        double dy = dLat * METERS_PER_DEG_LAT;
        double len = Math.hypot(dx, dy);
        double nx = len == 0 ? 0 : -dy / len;
        double ny = len == 0 ? 0 : dx / len;

        for (int i = 0; i <= segments; i++) {
            if (i == 0 && !includeStart) continue;
            double t = (double) i / segments;
            double bulge = Math.sin(Math.PI * t) * offsetMeters;
            double east = dx * t + nx * bulge;
            double north = dy * t + ny * bulge;
            double lat = lat1 + north / METERS_PER_DEG_LAT;
            double lng = lng1 + east / metersPerDegLng;
            pts.add(coord(round6(lat), round6(lng)));
        }
        return pts;
    }

    private Map<String, Object> point(String name, double lat, double lng, String tag) {
        Map<String, Object> p = coord(round6(lat), round6(lng));
        p.put("name", name);
        p.put("tag", tag);
        return p;
    }

    private Map<String, Object> coord(double lat, double lng) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("lat", lat);
        p.put("lng", lng);
        return p;
    }

    /** 两点间距离（米，Haversine） */
    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371008.8 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 点到线段的最短距离（米，小范围内用等距投影近似） */
    private double distanceToSegment(double pLat, double pLng,
                                     double aLat, double aLng, double bLat, double bLng) {
        double metersPerDegLng = METERS_PER_DEG_LAT * Math.cos(Math.toRadians(pLat));
        double px = pLng * metersPerDegLng;
        double py = pLat * METERS_PER_DEG_LAT;
        double ax = aLng * metersPerDegLng;
        double ay = aLat * METERS_PER_DEG_LAT;
        double bx = bLng * metersPerDegLng;
        double by = bLat * METERS_PER_DEG_LAT;

        double dx = bx - ax;
        double dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        double t = lenSq == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / lenSq;
        t = Math.max(0, Math.min(1, t));
        double cx = ax + t * dx;
        double cy = ay + t * dy;
        return Math.hypot(px - cx, py - cy);
    }

    private double round6(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String defaultValue(String value, String fallback) {
        return notBlank(value) ? value : fallback;
    }
}
