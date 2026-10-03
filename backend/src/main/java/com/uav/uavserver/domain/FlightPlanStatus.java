package com.uav.uavserver.domain;

public enum FlightPlanStatus {
    DRAFT,              // 草稿
    SUBMITTED,          // 已提交
    UNDER_REVIEW,       // 审核中
    APPROVED,           // 已批准
    REJECTED,           // 已拒绝
    CANCELLED,          // 已取消
    COMPLETED           // 已完成
}
