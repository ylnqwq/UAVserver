package com.uav.uavserver.domain;

/**
 * 用户角色。
 * 注意：这里的枚举值必须与前端（router/menu 的 roles 配置、stores/user.js）以及
 * init_database.sql 中的 users.role 取值保持一致，否则 JPA 读取用户记录时会抛出
 * "No enum constant ..." 异常（数据库里的字符串必须能映射成下面的常量）。
 */
public enum UserRole {
    ADMIN,           // 管理员
    SUPERVISOR,      // 监管人员
    AIR_CONTROL,     // 空管人员
    OPERATOR,        // 运营人员
    PILOT,           // 机长/飞行员
    MAINTENANCE,     // 维护人员
    CUSTOMER         // 客户（下单方）
}
