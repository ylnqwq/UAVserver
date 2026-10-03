package com.uav.uavserver.service;

import com.uav.uavserver.domain.Aircraft;
import com.uav.uavserver.domain.AircraftStatus;
import com.uav.uavserver.domain.AirspaceZone;
import com.uav.uavserver.domain.Order;
import com.uav.uavserver.dto.AircraftDto;
import com.uav.uavserver.repository.AircraftRepository;
import com.uav.uavserver.repository.AirspaceZoneRepository;
import com.uav.uavserver.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AircraftService {

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AirspaceZoneRepository airspaceZoneRepository;

    /** 圆形围栏：圆心 + 半径(km) */
    private record Zone(double lat, double lng, double radiusKm, String name, String type) {
    }

    /** 绕行后的航线：折线点 + 被绕开的围栏名称 */
    private record RoutePath(List<double[]> points, List<String> avoidedZones) {
    }

    /** 当前位置在航线上的投影：投影点 + 沿航线到该点的里程 */
    private record Projection(double[] point, double distance) {
    }

    /** 航线与围栏边界的安全间隔（km） */
    private static final double ROUTE_MARGIN_KM = 0.15;
    /** 绕行圆弧上插入的点数 */
    private static final int ARC_POINTS = 4;

    /** 视为「执行中」的订单状态：只有正在执行的任务才显示航线（停机的飞行器不画历史线） */
    private static final List<String> ACTIVE_ORDER_STATUS = List.of("IN_PROGRESS");

    @Transactional
    public AircraftDto.Response createAircraft(AircraftDto.CreateRequest request) {
        if (aircraftRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new RuntimeException("注册编号已存在");
        }

        Aircraft aircraft = new Aircraft();
        aircraft.setRegistrationNumber(request.getRegistrationNumber());
        aircraft.setName(request.getName());
        aircraft.setModel(request.getModel());
        aircraft.setManufacturer(request.getManufacturer());
        aircraft.setType(request.getType());
        aircraft.setSerialNumber(request.getSerialNumber());
        aircraft.setMaxTakeoffWeight(request.getMaxTakeoffWeight());
        aircraft.setMaxSpeed(request.getMaxSpeed());
        aircraft.setMaxFlightTime(request.getMaxFlightTime());
        aircraft.setMaxFlightDistance(request.getMaxFlightDistance());
        aircraft.setMaxAltitude(request.getMaxAltitude());
        aircraft.setOwner(request.getOwner());
        aircraft.setOperator(request.getOperator());
        aircraft.setDescription(request.getDescription());
        aircraft.setStatus(AircraftStatus.AVAILABLE);

        aircraft = aircraftRepository.save(aircraft);
        return toResponse(aircraft);
    }

    @Transactional
    public AircraftDto.Response updateAircraft(Long id, AircraftDto.UpdateRequest request) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("飞行器不存在"));

        if (request.getName() != null) aircraft.setName(request.getName());
        if (request.getModel() != null) aircraft.setModel(request.getModel());
        if (request.getManufacturer() != null) aircraft.setManufacturer(request.getManufacturer());
        if (request.getType() != null) aircraft.setType(request.getType());
        if (request.getMaxTakeoffWeight() != null) aircraft.setMaxTakeoffWeight(request.getMaxTakeoffWeight());
        if (request.getMaxSpeed() != null) aircraft.setMaxSpeed(request.getMaxSpeed());
        if (request.getMaxFlightTime() != null) aircraft.setMaxFlightTime(request.getMaxFlightTime());
        if (request.getMaxFlightDistance() != null) aircraft.setMaxFlightDistance(request.getMaxFlightDistance());
        if (request.getMaxAltitude() != null) aircraft.setMaxAltitude(request.getMaxAltitude());
        if (request.getStatus() != null) aircraft.setStatus(request.getStatus());
        if (request.getOwner() != null) aircraft.setOwner(request.getOwner());
        if (request.getOperator() != null) aircraft.setOperator(request.getOperator());
        if (request.getDescription() != null) aircraft.setDescription(request.getDescription());

        aircraft = aircraftRepository.save(aircraft);
        return toResponse(aircraft);
    }

    public AircraftDto.Response getAircraft(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("飞行器不存在"));
        return toResponse(aircraft);
    }

    public List<AircraftDto.Response> getAllAircraft() {
        return aircraftRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AircraftDto.Response> getAircraftByStatus(AircraftStatus status) {
        return aircraftRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 地图用：返回所有带经纬度的飞行器实时数据（字段名与前端 AirspaceMap.vue 约定一致）。
     * 「飞行中」的飞行器会把坐标对齐到它航线上对应的位置，保证地图上飞行器正好落在航线上。
     */
    public List<Map<String, Object>> getFlyingAircraft() {
        return aircraftRepository.findAll().stream()
                .filter(a -> a.getLatitude() != null && a.getLongitude() != null)
                .map(a -> {
                    Double lat = a.getLatitude();
                    Double lng = a.getLongitude();
                    if (a.getStatus() == AircraftStatus.IN_FLIGHT) {
                        try {
                            Map<String, Object> route = getAircraftRoute(a.getId());
                            if (Boolean.TRUE.equals(route.get("hasRoute"))
                                    && !Boolean.TRUE.equals(route.get("history"))
                                    && route.get("splitLat") != null && route.get("splitLng") != null) {
                                lat = (Double) route.get("splitLat");
                                lng = (Double) route.get("splitLng");
                            }
                        } catch (Exception ignored) {
                            // 取航线失败时退回飞行器自身坐标
                        }
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", a.getId());
                    data.put("deviceNo", a.getRegistrationNumber());
                    data.put("name", a.getName());
                    data.put("lat", lat);
                    data.put("lng", lng);
                    data.put("altitude", a.getCurrentAltitude() != null ? a.getCurrentAltitude() : 0);
                    data.put("speed", a.getCurrentSpeed() != null ? a.getCurrentSpeed() : 0);
                    data.put("status", a.getFlightStatus() != null ? a.getFlightStatus() : "grounded");
                    data.put("battery", a.getBatteryLevel() != null ? a.getBatteryLevel() : 100);
                    return data;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    /**
     * 飞行器航线：优先取执行中（已接单/已调度/配送中）的订单，
     * 没有则回退到最近一条已完成订单作为历史航线。
     * waypoints 顺序：起飞地 → 当前位置 → 目的地
     */
    public Map<String, Object> getAircraftRoute(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("飞行器不存在"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aircraftId", aircraft.getId());
        result.put("deviceNo", aircraft.getRegistrationNumber());
        result.put("flightStatus", aircraft.getFlightStatus());
        result.put("altitude", aircraft.getCurrentAltitude());
        result.put("speed", aircraft.getCurrentSpeed());

        List<Order> orders = orderRepository.findByAircraftId(id);
        Comparator<Order> byId = Comparator.comparing(Order::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
        Order target = orders.stream()
                .filter(o -> ACTIVE_ORDER_STATUS.contains(o.getStatus()))
                .max(byId)
                .orElse(null);
        boolean history = false;
        if (target == null) {
            result.put("hasRoute", false);
            result.put("message", "该飞行器当前没有执行中的航线");
            return result;
        }

        Double depLat = target.getPickupLatitude();
        Double depLng = target.getPickupLongitude();
        Double arrLat = target.getDeliveryLatitude();
        Double arrLng = target.getDeliveryLongitude();
        Double curLat = aircraft.getLatitude();
        Double curLng = aircraft.getLongitude();
        boolean hasCurrent = curLat != null && curLng != null;

        // 绕开所有圆形围栏（禁飞/限飞/临时管制）生成航线折线，再按当前位置投影得到已飞/剩余里程
        List<Zone> zones = airspaceZoneRepository.findByActiveTrue().stream()
                .filter(z -> z.getLatitude() != null && z.getLongitude() != null && z.getRadius() != null)
                .map(z -> new Zone(z.getLatitude(), z.getLongitude(), z.getRadius() / 1000.0, z.getName(), z.getType()))
                .collect(Collectors.toList());
        RoutePath routePath = buildAvoidancePath(depLat, depLng, arrLat, arrLng, zones);
        List<double[]> pts = routePath.points();
        double total = pathLengthKm(pts);
        double[] split = pts.get(pts.size() - 1);
        double flown = total;
        if (hasCurrent && !history && total > 0) {
            Projection proj = projectOnPath(pts, curLat, curLng);
            flown = Math.min(proj.distance(), total);
            split = proj.point();
        }
        if (!history && hasCurrent && total > 0 && (flown <= 0.02 || flown >= total - 0.02)) {
            // 飞行器偏离航线较远时按航程中点展示，避免执行中出现 0%/100% 的怪进度
            flown = total * 0.5;
            split = pointAtDistance(pts, flown);
        }
        double remain = Math.max(0, total - flown);
        int progress = history ? 100
                : (total > 0 ? (int) Math.max(0, Math.min(100, Math.round(flown / total * 100))) : 0);
        double speed = aircraft.getCurrentSpeed() != null && aircraft.getCurrentSpeed() > 0 ? aircraft.getCurrentSpeed() : 15;
        int eta = (int) Math.round(remain * 1000 / speed / 60.0);

        result.put("hasRoute", true);
        result.put("history", history);
        result.put("orderNo", target.getOrderNumber());
        result.put("orderStatus", target.getStatus());
        result.put("serviceType", target.getServiceType());
        result.put("departureName", target.getPickupLocation());
        result.put("departureLat", depLat);
        result.put("departureLng", depLng);
        result.put("arrivalName", target.getDeliveryLocation());
        result.put("arrivalLat", arrLat);
        result.put("arrivalLng", arrLng);
        result.put("currentLat", curLat);
        result.put("currentLng", curLng);
        result.put("routeAltitude", target.getRouteAltitude());
        result.put("totalDistance", round1(total));
        result.put("flownDistance", round1(flown));
        result.put("remainDistance", round1(remain));
        result.put("progress", progress);
        result.put("etaMinutes", eta);

        List<Map<String, Object>> waypoints = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            String type = i == 0 ? "pickup" : (i == pts.size() - 1 ? "arrival" : "waypoint");
            String name = i == 0 ? target.getPickupLocation()
                    : (i == pts.size() - 1 ? target.getDeliveryLocation() : "绕行点" + i);
            waypoints.add(waypoint(pts.get(i)[0], pts.get(i)[1], name, type, true));
        }
        result.put("waypoints", waypoints);
        result.put("splitLat", split[0]);
        result.put("splitLng", split[1]);
        result.put("avoidedZones", routePath.avoidedZones());
        result.put("avoidedCount", routePath.avoidedZones().size());
        return result;
    }

    // ==================== 航线避让围栏 ====================

    /**
     * 航线避让规划：在起终点外扩的网格上用 A* 搜索一条不进入任何围栏（半径+安全间隔）的路径，
     * 再做视线简化，得到尽量短且不穿越的折线。
     */
    private static RoutePath buildAvoidancePath(double sLat, double sLng, double eLat, double eLng, List<Zone> zones) {
        List<String> avoided = new ArrayList<>();
        for (Zone z : zones) {
            double[] cl = closestOnSegment(z.lat(), z.lng(), sLat, sLng, eLat, eLng);
            if (cl[1] < z.radiusKm() + ROUTE_MARGIN_KM) {
                avoided.add(z.name());
            }
        }

        double maxR = 1.0;
        for (Zone z : zones) {
            maxR = Math.max(maxR, z.radiusKm());
        }
        double pad = maxR + 3.0;
        double minLat = Math.min(sLat, eLat) - pad / 111.0;
        double maxLat = Math.max(sLat, eLat) + pad / 111.0;
        double cosLat = Math.cos(Math.toRadians((sLat + eLat) / 2));
        double minLng = Math.min(sLng, eLng) - pad / (111.0 * cosLat);
        double maxLng = Math.max(sLng, eLng) + pad / (111.0 * cosLat);

        double spanKm = Math.max((maxLat - minLat) * 111.0, (maxLng - minLng) * 111.0 * cosLat);
        double cell = Math.max(0.25, spanKm / 200.0);
        int rows = (int) Math.floor((maxLat - minLat) * 111.0 / cell) + 1;
        int cols = (int) Math.floor((maxLng - minLng) * 111.0 * cosLat / cell) + 1;
        if (rows < 3 || cols < 3) {
            List<double[]> straight = new ArrayList<>();
            straight.add(new double[]{sLat, sLng});
            straight.add(new double[]{eLat, eLng});
            return new RoutePath(straight, avoided);
        }

        boolean[] blocked = new boolean[rows * cols];
        for (Zone z : zones) {
            double rKm = z.radiusKm() + ROUTE_MARGIN_KM;
            int r0 = (int) Math.floor((z.lat() - rKm - minLat) * 111.0 / cell);
            int r1 = (int) Math.ceil((z.lat() + rKm - minLat) * 111.0 / cell);
            int c0 = (int) Math.floor((z.lng() - rKm / cosLat - minLng) * 111.0 * cosLat / cell);
            int c1 = (int) Math.ceil((z.lng() + rKm / cosLat - minLng) * 111.0 * cosLat / cell);
            for (int r = Math.max(0, r0); r <= Math.min(rows - 1, r1); r++) {
                double lat = minLat + r * cell / 111.0;
                for (int c = Math.max(0, c0); c <= Math.min(cols - 1, c1); c++) {
                    double lng = minLng + c * cell / (111.0 * cosLat);
                    if (distanceKm(lat, lng, z.lat(), z.lng()) < rKm) {
                        blocked[r * cols + c] = true;
                    }
                }
            }
        }

        int start = gridIndex(sLat, sLng, minLat, minLng, cell, cosLat, cols, rows);
        int goal = gridIndex(eLat, eLng, minLat, minLng, cell, cosLat, cols, rows);
        blocked[start] = false;
        blocked[goal] = false;

        double[] gScore = new double[rows * cols];
        double[] fScore = new double[rows * cols];
        int[] prev = new int[rows * cols];
        boolean[] closed = new boolean[rows * cols];
        java.util.Arrays.fill(gScore, Double.MAX_VALUE);
        java.util.Arrays.fill(fScore, Double.MAX_VALUE);
        java.util.Arrays.fill(prev, -1);

        final double[] fRef = fScore;
        PriorityQueue<Integer> open = new PriorityQueue<>(Comparator.comparingDouble(i -> fRef[i]));
        gScore[start] = 0;
        fScore[start] = gridHeuristic(start, goal, cols, cell);
        open.add(start);
        boolean found = false;

        while (!open.isEmpty()) {
            int cur = open.poll();
            if (cur == goal) {
                found = true;
                break;
            }
            if (closed[cur]) {
                continue;
            }
            closed[cur] = true;
            int cr = cur / cols;
            int cc = cur % cols;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) {
                        continue;
                    }
                    int nr = cr + dr;
                    int nc = cc + dc;
                    if (nr < 0 || nc < 0 || nr >= rows || nc >= cols) {
                        continue;
                    }
                    int ni = nr * cols + nc;
                    if (blocked[ni] || closed[ni]) {
                        continue;
                    }
                    double step = (dr != 0 && dc != 0) ? cell * 1.41421356 : cell;
                    double ng = gScore[cur] + step;
                    if (ng < gScore[ni]) {
                        gScore[ni] = ng;
                        fScore[ni] = ng + gridHeuristic(ni, goal, cols, cell);
                        prev[ni] = cur;
                        open.add(ni);
                    }
                }
            }
        }

        List<double[]> raw = new ArrayList<>();
        raw.add(new double[]{sLat, sLng});
        if (found) {
            LinkedList<double[]> rev = new LinkedList<>();
            for (int cur = goal; cur != -1; cur = prev[cur]) {
                rev.addFirst(new double[]{
                        minLat + (cur / cols) * cell / 111.0,
                        minLng + (cur % cols) * cell / (111.0 * cosLat)});
                if (cur == start) {
                    break;
                }
            }
            raw.addAll(rev);
        }
        raw.add(new double[]{eLat, eLng});
        return new RoutePath(applyNaturalOffset(simplifyPath(raw, zones), zones), avoided);
    }

    /**
     * 真实航迹不会是笔直的：对每段直线插入轻微横向偏移点（幅值≤总长约 5%、最大 400m），
     * 交替左右形成自然折线；若偏移点会进入围栏则取消该点偏移。
     */
    private static List<double[]> applyNaturalOffset(List<double[]> path, List<Zone> zones) {
        List<double[]> out = new ArrayList<>();
        out.add(path.get(0));
        // 由起终点派生稳定随机种子，保证同一条航线每次计算结果一致
        double[] s0 = path.get(0);
        double[] s1 = path.get(path.size() - 1);
        long rndSeed = Math.round(s0[0] * 100000) * 1000003L + Math.round(s0[1] * 100000) * 1013904223L
                + Math.round(s1[0] * 100000) * 22695477L + Math.round(s1[1] * 100000);
        double walk = 0;
        for (int i = 0; i + 1 < path.size(); i++) {
            double[] a = path.get(i);
            double[] b = path.get(i + 1);
            double seg = distanceKm(a[0], a[1], b[0], b[1]);
            if (seg < 1.5) {
                out.add(b);
                continue;
            }
            int parts = Math.max(2, Math.min(20, (int) Math.round(seg / 0.8)));
            double cosLat = Math.cos(Math.toRadians((a[0] + b[0]) / 2));
            double dx = (b[1] - a[1]) * cosLat;
            double dy = b[0] - a[0];
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-9) {
                out.add(b);
                continue;
            }
            double px = -dy / len;
            double py = dx / len;
            double amp = Math.min(0.3, seg * 0.03);
            List<double[]> cand = new ArrayList<>();
            for (int k = 1; k <= parts; k++) {
                double t = (double) k / (parts + 1);
                double lat = a[0] + (b[0] - a[0]) * t;
                double lng = a[1] + (b[1] - a[1]) * t;
                // 平滑随机游走：方向与幅度都在变，避免固定往复的锯齿
                rndSeed = rndSeed * 6364136223846793005L + 1442695040888963407L;
                double rr = ((rndSeed >>> 11) & 0x1FFFFF) / (double) 0x1FFFFF;
                walk = walk * 0.55 + (rr - 0.5) * 1.7;
                double norm = Math.max(-1.0, Math.min(1.0, walk));
                double signed = amp * norm;
                double latOff = lat + (py * signed) / 111.0;
                double lngOff = lng + (px * signed) / (111.0 * cosLat);
                if (insideAnyZone(latOff, lngOff, zones)) {
                    cand.add(new double[]{lat, lng});
                } else {
                    cand.add(new double[]{latOff, lngOff});
                }
            }
            // 偏移后的每一小段都不能进入围栏，否则本段整体取消偏移（改用中点）
            boolean ok = true;
            double[] prev = a;
            for (double[] p : cand) {
                if (!segmentClear(prev, p, zones)) {
                    ok = false;
                    break;
                }
                prev = p;
            }
            if (ok && !segmentClear(prev, b, zones)) {
                ok = false;
            }
            if (!ok) {
                cand.clear();
                for (int k = 1; k <= parts; k++) {
                    double t = (double) k / (parts + 1);
                    cand.add(new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
                }
            }
            out.addAll(cand);
            out.add(b);
        }
        return out;
    }

    private static boolean insideAnyZone(double lat, double lng, List<Zone> zones) {
        for (Zone z : zones) {
            if (distanceKm(lat, lng, z.lat(), z.lng()) < z.radiusKm() + 0.05) {
                return true;
            }
        }
        return false;
    }

    private static int gridIndex(double lat, double lng, double minLat, double minLng, double cell,
                                 double cosLat, int cols, int rows) {
        int r = (int) Math.round((lat - minLat) * 111.0 / cell);
        int c = (int) Math.round((lng - minLng) * 111.0 * cosLat / cell);
        r = Math.max(0, Math.min(rows - 1, r));
        c = Math.max(0, Math.min(cols - 1, c));
        return r * cols + c;
    }

    private static double gridHeuristic(int idx, int goal, int cols, double cell) {
        double dr = idx / cols - goal / cols;
        double dc = idx % cols - goal % cols;
        return Math.sqrt(dr * dr + dc * dc) * cell;
    }

    /** 视线简化：在不进入围栏（留 50m 余量）的前提下尽量拉直航线 */
    private static List<double[]> simplifyPath(List<double[]> path, List<Zone> zones) {
        List<double[]> out = new ArrayList<>();
        out.add(path.get(0));
        int i = 0;
        while (i < path.size() - 1) {
            int j = path.size() - 1;
            for (; j > i + 1; j--) {
                if (segmentClear(path.get(i), path.get(j), zones)) {
                    break;
                }
            }
            out.add(path.get(j));
            i = j;
        }
        return out;
    }

    private static boolean segmentClear(double[] a, double[] b, List<Zone> zones) {
        for (Zone z : zones) {
            if (closestOnSegment(z.lat(), z.lng(), a[0], a[1], b[0], b[1])[1] < z.radiusKm() + 0.05) {
                return false;
            }
        }
        return true;
    }

    /** 点到线段最近点的参数 t 与距离(km) */
    private static double[] closestOnSegment(double pLat, double pLng, double aLat, double aLng, double bLat, double bLng) {
        double cosLat = Math.cos(Math.toRadians((aLat + bLat) / 2));
        double px = (pLng - aLng) * cosLat;
        double py = pLat - aLat;
        double bx = (bLng - aLng) * cosLat;
        double by = bLat - aLat;
        double len2 = bx * bx + by * by;
        double t = len2 < 1e-12 ? 0 : Math.max(0, Math.min(1, (px * bx + py * by) / len2));
        double cx = aLng + (bLng - aLng) * t;
        double cy = aLat + (bLat - aLat) * t;
        return new double[]{t, distanceKm(pLat, pLng, cy, cx)};
    }

    private static double pathLengthKm(List<double[]> pts) {
        double sum = 0;
        for (int i = 0; i + 1 < pts.size(); i++) {
            sum += distanceKm(pts.get(i)[0], pts.get(i)[1], pts.get(i + 1)[0], pts.get(i + 1)[1]);
        }
        return sum;
    }

    /** 把当前位置投影到航线上：得到投影点与沿航线到该点的里程 */
    private static Projection projectOnPath(List<double[]> pts, double lat, double lng) {
        double bestDist = Double.MAX_VALUE;
        double bestAlong = 0;
        double[] bestPoint = pts.get(0);
        double along = 0;
        for (int i = 0; i + 1 < pts.size(); i++) {
            double[] a = pts.get(i);
            double[] b = pts.get(i + 1);
            double[] closest = closestOnSegment(lat, lng, a[0], a[1], b[0], b[1]);
            double segLen = distanceKm(a[0], a[1], b[0], b[1]);
            if (closest[1] < bestDist) {
                bestDist = closest[1];
                bestAlong = along + segLen * closest[0];
                bestPoint = new double[]{a[0] + (b[0] - a[0]) * closest[0], a[1] + (b[1] - a[1]) * closest[0]};
            }
            along += segLen;
        }
        return new Projection(bestPoint, bestAlong);
    }

    /** 航线上距起点指定里程处的点 */
    private static double[] pointAtDistance(List<double[]> pts, double targetKm) {
        double acc = 0;
        for (int i = 0; i + 1 < pts.size(); i++) {
            double seg = distanceKm(pts.get(i)[0], pts.get(i)[1], pts.get(i + 1)[0], pts.get(i + 1)[1]);
            if (seg > 1e-9 && acc + seg >= targetKm) {
                double t = (targetKm - acc) / seg;
                return new double[]{
                        pts.get(i)[0] + (pts.get(i + 1)[0] - pts.get(i)[0]) * t,
                        pts.get(i)[1] + (pts.get(i + 1)[1] - pts.get(i)[1]) * t};
            }
            acc += seg;
        }
        return pts.get(pts.size() - 1);
    }

    private static Map<String, Object> waypoint(Double lat, Double lng, String name, String type, boolean passed) {
        Map<String, Object> wp = new LinkedHashMap<>();
        wp.put("lat", lat);
        wp.put("lng", lng);
        wp.put("name", name);
        wp.put("type", type);
        wp.put("passed", passed);
        return wp;
    }

    /** Haversine 距离（公里） */
    private static double distanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return 0;
        }
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    @Transactional
    public void deleteAircraft(Long id) {
        if (!aircraftRepository.existsById(id)) {
            throw new RuntimeException("飞行器不存在");
        }
        aircraftRepository.deleteById(id);
    }

    private AircraftDto.Response toResponse(Aircraft aircraft) {
        AircraftDto.Response response = new AircraftDto.Response();
        response.setId(aircraft.getId());
        response.setRegistrationNumber(aircraft.getRegistrationNumber());
        response.setName(aircraft.getName());
        response.setModel(aircraft.getModel());
        response.setManufacturer(aircraft.getManufacturer());
        response.setType(aircraft.getType());
        response.setSerialNumber(aircraft.getSerialNumber());
        response.setMaxTakeoffWeight(aircraft.getMaxTakeoffWeight());
        response.setMaxSpeed(aircraft.getMaxSpeed());
        response.setMaxFlightTime(aircraft.getMaxFlightTime());
        response.setMaxFlightDistance(aircraft.getMaxFlightDistance());
        response.setMaxAltitude(aircraft.getMaxAltitude());
        response.setStatus(aircraft.getStatus());
        response.setOwner(aircraft.getOwner());
        response.setOperator(aircraft.getOperator());
        response.setDescription(aircraft.getDescription());
        response.setLatitude(aircraft.getLatitude());
        response.setLongitude(aircraft.getLongitude());
        response.setCurrentAltitude(aircraft.getCurrentAltitude());
        response.setCurrentSpeed(aircraft.getCurrentSpeed());
        response.setFlightStatus(aircraft.getFlightStatus());
        response.setBatteryLevel(aircraft.getBatteryLevel());
        response.setLastMaintenanceDate(aircraft.getLastMaintenanceDate());
        response.setNextMaintenanceDate(aircraft.getNextMaintenanceDate());
        response.setCreatedAt(aircraft.getCreatedAt());
        response.setUpdatedAt(aircraft.getUpdatedAt());
        return response;
    }
}
