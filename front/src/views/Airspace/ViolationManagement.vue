<template>
  <div class="violation-management">
    <el-card shadow="never">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card red">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><Warning /></el-icon>
              </div>
              <div class="stat-data">
                <div class="stat-label">本月违规</div>
                <div class="stat-value">{{ stats.monthTotal }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card orange">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><Position /></el-icon>
              </div>
              <div class="stat-data">
                <div class="stat-label">偏离航线</div>
                <div class="stat-value">{{ stats.routeDeviation }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card purple">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><MapLocation /></el-icon>
              </div>
              <div class="stat-data">
                <div class="stat-label">闯入禁飞区</div>
                <div class="stat-value">{{ stats.noFlyZone }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card blue">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><TopRight /></el-icon>
              </div>
              <div class="stat-data">
                <div class="stat-label">超高飞行</div>
                <div class="stat-value">{{ stats.heightViolation }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="违规编号">
          <el-input v-model="searchForm.violationNo" placeholder="请输入编号" clearable />
        </el-form-item>
        <el-form-item label="违规类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable style="width: 150px">
            <el-option label="偏离航线" value="route_deviation" />
            <el-option label="闯入禁飞区" value="no_fly_zone" />
            <el-option label="超高飞行" value="height_violation" />
            <el-option label="未经许可飞行" value="unauthorized" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="searchForm.severity" placeholder="请选择" clearable style="width: 150px">
            <el-option label="严重" :value="3" />
            <el-option label="较重" :value="2" />
            <el-option label="一般" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 150px">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已处理" value="handled" />
            <el-option label="已上报" value="reported" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="toolbar">
        <el-button type="danger" @click="handleReport">
          <el-icon><Upload /></el-icon>
          上报监管部门
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 违规列表 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :row-class-name="getRowClass"
      >
        <el-table-column prop="violationNo" label="违规编号" width="150" />
        <el-table-column prop="type" label="违规类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeColor(row.type)" size="small">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getSeverityColor(row.severity)" size="small">
              {{ getSeverityName(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deviceNo" label="涉事飞行器" width="120" />
        <el-table-column prop="location" label="违规位置" min-width="200" show-overflow-tooltip />
        <el-table-column prop="occurTime" label="发生时间" width="180" />
        <el-table-column prop="status" label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="warning"
              link
              @click="handleProcess(row)"
            >
              处理
            </el-button>
            <el-button
              v-if="row.status === 'handled'"
              type="danger"
              link
              @click="handleReportSingle(row)"
            >
              上报
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 违规详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="违规详情"
      width="1080px"
      @opened="initViolationMap"
      @closed="destroyViolationMap"
    >
      <el-row :gutter="20" v-if="currentViolation">
        <el-col :span="13">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="违规编号" :span="2">{{ currentViolation.violationNo }}</el-descriptions-item>
            <el-descriptions-item label="违规类型">
              <el-tag :type="getTypeColor(currentViolation.type)">{{ getTypeName(currentViolation.type) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="严重程度">
              <el-tag :type="getSeverityColor(currentViolation.severity)">
                {{ getSeverityName(currentViolation.severity) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="涉事飞行器">{{ currentViolation.deviceNo }}</el-descriptions-item>
            <el-descriptions-item label="关联订单">{{ currentViolation.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="机长">{{ currentViolation.pilotName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="违规位置">{{ currentViolation.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发生时间">{{ currentViolation.occurTime }}</el-descriptions-item>
            <el-descriptions-item label="发现方式">{{ currentViolation.detectMethod }}</el-descriptions-item>
            <el-descriptions-item label="违规详情" :span="2">
              {{ currentViolation.detail || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="处理状态">
              <el-tag :type="getStatusColor(currentViolation.status)">
                {{ getStatusName(currentViolation.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="处理人">{{ currentViolation.handler || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider>处理记录</el-divider>
          <el-timeline>
            <el-timeline-item
              v-for="(record, index) in processRecords"
              :key="index"
              :timestamp="record.time"
              placement="top"
            >
              <el-card>
                <h4>{{ record.action }}</h4>
                <p>{{ record.content }}</p>
                <p class="text-muted">操作人：{{ record.operator }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-col>

        <el-col :span="11">
          <el-card shadow="hover" style="margin-bottom: 20px">
            <template #header>
              <span>违规位置</span>
            </template>
            <!-- 有经纬度时显示地图，无坐标时给占位提示 -->
            <template v-if="hasCoords">
              <div ref="violationMapEl" class="violation-map"></div>

              <!-- 图例 -->
              <div class="violation-map-legend">
                <span><i class="lg-line lg-track"></i>实际航迹</span>
                <span v-if="hasPlannedRoute"><i class="lg-line lg-planned"></i>申报航线</span>
                <span v-if="drawableZones.length"><i class="lg-dot"></i>管制区域</span>
              </div>

              <!-- 航迹起点 → 终点 -->
              <div v-if="trackData?.origin" class="violation-map-caption">
                <el-icon><Guide /></el-icon>
                <span>{{ trackData.origin.name }} → {{ trackData.destination.name }}</span>
              </div>
              <div v-if="trackData?.sourceNote" class="violation-map-note">
                {{ trackData.sourceNote }}
              </div>
              <div v-if="relatedZoneNames" class="violation-map-note">
                关联区域：{{ relatedZoneNames }}
              </div>

              <div class="violation-map-caption">
                <el-icon><LocationInformation /></el-icon>
                <span>{{ currentViolation.location || '未记录地点描述' }}</span>
                <span class="text-muted">
                  {{ currentViolation.latitude }}, {{ currentViolation.longitude }}
                </span>
              </div>
            </template>
            <div v-else class="violation-map-empty">
              <div style="text-align: center">
                <el-icon :size="36"><MapLocation /></el-icon>
                <div style="margin-top: 8px">该记录未包含经纬度，无法定位</div>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover">
            <template #header>
              <span>证据材料</span>
            </template>
            <template v-if="hasEvidence">
              <el-image
                v-for="(img, index) in currentViolation.evidence"
                :key="index"
                :src="img"
                :preview-src-list="currentViolation.evidence"
                style="width: 100px; height: 100px; margin: 5px"
                fit="cover"
              />
            </template>
            <div v-else class="text-muted">暂无证据材料</div>
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>

    <!-- 处理对话框 -->
    <el-dialog v-model="processVisible" title="处理违规" width="600px">
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="违规编号">
          <el-input v-model="processForm.violationNo" disabled />
        </el-form-item>
        <el-form-item label="处理方式" required>
          <el-select v-model="processForm.action" placeholder="请选择" style="width: 100%">
            <el-option label="警告" value="warning" />
            <el-option label="罚款" value="fine" />
            <el-option label="暂停飞行许可" value="suspend" />
            <el-option label="吊销资质" value="revoke" />
          </el-select>
        </el-form-item>
        <el-form-item label="罚款金额" v-if="processForm.action === 'fine'">
          <el-input-number v-model="processForm.fineAmount" :min="0" :step="100" />
          <span style="margin-left: 10px">元</span>
        </el-form-item>
        <el-form-item label="暂停天数" v-if="processForm.action === 'suspend'">
          <el-input-number v-model="processForm.suspendDays" :min="1" :max="365" />
          <span style="margin-left: 10px">天</span>
        </el-form-item>
        <el-form-item label="处理说明" required>
          <el-input
            v-model="processForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细说明处理依据和措施"
          />
        </el-form-item>
        <el-form-item label="是否上报">
          <el-switch v-model="processForm.reportToAuthority" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px">
            严重违规需上报监管部门
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmProcess">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { violationAPI } from '@/api'

/**
 * 空域违规处理页面：数据全部来自后端 /api/violations
 *   列表 GET /api/violations      处理 POST /{id}/process
 *   修改 PUT /api/violations/{id}  上报 POST /{id}/report
 * 类型/严重程度/状态编码与后端一致（type: route_deviation…；severity: 1/2/3；status: pending/processing/handled/reported/closed）
 */

const loading = ref(false)
const allViolations = ref([])

const stats = reactive({
  monthTotal: 0,
  routeDeviation: 0,
  noFlyZone: 0,
  heightViolation: 0,
  pending: 0
})

const searchForm = reactive({
  violationNo: '',
  type: '',
  severity: null,
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 登记违规表单（后端支持 POST /api/violations）
const form = reactive({
  id: null,
  violationNo: '',
  type: '',
  severity: 1,
  deviceNo: '',
  location: '',
  occurTime: '',
  detectMethod: '系统自动检测',
  detail: ''
})

const detailVisible = ref(false)
const currentViolation = ref(null)
const processRecords = ref([])

// ===== 违规位置地图（Leaflet + 高德瓦片） =====
const violationMapEl = ref(null)
let violationMap = null
let trackLayer = null

// 航迹数据（/api/violations/{id}/track）
const trackData = ref(null)

const hasCoords = computed(() => {
  const v = currentViolation.value
  return !!(v && v.latitude != null && v.longitude != null)
})

const hasEvidence = computed(() => {
  const list = currentViolation.value?.evidence
  return Array.isArray(list) && list.length > 0
})

// 严重程度决定标记颜色
const severityColor = (severity) => ({ 3: '#f56c6c', 2: '#e6a23c', 1: '#409eff' }[severity] || '#909399')

// 管制区域颜色
const zoneColor = (type) => ({ nofly: '#f56c6c', restricted: '#e6a23c', temporary: '#909399' }[type] || '#909399')
const zoneTypeLabel = (type) => ({ nofly: '禁飞区', restricted: '限飞区', temporary: '临时管制' }[type] || type)

// 可绘制成圈的区域（矩形/多边形区域的 radius 为 0，不画圈，只在下方文字里列出）
const drawableZones = computed(() =>
  (trackData.value?.zones || []).filter((z) => z.latitude != null && z.longitude != null && (z.radius || 0) > 0)
)
const hasPlannedRoute = computed(() => (trackData.value?.plannedRoute || []).length > 1)
const relatedZoneNames = computed(() =>
  (trackData.value?.zones || []).map((z) => `${z.name}（${zoneTypeLabel(z.type)}）`).join('、')
)

// 航迹圆点图标（起点/终点用带字圆形，避免 Leaflet 默认图标 404）
const badgeIcon = (color, text) =>
  L.divIcon({
    html: `<div style="background:${color};color:#fff;width:22px;height:22px;border-radius:50%;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.35);display:flex;align-items:center;justify-content:center;font-size:12px;">${text}</div>`,
    className: 'track-badge',
    iconSize: [22, 22]
  })

// 拉取该违规的航迹，并在地图上重绘
const loadViolationTrack = async (id) => {
  trackData.value = null
  try {
    trackData.value = await violationAPI.getTrack(id)
  } catch (error) {
    console.error('加载违规航迹失败:', error)
  }
  renderTrackLayers()
}

// 在地图上画：管制区域 + 申报航线 + 实际航迹 + 起/终标记
const renderTrackLayers = () => {
  if (!violationMap || !trackData.value?.hasTrack) return
  const t = trackData.value

  if (trackLayer) {
    trackLayer.clearLayers()
  } else {
    trackLayer = L.layerGroup().addTo(violationMap)
  }

  const bounds = []
  const color = severityColor(t.severity)

  // 1. 违规点附近的管制区域
  drawableZones.value.forEach((z) => {
    const zc = zoneColor(z.type)
    L.circle([z.latitude, z.longitude], {
      radius: z.radius,
      color: zc,
      fillColor: zc,
      fillOpacity: 0.1,
      weight: 1.5
    })
      .bindTooltip(`${z.name}（${zoneTypeLabel(z.type)} 半径 ${z.radius}m）`, { sticky: true })
      .addTo(trackLayer)

    // 把圆的四个边界也纳入视野
    const dLat = z.radius / 111320
    const dLng = z.radius / (111320 * Math.cos((z.latitude * Math.PI) / 180))
    bounds.push([z.latitude, z.longitude])
    bounds.push([z.latitude + dLat, z.longitude], [z.latitude - dLat, z.longitude])
    bounds.push([z.latitude, z.longitude + dLng], [z.latitude, z.longitude - dLng])
  })

  // 2. 申报航线（白色描边 + 灰色虚线，保证在繁忙底图上看得清）
  if (hasPlannedRoute.value) {
    const planned = t.plannedRoute.map((p) => [p.lat, p.lng])
    L.polyline(planned, { color: '#ffffff', weight: 6, opacity: 0.85 }).addTo(trackLayer)
    L.polyline(planned, { color: '#606266', weight: 3, dashArray: '8 6', opacity: 0.95 })
      .bindTooltip('申报航线', { sticky: true })
      .addTo(trackLayer)
    bounds.push(...planned)
  }

  // 3. 实际航迹（白色描边 + 彩色实线 + 逐点小圆点）
  const track = (t.trackPoints || []).map((p) => [p.lat, p.lng])
  if (track.length > 1) {
    L.polyline(track, { color: '#ffffff', weight: 9, opacity: 0.9 }).addTo(trackLayer)
    L.polyline(track, { color, weight: 4, opacity: 1 }).bindTooltip('实际航迹（推演）', { sticky: true }).addTo(trackLayer)
    track.forEach((pt) => {
      L.circleMarker(pt, { radius: 3, color: '#ffffff', weight: 2, fillColor: color, fillOpacity: 1 }).addTo(trackLayer)
    })
    bounds.push(...track)
  }

  // 4. 起点 / 终点标记
  const addEndpoint = (p, text, badgeColor) => {
    if (!p) return
    L.marker([p.lat, p.lng], { icon: badgeIcon(badgeColor, text) })
      .bindTooltip(`${text === '起' ? '起点' : '终点'}：${p.name || ''}`, { direction: 'top' })
      .addTo(trackLayer)
    bounds.push([p.lat, p.lng])
  }
  addEndpoint(t.origin, '起', '#67c23a')
  addEndpoint(t.destination, '终', '#409eff')

  if (bounds.length) {
    violationMap.fitBounds(bounds, { padding: [30, 30], maxZoom: 15 })
    setTimeout(() => violationMap && violationMap.invalidateSize(), 120)
  }
}

const destroyViolationMap = () => {
  if (violationMap) {
    violationMap.remove()
    violationMap = null
  }
  trackLayer = null
}

const initViolationMap = () => {
  destroyViolationMap()
  const v = currentViolation.value
  if (!v || v.latitude == null || v.longitude == null) return

  // 等弹窗展开动画结束、容器有实际尺寸后再画地图
  nextTick(() => {
    if (!violationMapEl.value) return

    const center = [v.latitude, v.longitude]
    violationMap = L.map(violationMapEl.value, {
      center,
      zoom: 16,
      minZoom: 9,
      maxZoom: 18
    })

    // 与空域地图保持一致：高德瓦片，无需 key
    L.tileLayer('http://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      subdomains: ['1', '2', '3', '4'],
      attribution: '© 高德地图',
      maxZoom: 18
    }).addTo(violationMap)

    const color = severityColor(v.severity)
    const icon = L.divIcon({
      html: `<div style="background:${color};color:#fff;width:30px;height:30px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:bold;">!</div>`,
      className: 'violation-icon',
      iconSize: [30, 30]
    })

    L.marker(center, { icon, zIndexOffset: 1000 })
      .addTo(violationMap)
      .bindTooltip(`违规点：${v.violationNo}`, { direction: 'top' })
      .bindPopup(
        `<b>${v.violationNo}</b><br/>类型：${getTypeName(v.type)}<br/>位置：${v.location || '-'}<br/>发生：${v.occurTime || '-'}`
      )

    // 300 米范围示意圈
    L.circle(center, {
      radius: 300,
      color,
      fillColor: color,
      fillOpacity: 0.12,
      weight: 2
    }).addTo(violationMap)

    L.control.scale({ imperial: false, metric: true }).addTo(violationMap)

    // 弹窗动画结束后尺寸才准确，延迟校正一次
    setTimeout(() => {
      if (!violationMap) return
      violationMap.invalidateSize()
      renderTrackLayers()
    }, 200)
  })
}

const processVisible = ref(false)
const processForm = reactive({
  id: null,
  violationNo: '',
  action: '',
  fineAmount: 0,
  suspendDays: 0,
  description: '',
  reportToAuthority: false
})

const decorate = (v) => ({ ...v, evidence: [] })

const filtered = computed(() =>
  allViolations.value.filter((v) => {
    const noOk = !searchForm.violationNo || (v.violationNo || '').includes(searchForm.violationNo.trim())
    const typeOk = !searchForm.type || v.type === searchForm.type
    const severityOk = searchForm.severity == null || v.severity === searchForm.severity
    const statusOk = !searchForm.status || v.status === searchForm.status
    return noOk && typeOk && severityOk && statusOk
  })
)

const tableData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filtered.value.slice(start, start + pagination.size)
})

const refreshStats = () => {
  const now = new Date()
  const sameMonth = (time) => {
    if (!time) return false
    const d = new Date(time)
    return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth()
  }
  stats.monthTotal = allViolations.value.filter((v) => sameMonth(v.occurTime)).length
  stats.routeDeviation = allViolations.value.filter((v) => v.type === 'route_deviation').length
  stats.noFlyZone = allViolations.value.filter((v) => v.type === 'no_fly_zone').length
  stats.heightViolation = allViolations.value.filter((v) => v.type === 'height_violation').length
  stats.pending = allViolations.value.filter((v) => v.status === 'pending').length
}

const loadData = async () => {
  loading.value = true
  try {
    const list = await violationAPI.getList()
    allViolations.value = (list || []).map(decorate)
    pagination.total = filtered.value.length
    refreshStats()
  } catch (error) {
    console.error('加载违规数据失败:', error)
    ElMessage.error('加载违规数据失败')
  } finally {
    loading.value = false
  }
}

// ===== 查询 / 重置 / 刷新 / 导出 =====
const handleSearch = () => {
  pagination.page = 1
  pagination.total = filtered.value.length
}

const handleReset = () => {
  Object.assign(searchForm, { violationNo: '', type: '', severity: null, status: '' })
  handleSearch()
}

const refreshData = () => {
  loadData()
}

const handleExport = () => {
  const header = ['违规编号', '类型', '严重程度', '涉事飞行器', '机长', '违规位置', '发生时间', '状态', '处理人']
  const rows = filtered.value.map((v) => [
    v.violationNo,
    getTypeName(v.type),
    getSeverityName(v.severity),
    v.deviceNo || '',
    v.pilotName || '',
    v.location || '',
    v.occurTime || '',
    getStatusName(v.status),
    v.handler || ''
  ])
  const csv = [header, ...rows].map((r) => r.map((x) => `"${x ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `违规记录_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出当前筛选结果')
}

// ===== 查看详情 =====
const handleView = (row) => {
  currentViolation.value = row
  processRecords.value = row.handler
    ? [
        {
          time: row.handleTime,
          operator: row.handler,
          action: getActionName(row.handleResult),
          content: row.handleRemark || '无'
        }
      ]
    : []
  detailVisible.value = true
  // 拉取航迹，数据到达后自动画到地图上
  loadViolationTrack(row.id)
}

// ===== 处理违规 =====
const handleProcess = (row) => {
  processForm.id = row.id
  processForm.violationNo = row.violationNo
  processForm.action = ''
  processForm.fineAmount = row.fineAmount || 0
  processForm.suspendDays = row.suspendDays || 0
  processForm.description = ''
  processForm.reportToAuthority = row.severity === 3
  processVisible.value = true
}

const confirmProcess = async () => {
  if (!processForm.action) {
    ElMessage.warning('请选择处理方式')
    return
  }
  if (processForm.action === 'fine' && !processForm.fineAmount) {
    ElMessage.warning('请输入罚款金额')
    return
  }
  try {
    await violationAPI.process(processForm.id, {
      handleResult: processForm.action,
      fineAmount: processForm.fineAmount,
      suspendDays: processForm.suspendDays,
      description: processForm.description,
      reported: processForm.reportToAuthority
    })
    ElMessage.success('处理完成')
    processVisible.value = false
    loadData()
  } catch (error) {
    console.error('处理违规失败:', error)
  }
}

// ===== 上报 =====
const handleReportSingle = async (row) => {
  try {
    await ElMessageBox.confirm(`确定将违规「${row.violationNo}」上报上级监管吗？`, '提示', {
      type: 'warning'
    })
    await violationAPI.update(row.id, { status: 'reported' })
    await violationAPI.process(row.id, { reported: true })
    ElMessage.success('已上报')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('上报失败:', error)
    }
  }
}

// 批量上报：把严重（severity=3）且未上报的记录上报
const handleReport = async () => {
  const targets = filtered.value.filter((v) => v.severity === 3 && !v.reported)
  if (targets.length === 0) {
    ElMessage.info('没有需要上报的严重违规')
    return
  }
  try {
    await ElMessageBox.confirm(`将上报 ${targets.length} 条严重违规记录，确定继续吗？`, '批量上报', {
      type: 'warning'
    })
    await Promise.all(
      targets.map((v) => violationAPI.update(v.id, { status: 'reported' }).then(() => violationAPI.process(v.id, { reported: true })))
    )
    ElMessage.success('已上报')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量上报失败:', error)
    }
  }
}

// ===== 工具函数 =====
const getTypeColor = (type) => {
  const map = {
    route_deviation: 'warning',
    no_fly_zone: 'danger',
    height_violation: 'warning',
    unauthorized: 'danger',
    other: 'info'
  }
  return map[type] || ''
}

const getTypeName = (type) => {
  const map = {
    route_deviation: '偏离航线',
    no_fly_zone: '闯入禁飞区',
    height_violation: '超高飞行',
    unauthorized: '未经许可飞行',
    other: '其他'
  }
  return map[type] || type
}

const getSeverityColor = (severity) => {
  const map = { 3: 'danger', 2: 'warning', 1: 'info' }
  return map[severity] || ''
}

const getSeverityName = (severity) => {
  const map = { 3: '严重', 2: '较重', 1: '一般' }
  return map[severity] || '-'
}

const getStatusColor = (status) => {
  const map = { pending: 'danger', processing: 'warning', handled: 'success', reported: 'info', closed: 'info' }
  return map[status] || ''
}

const getStatusName = (status) => {
  const map = { pending: '待处理', processing: '处理中', handled: '已处理', reported: '已上报', closed: '已关闭' }
  return map[status] || status
}

const getActionName = (action) => {
  const map = { warning: '警告', fine: '罚款', suspend: '暂停飞行许可', revoke: '吊销资质' }
  return map[action] || '-'
}

const getRowClass = ({ row }) => {
  if (row.status === 'pending' && row.severity === 3) return 'row-danger'
  if (row.status === 'pending') return 'row-warning'
  return ''
}

onMounted(() => {
  loadData()
})

// 离开页面时销毁地图实例，避免内存泄漏
onUnmounted(() => {
  destroyViolationMap()
})
</script>

<style scoped>
.violation-management {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  color: white;
}

.stat-card.red .stat-icon {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.stat-card.orange .stat-icon {
  background: linear-gradient(135deg, #e6a23c 0%, #f0a742 100%);
}

.stat-card.purple .stat-icon {
  background: linear-gradient(135deg, #909399 0%, #b3b6bc 100%);
}

.stat-card.blue .stat-icon {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.stat-data {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.search-form {
  margin-bottom: 20px;
}

.toolbar {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.text-muted {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.violation-map {
  height: 340px;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
  background: #f5f7fa;
}

.violation-map-caption {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  color: #606266;
  font-size: 13px;
}

.violation-map-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  color: #606266;
  font-size: 12px;
}

.violation-map-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.lg-line {
  display: inline-block;
  width: 18px;
  height: 0;
  border-top-width: 3px;
  border-top-style: solid;
}

.lg-track {
  border-top-color: #409eff;
}

.lg-planned {
  border-top-style: dashed;
  border-top-color: #909399;
}

.lg-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f56c6c;
  opacity: 0.5;
}

.violation-map-note {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.violation-map-caption .text-muted {
  margin-top: 0;
}

.violation-map-empty {
  height: 300px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

:deep(.severe-row) {
  background-color: #fef0f0;
}
</style>
