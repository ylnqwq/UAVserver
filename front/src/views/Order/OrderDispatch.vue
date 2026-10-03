<template>
  <div class="order-dispatch">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>订单调度</span>
          <el-button @click="goBack">
            <el-icon><Back /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <!-- 订单信息 -->
      <el-alert
        :title="`订单号：${orderInfo.orderNo}`"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #default>
          <div>
            <strong>寄件地址：</strong>{{ orderInfo.sendAddr }}
          </div>
          <div>
            <strong>收件地址：</strong>{{ orderInfo.recvAddr }}
          </div>
          <div>
            <strong>物品信息：</strong>{{ orderInfo.itemType }} / {{ orderInfo.weight }}kg
          </div>
          <div>
            <strong>时效要求：</strong>
            <el-tag v-if="orderInfo.urgency" type="danger" size="small">加急</el-tag>
            <span v-else>普通</span>
          </div>
        </template>
      </el-alert>

      <!-- 调度步骤 -->
      <el-steps :active="currentStep" finish-status="success" style="margin-bottom: 30px">
        <el-step title="选择飞行器" />
        <el-step title="规划路线" />
        <el-step title="分配机长" />
        <el-step title="确认调度" />
      </el-steps>

      <!-- 步骤1: 选择飞行器 -->
      <div v-show="currentStep === 0">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>可用飞行器</span>
              <el-button size="small" @click="loadAvailableAircraft">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-alert
            v-if="recommendedAircraft"
            title="系统推荐"
            type="success"
            :closable="false"
            style="margin-bottom: 20px"
          >
            根据订单重量、配送距离和飞行器状态，推荐使用 <strong>{{ recommendedAircraft.deviceNo }}</strong>
          </el-alert>

          <el-alert
            v-else
            title="暂无推荐"
            type="info"
            :closable="false"
            style="margin-bottom: 20px"
          >
            暂无可用飞行器，请稍后刷新
          </el-alert>

          <el-table
            :data="availableAircraft"
            @row-click="handleSelectAircraft"
            highlight-current-row
            v-loading="loading"
          >
            <el-table-column type="index" width="50" />
            <el-table-column prop="deviceNo" label="飞行器编号" width="120" />
            <el-table-column prop="model" label="型号" width="120" />
            <el-table-column prop="maxLoad" label="最大载重(kg)" width="120" />
            <el-table-column prop="maxRange" label="最大航程(km)" width="120" />
            <el-table-column prop="battery" label="电量" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.battery" :color="getBatteryColor(row.battery)" />
              </template>
            </el-table-column>
            <el-table-column prop="location" label="当前位置" show-overflow-tooltip />
            <el-table-column prop="distance" label="距起点(km)" width="120" />
            <el-table-column label="推荐" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.recommended" type="success" size="small">推荐</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click.stop="selectAircraft(row)"
                  :disabled="row.selected"
                >
                  {{ row.selected ? '已选择' : '选择' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <!-- 步骤2: 规划路线 -->
      <div v-show="currentStep === 1">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>路线规划</span>
              <el-button-group size="small">
                <el-button :type="routeMode === 'auto' ? 'primary' : ''" @click="routeMode = 'auto'">
                  智能推荐
                </el-button>
                <el-button :type="routeMode === 'manual' ? 'primary' : ''" @click="routeMode = 'manual'">
                  手动规划
                </el-button>
              </el-button-group>
            </div>
          </template>

          <el-row :gutter="20">
            <el-col :span="16">
              <div id="route-map" style="height: 500px;"></div>
            </el-col>

            <el-col :span="8">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="起点">{{ orderInfo.sendAddr }}</el-descriptions-item>
                <el-descriptions-item label="终点">{{ orderInfo.recvAddr }}</el-descriptions-item>
                <el-descriptions-item label="预计距离">{{ routeInfo.distance }} km</el-descriptions-item>
                <el-descriptions-item label="预计时长">{{ routeInfo.duration }} 分钟</el-descriptions-item>
                <el-descriptions-item label="飞行高度">{{ routeInfo.altitude }} 米</el-descriptions-item>
                <el-descriptions-item label="途经禁飞区">{{ routeInfo.noFlyZones }} 个</el-descriptions-item>
                <el-descriptions-item label="已添加航点">{{ waypoints.length }} 个</el-descriptions-item>
              </el-descriptions>

              <el-alert
                v-if="routeMode === 'manual'"
                title="手动规划提示"
                type="info"
                :closable="false"
                style="margin-top: 15px"
              >
                <div style="font-size: 12px; line-height: 1.8;">
                  • 点击地图添加航点<br>
                  • 拖拽航点可调整位置<br>
                  • 右键点击航点删除
                </div>
              </el-alert>

              <el-alert
                v-if="routeInfo.noFlyZones > 0"
                title="路线预警"
                type="warning"
                :closable="false"
                style="margin-top: 15px"
              >
                当前路线途经 {{ routeInfo.noFlyZones }} 个禁飞区，建议调整路线
              </el-alert>

              <el-button
                v-if="routeMode === 'manual' && waypoints.length > 0"
                type="danger"
                plain
                style="width: 100%; margin-top: 15px"
                @click="clearWaypoints"
              >
                <el-icon><Delete /></el-icon>
                清除所有航点
              </el-button>

              <el-button
                type="primary"
                style="width: 100%; margin-top: 15px"
                @click="optimizeRoute"
              >
                <el-icon><Position /></el-icon>
                优化路线
              </el-button>
            </el-col>
          </el-row>
        </el-card>
      </div>

      <!-- 步骤3: 分配机长 -->
      <div v-show="currentStep === 2">
        <el-card shadow="hover">
          <template #header>
            <span>分配机长</span>
          </template>

          <el-alert
            v-if="recommendedPilot"
            title="系统推荐"
            type="success"
            :closable="false"
            style="margin-bottom: 20px"
          >
            根据机长资质、工作量和位置，推荐分配给 <strong>{{ recommendedPilot.name }}</strong>
          </el-alert>

          <el-alert
            v-else
            title="暂无推荐"
            type="info"
            :closable="false"
            style="margin-bottom: 20px"
          >
            暂无可用机长，请稍后刷新
          </el-alert>

          <el-table
            :data="availablePilots"
            @row-click="handleSelectPilot"
            highlight-current-row
            v-loading="loading"
          >
            <el-table-column type="index" width="50" />
            <el-table-column prop="name" label="机长姓名" width="120" />
            <el-table-column prop="phone" label="联系方式" width="150" />
            <el-table-column prop="certNo" label="驾驶证号" width="150" />
            <el-table-column prop="flightHours" label="飞行时长(h)" width="120" />
            <el-table-column prop="todayTasks" label="今日任务" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'available' ? 'success' : 'info'" size="small">
                  {{ row.status === 'available' ? '空闲' : '忙碌' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="推荐" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.recommended" type="success" size="small">推荐</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click.stop="selectPilot(row)"
                  :disabled="row.selected || row.status !== 'available'"
                >
                  {{ row.selected ? '已选择' : '选择' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <!-- 步骤4: 确认调度 -->
      <div v-show="currentStep === 3">
        <el-card shadow="hover">
          <template #header>
            <span>确认调度方案</span>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ orderInfo.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ orderInfo.customerName }}</el-descriptions-item>
            <el-descriptions-item label="飞行器">{{ selectedAircraft?.deviceNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="机长">{{ selectedPilot?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预计距离">{{ routeInfo.distance }} km</el-descriptions-item>
            <el-descriptions-item label="预计时长">{{ routeInfo.duration }} 分钟</el-descriptions-item>
            <el-descriptions-item label="计划起飞时间">
              <el-date-picker
                v-model="dispatchForm.takeoffTime"
                type="datetime"
                placeholder="选择起飞时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                :disabled-date="disabledDate"
              />
            </el-descriptions-item>
            <el-descriptions-item label="预计送达时间">
              {{ estimatedArrival }}
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">
              <el-input
                v-model="dispatchForm.remark"
                type="textarea"
                :rows="2"
                placeholder="调度备注（可选）"
              />
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            title="请确认调度方案无误后提交"
            type="warning"
            :closable="false"
            style="margin-top: 20px"
          />
        </el-card>
      </div>

      <!-- 操作按钮 -->
      <div class="actions">
        <el-button v-if="currentStep > 0" @click="currentStep--">
          <el-icon><Back /></el-icon>
          上一步
        </el-button>
        <el-button
          v-if="currentStep < 3"
          type="primary"
          @click="nextStep"
          :disabled="!canNext()"
        >
          下一步
          <el-icon><Right /></el-icon>
        </el-button>
        <el-button
          v-if="currentStep === 3"
          type="success"
          @click="submitDispatch"
          :loading="submitting"
        >
          <el-icon><Check /></el-icon>
          提交调度
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderAPI, aircraftAPI, userAPI, airspaceAPI } from '@/api'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

/**
 * 订单调度页面：真实数据
 *   订单   GET  /api/orders/{id}
 *   航线   GET  /api/orders/{id}/route   （按坐标算距离/时长，并检测途经禁飞区）
 *   飞行器 GET  /api/aircraft/status/AVAILABLE（为空则取全部）
 *   机长   GET  /api/users/role/PILOT
 *   提交   POST /api/orders/{id}/dispatch + PUT /api/orders/{id}（计划起飞时间/备注）
 */

const route = useRoute()
const router = useRouter()

const orderId = route.query.id

const loading = ref(false)
const submitting = ref(false)
const currentStep = ref(0)
const routeMode = ref('auto')

let map = null
let pickupMarker = null
let deliveryMarker = null
let routeLine = null
let waypointMarkers = []
let airspaceLayerGroup = null
const waypoints = ref([])
const airspaceZones = ref([])

const orderInfo = reactive({
  orderNo: '',
  customerName: '',
  sendAddr: '',
  recvAddr: '',
  itemType: '',
  weight: null,
  urgency: false,
  sendLat: null,
  sendLng: null,
  recvLat: null,
  recvLng: null
})

const availableAircraft = ref([])
const availablePilots = ref([])

const routeInfo = reactive({
  distance: 0,
  duration: 0,
  altitude: 120,
  noFlyZones: 0
})

const dispatchForm = reactive({
  takeoffTime: '',
  remark: ''
})

const selectedAircraft = ref(null)
const selectedPilot = ref(null)

const recommendedAircraft = computed(() => {
  return availableAircraft.value.find(a => a.recommended) || null
})

const recommendedPilot = computed(() => {
  return availablePilots.value.find(p => p.recommended) || null
})

const estimatedArrival = computed(() => {
  if (!dispatchForm.takeoffTime) return '-'
  const takeoff = new Date(dispatchForm.takeoffTime)
  const arrival = new Date(takeoff.getTime() + (routeInfo.duration || 0) * 60 * 1000)
  return arrival.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
})

// ===== 地图初始化 =====
const initMap = () => {
  if (map) return

  // 初始化地图（温州市中心）
  map = L.map('route-map').setView([28.0, 120.65], 11)

  // 添加高德地图图层
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    attribution: '&copy; 高德地图',
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18
  }).addTo(map)

  // 创建空域图层组
  airspaceLayerGroup = L.layerGroup().addTo(map)

  // 监听地图点击事件（手动规划模式）
  map.on('click', (e) => {
    if (routeMode.value === 'manual') {
      addWaypoint(e.latlng.lat, e.latlng.lng)
    }
  })
}

// ===== 加载空域数据 =====
const loadAirspaceZones = async () => {
  try {
    const mapData = await airspaceAPI.getMapData()
    const zones = mapData?.zones || []

    // 转换zone数据格式
    airspaceZones.value = zones.map(zone => ({
      id: zone.id,
      name: zone.name,
      type: zone.type,
      latitude: zone.latitude,
      longitude: zone.longitude,
      radius: zone.radius,
      shapeType: zone.shapeType || 'circle',
      coordinates: zone.coordinates,
      minAltitude: zone.minAltitude,
      maxAltitude: zone.maxAltitude,
      description: zone.description
    }))

    renderAirspaceZones()
  } catch (error) {
    console.error('加载空域数据失败:', error)
  }
}

// ===== 渲染空域区域 =====
const renderAirspaceZones = () => {
  if (!map || !airspaceLayerGroup) return

  // 清除旧图层
  airspaceLayerGroup.clearLayers()

  airspaceZones.value.forEach(zone => {
    let shape = null
    let color, fillColor, fillOpacity

    // 根据空域类型设置颜色
    switch (zone.type) {
      case 'nofly':
      case 'NO_FLY':
        color = '#f56c6c'
        fillColor = '#f56c6c'
        fillOpacity = 0.3
        break
      case 'restricted':
      case 'RESTRICTED':
        color = '#e6a23c'
        fillColor = '#e6a23c'
        fillOpacity = 0.2
        break
      case 'temporary':
      case 'TEMPORARY_CONTROL':
        color = '#ff9800'
        fillColor = '#ff9800'
        fillOpacity = 0.25
        break
      default:
        color = '#909399'
        fillColor = '#909399'
        fillOpacity = 0.15
    }

    // 根据形状类型绘制
    if (zone.shapeType === 'rectangle' && zone.coordinates) {
      const coords = JSON.parse(zone.coordinates)
      shape = L.polygon(coords.map(c => [c.lat, c.lng]), {
        color: color,
        weight: 2,
        fillColor: fillColor,
        fillOpacity: fillOpacity
      }).addTo(airspaceLayerGroup)
    } else if (zone.shapeType === 'polygon' && zone.coordinates) {
      const coords = JSON.parse(zone.coordinates)
      shape = L.polygon(coords.map(c => [c.lat, c.lng]), {
        color: color,
        weight: 2,
        fillColor: fillColor,
        fillOpacity: fillOpacity
      }).addTo(airspaceLayerGroup)
    } else {
      // 默认圆形
      shape = L.circle([zone.latitude, zone.longitude], {
        color: color,
        weight: 2,
        fillColor: fillColor,
        fillOpacity: fillOpacity,
        radius: zone.radius
      }).addTo(airspaceLayerGroup)
    }

    const typeText = zone.type === 'nofly' || zone.type === 'NO_FLY' ? '禁飞区' :
                     zone.type === 'restricted' || zone.type === 'RESTRICTED' ? '限飞区' : '临时管制'

    const sizeText = zone.shapeType === 'circle'
      ? `半径: ${(zone.radius / 1000).toFixed(1)}km`
      : `形状: ${zone.shapeType === 'rectangle' ? '矩形' : '多边形'}`

    shape.bindPopup(`
      <div style="font-size: 13px;">
        <b>${zone.name}</b><br>
        类型: ${typeText}<br>
        ${sizeText}<br>
        ${zone.minAltitude != null ? `最低高度: ${zone.minAltitude}m<br>` : ''}
        ${zone.maxAltitude != null ? `最高高度: ${zone.maxAltitude}m<br>` : ''}
        ${zone.description ? `说明: ${zone.description}` : ''}
      </div>
    `)
  })
}

// ===== 添加航点 =====
const addWaypoint = (lat, lng) => {
  waypoints.value.push({ lat, lng })

  // 创建航点标记
  const marker = L.marker([lat, lng], {
    icon: L.divIcon({
      className: 'custom-marker',
      html: `<div style="background: #409eff; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; border: 2px solid white; box-shadow: 0 2px 5px rgba(0,0,0,0.3); font-size: 12px;">${waypoints.value.length}</div>`,
      iconSize: [24, 24]
    }),
    draggable: true
  }).addTo(map)

  // 支持拖拽航点
  marker.on('dragend', (e) => {
    const index = waypointMarkers.indexOf(marker)
    if (index !== -1) {
      const newLatLng = e.target.getLatLng()
      waypoints.value[index] = { lat: newLatLng.lat, lng: newLatLng.lng }
      updateRouteMap()
    }
  })

  // 右键删除航点
  marker.on('contextmenu', () => {
    const index = waypointMarkers.indexOf(marker)
    if (index !== -1) {
      map.removeLayer(marker)
      waypointMarkers.splice(index, 1)
      waypoints.value.splice(index, 1)
      updateRouteMap()
    }
  })

  waypointMarkers.push(marker)
  marker.bindPopup(`<b>航点 ${waypoints.value.length}</b><br>右键删除`)

  // 更新路线
  updateRouteMap()
}

// ===== 清除所有航点 =====
const clearWaypoints = () => {
  waypointMarkers.forEach(marker => map.removeLayer(marker))
  waypointMarkers = []
  waypoints.value = []
  updateRouteMap()
}

// ===== 更新地图标记 =====
const updateRouteMap = () => {
  if (!map) return

  // 清除旧路线
  if (routeLine) map.removeLayer(routeLine)

  // 取件地标记（红色）
  if (orderInfo.sendLat && orderInfo.sendLng) {
    if (!pickupMarker) {
      pickupMarker = L.marker([orderInfo.sendLat, orderInfo.sendLng], {
        icon: L.divIcon({
          className: 'custom-marker',
          html: '<div style="background: #f56c6c; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; border: 2px solid white; box-shadow: 0 2px 5px rgba(0,0,0,0.3);">起</div>',
          iconSize: [30, 30]
        })
      }).addTo(map)
      pickupMarker.bindPopup(`<b>起点</b><br>${orderInfo.sendAddr}`)
    }
  }

  // 送达地标记（绿色）
  if (orderInfo.recvLat && orderInfo.recvLng) {
    if (!deliveryMarker) {
      deliveryMarker = L.marker([orderInfo.recvLat, orderInfo.recvLng], {
        icon: L.divIcon({
          className: 'custom-marker',
          html: '<div style="background: #67c23a; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; border: 2px solid white; box-shadow: 0 2px 5px rgba(0,0,0,0.3);">终</div>',
          iconSize: [30, 30]
        })
      }).addTo(map)
      deliveryMarker.bindPopup(`<b>终点</b><br>${orderInfo.recvAddr}`)
    }
  }

  // 规划航线
  if (orderInfo.sendLat && orderInfo.sendLng && orderInfo.recvLat && orderInfo.recvLng) {
    // 构建路线坐标数组：起点 → 航点 → 终点
    const routeCoords = [
      [orderInfo.sendLat, orderInfo.sendLng],
      ...waypoints.value.map(wp => [wp.lat, wp.lng]),
      [orderInfo.recvLat, orderInfo.recvLng]
    ]

    // 检测路线是否途经禁飞区
    const noFlyCount = checkRouteNoFlyZones(routeCoords)
    routeInfo.noFlyZones = noFlyCount

    const lineColor = noFlyCount > 0 ? '#f56c6c' : '#409eff'

    routeLine = L.polyline(routeCoords, {
      color: lineColor,
      weight: 3,
      opacity: 0.7
    }).addTo(map)
  }

  // 调整地图视野
  const bounds = []
  if (orderInfo.sendLat && orderInfo.sendLng) bounds.push([orderInfo.sendLat, orderInfo.sendLng])
  if (orderInfo.recvLat && orderInfo.recvLng) bounds.push([orderInfo.recvLat, orderInfo.recvLng])
  waypoints.value.forEach(wp => bounds.push([wp.lat, wp.lng]))

  if (bounds.length > 0) {
    map.fitBounds(bounds, { padding: [50, 50] })
  }
}

// ===== 检测路线是否途经禁飞区 =====
const checkRouteNoFlyZones = (routeCoords) => {
  let count = 0
  const checkedZones = new Set()

  // 遍历路线的每一段
  for (let i = 0; i < routeCoords.length - 1; i++) {
    const start = routeCoords[i]
    const end = routeCoords[i + 1]

    // 检查每个禁飞区和限飞区
    airspaceZones.value.forEach(zone => {
      if (checkedZones.has(zone.id)) return
      // 检测禁飞区和限飞区
      if (zone.type !== 'nofly' && zone.type !== 'NO_FLY' &&
          zone.type !== 'restricted' && zone.type !== 'RESTRICTED') return

      // 检查路线段是否与禁飞区/限飞区相交
      if (isLineIntersectZone(start, end, zone)) {
        checkedZones.add(zone.id)
        count++
      }
    })
  }

  return count
}

// ===== 判断线段是否与区域相交 =====
const isLineIntersectZone = (start, end, zone) => {
  // 简化算法：检查线段上的多个采样点是否在区域内
  const samples = 20
  for (let i = 0; i <= samples; i++) {
    const t = i / samples
    const lat = start[0] + (end[0] - start[0]) * t
    const lng = start[1] + (end[1] - start[1]) * t

    if (isPointInZone(lat, lng, zone)) {
      return true
    }
  }
  return false
}

// ===== 判断点是否在区域内 =====
const isPointInZone = (lat, lng, zone) => {
  if (zone.shapeType === 'circle' || !zone.shapeType) {
    // 圆形：计算距离
    const distance = haversine(lat, lng, zone.latitude, zone.longitude)
    return distance <= zone.radius
  } else if (zone.shapeType === 'polygon' || zone.shapeType === 'rectangle') {
    // 多边形/矩形：使用射线法
    try {
      const coords = JSON.parse(zone.coordinates)
      return isPointInPolygon(lat, lng, coords)
    } catch (e) {
      return false
    }
  }
  return false
}

// ===== 计算两点距离（米）=====
const haversine = (lat1, lon1, lat2, lon2) => {
  const R = 6371000 // 地球半径（米）
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

// ===== 射线法判断点是否在多边形内 =====
const isPointInPolygon = (lat, lng, coords) => {
  let inside = false
  for (let i = 0, j = coords.length - 1; i < coords.length; j = i++) {
    const xi = coords[i].lng || coords[i].longitude
    const yi = coords[i].lat || coords[i].latitude
    const xj = coords[j].lng || coords[j].longitude
    const yj = coords[j].lat || coords[j].latitude

    const intersect = ((yi > lat) !== (yj > lat)) &&
      (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)
    if (intersect) inside = !inside
  }
  return inside
}

// ===== 工具函数 =====
const getBatteryColor = (battery) => {
  if (battery < 20) return '#f56c6c'
  if (battery < 50) return '#e6a23c'
  return '#67c23a'
}

const disabledDate = (time) => time.getTime() < Date.now()

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

// ===== 加载订单 =====
const loadOrder = async () => {
  if (!orderId) {
    ElMessage.warning('缺少订单参数')
    return
  }
  try {
    const order = await orderAPI.getDetail(orderId)
    Object.assign(orderInfo, {
      orderNo: order.orderNo,
      customerName: order.customerName,
      sendAddr: order.sendAddr,
      recvAddr: order.recvAddr,
      itemType: order.itemType,
      weight: order.weight,
      urgency: !!order.urgencyFlag,
      sendLat: order.sendLat,
      sendLng: order.sendLng,
      recvLat: order.recvLat,
      recvLng: order.recvLng
    })
  } catch (error) {
    console.error('加载订单失败:', error)
  }
}

// ===== 加载航线信息（后端按坐标计算）=====
const loadRouteInfo = async () => {
  try {
    const info = await orderAPI.getRoute(orderId)
    routeInfo.distance = info.distance ?? haversineKm(orderInfo.sendLat, orderInfo.sendLng, orderInfo.recvLat, orderInfo.recvLng) ?? 0
    routeInfo.duration = info.duration ?? 0
    routeInfo.altitude = info.altitude ?? 120
    routeInfo.noFlyZones = info.noFlyZones ?? 0
  } catch (error) {
    console.error('加载航线信息失败:', error)
  }
}

// ===== 加载可用飞行器 / 机长 =====
const loadAvailableAircraft = async () => {
  loading.value = true
  try {
    let aircraft
    try {
      aircraft = await aircraftAPI.getByStatus('AVAILABLE')
    } catch (e) {
      aircraft = []
    }
    if (!aircraft || aircraft.length === 0) {
      aircraft = (await aircraftAPI.getList()) || []
    }

    const list = aircraft.map((a) => ({
      id: a.id,
      deviceNo: a.registrationNumber,
      model: a.model,
      maxLoad: a.maxTakeoffWeight,
      maxRange: a.maxFlightDistance,
      battery: a.batteryLevel ?? 0,
      location:
        a.latitude != null && a.longitude != null ? `${a.latitude}, ${a.longitude}` : '位置未知',
      distance: haversineKm(orderInfo.sendLat, orderInfo.sendLng, a.latitude, a.longitude),
      selected: false
    }))

    // 推荐：距离最近且电量 ≥ 50%
    const sorted = [...list].sort((x, y) => (x.distance ?? 9999) - (y.distance ?? 9999))
    const recommended = sorted.find((a) => a.battery >= 50) || sorted[0]
    list.forEach((a) => (a.recommended = recommended ? a.id === recommended.id : false))
    availableAircraft.value = list

    const pilots = (await userAPI.getByRole('PILOT', { silent: true })) || []
    const pilotList = pilots.map((p) => ({
      id: p.id,
      name: p.realName || p.username,
      phone: p.phone,
      certNo: p.certNo,
      flightHours: p.flightHours ?? 0,
      todayTasks: p.todayTasks ?? 0,
      status: p.status === 'ACTIVE' ? 'available' : 'busy',
      selected: false
    }))
    const leastBusy = [...pilotList]
      .filter((p) => p.status === 'available')
      .sort((x, y) => x.todayTasks - y.todayTasks)[0]
    pilotList.forEach((p) => (p.recommended = leastBusy ? p.id === leastBusy.id : false))
    availablePilots.value = pilotList
  } catch (error) {
    console.error('加载可用资源失败:', error)
  } finally {
    loading.value = false
  }
}

// ===== 选择 =====
const selectAircraft = (aircraft) => {
  availableAircraft.value.forEach((a) => (a.selected = false))
  aircraft.selected = true
  selectedAircraft.value = aircraft
}

const handleSelectAircraft = (row) => {
  selectAircraft(row)
}

const selectPilot = (pilot) => {
  availablePilots.value.forEach((p) => (p.selected = false))
  pilot.selected = true
  selectedPilot.value = pilot
}

const handleSelectPilot = (row) => {
  if (row.status !== 'available') {
    ElMessage.warning('该机长当前不可用')
    return
  }
  selectPilot(row)
}

// ===== 重新计算航线（真实重新拉取，不是假的"优化"）=====
const optimizeRoute = async () => {
  await loadRouteInfo()
  ElMessage.success('已按最新数据重新计算航线')
}

// ===== 步骤控制 =====
const canNext = () => {
  if (currentStep.value === 0) return selectedAircraft.value !== null
  if (currentStep.value === 1) return routeInfo.noFlyZones === 0
  if (currentStep.value === 2) return selectedPilot.value !== null
  return false
}

const nextStep = () => {
  if (!canNext()) {
    ElMessage.warning('请完成当前步骤')
    return
  }
  currentStep.value++

  // 切换到路线规划步骤时更新地图
  if (currentStep.value === 1) {
    setTimeout(() => {
      if (map) {
        map.invalidateSize()
      }
      updateRouteMap()
    }, 100)
  }
}

// ===== 提交调度 =====
const submitDispatch = async () => {
  if (!dispatchForm.takeoffTime) {
    ElMessage.warning('请选择计划起飞时间')
    return
  }
  if (routeInfo.noFlyZones > 0) {
    ElMessage.warning('航线途经禁飞区，请调整后再提交')
    return
  }

  submitting.value = true
  try {
    await orderAPI.dispatch(orderId, {
      aircraftId: selectedAircraft.value?.id,
      pilotId: selectedPilot.value?.id,
      routeAltitude: routeInfo.altitude,
      estimatedDuration: routeInfo.duration
    })
    await orderAPI.update(orderId, {
      scheduledTime: dispatchForm.takeoffTime,
      notes: dispatchForm.remark || undefined
    })
    ElMessage.success('调度方案已提交')
    router.push('/order/list')
  } catch (error) {
    console.error('提交调度失败:', error)
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(async () => {
  // 本页必须带订单 id 进入（从订单列表点「调度」），直接访问菜单会缺参数
  if (!orderId) {
    ElMessage.warning('请从订单列表点击「调度」进入本页')
    router.replace('/order/list')
    return
  }
  // 初始化地图
  initMap()
  // 加载空域数据
  await loadAirspaceZones()
  await loadOrder()
  await loadRouteInfo()
  await loadAvailableAircraft()
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped>
.order-dispatch {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  margin-top: 30px;
  text-align: center;
}

.actions .el-button {
  min-width: 120px;
}
</style>
