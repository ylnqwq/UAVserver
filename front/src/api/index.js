import request from '@/utils/request'

// 认证相关API
export const authAPI = {
  // 登录
  login(data) {
    return request.post('/auth/login', data)
  },
  // 注册
  register(data) {
    return request.post('/auth/register', data)
  },
  // 修改密码
  changePassword(data) {
    return request.post('/auth/change-password', data)
  }
}

// 飞行器管理API
export const aircraftAPI = {
  // 获取飞行器列表
  getList() {
    return request.get('/aircraft')
  },
  // 获取飞行器详情
  getDetail(id) {
    return request.get(`/aircraft/${id}`)
  },
  // 创建飞行器
  create(data) {
    return request.post('/aircraft', data)
  },
  // 更新飞行器
  update(id, data) {
    return request.put(`/aircraft/${id}`, data)
  },
  // 删除飞行器
  delete(id) {
    return request.delete(`/aircraft/${id}`)
  },
  // 按状态查询
  getByStatus(status) {
    return request.get(`/aircraft/status/${status}`)
  },
  // 地图用：获取有实时位置的飞行器
  getFlying() {
    return request.get('/aircraft/flying')
  },
  // 地图用：获取某架飞行器的当前/最近航线
  getRoute(id, config = {}) {
    return request.get(`/aircraft/${id}/route`, config)
  }
}

// 飞行计划API
export const flightPlanAPI = {
  // 获取飞行计划列表
  getList() {
    return request.get('/flight-plans')
  },
  // 获取飞行计划详情
  getDetail(id) {
    return request.get(`/flight-plans/${id}`)
  },
  // 创建飞行计划
  create(data) {
    return request.post('/flight-plans', data)
  },
  // 更新飞行计划
  update(id, data) {
    return request.put(`/flight-plans/${id}`, data)
  },
  // 提交飞行计划
  submit(id) {
    return request.post(`/flight-plans/${id}/submit`)
  },
  // 审批飞行计划
  approve(id, data) {
    return request.post(`/flight-plans/${id}/approve`, data)
  },
  // 取消飞行计划
  cancel(id) {
    return request.post(`/flight-plans/${id}/cancel`)
  },
  // 按状态查询
  getByStatus(status) {
    return request.get(`/flight-plans/status/${status}`)
  }
}

// 空域管理API
export const airspaceAPI = {
  // 获取空域列表
  getList() {
    return request.get('/airspace')
  },
  // 获取空域详情
  getDetail(id) {
    return request.get(`/airspace/${id}`)
  },
  // 创建空域
  create(data) {
    return request.post('/airspace', data)
  },
  // 更新空域
  update(id, data) {
    return request.put(`/airspace/${id}`, data)
  },
  // 删除空域
  delete(id) {
    return request.delete(`/airspace/${id}`)
  },
  // 获取所有启用的空域区域（禁飞区等）
  getZones() {
    return request.get('/airspace/zones')
  },
  // 获取所有空域区域（包括停用的）
  getAllZones() {
    return request.get('/airspace/zones/all')
  },
  // 获取地图数据（空域区域+场站）
  getMapData() {
    return request.get('/airspace/map-data')
  },
  // 获取启用的空域
  getActive() {
    return request.get('/airspace/active')
  },
  // 按类型查询
  getByType(type) {
    return request.get(`/airspace/type/${type}`)
  },
  // 地图聚合数据（空域区域 + 场站）
  getMapData() {
    return request.get('/airspace/map-data')
  },

  // ===== 空域区域（地图上的禁飞/限飞/临时管制圈）=====
  // 列表：包含已停用的空域（空域管理页面用）
  getZones(config) {
    return request.get('/airspace/zones/all', config)
  },
  // 仅启用的空域（地图绘制用）
  getActiveZones(config) {
    return request.get('/airspace/zones', config)
  },
  // 按类型查询启用的空域：nofly / restricted / temporary
  getZonesByType(type) {
    return request.get(`/airspace/zones/type/${type}`)
  },
  // 新增空域区域
  createZone(data) {
    return request.post('/airspace/zones', data)
  },
  // 修改空域区域
  updateZone(id, data) {
    return request.put(`/airspace/zones/${id}`, data)
  },
  // 删除空域区域
  deleteZone(id) {
    return request.delete(`/airspace/zones/${id}`)
  },

  // ===== 场站 =====
  // 全部场站（含维护中/停用，场站管理页面用）
  getStations(config) {
    return request.get('/airspace/stations/all', config)
  },
  // 仅启用中的场站（地图绘制用）
  getActiveStations(config) {
    return request.get('/airspace/stations', config)
  },
  createStation(data) {
    return request.post('/airspace/stations', data)
  },
  updateStation(id, data) {
    return request.put(`/airspace/stations/${id}`, data)
  },
  deleteStation(id) {
    return request.delete(`/airspace/stations/${id}`)
  }
}

// 用户管理API
export const userAPI = {
  // 用户列表（仅管理员）
  getList(config) {
    return request.get('/users', config)
  },
  // 按角色查询，例如查飞行员：getByRole('PILOT')
  getByRole(role, config) {
    return request.get(`/users/role/${role}`, config)
  },
  // 用户详情
  getDetail(id) {
    return request.get(`/users/${id}`)
  },
  // 当前登录用户
  getCurrent() {
    return request.get('/users/me')
  },
  // 新增用户（仅管理员）
  create(data) {
    return request.post('/users', data)
  },
  // 修改用户（仅管理员）
  update(id, data) {
    return request.put(`/users/${id}`, data)
  },
  // 删除用户（仅管理员）
  remove(id) {
    return request.delete(`/users/${id}`)
  },
  // 重置密码（仅管理员）
  resetPassword(id, newPassword) {
    return request.post(`/users/${id}/reset-password`, { newPassword })
  }
}

// 角色API（角色由后端枚举定义）
export const roleAPI = {
  // 角色列表（含用户数、内置权限）
  getList() {
    return request.get('/roles')
  },
  // 权限目录树
  getPermissions() {
    return request.get('/roles/permissions')
  },
  // 角色下拉选项
  getOptions() {
    return request.get('/roles/options')
  }
}

// 订单API
export const orderAPI = {
  // 订单列表
  getList(config) {
    return request.get('/orders', config)
  },
  // 订单详情
  getDetail(id) {
    return request.get(`/orders/${id}`)
  },
  // 按状态查询
  getByStatus(status, config) {
    return request.get(`/orders/status/${status}`, config)
  },
  // 某客户的订单
  getByCustomer(customerId, config) {
    return request.get(`/orders/customer/${customerId}`, config)
  },
  // 按订单号查询（订单跟踪）
  getByOrderNo(orderNo) {
    return request.get(`/orders/no/${orderNo}`)
  },
  // 航线预检（距离/时长/高度/途经禁飞区）
  getRoute(id) {
    return request.get(`/orders/${id}/route`)
  },
  // 下单
  create(data) {
    return request.post('/orders', data)
  },
  // 修改
  update(id, data) {
    return request.put(`/orders/${id}`, data)
  },
  // 平台接单
  accept(id) {
    return request.post(`/orders/${id}/accept`)
  },
  // 调度（指派飞行器/机长）
  dispatch(id, data) {
    return request.post(`/orders/${id}/dispatch`, data)
  },
  // 开始配送
  start(id) {
    return request.post(`/orders/${id}/start`)
  },
  // 完成
  complete(id) {
    return request.post(`/orders/${id}/complete`)
  },
  // 取消
  cancel(id) {
    return request.post(`/orders/${id}/cancel`)
  }
}

// 统计API
export const statisticsAPI = {
  // 平台总览
  getOverview() {
    return request.get('/statistics/overview')
  },
  // 工作台（指标卡 + 趋势 + 待办 + 告警）
  getDashboard() {
    return request.get('/statistics/dashboard')
  }
}

// 违规记录API
export const violationAPI = {
  getList(params, config) {
    return request.get('/violations', { params, ...config })
  },
  getDetail(id) {
    return request.get(`/violations/${id}`)
  },
  // 违规航迹（详情弹窗地图用：起点/航迹/终点/周围管制区）
  getTrack(id) {
    return request.get(`/violations/${id}/track`, { silent: true })
  },
  create(data) {
    return request.post('/violations', data)
  },
  update(id, data) {
    return request.put(`/violations/${id}`, data)
  },
  remove(id) {
    return request.delete(`/violations/${id}`)
  },
  // 处理违规
  process(id, data) {
    return request.post(`/violations/${id}/process`, data)
  },
  // 关闭违规
  close(id) {
    return request.post(`/violations/${id}/close`)
  }
}

// 应急告警API
export const alertAPI = {
  getList(params, config) {
    return request.get('/alerts', { params, ...config })
  },
  getDetail(id) {
    return request.get(`/alerts/${id}`)
  },
  create(data) {
    return request.post('/alerts', data)
  },
  update(id, data) {
    return request.put(`/alerts/${id}`, data)
  },
  remove(id) {
    return request.delete(`/alerts/${id}`)
  },
  // 处置告警
  process(id, data) {
    return request.post(`/alerts/${id}/process`, data)
  },
  // 关闭告警
  close(id) {
    return request.post(`/alerts/${id}/close`)
  }
}

// 客服工单API
export const workorderAPI = {
  getList(params, config) {
    return request.get('/workorders', { params, ...config })
  },
  getDetail(id) {
    return request.get(`/workorders/${id}`)
  },
  create(data) {
    return request.post('/workorders', data)
  },
  update(id, data) {
    return request.put(`/workorders/${id}`, data)
  },
  remove(id) {
    return request.delete(`/workorders/${id}`)
  },
  // 分派处理人
  assign(id, handler) {
    return request.post(`/workorders/${id}/assign`, { handler })
  },
  // 回复并解决
  reply(id, data) {
    return request.post(`/workorders/${id}/reply`, data)
  },
  // 关闭工单
  close(id) {
    return request.post(`/workorders/${id}/close`)
  }
}
