# 天枢·低空智能运营管控平台（UAVserver）

面向城市级 / 区域级低空资源综合管理与服务的运营管控平台，覆盖低空飞行「申请 — 审批 — 调度 — 监控 — 归档」全生命周期。

> 本文档按**代码实际状态**维护（最后核对：2026-09-17，前后端均已实机启动并完成登录联调）。
> 凡与本文档不符的运行现象，以代码与 `application.properties` 为准，并请顺手更新本文档。

---

## 一、当前状态速览

| 项目 | 实际情况 |
|---|---|
| 后端 | Spring Boot 3.2.0，端口 **8082**，启动约 4 秒，已连通 MySQL `uavserver` |
| 前端 | Vue 3 + Vite 5，端口 **5173**，`/api` 代理到 `http://localhost:8082` |
| 认证 | Spring Security + JWT（`jjwt 0.11.5`），登录接口 `POST /api/auth/login` 实测通过 |
| 数据库 | MySQL 8，**表结构由 JPA 实体自动生成**（`ddl-auto=update`），无 SQL 建表脚本参与运行 |
| 已接入路由的页面 | 18 个（含登录页共 19 个） |
| 已开发但未接入路由的页面 | 16 个（见 §六） |
| 后端规模 | 12 个 Controller、11 个 Service、11 个 Repository、11 个实体、8 个枚举 |

---

## 二、技术栈（实际依赖，以 `pom.xml` / `package.json` 为准）

**后端**（Spring Boot 3.2.0，Java 17 编译目标，实测 JDK 21 可运行）

| 依赖 | 用途 |
|---|---|
| spring-boot-starter-web | REST 接口 |
| spring-boot-starter-data-jpa（Hibernate 6.3） | ORM 与建表（`ddl-auto=update`） |
| spring-boot-starter-security + jjwt 0.11.5 | 认证授权、JWT 签发校验 |
| spring-boot-starter-validation | 参数校验 |
| mysql-connector-j | MySQL 驱动 |
| lombok / mapstruct | 简化实体与 DTO 映射 |
| poi-ooxml 5.2.5 / commons-csv / pdfbox 2.0.30 | Excel / CSV / PDF 导出能力 |
| h2（test 作用域） | 单元测试 |

**前端**

| 依赖 | 用途 |
|---|---|
| vue 3.4 / vue-router 4 / pinia 2 | 页面、路由、状态管理 |
| element-plus 2.5 + @element-plus/icons-vue | UI 组件库 |
| echarts 5.4 | 数据大屏与趋势图 |
| leaflet 1.9 | 二维空域地图、航线绘制 |
| axios + `src/utils/request.js` | HTTP 封装（统一注入 JWT、错误提示） |
| vite 5 | 构建与开发服务器 |

**尚未引入（勿在文档或报告中误写）**：MyBatis / MyBatis-Plus、Redis、Flyway、Swagger / springdoc、Three.js 三维地图。

---

## 三、目录结构

```
UAVserver/
├── backend/                              # 后端（Maven 单模块）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/uav/uavserver/
│       │   ├── UAVServerApplication.java
│       │   ├── config/                   # SecurityConfig / JwtTokenProvider /
│       │   │                             # JwtAuthenticationFilter / DataInitializer
│       │   ├── controller/               # 12 个 REST 控制器
│       │   ├── service/                  # 业务逻辑（含 UserDetailsServiceImpl）
│       │   ├── repository/               # Spring Data JPA 接口（11 个）
│       │   ├── domain/                   # 实体 + 枚举（11 实体 / 8 枚举）
│       │   ├── dto/                      # Aircraft/Airspace/Auth/FlightPlan/
│       │   │                             # FlightRecord/Order/Statistics/User DTO
│       │   └── exception/                # GlobalExceptionHandler
│       └── resources/application.properties
│
├── front/                                # 前端（Vite 工程）
│   ├── vite.config.js                    # 端口 5173，/api → 8082
│   └── src/
│       ├── api/index.js                  # 全部接口定义（按模块分组）
│       ├── router/index.js               # 路由 + 登录/角色守卫
│       ├── stores/user.js                # 用户态与角色
│       ├── layout/MainLayout.vue          # 侧边菜单 + 顶栏布局
│       ├── utils/request.js              # axios 实例
│       └── views/                        # 见 §六 页面清单
│
├── db/
│   └── gen-seed.mjs                      # 演示数据生成器（见 §十 注意事项）
│
├── logs/                                 # 运行日志（后端启动输出等）
└── README.md
```

---

## 四、快速开始

### 环境要求

- JDK 17+（本机 JDK 21 正常）
- Maven 3.6+（本机 `E:\maven\apache-maven-3.9.9`）
- Node.js 18+（本机 `C:\Program Files\nodejs`）
- MySQL 8.0+，数据库 `uavserver`（首次连接由 JDBC 参数 `createDatabaseIfNotExist=true` 自动建库）

### 关键配置（`backend/src/main/resources/application.properties`）

```properties
server.port=8082
spring.datasource.url=${MYSQL_URL:jdbc:mysql://localhost:3306/uavserver?createDatabaseIfNotExist=true&...}
spring.datasource.username=${MYSQL_USER:root}
spring.datasource.password=${MYSQL_PASSWORD:...}     # 支持环境变量覆盖
spring.jpa.hibernate.ddl-auto=update                 # 表结构由实体生成
jwt.secret / jwt.expiration=86400000                 # JWT 默认 24 小时
file.upload-dir / file.flight-plan-dir / file.report-dir
```

生产或换机时用环境变量 `MYSQL_URL`、`MYSQL_USER`、`MYSQL_PASSWORD`、`JWT_SECRET` 覆盖即可，不必改代码。

### 启动后端

```powershell
cd E:\desktop\UAV\UAVserver\backend
mvn spring-boot:run
```

看到 `Tomcat started on port 8082` 与 `Started UAVServerApplication` 即成功。

### 启动前端

```powershell
cd E:\desktop\UAV\UAVserver\front
npm install      # 首次或依赖变更时
npm run dev
```

访问 `http://localhost:5173`。

### 启动自检（推荐每次重启后执行）

```powershell
# 端口是否在监听
netstat -ano | findstr ":8082 :5173" | findstr LISTENING

# 登录冒烟测试
Invoke-RestMethod -Uri "http://localhost:8082/api/auth/login" -Method Post `
  -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
```

返回 `{ token, username, role, userId }` 说明后端 + 数据库链路正常。

---

## 五、演示账号（均已实测可登录）

| 角色（枚举） | 账号 | 密码 | 说明 |
|---|---|---|---|
| ADMIN | `admin` | `admin123` | 超级管理员，全功能可见 |
| SUPERVISOR | `supervisor` | `supervisor123` | 监管人员，可访问计划审批 |
| AIR_CONTROL | `aircontrol1` / `aircontrol2` | `aircontrol123` | 空管人员，空域与审批 |
| OPERATOR | `operator1` … `operator4` | `operator123` | 运营人员，订单调度与资源 |
| PILOT | `pilot` / `pilot2` … `pilot8` | `pilot123` | 机长，可申报飞行计划 |
| MAINTENANCE | `maintenance1` … `maintenance4` | `maintenance123` | 维修人员 |
| CUSTOMER | `customer1` … `customer8` | `customer123` | 客户，下单与工单 |

> `admin` / `supervisor` / `pilot` 三个账号由 `config/DataInitializer.java` 在后端启动时按需创建（不存在才建），其余为演示数据账号。新增角色时务必同步 `UserRole` 枚举、路由 `meta.roles` 与数据库中的字符串。

---

## 六、功能模块与页面清单

### 已接入路由（`front/src/router/index.js`）

| 路由 | 页面文件 | 可见角色 |
|---|---|---|
| `/login` | Login.vue | 公开 |
| `/dashboard` | Dashboard.vue | ADMIN / CUSTOMER / AIR_CONTROL / OPERATOR |
| `/account/users` | Account/UserManagement.vue | ADMIN |
| `/account/roles` | Account/RoleManagement.vue | ADMIN |
| `/order/list` | Order/OrderList.vue | ADMIN / CUSTOMER / OPERATOR |
| `/order/create` | Order/OrderCreate.vue | CUSTOMER |
| `/order/dispatch` | Order/OrderDispatch.vue | ADMIN / OPERATOR |
| `/order/tracking/:id` | Order/OrderTracking.vue | ADMIN / CUSTOMER / OPERATOR |
| `/resource/aircraft` | Aircraft/List.vue | ADMIN / OPERATOR |
| `/resource/station` | Resource/StationManagement.vue | ADMIN |
| `/resource/personnel` | Resource/PersonnelManagement.vue | ADMIN |
| `/airspace/map` | Airspace/AirspaceMap.vue | ADMIN / AIR_CONTROL |
| `/airspace/list` | Airspace/List.vue | ADMIN / AIR_CONTROL |
| `/airspace/flight-plan/list` | FlightPlan/List.vue | ADMIN / AIR_CONTROL |
| `/airspace/flight-plan/create` | FlightPlan/Create.vue | ADMIN / PILOT |
| `/airspace/flight-plan/approval` | FlightPlan/List.vue | ADMIN / AIR_CONTROL / SUPERVISOR |
| `/airspace/violation` | Airspace/ViolationManagement.vue | ADMIN / AIR_CONTROL |
| `/emergency/alert` | Emergency/EmergencyAlert.vue | ADMIN / AIR_CONTROL / OPERATOR |
| `/emergency/workorder` | Emergency/WorkorderManagement.vue | ADMIN / CUSTOMER |

路由守卫：未登录跳 `/login`；`meta.roles` 不含当前角色时回退 `/dashboard`。

### 已开发但未接入路由（16 个，位于 `front/src/views/`）

| 模块 | 文件 |
|---|---|
| 账号 | Account/AuditLog.vue、Account/QualificationManagement.vue |
| 订单 | Order/OrderBatchImport.vue、Order/OrderEvaluation.vue |
| 资源 | Resource/MaintenanceManagement.vue、Resource/ResourceSchedule.vue |
| 统计 | Statistics/AirControlDashboard.vue、Statistics/ReportManagement.vue、Statistics/AnnouncementManagement.vue |
| 应急客服 | Emergency/HelpCenter.vue、Emergency/NotificationCenter.vue |
| 系统配置 | System/SystemConfig.vue、System/ServiceAreaConfig.vue、System/InterfaceManagement.vue、System/BackupManagement.vue、System/SystemLog.vue |

这些页面目前既不在路由表中、也不在侧边菜单里，直接访问会落到 `/dashboard`。要上线需先在 `router/index.js` 注册并补 `MainLayout.vue` 菜单项。

### 核心业务闭环（均已打通）

1. **订单**：创建 → 接单 → 调度（指派飞行器/机长）→ 开始 → 完成 / 取消，含路线预检
2. **飞行计划**：草稿 → 提交 → 审核 → 批准 / 驳回 → 取消
3. **空域**：禁飞 / 限飞 / 临时管制区域与场站在 Leaflet 地图叠加展示，支持飞行器实时位置与航线
4. **监管**：违规记录登记 → 处理 → 关闭；应急告警登记 → 处置 → 关闭；客服工单建单 → 分派 → 回复 → 关闭

---

## 七、后端接口清单

统一前缀 `/api`，除 `/api/auth/**` 外均需 `Authorization: Bearer <token>`。以下为前端 `src/api/index.js` 实际联调的接口。

| 模块 | 前缀 | 主要接口 |
|---|---|---|
| 认证 | `/auth` | `login`、`register`、`change-password` |
| 用户 | `/users` | 列表、`role/{role}`、`me`、详情、增删改、`{id}/reset-password` |
| 角色 | `/roles` | 列表、`permissions`、`options` |
| 飞行器 | `/aircraft` | 列表、详情、增删改、`status/{status}`、`flying`、`{id}/route` |
| 场站/空域 | `/airspace` | 空域 CRUD、`active`、`map-data`、`zones`、`zones/all`、`zones/type/{type}`、`stations`、`stations/all`、场站 CRUD |
| 飞行计划 | `/flight-plans` | 列表、详情、增删改、`{id}/submit`、`{id}/approve`、`{id}/cancel`、`status/{status}` |
| 订单 | `/orders` | 列表、详情、`status/{status}`、`customer/{id}`、`no/{orderNo}`、`{id}/route`、增改、`accept`、`dispatch`、`start`、`complete`、`cancel` |
| 统计 | `/statistics` | `overview`、`dashboard`（工作台）。`admin` 接口保留在后端，前端「运营大屏」已删除 |
| 违规 | `/violations` | 列表、详情、增删改、`{id}/process`、`{id}/close`、`{id}/report` |
| 违规航迹 | `/violations/{id}/track` | 详情弹窗地图用：起点/申报航线/实际航迹/周围禁飞限飞区（真实锚点推演，返回 `source`/`sourceNote` 标明数据来源） |
| 应急告警 | `/alerts` | 列表、详情、增删改、`{id}/process`、`{id}/close` |
| 客服工单 | `/workorders` | 列表、详情、增删改、`{id}/assign`、`{id}/reply`、`{id}/close` |

> 未引入 Swagger，`/swagger-ui.html` 不可用；调试请直接看 Controller 或前端 API 定义。

---

## 八、数据模型与枚举

**实体（11）**：`User`、`Aircraft`、`Station`、`Airspace`、`AirspaceZone`、`FlightPlan`、`FlightRecord`、`Order`、`Violation`、`EmergencyAlert`、`Workorder`

**枚举（8）**：`UserRole`、`UserStatus`、`AircraftType`、`AircraftStatus`、`AirspaceType`、`FlightPlanStatus`、`FlightStatus`、`FlightPurpose`

`UserRole` 现取值：`ADMIN`、`SUPERVISOR`、`AIR_CONTROL`、`OPERATOR`、`PILOT`、`CUSTOMER`、`MAINTENANCE`、`GUEST`。

**枚举一致性红线**：枚举字段在数据库中以字符串存储，取值必须与 Java 枚举常量完全一致。任何大小写或命名差异都会让 JPA 读取时报 `No enum constant ...`，并被 `GlobalExceptionHandler` 转成 400 —— 接口"突然报错"时优先排查这里。

---

## 九、演示数据规模（2026-09-17 实测 `/api/statistics/overview`）

| 数据 | 数量 | 分布 |
|---|---|---|
| 用户 | 28 | ADMIN 1 / SUPERVISOR 1 / AIR_CONTROL 2 / OPERATOR 4 / PILOT 8 / MAINTENANCE 4 / CUSTOMER 8 |
| 飞行器 | 48 | 可用 17 / 飞行中 16 / 维保 7 / 损坏 4 / 退役 4；多旋翼 30 / 复合翼 6 / 固定翼 6 / 混动 6 |
| 空域区域 | 56 | 禁飞 10 / 限飞 36 / 临时管制 10 |
| 场站 | 35 | 起降 20 / 充电 2 / 中转 3 / 应急 7 / 维保 3 |
| 飞行计划 | 45 | 已完成 17 / 已批准 9 / 已提交 7 / 审核中 4 / 草稿 4 / 驳回 2 / 取消 2 |
| 订单 | 240 | 已完成 170 / 进行中 26 / 待接单 13 / 已调度 13 / 已取消 11 / 已接单 7 |
| 违规记录 | 45 | — |
| 应急告警 | 30 | — |
| 客服工单 | 29 | — |

---

## 十、注意事项与踩坑记录

1. **表结构以实体为准**：`ddl-auto=update`，改实体即改表；不要指望 SQL 脚本建表。
2. **历史 Flyway 脚本已清理**：`V3__Create_Airspace_Tables.sql`、`V4__Update_Aircraft_Location.sql` 曾被放在 `resources/db/migration/` 下当作迁移脚本，但 `pom.xml` 从未引入 Flyway，因此**从未执行过**；现已连同 `db/legacy/` 一起删除，`db/` 目录只剩 `gen-seed.mjs`。后续若真要用 Flyway，需先加 `flyway-core` + `flyway-mysql` 依赖并把 `ddl-auto` 改为 `validate`，否则两套建表机制会互相打架。
3. **`db/gen-seed.mjs` 不是即插即用**：它读取根目录 `init_database.sql`（当前仓库中已不存在），直接运行会报文件找不到；演示数据目前只存在于本机 MySQL 库中。若换机需要重建演示数据集，请先补回该 SQL 文件或改写该脚本。
4. **重启服务时按端口杀进程**：批量 `taskkill` 所有 Java 进程可能连带杀掉 Maven/构建守护进程，导致后续命令异常。正确做法：
   ```powershell
   netstat -ano | findstr :8082      # 找到 PID
   taskkill /PID <PID> /F /T
   ```
5. **前端代理**：`vite.config.js` 中 `/api` → `http://localhost:8082`；后端未启动时前端会刷 `http proxy error: ECONNREFUSED`，属预期现象。
6. **违规航迹是推演数据**：库中 `flight_records` 为空表、`flight_plans.route_coordinates` 全为 NULL，没有实测航迹可用，因此 `/api/violations/{id}/track` 按「关联订单取件地→送达地」或「最近场站→违规点」推演，并叠加违规点附近的真实禁飞/限飞区；同一记录结果固定（无随机）。详情弹窗地图下方会显示来源说明。
6. **日志位置**：后端前台运行输出在控制台；后台方式启动时将输出写入 `logs/`。

---

## 十一、常见问题

**Q：登录返回 400 / 401？**
先确认后端 8082 已监听；再核对账号密码（见 §五）；若数据库里存在自定义角色字符串，检查是否与 `UserRole` 枚举一致。

**Q：页面能打开但列表全空？**
多为接口 4xx。打开浏览器控制台看 `/api/...` 响应体，`GlobalExceptionHandler` 会把原因写在 `message` 里。

**Q：换了 MySQL 密码怎么办？**
用环境变量启动，无需改代码：`$env:MYSQL_PASSWORD="新密码"` 后再 `mvn spring-boot:run`。

**Q：怎么新增一个页面？**
建 `views/<模块>/Xxx.vue` → 在 `router/index.js` 的对应 children 里注册（写 `meta.title` 与 `meta.roles`）→ 在 `layout/MainLayout.vue` 加菜单项 → 在 `src/api/index.js` 补接口方法。
