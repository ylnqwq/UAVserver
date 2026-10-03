package com.uav.uavserver.controller;

import com.uav.uavserver.domain.UserRole;
import com.uav.uavserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色目录接口。
 * 角色本身是后端枚举（不是数据库表），这里把「角色定义 + 当前用户数量」提供给前端，
 * 避免前端再维护一份写死的角色/权限清单。
 *
 *   GET /api/roles          角色列表（含每个角色的用户数）
 *   GET /api/roles/options  角色下拉选项（code + name）
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final UserService userService;

    private static final Map<UserRole, String> ROLE_NAMES = new LinkedHashMap<>();
    private static final Map<UserRole, String> ROLE_DESCRIPTIONS = new LinkedHashMap<>();

    static {
        ROLE_NAMES.put(UserRole.ADMIN, "系统管理员");
        ROLE_NAMES.put(UserRole.SUPERVISOR, "监管人员");
        ROLE_NAMES.put(UserRole.AIR_CONTROL, "空管人员");
        ROLE_NAMES.put(UserRole.OPERATOR, "运营人员");
        ROLE_NAMES.put(UserRole.PILOT, "飞行员");
        ROLE_NAMES.put(UserRole.MAINTENANCE, "维护人员");
        ROLE_NAMES.put(UserRole.CUSTOMER, "客户");

        ROLE_DESCRIPTIONS.put(UserRole.ADMIN, "拥有系统全部权限，负责账号与系统配置管理");
        ROLE_DESCRIPTIONS.put(UserRole.SUPERVISOR, "审批飞行计划、管理空域与违规处理");
        ROLE_DESCRIPTIONS.put(UserRole.AIR_CONTROL, "空域监控与飞行计划审批");
        ROLE_DESCRIPTIONS.put(UserRole.OPERATOR, "飞行器与场站资源日常运营");
        ROLE_DESCRIPTIONS.put(UserRole.PILOT, "创建并执行飞行计划");
        ROLE_DESCRIPTIONS.put(UserRole.MAINTENANCE, "负责飞行器维护与资源状态查看");
        ROLE_DESCRIPTIONS.put(UserRole.CUSTOMER, "下单与查询订单进度");
    }

    /** 权限目录（模块 → 操作），由后端统一定义，前端只负责渲染 */
    private static final List<Map<String, Object>> PERMISSION_TREE = buildPermissionTree();

    /** 各角色内置权限（权限由后端角色定义，前端不可改） */
    private static final Map<UserRole, List<String>> ROLE_PERMISSIONS = buildRolePermissions();

    private static List<Map<String, Object>> buildPermissionTree() {
        List<Map<String, Object>> tree = new ArrayList<>();
        tree.add(module("aircraft", "飞行器管理", "aircraft", "view", "create", "edit", "delete", "export"));
        tree.add(module("airspace", "空域管理", "airspace", "view", "create", "edit", "delete"));
        tree.add(module("flightPlan", "飞行计划", "flightPlan", "view", "create", "edit", "approve", "cancel"));
        tree.add(module("station", "场站管理", "station", "view", "create", "edit", "delete"));
        tree.add(module("order", "订单管理", "order", "view", "create", "dispatch", "track"));
        tree.add(module("statistics", "数据统计", "statistics", "view", "export"));
        tree.add(module("system", "系统管理", "system", "user", "role"));
        return tree;
    }

    private static Map<String, String> actionNameMap() {
        return Map.ofEntries(
                Map.entry("view", "查看"),
                Map.entry("create", "新增"),
                Map.entry("edit", "编辑"),
                Map.entry("delete", "删除"),
                Map.entry("export", "导出"),
                Map.entry("approve", "审批"),
                Map.entry("cancel", "取消"),
                Map.entry("dispatch", "调度"),
                Map.entry("track", "跟踪"),
                Map.entry("user", "用户管理"),
                Map.entry("role", "角色权限"));
    }

    private static Map<String, Object> module(String key, String label, String prefix, String... actions) {
        Map<String, String> actionNames = actionNameMap();
        List<Map<String, Object>> children = new ArrayList<>();
        for (String action : actions) {
            Map<String, Object> leaf = new LinkedHashMap<>();
            leaf.put("id", prefix + ":" + action);
            leaf.put("label", actionNames.getOrDefault(action, action));
            children.add(leaf);
        }
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", key);
        node.put("label", label);
        node.put("children", children);
        return node;
    }

    private static Map<UserRole, List<String>> buildRolePermissions() {
        Map<UserRole, List<String>> map = new LinkedHashMap<>();
        List<String> all = new ArrayList<>();
        for (Map<String, Object> node : PERMISSION_TREE.isEmpty() ? buildPermissionTree() : PERMISSION_TREE) {
            for (Map<String, Object> leaf : (List<Map<String, Object>>) node.get("children")) {
                all.add((String) leaf.get("id"));
            }
        }
        map.put(UserRole.ADMIN, all);
        map.put(UserRole.SUPERVISOR, List.of("aircraft:view", "airspace:view", "airspace:edit", "flightPlan:view",
                "flightPlan:approve", "station:view", "statistics:view", "statistics:export"));
        map.put(UserRole.AIR_CONTROL, List.of("aircraft:view", "airspace:view", "airspace:create", "airspace:edit",
                "flightPlan:view", "flightPlan:approve", "station:view"));
        map.put(UserRole.OPERATOR, List.of("aircraft:view", "aircraft:create", "aircraft:edit", "station:view",
                "station:edit", "flightPlan:view", "order:view", "order:accept", "order:dispatch", "order:track",
                "statistics:view"));
        map.put(UserRole.PILOT, List.of("aircraft:view", "flightPlan:view", "flightPlan:create", "flightPlan:edit", "flightPlan:cancel"));
        map.put(UserRole.MAINTENANCE, List.of("aircraft:view", "station:view"));
        map.put(UserRole.CUSTOMER, List.of("order:view", "order:create", "order:track"));
        return map;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        Map<UserRole, Long> counts = new LinkedHashMap<>();
        userService.countUsersByRole().forEach(c -> counts.put(c.role(), c.count()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserRole role : UserRole.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", role.name());
            item.put("name", ROLE_NAMES.getOrDefault(role, role.name()));
            item.put("description", ROLE_DESCRIPTIONS.getOrDefault(role, ""));
            item.put("userCount", counts.getOrDefault(role, 0L));
            item.put("builtIn", true);
            item.put("permissions", ROLE_PERMISSIONS.getOrDefault(role, List.of()));
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    /** 权限目录树（前端「角色权限」页面渲染用） */
    @GetMapping("/permissions")
    public ResponseEntity<List<Map<String, Object>>> getPermissionTree() {
        return ResponseEntity.ok(PERMISSION_TREE);
    }

    @GetMapping("/options")
    public ResponseEntity<List<Map<String, String>>> getRoleOptions() {
        List<Map<String, String>> result = new ArrayList<>();
        for (UserRole role : UserRole.values()) {
            result.add(Map.of("code", role.name(), "name", ROLE_NAMES.getOrDefault(role, role.name())));
        }
        return ResponseEntity.ok(result);
    }
}
