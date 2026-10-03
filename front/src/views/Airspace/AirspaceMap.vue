<template>
  <div class="airspace-map">
    <el-card shadow="never" :body-style="{ padding: 0 }">
      <template #header>
        <div class="header-toolbar">
          <span>空域地图</span>
          <div class="toolbar-right">
            <el-button @click="refreshMap" size="small">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div class="map-layout">
        <!-- 左侧控制面板 -->
        <div class="control-panel">
          <el-card shadow="hover">
            <template #header>
              <span>图层控制</span>
            </template>
            <el-checkbox-group v-model="activeLayers" @change="updateLayers">
              <el-checkbox value="nofly">禁飞区</el-checkbox>
              <el-checkbox value="restricted">限飞区</el-checkbox>
              <el-checkbox value="temporary">临时管制</el-checkbox>
              <el-checkbox value="aircraft">飞行器</el-checkbox>
              <el-checkbox value="stations">场站</el-checkbox>
            </el-checkbox-group>
          </el-card>

          <el-card shadow="hover" style="margin-top: 10px;">
            <template #header>
              <span>空域信息</span>
            </template>
            <el-collapse accordion>
              <el-collapse-item title="禁飞区">
                <el-tag
                  v-for="item in airspaceData.noFlyZones"
                  :key="item.id"
                  type="danger"
                  size="small"
                  @click="focusZone(item)"
                  style="cursor: pointer; margin: 5px;"
                >
                  {{ item.name }}
                </el-tag>
              </el-collapse-item>
              <el-collapse-item title="限飞区">
                <el-tag
                  v-for="item in airspaceData.restrictedZones"
                  :key="item.id"
                  type="warning"
                  size="small"
                  @click="focusZone(item)"
                  style="cursor: pointer; margin: 5px;"
                >
                  {{ item.name }}
                </el-tag>
              </el-collapse-item>
            </el-collapse>
          </el-card>
        </div>

        <!-- 地图容器 -->
        <div class="map-content">
          <div ref="mapContainer" style="width: 100%; height: 100%;"></div>

          <!-- 可拖动浮动信息卡 -->
          <div
            v-if="cardVisible"
            class="flight-card"
            :style="{ left: cardPos.x + 'px', top: cardPos.y + 'px' }"
            @mousedown.stop.prevent="onDragStart"
          >
            <div class="flight-card__head">
              <span>飞行器 {{ cardAircraft.deviceNo || '-' }}</span>
              <span class="flight-card__close" @mousedown.stop="() => {}" @click.stop="closeCard">✕</span>
            </div>
            <div class="flight-card__body">
              <div>高度: {{ cardAircraft.altitude }}米 · 速度: {{ cardAircraft.speed }}m/s</div>
              <div>状态: {{ statusText(cardAircraft.status) }}</div>
              <template v-if="cardRoute && cardRoute.hasRoute">
                <div class="flight-card__sep"></div>
                <div class="flight-card__title">航线（执行中）</div>
                <div>订单: {{ cardRoute.orderNo }}</div>
                <div>起点: {{ cardRoute.departureName }}</div>
                <div>终点: {{ cardRoute.arrivalName }}</div>
                <div>航线里程: {{ cardRoute.totalDistance }}km（已飞 {{ cardRoute.flownDistance }} · 剩余 {{ cardRoute.remainDistance }}）</div>
                <div>进度: {{ cardRoute.progress }}% · 计划高度 {{ cardRoute.routeAltitude }}米</div>
                <div v-if="cardRoute.avoidedCount" class="flight-card__avoid">已绕开 {{ cardRoute.avoidedCount }} 个管制区</div>
                <div class="flight-card__eta">预计 {{ cardRoute.etaMinutes }} 分钟后到达</div>
              </template>
              <div v-else-if="cardMessage" class="flight-card__empty">{{ cardMessage }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { airspaceAPI, aircraftAPI } from '@/api'

const mapContainer = ref(null)
const activeLayers = ref(['nofly', 'restricted', 'temporary', 'aircraft', 'stations'])

let map = null
const layers = {
  nofly: [],
  restricted: [],
  temporary: [],
  aircraft: [],
  stations: [],
  route: []
}

// 空域数据（从后端加载）
const airspaceData = reactive({
  noFlyZones: [],
  restrictedZones: [],
  temporaryZones: [],
  aircraft: [],
  stations: []
})

// 从后端加载地图数据
const loadMapData = async () => {
  try {
    // 空域 + 场站
    const mapData = await airspaceAPI.getMapData()
    const zones = mapData?.zones || []
    const stations = mapData?.stations || []

    // 飞行器实时位置
    const aircraftData = (await aircraftAPI.getFlying()) || []

    // 清空现有数据
    airspaceData.noFlyZones = []
    airspaceData.restrictedZones = []
    airspaceData.temporaryZones = []

    // 按类型分类空域数据
    zones.forEach(zone => {
      const zoneData = {
        id: zone.id,
        name: zone.name,
        lat: zone.latitude,
        lng: zone.longitude,
        radius: zone.radius,
        shapeType: zone.shapeType || 'circle',
        coordinates: zone.coordinates
      }

      if (zone.type === 'nofly') {
        airspaceData.noFlyZones.push(zoneData)
      } else if (zone.type === 'restricted') {
        airspaceData.restrictedZones.push(zoneData)
      } else if (zone.type === 'temporary') {
        airspaceData.temporaryZones.push(zoneData)
      }
    })

    // 转换场站数据
    airspaceData.stations = stations.map(station => ({
      id: station.id,
      name: station.name,
      lat: station.latitude,
      lng: station.longitude,
      type: station.type,
      capacity: station.capacity
    }))

    // 转换飞行器数据
    airspaceData.aircraft = aircraftData

    console.log('加载的数据:', {
      zones: zones.length,
      stations: stations.length,
      aircraft: aircraftData.length
    })

    // 数据加载完成后重新绘制
    if (map) {
      drawAllLayers()
    }

    ElMessage.success(`地图数据加载成功: ${zones.length}个空域, ${stations.length}个场站, ${aircraftData.length}架飞行器`)
  } catch (error) {
    // 具体错误提示已由 utils/request.js 的统一拦截器给出
    console.error('加载地图数据失败:', error)
  }
}

// 视野范围（温州都市区：鹿城/龙湾/瓯海 + 永嘉/乐清/瑞安近郊，全部内陆）
const REGION_BOUNDS = [
  [27.6, 120.1],
  [28.3, 121.15]
]
// 数据分布范围：初始视野自动完整包含所有数据（无海岛/海上坐标）
const DATA_BOUNDS = [
  [27.7, 120.25],
  [28.22, 121.02]
]

// 初始化地图
const initMap = () => {
  if (!mapContainer.value) return

  // 创建地图（温州全域，视野限定在本区域，不允许拖出范围）
  map = L.map(mapContainer.value, {
    center: [27.95, 120.68],
    zoom: 11,
    minZoom: 9,
    maxZoom: 18,
    maxBounds: REGION_BOUNDS,
    maxBoundsViscosity: 1.0,
    zoomControl: false  // 禁用缩放控件，只保留滚轮缩放
  })

  // 使用高德地图瓦片服务（不需要key）
  L.tileLayer('http://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图',
    maxZoom: 18
  }).addTo(map)

  // 添加比例尺
  L.control.scale({ imperial: false, metric: true }).addTo(map)

  // 初始视野：完整包含温州市域内全部数据点
  map.fitBounds(DATA_BOUNDS, { padding: [24, 24] })
  setTimeout(() => {
    if (!map) return
    map.invalidateSize()
    map.fitBounds(DATA_BOUNDS, { padding: [24, 24] })
    map.setMaxBounds(REGION_BOUNDS)
  }, 200)

  // 绘制所有图层
  drawAllLayers()

  ElMessage.success('地图加载完成')
}

// 绘制所有图层
const drawAllLayers = () => {
  drawNoFlyZones()
  drawRestrictedZones()
  drawTemporaryZones()
  drawAircraft()
  drawStations()
}

// 绘制禁飞区
const drawNoFlyZones = () => {
  clearLayer('nofly')
  if (!activeLayers.value.includes('nofly')) return

  airspaceData.noFlyZones.forEach(zone => {
    let shape = null

    // 根据形状类型绘制不同图形
    if (zone.shapeType === 'rectangle' && zone.coordinates) {
      // 绘制矩形
      const coords = JSON.parse(zone.coordinates)
      shape = L.polygon(coords.map(c => [c.lat, c.lng]), {
        color: '#f56c6c',
        fillColor: '#f56c6c',
        fillOpacity: 0.2
      }).addTo(map)
    } else if (zone.shapeType === 'polygon' && zone.coordinates) {
      // 绘制多边形
      const coords = JSON.parse(zone.coordinates)
      shape = L.polygon(coords.map(c => [c.lat, c.lng]), {
        color: '#f56c6c',
        fillColor: '#f56c6c',
        fillOpacity: 0.2
      }).addTo(map)
    } else {
      // 默认绘制圆形
      shape = L.circle([zone.lat, zone.lng], {
        color: '#f56c6c',
        fillColor: '#f56c6c',
        fillOpacity: 0.2,
        radius: zone.radius
      }).addTo(map)
    }

    const sizeText = zone.shapeType === 'circle'
      ? `半径: ${(zone.radius / 1000).toFixed(1)}km`
      : `形状: ${zone.shapeType === 'rectangle' ? '矩形' : '多边形'}`

    shape.bindPopup(`
      <div style="text-align: center;">
        <strong style="color: #f56c6c;">禁飞区</strong><br>
        ${zone.name}<br>
        ${sizeText}
      </div>
    `)

    layers.nofly.push(shape)
  })
}

// 绘制限飞区
const drawRestrictedZones = () => {
  clearLayer('restricted')
  if (!activeLayers.value.includes('restricted')) return

  airspaceData.restrictedZones.forEach(zone => {
    let shape = null

    // 根据形状类型绘制不同图形
    if (zone.shapeType === 'rectangle' && zone.coordinates) {
      const coords = JSON.parse(zone.coordinates)
      shape = L.polygon(coords.map(c => [c.lat, c.lng]), {
        color: '#e6a23c',
        fillColor: '#e6a23c',
        fillOpacity: 0.2
      }).addTo(map)
    } else if (zone.shapeType === 'polygon' && zone.coordinates) {
      const coords = JSON.parse(zone.coordinates)
      shape = L.polygon(coords.map(c => [c.lat, c.lng]), {
        color: '#e6a23c',
        fillColor: '#e6a23c',
        fillOpacity: 0.2
      }).addTo(map)
    } else {
      shape = L.circle([zone.lat, zone.lng], {
        color: '#e6a23c',
        fillColor: '#e6a23c',
        fillOpacity: 0.2,
        radius: zone.radius
      }).addTo(map)
    }

    const sizeText = zone.shapeType === 'circle'
      ? `半径: ${(zone.radius / 1000).toFixed(1)}km`
      : `形状: ${zone.shapeType === 'rectangle' ? '矩形' : '多边形'}`

    shape.bindPopup(`
      <div style="text-align: center;">
        <strong style="color: #e6a23c;">限飞区</strong><br>
        ${zone.name}<br>
        ${sizeText}
      </div>
    `)

    layers.restricted.push(shape)
  })
}

// 绘制临时管制区
const drawTemporaryZones = () => {
  clearLayer('temporary')
  if (!activeLayers.value.includes('temporary')) return

  airspaceData.temporaryZones.forEach(zone => {
    const circle = L.circle([zone.lat, zone.lng], {
      color: '#909399',
      fillColor: '#909399',
      fillOpacity: 0.2,
      radius: zone.radius
    }).addTo(map)

    circle.bindPopup(`
      <div style="text-align: center;">
        <strong style="color: #909399;">临时管制</strong><br>
        ${zone.name}<br>
        半径: ${(zone.radius / 1000).toFixed(1)}km
      </div>
    `)

    layers.temporary.push(circle)
  })
}

// 绘制飞行器
const drawAircraft = () => {
  clearLayer('aircraft')
  if (!activeLayers.value.includes('aircraft')) return

  airspaceData.aircraft.forEach(aircraft => {
    const icon = L.divIcon({
      html: `<div style="background: #409eff; width: 24px; height: 24px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; color: white; font-size: 12px; font-weight: bold;">✈</div>`,
      className: 'aircraft-icon',
      iconSize: [24, 24]
    })

    const marker = L.marker([aircraft.lat, aircraft.lng], { icon }).addTo(map)

    // 点击飞行器：画航线 + 弹出可拖动信息卡
    marker.on('click', async () => {
      clearRoute()
      openCard(aircraft)
      try {
        const route = await aircraftAPI.getRoute(aircraft.id, { silent: true })
        if (!route || !route.hasRoute) {
          cardMessage.value = (route && route.message) || '暂无关联航线'
          return
        }
        drawRoute(route)
        cardRoute.value = route
      } catch (e) {
        cardMessage.value = '航线加载失败'
      }
    })

    layers.aircraft.push(marker)
  })
}

// 绘制场站
const drawStations = () => {
  clearLayer('stations')
  if (!activeLayers.value.includes('stations')) return

  airspaceData.stations.forEach(station => {
    // 根据场站类型选择颜色
    let color = '#67c23a'
    let icon = '🏢'
    if (station.type === 'charging') {
      color = '#e6a23c'
      icon = '⚡'
    } else if (station.type === 'maintenance') {
      color = '#909399'
      icon = '🔧'
    } else if (station.type === 'landing') {
      color = '#409eff'
      icon = '🛬'
    } else if (station.type === 'emergency') {
      color = '#f56c6c'
      icon = '🚨'
    } else if (station.type === 'transfer') {
      color = '#67c23a'
      icon = '📦'
    }

    const stationIcon = L.divIcon({
      html: `<div style="background: ${color}; width: 24px; height: 24px; border-radius: 4px; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; font-size: 12px;">${icon}</div>`,
      className: 'station-icon',
      iconSize: [24, 24]
    })

    const marker = L.marker([station.lat, station.lng], { icon: stationIcon }).addTo(map)

    const typeNames = {
      landing: '起降场',
      charging: '换电站',
      maintenance: '维修站',
      emergency: '应急站',
      transfer: '中转站'
    }

    marker.bindPopup(`
      <div style="text-align: center;">
        <strong style="color: ${color};">${typeNames[station.type]}</strong><br>
        ${station.name}<br>
        容量: ${station.capacity}个位
      </div>
    `)

    layers.stations.push(marker)
  })
}

// ===== 可拖动浮动信息卡 =====
const cardVisible = ref(false)
const cardPos = reactive({ x: 16, y: 16 })
const cardAircraft = ref({})
const cardRoute = ref(null)
const cardMessage = ref('')
let cardMoved = false
let dragState = null

const statusText = (s) => (s === 'flying' ? '飞行中' : s === 'grounded' ? '地面待命' : '返航中')

const openCard = (aircraft) => {
  cardAircraft.value = aircraft || {}
  cardRoute.value = null
  cardMessage.value = ''
  cardVisible.value = true
  if (!cardMoved && mapContainer.value) {
    cardPos.x = 16
    cardPos.y = 16
  }
}

const closeCard = () => {
  cardVisible.value = false
  clearRoute()
}

const onDragStart = (e) => {
  if (!mapContainer.value) return
  const rect = mapContainer.value.getBoundingClientRect()
  dragState = { dx: e.clientX - rect.left - cardPos.x, dy: e.clientY - rect.top - cardPos.y, rect }
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
}

const onDragMove = (e) => {
  if (!dragState) return
  const { rect, dx, dy } = dragState
  cardMoved = true
  cardPos.x = Math.max(0, Math.min(rect.width - 60, e.clientX - rect.left - dx))
  cardPos.y = Math.max(0, Math.min(rect.height - 40, e.clientY - rect.top - dy))
}

const onDragEnd = () => {
  dragState = null
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

// ===== 航线：起飞地 → 分割点（实线） + 分割点 → 目的地（虚线），全程绕开围栏 =====
const routeKm = (lat1, lng1, lat2, lng2) => {
  const r = 6371
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

const drawRoute = (route) => {
  clearRoute()
  const pts = (route.waypoints || [])
    .filter((w) => w.lat != null && w.lng != null)
    .map((w) => [w.lat, w.lng])
  if (pts.length < 2) return

  // 按「已飞里程」把航线切成已飞段与剩余段
  const flown = route.flownDistance || 0
  let acc = 0
  let idx = 0
  for (let i = 0; i + 1 < pts.length; i++) {
    acc += routeKm(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1])
    idx = i
    if (acc >= flown) break
  }
  const split = route.splitLat != null && route.splitLng != null ? [route.splitLat, route.splitLng] : pts[idx]
  const passed = pts.slice(0, idx + 1).concat([split])
  const remain = [split].concat(pts.slice(idx + 1))

  if (passed.length > 1) {
    layers.route.push(L.polyline(passed, { color: '#409eff', weight: 4, opacity: 0.85 }).addTo(map))
  }
  if (remain.length > 1) {
    layers.route.push(L.polyline(remain, { color: '#e6a23c', weight: 3, dashArray: '8,8', opacity: 0.9 }).addTo(map))
  }

  const endpointIcon = (color, text) =>
    L.divIcon({
      html: `<div style="background:${color};color:#fff;width:22px;height:22px;border-radius:50%;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.3);display:flex;align-items:center;justify-content:center;font-size:12px;">${text}</div>`,
      className: 'route-endpoint',
      iconSize: [22, 22]
    })

  layers.route.push(L.marker(pts[0], { icon: endpointIcon('#67c23a', '起') }).addTo(map))
  layers.route.push(L.marker(pts[pts.length - 1], { icon: endpointIcon('#f56c6c', '终') }).addTo(map))
}

const routeHtml = (route) => {
  const tag = route.history
    ? '<span style="color:#909399;">（历史航线）</span>'
    : route.flightStatus === 'flying'
      ? '<span style="color:#409eff;">（执行中）</span>'
      : '<span style="color:#e6a23c;">（待起飞）</span>'
  return `
    <div style="text-align:left;line-height:1.75;">
      航线 ${tag}<br>
      订单: ${route.orderNo || '-'}<br>
      起点: ${route.departureName || '-'}<br>
      终点: ${route.arrivalName || '-'}<br>
      航线里程: ${route.totalDistance}km（已飞 ${route.flownDistance} · 剩余 ${route.remainDistance}）<br>
      进度: ${route.progress}% · 计划高度 ${route.routeAltitude || '-'}米<br>
      ${route.avoidedCount ? '<span style="color:#67c23a;">已绕开 ' + route.avoidedCount + ' 个管制区</span><br>' : ''}
      ${route.history ? '已完成' : route.flightStatus === 'flying' ? '预计 ' + route.etaMinutes + ' 分钟后到达' : '待起飞 · 约 ' + route.etaMinutes + ' 分钟'} 
    </div>
  `
}

const clearRoute = () => {
  layers.route.forEach((item) => item.remove())
  layers.route = []
}

// 清除指定图层
const clearLayer = (layerName) => {
  layers[layerName].forEach(item => item.remove())
  layers[layerName] = []
}

// 更新图层显示
const updateLayers = () => {
  drawAllLayers()
}

// 聚焦到指定区域
const focusZone = (zone) => {
  if (map) {
    map.setView([zone.lat, zone.lng], 13)
  }
}

// 刷新地图
const refreshMap = () => {
  loadMapData()
}

onMounted(() => {
  initMap()
  loadMapData()
})

onUnmounted(() => {
  if (map) {
    map.remove()
  }
})
</script>

<style scoped>
/* 地图容器需要作为浮动卡的定位基准 */
.map-content {
  position: relative;
}

/* 可拖动浮动信息卡 */
.flight-card {
  position: absolute;
  z-index: 800;
  width: 240px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
  cursor: move;
  user-select: none;
  font-size: 12px;
  color: #303133;
}

.flight-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
  border-radius: 6px 6px 0 0;
  font-weight: 600;
  color: #409eff;
}

.flight-card__close {
  cursor: pointer;
  color: #909399;
  font-weight: normal;
  padding: 0 2px;
}

.flight-card__close:hover {
  color: #f56c6c;
}

.flight-card__body {
  padding: 8px 10px;
  line-height: 1.85;
}

.flight-card__sep {
  height: 1px;
  background: #ebeef5;
  margin: 6px 0;
}

.flight-card__title {
  font-weight: 600;
  color: #409eff;
}

.flight-card__avoid {
  color: #67c23a;
}

.flight-card__eta {
  color: #e6a23c;
}

.flight-card__empty {
  color: #909399;
}

.airspace-map {
  padding: 20px;
}

.header-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.map-layout {
  display: flex;
  height: calc(100vh - 200px);
}

.control-panel {
  width: 280px;
  padding: 15px;
  background: #f5f7fa;
  overflow-y: auto;
}

.map-content {
  flex: 1;
  position: relative;
}

:deep(.el-checkbox) {
  display: block;
  margin: 10px 0;
}
</style>
