// ============================================================================
// 演示数据生成器：node db/gen-seed.mjs
//   - 读取 init_database.sql 中已存在的列清单与账号密码哈希（避免手抄出错）
//   - 生成覆盖「温州全域」的演示数据并写回 init_database.sql
//   - 坐标全部在温州市域内：鹿城/龙湾/瓯海/洞头 + 瑞安/乐清/永嘉/平阳/苍南/文成/泰顺/龙港
//   - 固定随机种子，多次运行结果一致
// ============================================================================
import fs from 'fs'

const FILE = new URL('../init_database.sql', import.meta.url).pathname.replace(/^\//, '')
const src = fs.readFileSync(FILE, 'utf8')

const cols = {}
for (const m of src.matchAll(/INSERT INTO\s+(\w+)\s*\(([^)]*)\)/g)) cols[m[1]] = m[2].split(',').map((s) => s.trim())
const hashes = {}
for (const m of src.matchAll(/\('(\w+)',\s*'(\$2a\$[^']+)'/g)) hashes[m[1]] = m[2]
const MAINT_HASH = '$2a$10$hEdmU0NqTZvXgmIxWP8wb.vOEZDdZpzKii9PFvEDLv2NMbfK4aoAW' // maintenance123
const AIR_HASH = '$2a$10$67V2kaDBA7/aBXjWW32xKOQ0YdXoyWQ6ht5k/PcqO6q0Ipf.H1ppu' // aircontrol123

let seed = 20240915
const rnd = () => ((seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff)
const pick = (a) => a[Math.floor(rnd() * a.length)]
const int = (a, b) => a + Math.floor(rnd() * (b - a + 1))
const dec = (a, b, d = 2) => Number((a + rnd() * (b - a)).toFixed(d))
const q = (s) => (s === null || s === undefined ? 'NULL' : `'${String(s).replace(/'/g, "''")}'`)
const ago = (min) => `DATE_SUB(NOW(), INTERVAL ${Math.max(0, Math.round(min))} MINUTE)`
const userRef = (u) => `(SELECT id FROM users WHERE username = '${u}')`
const acRef = (no) => `(SELECT id FROM aircraft WHERE registration_number = '${no}')`
const PILOT_IDS = ['pilot', 'pilot2', 'pilot3', 'pilot4', 'pilot5', 'pilot6', 'pilot7', 'pilot8']

// ---------------- 温州市域地点 ----------------
const P = [
  ['温州商学院', 27.9165, 120.689], ['温州南站', 27.9315, 120.6108], ['温州医科大学附属第一医院', 27.9282, 120.6295],
  ['三垟湿地公园', 27.947, 120.672], ['温州乐园', 27.9455, 120.7085], ['泽雅风景区', 28.03, 120.47],
  ['瓯海区娄桥街道', 27.93, 120.621], ['温州大学茶山校区', 27.926, 120.7005], ['温州市行政管理中心', 27.9933, 120.6994],
  ['江心屿', 28.025, 120.66], ['五马街商圈', 28.011, 120.656], ['温州国际会展中心', 27.98, 120.73],
  ['七都岛', 28.02, 120.78], ['鹿城区南汇街道', 27.988, 120.676], ['温州龙湾国际机场', 27.9126, 120.8522],
  ['龙湾区永中街道', 27.918, 120.83], ['龙湾区海城街道', 27.875, 120.815], ['龙湾区沙城街道', 27.865, 120.79],
  ['瓯海区郭溪街道', 27.96, 120.62], ['瓯海区潘桥街道', 27.92, 120.59], ['瑞安市政府', 27.7783, 120.6554],
  ['瑞安塘下镇', 27.83, 120.72], ['乐清市政府', 28.1392, 120.9609], ['永嘉县黄田街道', 28.06, 120.69],
  ['乐清柳市镇', 28.0732, 120.8508], ['永嘉县政府', 28.1534, 120.6916], ['龙湾区永兴街道', 27.91, 120.8],
  ['永嘉县三江街道', 28.04, 120.7], ['永嘉县乌牛街道', 28.03, 120.75], ['永嘉县瓯北街道', 28.035, 120.68],
  ['鹿城区双屿街道', 28.02, 120.64], ['鹿城区南塘街道', 27.985, 120.674],
  ['大罗山风景区', 27.935, 120.76], ['吹台山森林公园', 27.95, 120.65], ['瓯海藤桥街道', 28.048, 120.56],
  ['瓯海仙岩街道', 27.885, 120.645], ['永嘉桥头镇', 28.14, 120.52], ['永嘉桥下镇', 28.118, 120.572],
  ['永嘉沙头镇', 28.08, 120.73], ['永嘉岩头镇', 28.136, 120.783], ['乐清北白象镇', 28.03, 120.872],
  ['乐清市城南街道', 28.11, 120.95], ['鹿城区滨江街道', 28.01, 120.69], ['乐清市乐成街道', 28.14, 120.96],
  ['瑞安芳庄乡', 27.86, 120.43], ['瑞安平阳坑镇', 27.8, 120.45], ['瑞安寨寮溪风景区', 27.83, 120.33],
  ['瓯海区梧田街道', 27.94, 120.66], ['瓯海区三垟街道', 27.95, 120.68], ['龙湾区状元街道', 27.97, 120.76],
  ['龙湾区瑶溪街道', 27.93, 120.78], ['鹿城区江滨路片区', 28.022, 120.665]
]
const near = (p, span = 0.002) => [
  Number((p[1] + (rnd() - 0.5) * span * 2).toFixed(4)),
  Number((p[2] + (rnd() - 0.5) * span * 2).toFixed(4))
]
const hav = (a, b, c, d) => {
  const r = 6371, dLat = ((c - a) * Math.PI) / 180, dLng = ((d - b) * Math.PI) / 180
  const x = Math.sin(dLat / 2) ** 2 + Math.cos((a * Math.PI) / 180) * Math.cos((c * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return Math.round(2 * r * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x)) * 10) / 10
}

// ---- 空域围栏几何（顺序与第 3 节 ZONES 完全一致）：[地点索引, 半径m] ----
const ZONE_GEO = [
  [14, 3500], [8, 2000], [17, 2000], [31, 1500], [20, 1500], [18, 2000],
  [10, 1200], [9, 1000], [3, 1500], [1, 1300], [2, 900], [11, 800],
  [12, 1100], [4, 700], [7, 900], [22, 1200], [25, 1200], [27, 1100],
  [28, 1000], [29, 1200], [30, 1200], [26, 1500], [23, 1500], [19, 1200],
  [5, 1500], [21, 1000], [32, 1800], [33, 1500], [34, 1000], [35, 1200],
  [36, 1000], [37, 900], [40, 1000], [41, 900], [42, 1200], [43, 1200],
  [46, 1500], [47, 1500], [49, 1500], [51, 1200]
]
const ZONE_CLEAR_KM = 0.25 // 距圈边界额外净空，避免随机抖动落入圈内
// 围栏圆心就落在它对应的地标上（保证地图上不错位）
const zoneGeo = ZONE_GEO.map(([pi, r]) => ({ pi, r, c: [P[pi][1], P[pi][2]] }))
// 禁飞圈 / 限飞圈 / 临时管制圈内均不布点
const inAnyZone = (lat, lng) =>
  zoneGeo.some((z) => hav(lat, lng, z.c[0], z.c[1]) * 1000 < z.r + ZONE_CLEAR_KM * 1000)
// 地标中未被围栏覆盖的点
const LANDMARK_SAFE = P.map((_, i) => i).filter((i) => !inAnyZone(P[i][1], P[i][2]))
// 额外运营点位：温州真实街道/镇；若落在围栏内则就近微调（≤1.2km）后追加到 P 末尾
const EXTRA_PLACES = [
  ['蒲州街道', 27.96, 120.74], ['天河街道', 27.88, 120.83], ['鹿城區南郊街道', 27.985, 120.655],
  ['广化街道', 28.0, 120.64], ['松台街道', 28.0, 120.65], ['滨江街道', 28.005, 120.69],
  ['景山街道', 28.0, 120.63], ['南白象街道', 27.93, 120.66], ['梧田街道', 27.94, 120.66],
  ['三垟街道', 27.95, 120.68], ['瞿溪街道', 27.96, 120.545], ['新桥街道', 27.965, 120.64],
  ['丽岙街道', 27.88, 120.64], ['茶山高教园区', 27.92, 120.7], ['五马街道', 28.011, 120.656],
  ['永嘉县枫林镇', 28.16, 120.8], ['永嘉县大若岩镇', 28.2, 120.62], ['乐清市淡溪镇', 28.2, 120.95],
  ['瑞安市湖岭镇', 27.9, 120.47], ['瑞安市高楼镇', 27.8, 120.42], ['瑞安市马屿镇', 27.75, 120.49],
  ['安阳街道', 27.78, 120.655], ['锦湖街道', 27.79, 120.66], ['瑞安市陶山镇', 27.86, 120.61],
  ['仙降街道', 27.79, 120.62], ['潘岱街道', 27.84, 120.6], ['藤桥街道', 28.055, 120.56],
  ['仰义街道', 28.06, 120.61], ['山福镇', 28.09, 120.54], ['永嘉縣三江街道', 28.04, 120.7]
]
const placeSafe = (p) => {
  if (!inAnyZone(p[1], p[2])) return p
  for (const d of [0.004, 0.008, 0.012]) {
    for (const [dy, dx] of [[1, 0], [0, 1], [-1, 0], [0, -1], [0.7, 0.7], [-0.7, 0.7], [0.7, -0.7], [-0.7, -0.7]]) {
      const lat = Number((p[1] + dy * d).toFixed(4))
      const lng = Number((p[2] + dx * d).toFixed(4))
      if (!inAnyZone(lat, lng)) return [p[0], lat, lng]
    }
  }
  return null
}
const EXTRA_IDX = []
EXTRA_PLACES.forEach((p) => {
  const s = placeSafe(p)
  if (s) {
    P.push(s)
    EXTRA_IDX.push(P.length - 1)
  }
})
// 追加围栏（按地标名称引用新增点位）：市区外围的限飞/临时管制
const EXTRA_ZONE_SPEC = [
  ['仰义街道限飞区', 'restricted', '仰义街道', 1200, '城西居民区上空，限高120米'],
  ['山福镇限飞区', 'restricted', '山福镇', 1000, '镇区上空限飞'],
  ['瞿溪街道限飞区', 'restricted', '瞿溪街道', 1000, '城镇建成区，限高120米'],
  ['新桥街道限飞区', 'restricted', '新桥街道', 1100, '居民密集区'],
  ['鹿城南部限飞区', 'restricted', '鹿城区南郊街道', 1100, '城区南部限飞'],
  ['藤桥街道限飞区', 'restricted', '藤桥街道', 1000, '镇区上空限飞'],
  ['瑞安湖岭镇限飞区', 'restricted', '瑞安市湖岭镇', 1000, '镇区上空限飞'],
  ['瑞安高楼镇限飞区', 'restricted', '瑞安市高楼镇', 1000, '镇区上空限飞'],
  ['瑞安马屿镇限飞区', 'restricted', '瑞安市马屿镇', 1200, '镇区上空限飞'],
  ['永嘉枫林镇限飞区', 'restricted', '永嘉县枫林镇', 1000, '镇区上空限飞'],
  ['永嘉大若岩镇临时管制区', 'temporary', '永嘉县大若岩镇', 1500, '景区旺季临时管制'],
  ['乐清淡溪镇临时管制区', 'temporary', '乐清市淡溪镇', 1500, '活动期间临时管制']
]
const EXTRA_ZONE_ROWS = []
EXTRA_ZONE_SPEC.forEach(([name, type, placeName, radius, desc]) => {
  const pi = P.findIndex((p) => p[0] === placeName)
  if (pi >= 0) {
    ZONE_GEO.push([pi, radius])
    zoneGeo.push({ pi, r: radius, c: [P[pi][1], P[pi][2]] })
    EXTRA_ZONE_ROWS.push([name, type, pi, radius, desc])
  }
})
// 可运营点位 = 未被任何围栏覆盖的地点
const SAFE_IDX = P.map((_, i) => i).filter((i) => !inAnyZone(P[i][1], P[i][2]))
// 取点：安全地点附近抖动；万一落入围栏圈则回退到地点中心
const safePoint = (place) => {
  const p = near(place, 0.0015)
  return inAnyZone(p[0], p[1]) ? [place[1], place[2]] : p
}
const row = (t, o) => '  (' + cols[t].map((c) => (o[c] === undefined ? 'NULL' : o[c])).join(', ') + ')'
const ins = (t, rows) => `INSERT INTO ${t} (${cols[t].join(', ')})\nVALUES\n${rows.join(',\n')};\n`

const out = []
const P0 = P[0]

out.push(`-- ====================================================================
-- 天枢·低空智能运营管控平台 —— 演示数据初始化脚本（可重复执行）
-- 由 db/gen-seed.mjs 生成：坐标全部位于温州市域（鹿城/龙湾/瓯海/洞头 + 瑞安/乐清/永嘉/平阳/苍南/文成/泰顺/龙港）
-- ====================================================================
-- 【重要】本脚本只初始化「演示数据」，不负责建表：
--   表结构由 JPA 实体（spring.jpa.hibernate.ddl-auto=update）自动创建。
--   用法：1) 先启动后端一次；2) mysql -uroot -p < init_database.sql
--
-- 【枚举取值需与 Java 枚举/前端一致】
--   users.role            : ADMIN/SUPERVISOR/AIR_CONTROL/OPERATOR/PILOT/CUSTOMER/MAINTENANCE/GUEST
--   aircraft.type         : MULTI_ROTOR/FIXED_WING/ROTARY_WING/HYBRID/VTOL/OTHER
--   aircraft.status       : AVAILABLE/IN_FLIGHT/MAINTENANCE/RETIRED/DAMAGED
--   airspace_zones.type   : nofly/restricted/temporary
--   stations.type/status  : landing|charging|maintenance|emergency|transfer ; active|maintenance|closed
--   flight_plans.purpose  : COMMERCIAL/SURVEY/INSPECTION/DELIVERY/PHOTOGRAPHY/AGRICULTURE/EMERGENCY/TRAINING/RESEARCH/OTHER
--   flight_plans.status   : DRAFT/SUBMITTED/UNDER_REVIEW/APPROVED/REJECTED/CANCELLED/COMPLETED
--   violations.type       : route_deviation/height_violation/no_fly_zone/unauthorized
--   violations.status     : pending/processing/handled/reported
--   orders.status         : PENDING/ACCEPTED/DISPATCHED/IN_PROGRESS/COMPLETED/CANCELLED
--   emergency_alerts.level: 1(一般)/2(严重)/3(紧急)
--   work_orders.status    : pending/processing/completed/closed
--
-- 【演示账号】（平台自营，无商家角色）
--   admin/admin123                 supervisor/supervisor123
--   operator1|operator2|operator3 均为 operator123
--   maintenance1|maintenance2|maintenance3 均为 maintenance123
--   pilot|pilot2|pilot3|pilot4|pilot5|pilot6 均为 pilot123
--   customer1~6 均为 customer123
-- ====================================================================
CREATE DATABASE IF NOT EXISTS uavserver DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE uavserver;
SET NAMES utf8mb4;

-- ====================================================================
-- 0. 清空演示数据（先关外键检查，避免删除顺序受限），随后按外键顺序重新写入
-- ====================================================================
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM flight_records;
DELETE FROM flight_plans;
DELETE FROM orders;
DELETE FROM violations;
DELETE FROM emergency_alerts;
DELETE FROM work_orders;
DELETE FROM airspace_zones;
DELETE FROM stations;
DELETE FROM aircraft;
DELETE FROM users;
SET FOREIGN_KEY_CHECKS = 1;
`)

// ---------------- 1. 用户（14） ----------------
const users = [
  ['admin', '系统管理员', 'ADMIN', '平台运营中心', null, null, null, 0, 0, '平台管理员', 'admin123'],
  ['supervisor', '陈监管', 'SUPERVISOR', '温州市低空经济监管处', null, null, null, 0, 0, '空域监管负责人', 'supervisor123'],
  ['operator1', '赵运营', 'OPERATOR', '运营调度中心', null, null, null, 0, 0, '负责订单调度', 'operator123'],
  ['operator2', '钱运营', 'OPERATOR', '运营调度中心', null, null, null, 0, 0, '负责场站协调', 'operator123'],
  ['pilot', '王机长', 'PILOT', '飞行作业部', '330302199001011234', 'CAAC-UAV-2021-0001', 400, 520.5, 3, '民航无人机驾驶员复训合格', 'pilot123'],
  ['pilot2', '李机长', 'PILOT', '飞行作业部', '330302199203055678', 'CAAC-UAV-2022-0031', 25, 380, 5, '应急飞行演练优秀', 'pilot123'],
  ['pilot3', '陈机长', 'PILOT', '飞行作业部', '330304199505122345', 'CAAC-UAV-2023-0117', 220, 265.5, 2, '超视距驾驶员培训合格', 'pilot123'],
  ['pilot4', '张机长', 'PILOT', '飞行作业部', '330305199808201234', 'CAAC-UAV-2023-0242', 610, 143.5, 4, '多旋翼机型改装培训', 'pilot123'],
  ['maintenance1', '孙维修', 'MAINTENANCE', '维修保障组', null, 'UAV-MT-2020-0088', 200, 0, 2, '大疆M300机型维护认证', 'maintenance123'],
  ['maintenance2', '周维修', 'MAINTENANCE', '维修保障组', null, 'UAV-MT-2021-0126', 320, 0, 1, '动力系统专项维护认证', 'maintenance123'],
  ['customer1', '温州速运', 'CUSTOMER', '温州速运', null, null, null, 0, 0, null, 'customer123'],
  ['customer2', '茶山仓配', 'CUSTOMER', '茶山仓配中心', null, null, null, 0, 0, null, 'customer123'],
  ['customer3', '瓯海生鲜', 'CUSTOMER', '瓯海生鲜配送', null, null, null, 0, 0, null, 'customer123'],
  ['customer4', '龙湾医药物流', 'CUSTOMER', '龙湾医药物流', null, null, null, 0, 0, null, 'customer123'],
  ['pilot5', '林机长', 'PILOT', '飞行作业部', '330302199912011111', 'CAAC-UAV-2024-0311', 500, 180, 3, '夜间飞行资质认证', 'pilot123'],
  ['pilot6', '吴机长', 'PILOT', '飞行作业部', '330303199710155555', 'CAAC-UAV-2024-0455', 300, 120, 2, '海岛航线专项培训', 'pilot123'],
  ['operator3', '孙运营', 'OPERATOR', '运营调度中心', null, null, null, 0, 0, '负责海岛航线调度', 'operator123'],
  ['maintenance3', '郑维修', 'MAINTENANCE', '维修保障组', null, 'UAV-MT-2022-0198', 260, 0, 2, '飞控系统深度检修认证', 'maintenance123'],
  ['customer5', '瑞安仓配', 'CUSTOMER', '瑞安仓配中心', null, null, null, 0, 0, null, 'customer123'],
  ['customer6', '永嘉农产', 'CUSTOMER', '永嘉农产品配送', null, null, null, 0, 0, null, 'customer123'],
  ['pilot7', '黄机长', 'PILOT', '飞行作业部', '330302200001123456', 'CAAC-UAV-2024-0512', 420, 210, 3, '高原机型改装培训', 'pilot123'],
  ['pilot8', '徐机长', 'PILOT', '飞行作业部', '330303199606078765', 'CAAC-UAV-2024-0622', 360, 165, 2, '海岛航线专项培训', 'pilot123'],
  ['operator4', '李运营', 'OPERATOR', '运营调度中心', null, null, null, 0, 0, '负责夜间航班调度', 'operator123'],
  ['maintenance4', '吴维修', 'MAINTENANCE', '维修保障组', null, 'UAV-MT-2023-0241', 180, 0, 1, '电池组检测认证', 'maintenance123'],
  ['customer7', '平阳物流', 'CUSTOMER', '平阳物流配送', null, null, null, 0, 0, null, 'customer123'],
  ['customer8', '乐清电商', 'CUSTOMER', '乐清电商仓配', null, null, null, 0, 0, null, 'customer123'],
  ['aircontrol1', '郑空管', 'AIR_CONTROL', '温州空管中心', null, 'ACC-WZ-2023-011', 500, 0, 0, '空域协调值班', 'aircontrol123'],
  ['aircontrol2', '冯空管', 'AIR_CONTROL', '温州空管中心', null, 'ACC-WZ-2024-026', 600, 0, 0, '空域协调值班', 'aircontrol123']
]
const userRows = users.map(([u, name, role, org, idCard, certNo, certDays, hours, todayTasks, training, pwd], i) => {
  const hash = hashes[u] || hashes[pwd === 'pilot123' ? 'pilot' : pwd === 'customer123' ? 'customer1' : pwd === 'operator123' ? 'operator1' : 'admin']
  return row('users', {
    username: q(u),
    password: q(u.startsWith('maintenance') ? MAINT_HASH : u.startsWith('aircontrol') ? AIR_HASH : hash),
    email: q(`${u}@uav.com`),
    phone: q(`139${String(10000000 + i * 1111).slice(-8)}`),
    real_name: q(name), role: q(role), status: q('ACTIVE'), organization: q(org),
    id_card: q(idCard),
    cert_no: q(certNo),
    cert_expiry: certDays ? `DATE_ADD(CURDATE(), INTERVAL ${certDays} DAY)` : 'NULL',
    join_date: q(`202${2 + (i % 3)}-0${1 + (i % 9)}-1${i % 9}`),
    flight_hours: String(hours || 0), today_tasks: String(todayTasks),
    total_tasks: role === 'PILOT' ? String(Math.round((hours || 0) * 0.35)) : '0',
    training: q(training),
    remark: role === 'PILOT' && u === 'pilot2' ? q('执照即将到期') : 'NULL',
    created_at: 'NOW()', updated_at: 'NOW()'
  })
})
out.push(`-- ====================================================================
-- 1. 用户（${userRows.length}）：管理员/监管/运营/机长/维修/客户
-- ====================================================================
` + ins('users', userRows))

// ---------------- 2. 飞行器（24） ----------------
const MODELS = [
  ['DJI-M300', '大疆', 'MULTI_ROTOR', 6.5, 23, 55, 15, 500],
  ['DJI-M350', '大疆', 'MULTI_ROTOR', 9.2, 23, 55, 18, 500],
  ['DJI-M30T', '大疆', 'MULTI_ROTOR', 3.6, 23, 41, 12, 500],
  ['DJI-FlyCart30', '大疆', 'MULTI_ROTOR', 12.5, 20, 50, 16, 500],
  ['Zhuihai-V50', '纵横', 'VTOL', 22, 27, 180, 45, 1000],
  ['Zhuihai-CW25', '纵横', 'FIXED_WING', 25, 26, 240, 60, 1200],
  ['Fengyi-FY20', '峰飞', 'HYBRID', 30, 30, 300, 80, 1500],
  ['EHang-216', '亿航', 'MULTI_ROTOR', 220, 130, 30, 30, 300]
]
const acStatus = (i) =>
  i % 7 === 0 && i > 14 ? 'RETIRED' : i % 9 === 0 && i > 9 ? 'DAMAGED' : i % 5 === 0 ? 'MAINTENANCE' : i % 2 === 0 ? 'IN_FLIGHT' : 'AVAILABLE'
// 「飞行中」的飞行器清单：用于保证它们都有执行中订单（地图才能点开航线）
const FLYING_AC = Array.from({ length: 48 }, (_, k) => k + 1)
  .filter((i) => acStatus(i) === 'IN_FLIGHT')
  .map((i) => `UAV-${String(i).padStart(3, '0')}`)

// 「飞行中」飞行器的任务航线：起终点用安全地点，当前位置按比例插值；
// 若该插值点落入围栏则換一个比例，保证飞行器本身也不在围栏内。
// 短途任务配对：无人机配送距离限制在 1.5~16km（绕行后一般不超过 20km）
const pickPair = (seed) => {
  for (let k = 0; k < 60; k++) {
    const a = P[SAFE_IDX[(seed + k * 13) % SAFE_IDX.length]]
    const b = P[SAFE_IDX[(seed + k * 29 + 7) % SAFE_IDX.length]]
    const d = hav(a[1], a[2], b[1], b[2])
    if (d >= 1.5 && d <= 16) return [a, b]
  }
  const a = P[SAFE_IDX[seed % SAFE_IDX.length]]
  return [a, a]
}
const INFLIGHT_PLAN = FLYING_AC.map((reg, k) => {
  const pair = pickPair(k * 97 + 11)
  const a = pair[0]
  const b = pair[1]
  let pos = null
  for (const f of [0.5, 0.45, 0.55, 0.4, 0.6, 0.35, 0.65]) {
    const la = a[1] + (b[1] - a[1]) * f
    const ln = a[2] + (b[2] - a[2]) * f
    if (!inAnyZone(la, ln)) {
      pos = { lat: Number(la.toFixed(4)), lng: Number(ln.toFixed(4)), frac: f }
      break
    }
  }
  if (!pos) pos = { lat: a[1], lng: a[2], frac: 0.05 }
  return { reg, from: a, to: b, lat: pos.lat, lng: pos.lng, frac: pos.frac }
})

const acRows = []
for (let i = 1; i <= 48; i++) {
  const m = MODELS[(i - 1) % MODELS.length]
  const no = `UAV-${String(i).padStart(3, '0')}`
  const status = acStatus(i)
  // 「飞行中」的飞行器直接落在它的任务航线中途（且不在任何围栏内）
  const plan = INFLIGHT_PLAN.find((p) => p.reg === no)
  const place = plan ? plan.from : P[SAFE_IDX[(i * 3) % SAFE_IDX.length]]
  const [lat, lng] = plan ? [plan.lat, plan.lng] : safePoint(place)
  const flying = status === 'IN_FLIGHT'
  acRows.push(row('aircraft', {
    registration_number: q(no),
    name: q(`${m[0].includes('CW') ? '固定翼' : m[2] === 'VTOL' ? '垂直起降' : '多旋翼'}-${String(i).padStart(3, '0')}号机`),
    model: q(m[0]), manufacturer: q(m[1]), type: q(m[2]), serial_number: q(`SN${m[0].replace(/[^A-Z0-9]/gi, '')}${String(1000 + i)}`),
    status: q(status),
    max_takeoff_weight: String(m[3]), max_speed: String(m[4]), max_flight_time: String(m[5]),
    max_flight_distance: String(m[6]), max_altitude: String(m[7]),
    latitude: flying || status === 'AVAILABLE' ? String(lat) : 'NULL',
    longitude: flying || status === 'AVAILABLE' ? String(lng) : 'NULL',
    current_altitude: flying ? String(int(60, 120)) : '0',
    current_speed: flying ? String(int(8, 18)) : '0',
    flight_status: q(flying ? 'flying' : i % 6 === 0 ? 'returning' : 'grounded'),
    battery_level: i === 3 ? 28 : i === 11 ? 35 : int(45, 100),
    owner: q('平台自有'), operator: q('温州低空运营中心'),
    description: q(`部署于${place[0]}，负责${['市域配送', '空域巡检', '应急投送', '测绘采集'][i % 4]}`),
    created_at: 'NOW()', updated_at: 'NOW()'
  }))
}
out.push(`-- ====================================================================
-- 2. 飞行器（24）：覆盖多旋翼/固定翼/垂直起降/混合型，含维护中、损坏、退役
-- ====================================================================
` + ins('aircraft', acRows))

// ---------------- 3. 空域区域（26） ----------------
const ZONES = [
  ['温州龙湾国际机场禁飞区', 'nofly', 14, 3500, '机场净空保护区，禁止一切无人机飞行'],
  ['温州市行政管理中心禁飞区', 'nofly', 8, 2000, '市级行政办公区上空禁飞'],
  ['龙湾沙城管制空域禁飞区', 'nofly', 17, 2000, '龙湾区南部重点管制空域'],
  ['鹿城南塘禁飞区', 'nofly', 31, 1500, '城区重点单位禁飞'],
  ['瑞安市政府禁飞区', 'nofly', 20, 1500, '瑞安市行政中心禁飞'],
  ['瓯海郭溪管制空域禁飞区', 'nofly', 18, 2000, '瓯海西部重点管制空域'],
  ['五马街商圈限飞区', 'restricted', 10, 1200, '人员密集商圈，限高120米'],
  ['江心屿景区限飞区', 'restricted', 9, 1000, '历史文物保护区域，限高100米'],
  ['三垟湿地生态限飞区', 'restricted', 3, 1500, '湿地生态保护区，限高100米'],
  ['温州南站枢纽限飞区', 'restricted', 1, 1300, '高铁枢纽周边，需提前报备'],
  ['温州医科大学附属第一医院限飞区', 'restricted', 2, 900, '医院上空，仅允许医疗应急飞行'],
  ['温州国际会展中心限飞区', 'restricted', 11, 800, '大型活动期间临时加强管控'],
  ['七都岛居住区限飞区', 'restricted', 12, 1100, '居民区上空，限高120米'],
  ['温州乐园游客区限飞区', 'restricted', 4, 700, '游乐场人员密集区'],
  ['温州大学茶山校区限飞区', 'restricted', 7, 900, '校园上空，教学时段禁飞'],
  ['乐清市政府限飞区', 'restricted', 22, 1200, '乐清市行政中心'],
  ['永嘉县政府限飞区', 'restricted', 25, 1200, '永嘉县行政中心'],
  ['永嘉三江限飞区', 'restricted', 27, 1100, '永嘉三江街道建成区'],
  ['永嘉乌牛限飞区', 'restricted', 28, 1000, '永嘉乌牛街道建成区'],
  ['永嘉瓯北限飞区', 'restricted', 29, 1200, '瓯北城区上空'],
  ['鹿城双屿限飞区', 'restricted', 30, 1200, '鹿城西部建成区'],
  ['龙湾永兴临时管制区', 'temporary', 26, 1500, '大型活动期间临时管制'],
  ['永嘉黄田临时管制区', 'temporary', 23, 1500, '大型活动期间临时管制'],
  ['瓯海潘桥临时管制区', 'temporary', 19, 1200, '大型活动期间临时管制'],
  ['泽雅风景区临时管制区', 'temporary', 5, 1500, '漂流季临时管制'],
  ['瑞安塘下施工临时管制区', 'temporary', 21, 1000, '重点工程施工期间管制'],
  ['大罗山山体保护区限飞区', 'restricted', 32, 1800, '山体地形复杂，限高150米'],
  ['吹台山森林公园限飞区', 'restricted', 33, 1500, '森林防火重点区域，限高120米'],
  ['藤桥街道居住区限飞区', 'restricted', 34, 1000, '居民密集区，限高120米'],
  ['仙岩风景名胜区限飞区', 'restricted', 35, 1200, '省级风景名胜区保护范围'],
  ['永嘉桥头镇限飞区', 'restricted', 36, 1000, '城镇建成区上空'],
  ['永嘉桥下镇限飞区', 'restricted', 37, 900, '人员密集区域'],
  ['乐清北白象工业区限飞区', 'restricted', 40, 1000, '工业园区上空'],
  ['乐清城南工业区限飞区', 'restricted', 41, 900, '工业园区上空'],
  ['鹿城滨江临时管制区', 'temporary', 42, 1200, '市政工程施工期间临时管制'],
  ['乐清乐成会展临时管制区', 'temporary', 43, 1200, '大型展会期间管制'],
  ['瑞安寨寮溪景区临时管制区', 'temporary', 46, 1500, '旅游旺季临时管制'],
  ['瓯海梧田航线管制区', 'restricted', 47, 1500, '城区航线交汇区'],
  ['龙湾状元枢纽禁飞区', 'nofly', 49, 1500, '交通枢纽及周边禁飞'],
  ['鹿城江滨路禁飞区', 'nofly', 51, 1200, '重点单位及周边禁飞']
]
const ALL_ZONES = ZONES.concat(EXTRA_ZONE_ROWS)
const zoneRows = ALL_ZONES.map(([name, type, pi, radius, desc], zi) => {
  const [lat, lng] = zoneGeo[zi].c
  return row('airspace_zones', {
    name: q(name), type: q(type), latitude: String(lat), longitude: String(lng), radius: String(radius),
    description: q(desc), active: 'TRUE', created_at: 'NOW()', updated_at: 'NOW()'
  })
})
out.push(`-- ====================================================================
-- 3. 空域区域（${zoneRows.length}）：禁飞/限飞/临时管制，覆盖温州各县市区
-- ====================================================================
DELETE FROM airspace_zones;
` + ins('airspace_zones', zoneRows))

// ---------------- 4. 场站（15） ----------------
const STATION_SPEC = [
  ['landing', '王机长', 10, 3, 15], ['charging', '李机长', 8, 2, 28], ['transfer', '赵运营', 12, 5, 32],
  ['transfer', '钱运营', 16, 9, 41], ['emergency', '钱运营', 6, 1, 6], ['emergency', '陈机长', 5, 0, 3],
  ['emergency', '陈机长', 6, 2, 11], ['landing', '张机长', 8, 4, 22], ['landing', '张机长', 8, 2, 9],
  ['landing', '王机长', 7, 3, 13], ['landing', '李机长', 6, 1, 5], ['landing', '陈机长', 6, 2, 7],
  ['emergency', '张机长', 5, 1, 4], ['maintenance', '孙维修', 10, 6, 14], ['landing', '周维修', 5, 1, 8],
  ['landing', '陈机长', 5, 1, 6], ['landing', '王机长', 6, 2, 8], ['landing', '张机长', 7, 2, 7],
  ['landing', '陈机长', 8, 3, 12], ['landing', '李机长', 5, 1, 5], ['emergency', '张机长', 4, 0, 4],
  ['landing', '王机长', 6, 2, 9], ['maintenance', '周维修', 8, 4, 10], ['landing', '陈机长', 4, 1, 2],
  ['landing', '黄机长', 7, 2, 9], ['emergency', '徐机长', 5, 1, 6], ['charging', '李运营', 9, 3, 18],
  ['landing', '李运营', 6, 2, 11], ['transfer', '郑空管', 14, 6, 26], ['landing', '冯空管', 6, 1, 7],
  ['emergency', '吴维修', 4, 0, 3], ['landing', '黄机长', 8, 3, 15], ['landing', '徐机长', 6, 2, 10],
  ['maintenance', '吴维修', 9, 5, 12], ['landing', '李机长', 5, 1, 4]
]
const STATION_SUFFIX = {
  landing: '起降场',
  charging: '换电站',
  maintenance: '维修站',
  transfer: '中转站',
  emergency: '应急起降点'
}
// 场站位置从可运营点位中顺次取，名称随地点生成（保证名称与坐标一致）
const stRows = STATION_SPEC.map(([type, manager, cap, cur, today], k) => {
  const pi = SAFE_IDX[(k * 5 + 2) % SAFE_IDX.length]
  const place = P[pi]
  const [lat, lng] = safePoint(place)
  const status = type === 'maintenance' && k === 13 ? 'maintenance' : k === 23 ? 'closed' : 'active'
  return row('stations', {
    name: q(`${place[0]}${STATION_SUFFIX[type]}`), type: q(type), latitude: String(lat), longitude: String(lng),
    capacity: String(cap), current_usage: String(cur), today_usage: String(today),
    address: q(`温州市${place[0]}`), manager: q(manager),
    phone: q(`139${String(20000000 + pi * 1234).slice(-8)}`),
    operation_time: q(type === 'emergency' ? '全天' : '06:00-22:00'),
    facilities: q(type === 'charging' ? '快速充电桩8个、电池更换服务' : type === 'maintenance' ? '检修工位、配件库、备用电池' : '自动起降平台、充电桩、GPS差分基站'),
    status: q(status), remark: q(status === 'maintenance' ? '设备升级改造中' : status === 'closed' ? '场地临时停用' : null),
    created_at: 'NOW()', updated_at: 'NOW()'
  })
})
out.push(`-- ====================================================================
-- 4. 场站（${stRows.length}）：起降场/充电站/中转站/应急点/维修车间
-- ====================================================================
DELETE FROM stations;
` + ins('stations', stRows))

// ---------------- 5. 飞行计划（18） ----------------
const PLAN_STATUS = ['COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'APPROVED', 'APPROVED', 'APPROVED', 'SUBMITTED', 'SUBMITTED', 'SUBMITTED', 'UNDER_REVIEW', 'UNDER_REVIEW', 'DRAFT', 'DRAFT', 'REJECTED', 'CANCELLED', 'COMPLETED']
const PURPOSE = ['DELIVERY', 'SURVEY', 'INSPECTION', 'PHOTOGRAPHY', 'EMERGENCY', 'AGRICULTURE', 'TRAINING', 'RESEARCH', 'COMMERCIAL']
const planRows = []
for (let i = 0; i < 45; i++) {
  const st = PLAN_STATUS[i % PLAN_STATUS.length]
  const from = P[(i * 5) % P.length], to = P[(i * 5 + 7) % P.length]
  const startMin = int(60, 7 * 24 * 60)
  const dur = int(25, 120)
  const no = `FP${20240900 + i + 1}`
  const ac = `UAV-${String((i % 48) + 1).padStart(3, '0')}`
  const pilot = PILOT_IDS[i % PILOT_IDS.length]
  const done = st === 'COMPLETED'
  const approved = ['COMPLETED', 'APPROVED'].includes(st)
  planRows.push(row('flight_plans', {
    plan_number: q(no), plan_name: q(`${PURPOSE[i % 9] === 'DELIVERY' ? '配送' : PURPOSE[i % 9] === 'PHOTOGRAPHY' ? '航拍' : PURPOSE[i % 9] === 'SURVEY' ? '测绘' : PURPOSE[i % 9] === 'INSPECTION' ? '巡检' : PURPOSE[i % 9] === 'EMERGENCY' ? '应急救援' : PURPOSE[i % 9] === 'AGRICULTURE' ? '植保' : PURPOSE[i % 9] === 'TRAINING' ? '训练' : PURPOSE[i % 9] === 'RESEARCH' ? '科研' : '商业'}任务-${from[0]}至${to[0]}`), 
    aircraft_id: acRef(ac), pilot_id: userRef(pilot), applicant_id: userRef(i % 3 === 0 ? 'operator1' : i % 3 === 1 ? 'operator2' : pilot),
    purpose: q(PURPOSE[i % 9]), status: q(st),
    departure_location: q(from[0]), arrival_location: q(to[0]),
    planned_altitude: String(int(60, 120)),
    planned_start_time: ago(startMin), planned_end_time: ago(startMin - dur),
    planned_duration: String(dur),
    route_description: q(`由${from[0]}起飞，途经${P[(i * 5 + 3) % P.length][0]}，抵达${to[0]}`),
    remarks: q(st === 'REJECTED' ? '航线穿越限飞区，需重新规划' : st === 'CANCELLED' ? '客户取消任务' : null),
    submitted_at: ['DRAFT'].includes(st) ? 'NULL' : ago(startMin + 120),
    approved_at: approved ? ago(startMin + 90) : 'NULL',
    approved_by: approved ? userRef('supervisor') : 'NULL',
    created_at: ago(startMin + 240), updated_at: 'NOW()'
  }))
}
out.push(`-- ====================================================================
-- 5. 飞行计划（18）：覆盖草稿/已提交/审核中/已批准/已拒绝/已取消/已完成
-- ====================================================================
DELETE FROM flight_records;
DELETE FROM flight_plans;
` + ins('flight_plans', planRows))

// ---------------- 6. 订单（60，近 7 天） ----------------
const SERVICES = ['快递配送', '医疗物资', '日用品', '电子产品', '食品', '测绘服务', '航拍服务', '文件资料']
const RECV = ['张先生', '李女士', '王女士', '陈先生', '刘经理', '周医生', '吴老师', '郑先生', '孙小姐', '黄主管']
const NOTES = ['按时送达', '客户已签收', '放至前台代收', '需电话联系', '正在配送中', '待运营调度', '客户要求加急', '已完成配送']
const orderRows = []
let oIdx = 0
let flyingIdx = 0 // 执行中订单依次分配给「飞行中」的飞行器，保证每架都有航线
for (let day = 6; day >= 0; day--) {
  const count = [30, 32, 36, 32, 40, 36, 34][6 - day]
  for (let k = 0; k < count && oIdx < 240; k++) {
    oIdx++
    // 越近期越可能是进行中/待处理
    let status
    const r = rnd()
    if (day >= 3) status = r < 0.85 ? 'COMPLETED' : r < 0.93 ? 'CANCELLED' : 'IN_PROGRESS'
    else if (day >= 1) status = r < 0.6 ? 'COMPLETED' : r < 0.72 ? 'IN_PROGRESS' : r < 0.8 ? 'DISPATCHED' : r < 0.88 ? 'ACCEPTED' : r < 0.95 ? 'PENDING' : 'CANCELLED'
    else status = r < 0.3 ? 'COMPLETED' : r < 0.5 ? 'IN_PROGRESS' : r < 0.65 ? 'DISPATCHED' : r < 0.8 ? 'ACCEPTED' : 'PENDING'
    // 执行中订单直接复用「飞行中」飞行器的任务航线，保证飞行器正好在航线上
    const plan = status === 'IN_PROGRESS' ? INFLIGHT_PLAN[flyingIdx++ % INFLIGHT_PLAN.length] : null
    const pair = plan ? null : pickPair(oIdx * 53 + 7)
    const from = plan ? plan.from : pair[0]
    const to = plan ? plan.to : pair[1]
    const pFrom = plan ? null : safePoint(from)
    const pTo = plan ? null : safePoint(to)
    const plat = plan ? plan.from[1] : pFrom[0]
    const plng = plan ? plan.from[2] : pFrom[1]
    const dlat = plan ? plan.to[1] : pTo[0]
    const dlng = plan ? plan.to[2] : pTo[1]
    const distance = hav(plat, plng, dlat, dlng)
    const createdMin = day * 24 * 60 + int(-720, -30)
    const mins = Math.max(5, createdMin)
    const done = status === 'COMPLETED'
    const accepted = ['ACCEPTED', 'DISPATCHED', 'IN_PROGRESS', 'COMPLETED'].includes(status)
    const dispatched = ['DISPATCHED', 'IN_PROGRESS', 'COMPLETED'].includes(status)
    const flying = ['IN_PROGRESS', 'COMPLETED'].includes(status)
    const dur = Math.round((distance * 1000) / 15 / 60)
    const urgent = rnd() < 0.25
    orderRows.push(row('orders', {
      order_number: q(`ORD${2024090000 + oIdx}`),
      customer_id: userRef(
        ['customer1', 'customer2', 'customer3', 'customer4', 'customer5', 'customer6', 'customer7', 'customer8'][oIdx % 8]
      ),
      merchant_id: 'NULL',
      aircraft_id: accepted ? acRef(plan ? plan.reg : `UAV-${String((oIdx % 48) + 1).padStart(3, '0')}`) : 'NULL',
      pilot_id: accepted ? userRef(PILOT_IDS[oIdx % PILOT_IDS.length]) : 'NULL',
      service_type: q(SERVICES[oIdx % 8]),
      pickup_location: q(from[0]), pickup_latitude: String(plat), pickup_longitude: String(plng),
      delivery_location: q(to[0]), delivery_latitude: String(dlat), delivery_longitude: String(dlng),
      distance: String(distance), estimated_duration: String(dur),
      status: q(status), priority: q(urgent ? 'URGENT' : 'NORMAL'),
      amount: String(Math.round((5 + distance * 12) * 100) / 100),
      weight: String(dec(0.5, 8, 1)), volume: String(int(5, 60)),
      recv_name: q(RECV[oIdx % 10]), recv_phone: q(`139${String(30000000 + oIdx * 37).slice(-8)}`),
      time_requirement: q(urgent ? 'urgent' : 'normal'),
      special_requirements: q(oIdx % 5 === 0 ? 'fragile' : oIdx % 7 === 0 ? 'cold' : oIdx % 9 === 0 ? 'sign' : null),
      route_altitude: String(int(60, 120)),
      scheduled_time: ago(mins - 10),
      accepted_at: accepted ? ago(mins - 3) : 'NULL',
      dispatched_at: dispatched ? ago(mins - 2) : 'NULL',
      actual_start_time: flying ? ago(mins - 1) : 'NULL',
      actual_end_time: done ? ago(Math.max(1, mins - Math.max(10, dur))) : 'NULL',
      notes: q(done ? NOTES[oIdx % 4] : status === 'PENDING' ? '待运营调度' : status === 'CANCELLED' ? '客户主动取消' : '正在配送中'),
      created_at: ago(mins), updated_at: ago(Math.max(1, mins - 1))
    }))
  }
}
out.push(`-- ====================================================================
-- 6. 订单（${orderRows.length}）：分布在最近 7 天，覆盖待接单/已接单/已调度/配送中/已完成/已取消
-- ====================================================================
DELETE FROM orders;
` + ins('orders', orderRows))

// ---------------- 7. 违规（20） ----------------
const VIO_TYPES = ['route_deviation', 'height_violation', 'no_fly_zone', 'unauthorized']
const VIO_DETAIL = {
  route_deviation: '实际航线偏离申报航线超过500米',
  height_violation: '实际飞行高度超过申报高度',
  no_fly_zone: '飞行轨迹进入禁飞/限飞区域',
  unauthorized: '未取得飞行许可即起飞'
}
const DETECT = ['系统自动检测', '人工巡查发现', '群众举报核实', '平台数据比对']
const vioRows = []
for (let i = 0; i < 45; i++) {
  const t = VIO_TYPES[i % 4]
  // 禁飞类违规：坐标就用禁飞圈中心（与“飞行器已避让围栏”逻辑一致）
  const nfZones = zoneGeo.filter((_, zi) => ALL_ZONES[zi][1] === 'nofly')
  const nf = nfZones[i % nfZones.length]
  const place = t === 'no_fly_zone' ? P[nf.pi] : P[SAFE_IDX[(i * 7) % SAFE_IDX.length]]
  const [lat, lng] = t === 'no_fly_zone' ? nf.c : safePoint(place)
  const st = i % 5 === 0 ? 'reported' : i % 3 === 0 ? 'handled' : i % 3 === 1 ? 'processing' : 'pending'
  const handled = ['handled', 'reported'].includes(st)
  const ac = `UAV-${String((i % 48) + 1).padStart(3, '0')}`
  vioRows.push(row('violations', {
    violation_no: q(`VIO${2024090000 + i + 1}`),
    type: q(t), severity: String(t === 'no_fly_zone' ? 3 : t === 'unauthorized' ? int(2, 3) : int(1, 2)),
    status: q(st),
    aircraft_id: acRef(ac), device_no: q(ac),
    pilot_id: userRef(PILOT_IDS[i % PILOT_IDS.length]), pilot_name: q(users.find((u) => u[0] === PILOT_IDS[i % PILOT_IDS.length])[1]),
    merchant_id: 'NULL', merchant_name: 'NULL',
    order_no: i % 3 === 0 ? q(`ORD${2024090000 + ((i % 60) + 1)}`) : 'NULL',
    location: q(`${place[0]}附近空域`), latitude: String(lat), longitude: String(lng),
    occur_time: ago(int(60, 6 * 24 * 60)), detect_method: q(DETECT[i % 4]),
    detail: q(VIO_DETAIL[t]),
    handler: handled ? q(i % 2 === 0 ? '陈监管' : '钱运营') : 'NULL',
    handle_result: handled ? q(['warning', 'fine', 'suspend', 'revoke'][i % 4]) : 'NULL',
    handle_remark: handled ? q('已通知运营方整改，并完成安全复训') : 'NULL',
    fine_amount: handled && i % 4 === 1 ? String(int(500, 5000)) : 'NULL',
    suspend_days: handled && i % 4 === 2 ? String(int(1, 7)) : 'NULL',
    reported: st === 'reported' ? 'TRUE' : 'FALSE',
    handle_time: handled ? ago(int(30, 3 * 24 * 60)) : 'NULL',
    created_at: 'NOW()', updated_at: 'NOW()'
  }))
}
out.push(`-- ====================================================================
-- 7. 违规记录（${vioRows.length}）
-- ====================================================================
DELETE FROM violations;
` + ins('violations', vioRows))

// ---------------- 8. 应急告警（14） ----------------
const ALERT_TYPES = [
  ['device_fault', '飞行器动力系统异常，输出功率波动', 'UAV-009'],
  ['route_deviation', '飞行器偏离预定航线，已触发航线纠偏', 'UAV-004'],
  ['battery_low', '飞行器电量低于30%，建议立即返航', 'UAV-003'],
  ['airspace_conflict', '航线与临时管制空域冲突，需重新规划', 'UAV-011'],
  ['order_abnormal', '订单配送超时，客户反馈未收到货物', 'UAV-006'],
  ['device_fault', '机载图传信号中断超过30秒', 'UAV-013'],
  ['battery_low', '备用电池组温度异常升高', 'UAV-005'],
  ['route_deviation', '强风导致航向偏差，已自动修正', 'UAV-017'],
  ['airspace_conflict', '检测到禁飞区边缘接近告警', 'UAV-015'],
  ['order_abnormal', '收货地址无法精确定位，等待人工确认', 'UAV-007'],
  ['device_fault', '起降架回位传感器异常', 'UAV-019'],
  ['battery_low', '充电桩故障导致补电失败', 'UAV-010'],
  ['route_deviation', '低空障碍物逼近告警，已爬升避让', 'UAV-021'],
  ['device_fault', '导航模块定位精度下降（HDOP>2）', 'UAV-002'],
  ['device_fault', '机载避障雷达误报障碍物', 'UAV-025'],
  ['battery_low', '长航线飞行电量预警（剩余32%）', 'UAV-027'],
  ['route_deviation', '山区信号遮挡导致航迹漂移', 'UAV-029'],
  ['airspace_conflict', '接近龙湾状元枢纽禁飞区边界', 'UAV-031'],
  ['order_abnormal', '跨区配送超时未通知客户', 'UAV-033'],
  ['device_fault', '电机温度过高自动降速', 'UAV-035'],
  ['device_fault', '右侧电机转速异常波动', 'UAV-037'],
  ['battery_low', '返航途中电量降至 28%', 'UAV-039'],
  ['route_deviation', '山区信号遮挡导致航迹漂移 120m', 'UAV-041'],
  ['airspace_conflict', '航线接近乐清湾沿岸管制区', 'UAV-043'],
  ['order_abnormal', '收货人电话连续两次未接通', 'UAV-045'],
  ['device_fault', '图传链路延迟超过 800ms', 'UAV-047'],
  ['battery_low', '备用电池组温度升高至 52℃', 'UAV-048'],
  ['route_deviation', 'GPS 定位跳变触发航迹告警', 'UAV-036'],
  ['airspace_conflict', '接近瑞安市政府禁飞区边界', 'UAV-020'],
  ['order_abnormal', '客户临时变更收货地址', 'UAV-010']
]
const alertRows = ALERT_TYPES.map(([type, content, ac], i) => {
  const place = P[SAFE_IDX[(i * 6) % SAFE_IDX.length]]
  const [lat, lng] = safePoint(place)
  const level = type === 'device_fault' ? (i % 2 === 0 ? 3 : 2) : type === 'battery_low' ? 3 : type === 'airspace_conflict' ? 2 : 1
  const st = i % 4 === 0 ? 'pending' : i % 4 === 1 ? 'processing' : i % 4 === 2 ? 'handled' : 'closed'
  const handled = ['handled', 'closed'].includes(st)
  return row('emergency_alerts', {
    alert_no: q(`ALT${2024090000 + i + 1}`), level: String(level), type: q(type), content: q(content),
    related_object: q(ac), status: q(st), location: q(`${place[0]}上空`), latitude: String(lat), longitude: String(lng),
    aircraft_id: acRef(ac), occur_time: ago(int(30, 5 * 24 * 60)),
    handler: st === 'pending' ? 'NULL' : q(['陈监管', '钱运营', '孙维修'][i % 3]),
    handle_method: st === 'pending' ? 'NULL' : q(['on_site', 'remote', 'coordinate'][i % 3]),
    handle_measure: handled ? q('已通知机长返航检查，飞行器状态恢复正常') : st === 'processing' ? q('已派人现场核查，等待处置结果') : 'NULL',
    expected_time: handled ? 'NULL' : ago(-int(60, 600)),
    handle_time: handled ? ago(int(20, 2 * 24 * 60)) : 'NULL',
    close_time: st === 'closed' ? ago(int(10, 24 * 60)) : 'NULL',
    created_at: 'NOW()', updated_at: 'NOW()'
  })
})
out.push(`-- ====================================================================
-- 8. 应急告警（${alertRows.length}）
-- ====================================================================
DELETE FROM emergency_alerts;
` + ins('emergency_alerts', alertRows))

// ---------------- 9. 客服工单（14） ----------------
const WO = [
  ['complaint', 2, '配送延误未提前告知', '订单约定30分钟送达，实际延误20分钟未收到通知。', 'customer1', '温州速运'],
  ['inquiry', 1, '咨询无人机配送可送区域', '想了解龙湾区哪些范围支持无人机配送。', 'customer3', '瓯海生鲜'],
  ['fault', 3, '起降场充电桩故障', '茶山换电站B的2号充电桩无法充电。', 'operator2', '运营调度中心'],
  ['suggestion', 1, '建议开放夜间配送', '希望鹿城核心区能支持夜间配送服务。', 'customer2', '茶山仓配'],
  ['complaint', 2, '货物外包装破损', '收到的冷链箱有磕碰痕迹，要求核查。', 'customer4', '龙湾医药物流'],
  ['inquiry', 1, '如何申请空域飞行报备', '想了解企业申请常态化飞行报备的流程。', 'customer1', '温州速运'],
  ['fault', 3, '平台地图偶尔加载失败', '地图页偶发空白，刷新后恢复。', 'operator1', '运营调度中心'],
  ['suggestion', 1, '希望增加配送实时轨迹', '希望在订单跟踪里看到历史飞行轨迹回放。', 'customer3', '瓯海生鲜'],
  ['complaint', 1, '客服电话长时间占线', '连续拨打三次均未接通人工客服。', 'customer2', '茶山仓配'],
  ['inquiry', 1, '医疗物资配送温控要求', '想知道冷链药品配送的温度记录如何查询。', 'customer4', '龙湾医药物流'],
  ['fault', 2, '飞行器上报电量异常', '同一架飞行器连续两次显示电量跳变。', 'maintenance1', '维修保障组'],
  ['suggestion', 2, '建议开放企业批量下单接口', '希望提供API对接。', 'customer1', '温州速运'],
  ['complaint', 2, '瑞安线路取消未说明原因', '订单被取消但未收到原因说明。', 'customer3', '瓯海生鲜'],
  ['inquiry', 1, '场站自建需要什么资质', '企业自建起降场需要哪些审批材料。', 'customer2', '茶山仓配'],
  ['complaint', 2, '跨区航线配送延误', '苍南方向订单延误 3 小时未提前告知。', 'customer3', '瓯海生鲜'],
  ['inquiry', 1, '景区航拍如何报备', '大罗山、仙岩景区航拍需要办理哪些手续。', 'customer4', '龙湾医药物流'],
  ['suggestion', 2, '希望增加夜间应急配送', '医院急需物资希望支持夜间空运。', 'customer1', '温州速运'],
  ['fault', 2, '起降场夜间照明故障', '鹿城滨江起降点照明灯不亮。', 'operator1', '运营调度中心'],
  ['complaint', 1, '配送费用计算疑问', '对运费计算规则有疑问。', 'customer2', '茶山仓配'],
  ['inquiry', 1, '企业月结合作方式', '想了解长期合作的价格方案。', 'customer5', '瑞安仓配'],
  ['complaint', 2, '乐清线路配送超时', '乐清柳市订单延误 40 分钟。', 'customer8', '乐清电商仓配'],
  ['inquiry', 1, '平阳线路是否可送', '想确认平阳地区的配送覆盖范围。', 'customer7', '平阳物流配送'],
  ['suggestion', 1, '建议开放山区航线', '希望永嘉大若岩方向能开通配送。', 'customer6', '永嘉农产品配送'],
  ['fault', 2, '换电站充电枪识别失败', '蒲州街道换电站 3 号枪识别异常。', 'operator4', '运营调度中心'],
  ['complaint', 1, '无人机噪声反馈', '夜间飞行噪声影响休息。', 'customer2', '茶山仓配'],
  ['inquiry', 1, '空域报备需要提前多久', '常态化航线需要提前多久报备。', 'customer1', '温州速运'],
  ['suggestion', 2, '建议增加轨迹回放', '希望在跟踪页看到历史轨迹回放。', 'customer5', '瑞安仓配'],
  ['fault', 2, '大屏页偶发白屏', '大屏切换时偶发空白。', 'operator1', '运营调度中心'],
  ['complaint', 2, '冷链箱温度记录缺失', '收到的药品箱未附温度记录。', 'customer4', '龙湾医药物流']
]
const woRows = WO.map(([type, priority, title, content, customer, org], i) => {
  const st = i % 4 === 0 ? 'completed' : i % 4 === 1 ? 'completed' : i % 4 === 2 ? 'processing' : 'pending'
  const done = st === 'completed'
  const createdMin = int(60, 6 * 24 * 60)
  return row('work_orders', {
    workorder_no: q(`WO${2024090000 + i + 1}`), type: q(type), priority: String(priority),
    title: q(title), content: q(content),
    customer_id: userRef(customer), customer_name: q(org),
    contact: q(`139${String(40000000 + i * 29).slice(-8)}`),
    status: q(st),
    handler: st === 'pending' ? 'NULL' : q(['钱运营', '赵运营', '陈监管'][i % 3]),
    reply: done ? q('已核实并完成处理，感谢您的反馈。') : st === 'processing' ? q('已受理，正在处理中。') : 'NULL',
    order_no: i % 3 === 0 ? q(`ORD${2024090000 + ((i % 60) + 1)}`) : 'NULL',
    resolved_at: done ? ago(Math.max(5, createdMin - int(60, 600))) : 'NULL',
    created_at: ago(createdMin), updated_at: 'NOW()'
  })
})
out.push(`-- ====================================================================
-- 9. 客服工单（${woRows.length}）
-- ====================================================================
DELETE FROM work_orders;
` + ins('work_orders', woRows))

// ---------------- 清理与校验 ----------------
out.push(`-- ====================================================================
-- 10. 平台自营：清理历史「商家」归属，并回填接单人
-- ====================================================================
UPDATE orders SET merchant_id = NULL;
UPDATE violations SET merchant_id = NULL, merchant_name = NULL;
UPDATE orders SET accepted_by = (SELECT real_name FROM users WHERE username = 'operator1')
  WHERE status IN ('ACCEPTED', 'DISPATCHED', 'IN_PROGRESS', 'COMPLETED');

-- 飞行计划申请人统一改为运营人员（原商家账号已删除）
UPDATE flight_plans SET applicant_id = (SELECT id FROM users WHERE username = 'operator1');

-- 保证「飞行中」的飞行器都有执行中订单（地图上点开就能看到航线）
UPDATE orders o
JOIN (SELECT aircraft_id, MAX(id) mid FROM orders WHERE status IN ('ACCEPTED', 'DISPATCHED', 'COMPLETED') GROUP BY aircraft_id) t
  ON t.mid = o.id
JOIN aircraft a ON a.id = o.aircraft_id
LEFT JOIN (SELECT DISTINCT aircraft_id FROM orders WHERE status = 'IN_PROGRESS') x ON x.aircraft_id = o.aircraft_id
SET o.status = 'IN_PROGRESS',
    o.actual_start_time = COALESCE(o.actual_start_time, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
    o.actual_end_time = NULL
WHERE a.status = 'IN_FLIGHT' AND x.aircraft_id IS NULL;

-- 执行中的飞行器：实时高度对齐订单计划高度（订单选取口径与后端一致：已接单/已调度/配送中里 id 最大的一条）
UPDATE aircraft a
JOIN (SELECT aircraft_id, MAX(id) mid FROM orders WHERE status = 'IN_PROGRESS' GROUP BY aircraft_id) t
  ON t.aircraft_id = a.id
JOIN orders o ON o.id = t.mid
SET a.current_altitude = o.route_altitude
WHERE a.status = 'IN_FLIGHT';

-- ====================================================================
-- 11. 校验
-- ====================================================================
SELECT '============ 演示数据初始化完成 ============' AS message;
SELECT COUNT(*) AS 用户数 FROM users;
SELECT COUNT(*) AS 飞行器数 FROM aircraft;
SELECT COUNT(*) AS 空域区域数 FROM airspace_zones;
SELECT COUNT(*) AS 场站数 FROM stations;
SELECT COUNT(*) AS 飞行计划数 FROM flight_plans;
SELECT COUNT(*) AS 订单数 FROM orders;
SELECT COUNT(*) AS 违规记录数 FROM violations;
SELECT COUNT(*) AS 应急告警数 FROM emergency_alerts;
SELECT COUNT(*) AS 客服工单数 FROM work_orders;
-- 坐标范围核对（应落在温州：lat 27.5~28.4 / lng 119.7~121.2）
SELECT MIN(latitude) AS 最小纬度, MAX(latitude) AS 最大纬度, MIN(longitude) AS 最小经度, MAX(longitude) AS 最大经度 FROM aircraft WHERE latitude IS NOT NULL;
SELECT MIN(latitude) AS 最小纬度, MAX(latitude) AS 最大纬度, MIN(longitude) AS 最小经度, MAX(longitude) AS 最大经度 FROM airspace_zones;
SELECT MIN(pickup_latitude) AS 最小纬度, MAX(delivery_latitude) AS 最大纬度, MIN(pickup_longitude) AS 最小经度, MAX(delivery_longitude) AS 最大经度 FROM orders;
`)

fs.writeFileSync(FILE, out.join('\n'), 'utf8')
const counts = { users: userRows.length, aircraft: acRows.length, zones: zoneRows.length, stations: stRows.length, plans: planRows.length, orders: orderRows.length, violations: vioRows.length, alerts: alertRows.length, workorders: woRows.length }
console.log('已生成', FILE)
console.log(counts)
