<template>
  <div class="order-tracking">
    <el-card shadow="never">
      <!-- 订单基本信息 -->
      <div class="order-header">
        <div class="order-info">
          <h2>订单跟踪 - {{ orderInfo.orderNo }}</h2>
          <el-tag :type="getStatusType(orderInfo.status)" size="large">
            {{ getStatusName(orderInfo.status) }}
          </el-tag>
        </div>
        <div class="order-actions">
          <el-button @click="refreshData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button @click="goBack">
            <el-icon><Back /></el-icon>
            返回
          </el-button>
        </div>
      </div>

      <el-divider />

      <!-- 订单详细信息 -->
      <el-row :gutter="20" class="mb-20">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>订单信息</span>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="订单号">{{ orderInfo.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="客户">{{ orderInfo.customerName }}</el-descriptions-item>
              <el-descriptions-item label="接单人">{{ orderInfo.acceptedBy }}</el-descriptions-item>
              <el-descriptions-item label="寄件地址">{{ orderInfo.sendAddr }}</el-descriptions-item>
              <el-descriptions-item label="收件地址">{{ orderInfo.recvAddr }}</el-descriptions-item>
              <el-descriptions-item label="物品类型">{{ orderInfo.itemType }}</el-descriptions-item>
              <el-descriptions-item label="重量">{{ orderInfo.weight }} kg</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ orderInfo.createTime }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>配送信息</span>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="飞行器">{{ flightInfo.deviceNo }}</el-descriptions-item>
              <el-descriptions-item label="机长">{{ flightInfo.pilotName }}</el-descriptions-item>
              <el-descriptions-item label="计划编号">{{ flightInfo.planNo }}</el-descriptions-item>
              <el-descriptions-item label="起飞时间">{{ flightInfo.takeoffTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="预计送达">{{ flightInfo.estimatedArrival || '-' }}</el-descriptions-item>
              <el-descriptions-item label="当前位置">{{ flightInfo.currentLocation || '-' }}</el-descriptions-item>
              <el-descriptions-item label="飞行高度">{{ flightInfo.altitude || '-' }} 米</el-descriptions-item>
              <el-descriptions-item label="剩余电量">
                <el-progress
                  :percentage="flightInfo.battery"
                  :color="getBatteryColor(flightInfo.battery)"
                  :status="flightInfo.battery < 20 ? 'exception' : ''"
                />
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <!-- 地图和轨迹 -->
      <el-row :gutter="20" class="mb-20">
        <el-col :span="16">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>实时位置</span>
                <el-button-group size="small">
                  <el-button :type="mapMode === 'realtime' ? 'primary' : ''" @click="mapMode = 'realtime'">
                    实时
                  </el-button>
                  <el-button :type="mapMode === 'trajectory' ? 'primary' : ''" @click="mapMode = 'trajectory'">
                    轨迹
                  </el-button>
                </el-button-group>
              </div>
            </template>
            <div id="tracking-map" style="height: 500px;"></div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>
              <span>飞行参数</span>
            </template>
            <div class="flight-params">
              <div class="param-item">
                <div class="param-label">速度</div>
                <div class="param-value">{{ flightInfo.speed || 0 }} m/s</div>
              </div>
              <div class="param-item">
                <div class="param-label">高度</div>
                <div class="param-value">{{ flightInfo.altitude || 0 }} m</div>
              </div>
              <div class="param-item">
                <div class="param-label">航向</div>
                <div class="param-value">{{ flightInfo.heading || 0 }}°</div>
              </div>
              <div class="param-item">
                <div class="param-label">已飞行</div>
                <div class="param-value">{{ flightInfo.distance || 0 }} km</div>
              </div>
              <div class="param-item">
                <div class="param-label">剩余航程</div>
                <div class="param-value">{{ flightInfo.remainingDistance || 0 }} km</div>
              </div>
              <div class="param-item">
                <div class="param-label">电量</div>
                <div class="param-value">{{ flightInfo.battery || 0 }}%</div>
              </div>
            </div>

            <el-divider />

            <div class="status-alerts">
              <el-alert
                v-if="flightInfo.battery < 20"
                title="电量预警"
                type="error"
                :closable="false"
                show-icon
              >
                飞行器电量低于20%，请关注返航情况
              </el-alert>
              <el-alert
                v-if="alerts.length > 0"
                title="异常告警"
                type="warning"
                :closable="false"
                show-icon
                style="margin-top: 10px"
              >
                {{ alerts[0].content }}
              </el-alert>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 订单状态时间线 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>订单状态变更</span>
            </template>
            <el-timeline>
              <el-timeline-item
                v-for="(record, index) in statusHistory"
                :key="index"
                :timestamp="record.time"
                :type="record.type"
                placement="top"
              >
                <el-card>
                  <h4>{{ record.status }}</h4>
                  <p v-if="record.remark">{{ record.remark }}</p>
                  <p class="text-muted">操作人：{{ record.operator }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>异常记录</span>
            </template>
            <el-timeline v-if="alerts.length > 0">
              <el-timeline-item
                v-for="(alert, index) in alerts"
                :key="index"
                :timestamp="alert.time"
                :type="alert.level === '紧急' ? 'danger' : 'warning'"
                placement="top"
              >
                <el-card>
                  <el-tag :type="alert.level === '紧急' ? 'danger' : 'warning'" size="small">
                    {{ alert.level }}
                  </el-tag>
                  <p style="margin-top: 10px">{{ alert.content }}</p>
                  <p class="text-muted" v-if="alert.handler">处理人：{{ alert.handler }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无异常记录" />
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderAPI, aircraftAPI, flightPlanAPI } from '@/api'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

/**
 * 订单跟踪页面：全部来自后端
 *   订单   GET /api/orders/{id}
 *   航线   GET /api/orders/{id}/route
 *   飞行器 GET /api/aircraft/{id}
 *   计划   GET /api/flight-plans（按飞行器注册号匹配计划编号）
 * 状态时间线、告警均由真实数据生成；无 WebSocket，采用 30 秒轮询刷新实时位置。
 */

const route = useRoute()
const router = useRouter()

const orderId = route.params.id
const mapMode = ref('realtime')

let map = null
let pickupMarker = null
let deliveryMarker = null
let aircraftMarker = null
let routeLine = null

const orderInfo = reactive({
  orderNo: '',
  status: 1,
  customerName: '',
  acceptedBy: '',
  sendAddr: '',
  recvAddr: '',
  itemType: '',
  weight: null,
  createTime: ''
})

const flightInfo = reactive({
  deviceNo: '-',
  pilotName: '-',
  planNo: '-',
  takeoffTime: '-',
  estimatedArrival: '-',
  currentLocation: '-',
  altitude: null,
  speed: null,
  heading: null,
  distance: null,
  remainingDistance: null,
  battery: null
})

const statusHistory = ref([])
const alerts = ref([])

// 后端状态 → 页面展示编码
const STATUS_CODE = {
  PENDING: 1,
  ACCEPTED: 2,
  DISPATCHED: 2,
  IN_PROGRESS: 3,
  ARRIVED: 4,
  COMPLETED: 5,
  CANCELLED: 6
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

const haversineKm = (lat1, lng1, lat2, lng2) => {
  if ([lat1, lng1, lat2, lng2].some((v) => v === null || v === undefined)) return null
  const r = 6371
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return Math.round(2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 100) / 100
}

// 方位角（正北为 0，顺时针）
const bearing = (lat1, lng1, lat2, lng2) => {
  if ([lat1, lng1, lat2, lng2].some((v) => v === null || v === undefined)) return null
  const toRad = (d) => (d * Math.PI) / 180
  const y = Math.sin(toRad(lng2 - lng1)) * Math.cos(toRad(lat2))
  const x =
    Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
    Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(toRad(lng2 - lng1))
  return Math.round(((Math.atan2(y, x) * 180) / Math.PI + 360) % 360)
}

let currentOrder = null

// ===== 地图初始化 =====
const initMap = () => {
  if (map) return

  // 初始化地图（温州市中心）
  map = L.map('tracking-map').setView([28.0, 120.65], 11)

  // 添加高德地图图层
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    attribution: '&copy; 高德地图',
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18
  }).addTo(map)
}

// ===== 更新地图标记 =====
const updateMapMarkers = async () => {
  if (!map || !currentOrder) return

  const order = currentOrder

  // 取件地标记（红色）
  if (order.sendLat && order.sendLng) {
    if (pickupMarker) {
      pickupMarker.setLatLng([order.sendLat, order.sendLng])
    } else {
      pickupMarker = L.marker([order.sendLat, order.sendLng], {
        icon: L.divIcon({
          className: 'custom-marker',
          html: '<div style="background: #f56c6c; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; border: 2px solid white; box-shadow: 0 2px 5px rgba(0,0,0,0.3);">取</div>',
          iconSize: [30, 30]
        })
      }).addTo(map)
      pickupMarker.bindPopup(`<b>取件地</b><br>${order.sendAddr}`)
    }
  }

  // 送达地标记（绿色）
  if (order.recvLat && order.recvLng) {
    if (deliveryMarker) {
      deliveryMarker.setLatLng([order.recvLat, order.recvLng])
    } else {
      deliveryMarker = L.marker([order.recvLat, order.recvLng], {
        icon: L.divIcon({
          className: 'custom-marker',
          html: '<div style="background: #67c23a; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; border: 2px solid white; box-shadow: 0 2px 5px rgba(0,0,0,0.3);">送</div>',
          iconSize: [30, 30]
        })
      }).addTo(map)
      deliveryMarker.bindPopup(`<b>送达地</b><br>${order.recvAddr}`)
    }
  }

  // 规划航线（虚线）
  if (order.sendLat && order.sendLng && order.recvLat && order.recvLng) {
    if (routeLine) {
      map.removeLayer(routeLine)
    }
    routeLine = L.polyline(
      [[order.sendLat, order.sendLng], [order.recvLat, order.recvLng]],
      { color: '#409eff', weight: 2, dashArray: '5, 10', opacity: 0.6 }
    ).addTo(map)
  }

  // 飞行器位置（飞行中时）
  if (order.aircraftId && orderInfo.status === 3) {
    try {
      const aircraft = await aircraftAPI.getDetail(order.aircraftId)
      if (aircraft.latitude && aircraft.longitude) {
        if (aircraftMarker) {
          aircraftMarker.setLatLng([aircraft.latitude, aircraft.longitude])
        } else {
          aircraftMarker = L.marker([aircraft.latitude, aircraft.longitude], {
            icon: L.divIcon({
              className: 'custom-marker',
              html: '<div style="background: #409eff; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 18px; border: 2px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.4);">✈️</div>',
              iconSize: [36, 36]
            })
          }).addTo(map)
          aircraftMarker.bindPopup(`<b>飞行器位置</b><br>${aircraft.registrationNumber}<br>高度: ${aircraft.currentAltitude || 0}m<br>电量: ${aircraft.batteryLevel || 0}%`)
        }
      }
    } catch (error) {
      console.error('获取飞行器位置失败:', error)
    }
  }

  // 调整地图视野以包含所有标记
  const bounds = []
  if (order.sendLat && order.sendLng) bounds.push([order.sendLat, order.sendLng])
  if (order.recvLat && order.recvLng) bounds.push([order.recvLat, order.recvLng])
  if (bounds.length > 0) {
    map.fitBounds(bounds, { padding: [50, 50] })
  }
}

// ===== 订单基本信息 =====
const loadOrder = async () => {
  try {
    // 兼容两种入口：/order/tracking/{订单id} 或 /order/tracking/{订单号}
    const key = String(orderId)
    const order = /^\d+$/.test(key)
      ? await orderAPI.getDetail(key)
      : await orderAPI.getByOrderNo(key)
    currentOrder = order
    Object.assign(orderInfo, {
      orderNo: order.orderNo,
      status: STATUS_CODE[order.status] ?? 1,
      customerName: order.customerName || '-',
      acceptedBy: order.acceptedBy || '-',
      sendAddr: order.sendAddr,
      recvAddr: order.recvAddr,
      itemType: order.itemType,
      weight: order.weight,
      createTime: formatTime(order.createTime)
    })
  } catch (error) {
    console.error('加载订单失败:', error)
    ElMessage.error('加载订单失败')
  }
}

// ===== 飞行信息 =====
const loadFlightInfo = async () => {
  const order = currentOrder
  if (!order) return

  flightInfo.pilotName = order.pilotName || '-'
  flightInfo.distance = order.distance ?? null
  flightInfo.takeoffTime = formatTime(order.actualStartTime || order.scheduledTime)

  // 预计送达 = 起飞时间 + 预计时长
  const start = order.actualStartTime || order.scheduledTime
  if (start && order.estimatedDuration != null) {
    const arrival = new Date(new Date(start).getTime() + order.estimatedDuration * 60 * 1000)
    flightInfo.estimatedArrival = formatTime(arrival)
  } else {
    flightInfo.estimatedArrival = '-'
  }

  flightInfo.heading = bearing(order.sendLat, order.sendLng, order.recvLat, order.recvLng)

  if (!order.aircraftId) {
    flightInfo.deviceNo = '-'
    flightInfo.currentLocation = '尚未调度飞行器'
    flightInfo.remainingDistance = order.distance ?? null
    return
  }

  try {
    const aircraft = await aircraftAPI.getDetail(order.aircraftId)
    flightInfo.deviceNo = aircraft.registrationNumber
    flightInfo.altitude = aircraft.currentAltitude
    flightInfo.speed = aircraft.currentSpeed
    flightInfo.battery = aircraft.batteryLevel ?? null
    flightInfo.currentLocation =
      aircraft.latitude != null && aircraft.longitude != null
        ? `${aircraft.latitude}, ${aircraft.longitude}`
        : '位置未知'
    flightInfo.remainingDistance = haversineKm(
      aircraft.latitude,
      aircraft.longitude,
      order.recvLat,
      order.recvLng
    )

    // 匹配该飞行器对应的飞行计划编号
    try {
      const plans = (await flightPlanAPI.getList()) || []
      const matched = plans.find((p) => p.aircraftRegistration === aircraft.registrationNumber)
      flightInfo.planNo = matched ? matched.planNumber : '-'
    } catch (e) {
      flightInfo.planNo = '-'
    }
  } catch (error) {
    console.error('加载飞行器信息失败:', error)
  }
}

// ===== 状态时间线（按订单真实时间戳生成）=====
const buildStatusHistory = (order) => {
  const events = []

  events.push({
    time: order.createTime,
    status: '订单创建',
    type: 'info',
    operator: order.customerName || '客户',
    remark: `${order.itemType || '订单'}，寄往 ${order.recvAddr || '-'}`
  })

  if (order.acceptedTime) {
    events.push({
      time: order.acceptedTime,
      status: '平台接单',
      type: 'success',
      operator: order.acceptedBy || '运营中心',
      remark: '平台已确认接单'
    })
  }

  if (order.dispatchedTime) {
    events.push({
      time: order.dispatchedTime,
      status: '调度完成',
      type: 'success',
      operator: '调度系统',
      remark: `已分配飞行器 ${order.deviceNo || '-'}${order.pilotName ? '，机长：' + order.pilotName : ''}`
    })
  }

  if (order.actualStartTime) {
    events.push({
      time: order.actualStartTime,
      status: '飞行器起飞',
      type: 'primary',
      operator: order.pilotName || '机长',
      remark: `从 ${order.sendAddr || '-'} 起飞`
    })
  }

  if (order.actualEndTime) {
    const cancelled = order.status === 'CANCELLED'
    events.push({
      time: order.actualEndTime,
      status: cancelled ? '订单取消' : '订单完成',
      type: cancelled ? 'danger' : 'success',
      operator: order.pilotName || '系统',
      remark: cancelled ? '订单已取消' : `已送达 ${order.recvAddr || '-'}`
    })
  }

  // 按时间倒序（最新在前）
  return events
    .filter((e) => e.time)
    .sort((a, b) => new Date(b.time) - new Date(a.time))
    .map((e) => ({ ...e, time: formatTime(e.time) }))
}

// ===== 告警（由真实数据推导）=====
const buildAlerts = async () => {
  const list = []
  const order = currentOrder
  if (!order) {
    alerts.value = list
    return
  }

  try {
    const route = await orderAPI.getRoute(orderId)
    if (route?.noFlyZones > 0) {
      list.push({
        time: formatTime(new Date()),
        level: '严重',
        content: `航线途经 ${route.noFlyZones} 个禁飞区，请人工确认`,
        handler: '待处理'
      })
    }
  } catch (e) {
    // 忽略
  }

  if (order.aircraftId) {
    try {
      const aircraft = await aircraftAPI.getDetail(order.aircraftId)
      if (aircraft.batteryLevel != null && aircraft.batteryLevel <= 30) {
        list.push({
          time: formatTime(new Date()),
          level: aircraft.batteryLevel <= 15 ? '紧急' : '一般',
          content: `飞行器 ${aircraft.registrationNumber} 电量 ${aircraft.batteryLevel}%，建议就近返航`,
          handler: '待处理'
        })
      }
      if (aircraft.status === 'MAINTENANCE') {
        list.push({
          time: formatTime(new Date()),
          level: '一般',
          content: `飞行器 ${aircraft.registrationNumber} 处于维护状态`,
          handler: '建议更换飞行器'
        })
      }
    } catch (e) {
      // 忽略
    }
  }

  alerts.value = list
}

const loadAll = async () => {
  await loadOrder()
  await loadFlightInfo()
  if (currentOrder) {
    statusHistory.value = buildStatusHistory(currentOrder)
  }
  await buildAlerts()
  // 更新地图标记
  await updateMapMarkers()
}

// 刷新数据
const refreshData = async () => {
  await loadAll()
  ElMessage.success('数据已刷新')
}

const goBack = () => {
  router.back()
}

// ===== 工具函数 =====
const getStatusType = (status) => {
  const types = { 1: 'info', 2: 'warning', 3: 'primary', 4: 'success', 5: 'success', 6: 'danger' }
  return types[status] || ''
}

const getStatusName = (status) => {
  const names = { 1: '待接单', 2: '待调度', 3: '飞行中', 4: '已送达', 5: '已完成', 6: '已取消' }
  return names[status] || ''
}

const getBatteryColor = (battery) => {
  if (battery == null) return '#909399'
  if (battery < 20) return '#f56c6c'
  if (battery < 50) return '#e6a23c'
  return '#67c23a'
}

// 30 秒轮询刷新实时位置（真实数据，非随机模拟）
let updateTimer = null

onMounted(async () => {
  // 必须带订单 id/订单号进入，直接访问菜单会缺参数
  if (!orderId) {
    ElMessage.warning('请从订单列表点击「跟踪」进入本页')
    router.replace('/order/list')
    return
  }
  // 初始化地图
  initMap()
  await loadAll()
  updateTimer = setInterval(() => {
    if (orderInfo.status === 3) {
      loadFlightInfo()
      updateMapMarkers()
    }
  }, 30000)
})

onUnmounted(() => {
  if (updateTimer) clearInterval(updateTimer)
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped>
.order-tracking {
  padding: 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info h2 {
  margin: 0 0 10px 0;
}

.mb-20 {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.flight-params {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.param-item {
  text-align: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.param-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.param-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.status-alerts {
  margin-top: 20px;
}

.text-muted {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}
</style>
