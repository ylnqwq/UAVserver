<template>
  <div class="aircontrol-dashboard">
    <!-- 顶部标题栏 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1>空管监管数据大屏</h1>
        <div class="current-time">{{ currentTime }}</div>
      </div>
      <div class="header-right">
        <el-tag type="success" size="large">系统运行正常</el-tag>
        <el-button @click="handleFullScreen" circle>
          <el-icon><FullScreen /></el-icon>
        </el-button>
        <el-button @click="handleRefresh" circle>
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-row">
      <el-card class="stat-card primary" shadow="hover">
        <div class="stat-icon"><el-icon><Promotion /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.todayFlights }}</div>
          <div class="stat-label">今日飞行架次</div>
          <div class="stat-trend up">
            <el-icon><CaretTop /></el-icon> {{ stats.flightsTrend }}%
          </div>
        </div>
      </el-card>

      <el-card class="stat-card success" shadow="hover">
        <div class="stat-icon"><el-icon><View /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.onlineAircraft }}</div>
          <div class="stat-label">在线飞行器</div>
          <div class="stat-subtitle">总数: {{ stats.totalAircraft }}</div>
        </div>
      </el-card>

      <el-card class="stat-card warning" shadow="hover">
        <div class="stat-icon"><el-icon><Warning /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.alerts }}</div>
          <div class="stat-label">预警数量</div>
          <div class="stat-subtitle">待处理: {{ stats.pendingAlerts }}</div>
        </div>
      </el-card>

      <el-card class="stat-card danger" shadow="hover">
        <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.violations }}</div>
          <div class="stat-label">违规次数</div>
          <div class="stat-trend down">
            <el-icon><CaretBottom /></el-icon> {{ stats.violationsTrend }}%
          </div>
        </div>
      </el-card>

      <el-card class="stat-card info" shadow="hover">
        <div class="stat-icon"><el-icon><Clock /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.avgFlightTime }}</div>
          <div class="stat-label">平均飞行时长</div>
          <div class="stat-subtitle">分钟</div>
        </div>
      </el-card>
    </div>

    <!-- 主要内容区 -->
    <div class="dashboard-main">
      <!-- 左侧列 -->
      <div class="left-column">
        <!-- 空域利用率 -->
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>空域利用率</span>
              <el-tag type="primary">实时</el-tag>
            </div>
          </template>
          <div ref="airspaceUtilizationChart" class="chart" style="height: 300px"></div>
        </el-card>

        <!-- 飞行计划审批统计 -->
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>飞行计划审批统计</span>
              <el-radio-group v-model="approvalPeriod" size="small" @change="updateApprovalChart">
                <el-radio-button value="today">今日</el-radio-button>
                <el-radio-button value="week">本周</el-radio-button>
                <el-radio-button value="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="approvalChart" class="chart" style="height: 300px"></div>
        </el-card>

        <!-- 违规类型分布 -->
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>违规类型分布</span>
            </div>
          </template>
          <div ref="violationTypeChart" class="chart" style="height: 280px"></div>
        </el-card>
      </div>

      <!-- 中间列 -->
      <div class="center-column">
        <!-- 实时空域监控地图 -->
        <el-card class="map-card">
          <template #header>
            <div class="card-header">
              <span>实时空域监控</span>
              <div>
                <el-tag type="success">飞行中: {{ realtimeFlights.length }}</el-tag>
                <el-button size="small" @click="handleMapRefresh">刷新</el-button>
              </div>
            </div>
          </template>
          <div class="map-container">
            <div class="map-placeholder">
              <el-icon style="font-size: 48px; color: #909399"><MapLocation /></el-icon>
              <p>空域监控地图</p>
              <p style="font-size: 12px; color: #909399">集成高德/百度地图API后显示</p>
            </div>
            <!-- 飞行器实时列表 -->
            <div class="flight-list">
              <div class="flight-item" v-for="flight in realtimeFlights" :key="flight.id">
                <div class="flight-info">
                  <span class="flight-no">{{ flight.aircraftNo }}</span>
                  <el-tag :type="getFlightStatusTag(flight.status)" size="small">
                    {{ flight.status }}
                  </el-tag>
                </div>
                <div class="flight-details">
                  <div>高度: {{ flight.altitude }}m</div>
                  <div>速度: {{ flight.speed }}km/h</div>
                  <div>电量: {{ flight.battery }}%</div>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 飞行时段分布 -->
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>飞行时段分布（今日）</span>
            </div>
          </template>
          <div ref="timeDistributionChart" class="chart" style="height: 240px"></div>
        </el-card>
      </div>

      <!-- 右侧列 -->
      <div class="right-column">
        <!-- 最新告警 -->
        <el-card class="alert-card">
          <template #header>
            <div class="card-header">
              <span>最新告警</span>
              <el-badge :value="stats.pendingAlerts" type="danger" />
            </div>
          </template>
          <el-scrollbar height="350px">
            <div class="alert-list">
              <div
                class="alert-item"
                v-for="alert in latestAlerts"
                :key="alert.id"
                :class="alert.level"
              >
                <div class="alert-header">
                  <el-tag :type="getAlertLevelTag(alert.level)" size="small">
                    {{ alert.level }}
                  </el-tag>
                  <span class="alert-time">{{ alert.time }}</span>
                </div>
                <div class="alert-content">{{ alert.content }}</div>
                <div class="alert-aircraft">{{ alert.aircraft }}</div>
              </div>
            </div>
          </el-scrollbar>
        </el-card>

        <!-- 待审批计划 -->
        <el-card class="pending-card">
          <template #header>
            <div class="card-header">
              <span>待审批计划</span>
              <el-badge :value="pendingPlans.length" type="warning" />
            </div>
          </template>
          <el-scrollbar height="280px">
            <div class="pending-list">
              <div class="pending-item" v-for="plan in pendingPlans" :key="plan.id">
                <div class="pending-header">
                  <span class="plan-no">{{ plan.planNo }}</span>
                  <el-tag size="small" v-if="plan.urgent" type="danger">加急</el-tag>
                </div>
                <div class="pending-info">
                  <div>商家: {{ plan.merchant }}</div>
                  <div>时间: {{ plan.scheduledTime }}</div>
                  <div>空域: {{ plan.airspace }}</div>
                </div>
                <div class="pending-actions">
                  <el-button size="small" type="success" @click="handleQuickApprove(plan)">
                    通过
                  </el-button>
                  <el-button size="small" type="danger" @click="handleQuickReject(plan)">
                    驳回
                  </el-button>
                </div>
              </div>
            </div>
          </el-scrollbar>
        </el-card>

        <!-- 运营商排行 -->
        <el-card class="rank-card">
          <template #header>
            <div class="card-header">
              <span>运营商飞行排行</span>
              <el-tag type="info" size="small">本月</el-tag>
            </div>
          </template>
          <div class="rank-list">
            <div class="rank-item" v-for="(merchant, index) in merchantRank" :key="merchant.id">
              <div class="rank-number" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
              <div class="rank-info">
                <div class="merchant-name">{{ merchant.name }}</div>
                <div class="merchant-flights">飞行 {{ merchant.flights }} 次</div>
              </div>
              <div class="rank-rate">
                <el-progress
                  :percentage="merchant.complianceRate"
                  :color="getComplianceColor(merchant.complianceRate)"
                  :show-text="false"
                  style="width: 60px"
                />
                <span style="font-size: 12px; margin-left: 5px">
                  {{ merchant.complianceRate }}%
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import {
  FullScreen,
  Refresh,
  Promotion,
  View,
  Warning,
  CircleClose,
  Clock,
  CaretTop,
  CaretBottom,
  MapLocation
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 当前时间
const currentTime = ref('')
let timeInterval = null

// 统计数据
const stats = reactive({
  todayFlights: 342,
  flightsTrend: 12.5,
  onlineAircraft: 28,
  totalAircraft: 56,
  alerts: 5,
  pendingAlerts: 3,
  violations: 8,
  violationsTrend: 15.3,
  avgFlightTime: 35
})

// 审批周期
const approvalPeriod = ref('today')

// 实时飞行
const realtimeFlights = ref([
  { id: 1, aircraftNo: 'M300-001', status: '飞行中', altitude: 120, speed: 45, battery: 78 },
  { id: 2, aircraftNo: 'M350-002', status: '飞行中', altitude: 95, speed: 38, battery: 85 },
  { id: 3, aircraftNo: 'M300-005', status: '巡航', altitude: 110, speed: 42, battery: 92 },
  { id: 4, aircraftNo: 'M350-008', status: '返航', altitude: 80, speed: 50, battery: 35 }
])

// 最新告警
const latestAlerts = ref([
  {
    id: 1,
    level: '严重',
    time: '14:32',
    content: '飞行器偏离航线超过100米',
    aircraft: 'M300-003'
  },
  {
    id: 2,
    level: '一般',
    time: '14:28',
    content: '飞行高度接近限制高度',
    aircraft: 'M350-005'
  },
  {
    id: 3,
    level: '紧急',
    time: '14:15',
    content: '飞行器进入禁飞区边缘',
    aircraft: 'M300-007'
  },
  {
    id: 4,
    level: '一般',
    time: '14:05',
    content: '电池电量低于30%',
    aircraft: 'M350-002'
  },
  {
    id: 5,
    level: '严重',
    time: '13:52',
    content: '空域冲突预警',
    aircraft: 'M300-001'
  }
])

// 待审批计划
const pendingPlans = ref([
  {
    id: 1,
    planNo: 'FP-2024091700089',
    merchant: '天翔物流',
    scheduledTime: '15:00-16:00',
    airspace: '朝阳区A3',
    urgent: true
  },
  {
    id: 2,
    planNo: 'FP-2024091700090',
    merchant: '顺丰速运',
    scheduledTime: '15:30-16:30',
    airspace: '海淀区B2',
    urgent: false
  },
  {
    id: 3,
    planNo: 'FP-2024091700091',
    merchant: '京东物流',
    scheduledTime: '16:00-17:00',
    airspace: '朝阳区A5',
    urgent: false
  }
])

// 运营商排行
const merchantRank = ref([
  { id: 1, name: '天翔物流', flights: 458, complianceRate: 98.5 },
  { id: 2, name: '顺丰速运', flights: 412, complianceRate: 97.8 },
  { id: 3, name: '京东物流', flights: 385, complianceRate: 96.2 },
  { id: 4, name: '美团配送', flights: 342, complianceRate: 95.6 },
  { id: 5, name: '饿了么', flights: 298, complianceRate: 94.8 }
])

// 图表引用
const airspaceUtilizationChart = ref(null)
const approvalChart = ref(null)
const violationTypeChart = ref(null)
const timeDistributionChart = ref(null)

let charts = {}

// 初始化
onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  initCharts()
  startAutoRefresh()
})

onBeforeUnmount(() => {
  if (timeInterval) clearInterval(timeInterval)
  Object.values(charts).forEach(chart => chart?.dispose())
})

// 更新时间
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

// 初始化图表
const initCharts = () => {
  // 空域利用率雷达图
  charts.airspaceUtilization = echarts.init(airspaceUtilizationChart.value)
  charts.airspaceUtilization.setOption({
    tooltip: {},
    radar: {
      indicator: [
        { name: '朝阳区', max: 100 },
        { name: '海淀区', max: 100 },
        { name: '丰台区', max: 100 },
        { name: '石景山区', max: 100 },
        { name: '通州区', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: [78, 85, 62, 55, 68],
          name: '利用率(%)'
        }
      ]
    }]
  })

  // 审批统计柱状图
  updateApprovalChart()

  // 违规类型饼图
  charts.violationType = echarts.init(violationTypeChart.value)
  charts.violationType.setOption({
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: '5%',
      left: 'center'
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 20,
          fontWeight: 'bold'
        }
      },
      data: [
        { value: 35, name: '偏离航线' },
        { value: 28, name: '超速飞行' },
        { value: 18, name: '进入禁飞区' },
        { value: 12, name: '超高飞行' },
        { value: 7, name: '其他违规' }
      ]
    }]
  })

  // 飞行时段分布
  charts.timeDistribution = echarts.init(timeDistributionChart.value)
  charts.timeDistribution.setOption({
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['0-4时', '4-8时', '8-12时', '12-16时', '16-20时', '20-24时']
    },
    yAxis: {
      type: 'value',
      name: '飞行架次'
    },
    series: [{
      data: [5, 12, 98, 156, 125, 45],
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 1, color: '#188df0' }
        ])
      }
    }]
  })
}

// 更新审批图表
const updateApprovalChart = () => {
  if (!approvalChart.value) return

  if (!charts.approval) {
    charts.approval = echarts.init(approvalChart.value)
  }

  const dataMap = {
    today: { approved: 45, rejected: 3, pending: 8 },
    week: { approved: 312, rejected: 18, pending: 23 },
    month: { approved: 1256, rejected: 67, pending: 45 }
  }

  const data = dataMap[approvalPeriod.value]

  charts.approval.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['已通过', '已驳回', '待审批']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value'
    },
    yAxis: {
      type: 'category',
      data: ['飞行计划']
    },
    series: [
      {
        name: '已通过',
        type: 'bar',
        stack: 'total',
        data: [data.approved],
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '已驳回',
        type: 'bar',
        stack: 'total',
        data: [data.rejected],
        itemStyle: { color: '#f56c6c' }
      },
      {
        name: '待审批',
        type: 'bar',
        stack: 'total',
        data: [data.pending],
        itemStyle: { color: '#e6a23c' }
      }
    ]
  })
}

// 自动刷新
const startAutoRefresh = () => {
  setInterval(() => {
    // 模拟数据更新
    stats.onlineAircraft = 28 + Math.floor(Math.random() * 5)
    stats.alerts = 3 + Math.floor(Math.random() * 5)
  }, 5000)
}

// 刷新
const handleRefresh = () => {
  ElMessage.success('数据已刷新')
  initCharts()
}

// 全屏
const handleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 地图刷新
const handleMapRefresh = () => {
  ElMessage.success('地图数据已刷新')
}

// 快速审批
const handleQuickApprove = (plan) => {
  ElMessage.success(`计划 ${plan.planNo} 已通过`)
  const index = pendingPlans.value.findIndex(p => p.id === plan.id)
  if (index > -1) {
    pendingPlans.value.splice(index, 1)
  }
}

// 快速驳回
const handleQuickReject = (plan) => {
  ElMessage.warning(`计划 ${plan.planNo} 已驳回`)
  const index = pendingPlans.value.findIndex(p => p.id === plan.id)
  if (index > -1) {
    pendingPlans.value.splice(index, 1)
  }
}

// 辅助函数
const getFlightStatusTag = (status) => {
  const map = { '飞行中': 'primary', '巡航': 'success', '返航': 'warning' }
  return map[status] || 'info'
}

const getAlertLevelTag = (level) => {
  const map = { '紧急': 'danger', '严重': 'warning', '一般': 'info' }
  return map[level] || 'info'
}

const getComplianceColor = (rate) => {
  if (rate >= 95) return '#67c23a'
  if (rate >= 90) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.aircontrol-dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  padding: 20px;
  color: #fff;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left h1 {
  margin: 0;
  font-size: 28px;
  font-weight: bold;
}

.current-time {
  font-size: 18px;
  color: #a0cfff;
  margin-top: 5px;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  background: rgba(255, 255, 255, 0.15);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.3);
}

.stat-icon {
  font-size: 48px;
  margin-right: 15px;
  opacity: 0.9;
}

.stat-card.primary .stat-icon { color: #409eff; }
.stat-card.success .stat-icon { color: #67c23a; }
.stat-card.warning .stat-icon { color: #e6a23c; }
.stat-card.danger .stat-icon { color: #f56c6c; }
.stat-card.info .stat-icon { color: #909399; }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 5px;
}

.stat-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  margin-top: 5px;
}

.stat-trend.up { color: #67c23a; }
.stat-trend.down { color: #f56c6c; }

.dashboard-main {
  display: grid;
  grid-template-columns: 1fr 2fr 1fr;
  gap: 20px;
}

.chart-card, .map-card, .alert-card, .pending-card, .rank-card {
  background: rgba(255, 255, 255, 0.95);
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.chart {
  width: 100%;
}

.map-container {
  position: relative;
  height: 500px;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
}

.flight-list {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 200px;
  max-height: 480px;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 4px;
  padding: 10px;
}

.flight-item {
  padding: 10px;
  margin-bottom: 10px;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 12px;
}

.flight-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.flight-no {
  font-weight: bold;
}

.flight-details {
  display: flex;
  flex-direction: column;
  gap: 3px;
  color: #606266;
}

.alert-list, .pending-list {
  padding: 10px;
}

.alert-item {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 4px;
  border-left: 4px solid;
}

.alert-item.紧急 {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.alert-item.严重 {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.alert-item.一般 {
  background: #f4f4f5;
  border-left-color: #909399;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.alert-time {
  font-size: 12px;
  color: #909399;
}

.alert-content {
  font-size: 14px;
  margin-bottom: 5px;
}

.alert-aircraft {
  font-size: 12px;
  color: #606266;
}

.pending-item {
  padding: 12px;
  margin-bottom: 12px;
  background: #f0f2f5;
  border-radius: 4px;
}

.pending-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.plan-no {
  font-weight: bold;
}

.pending-info {
  font-size: 13px;
  line-height: 1.8;
  margin-bottom: 10px;
  color: #606266;
}

.pending-actions {
  display: flex;
  gap: 10px;
}

.rank-list {
  padding: 10px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  margin-bottom: 10px;
  background: #f0f2f5;
  border-radius: 4px;
  transition: all 0.3s;
}

.rank-item:hover {
  background: #e4e7ed;
}

.rank-number {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
  background: #909399;
  color: white;
  border-radius: 50%;
}

.rank-number.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  color: #303133;
}

.rank-number.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #e8e8e8);
  color: #303133;
}

.rank-number.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #e0a868);
  color: white;
}

.rank-info {
  flex: 1;
}

.merchant-name {
  font-weight: bold;
  margin-bottom: 3px;
}

.merchant-flights {
  font-size: 12px;
  color: #909399;
}

.rank-rate {
  display: flex;
  align-items: center;
}

:deep(.el-card__header) {
  background: rgba(64, 158, 255, 0.1);
  border-bottom: 1px solid rgba(64, 158, 255, 0.2);
}
</style>
