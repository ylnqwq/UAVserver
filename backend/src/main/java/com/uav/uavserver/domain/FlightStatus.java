package com.uav.uavserver.domain;

public enum FlightStatus {
    SCHEDULED,          // 已排期
    PREFLIGHT_CHECK,    // 飞行前检查
    READY,              // 准备就绪
    IN_FLIGHT,          // 飞行中
    COMPLETED,          // 已完成
    CANCELLED,          // 已取消
    ABORTED,            // 中止
    EMERGENCY           // 紧急情况
}
