<template>
  <div class="dashboard">
    <!-- 顶部欢迎横幅 -->
    <div class="hero">
      <div class="hero__left">
        <div class="hero__hello">
          {{ greeting }}，{{ username }}
          <span class="hero__role">{{ roleLabel }}</span>
        </div>
        <div class="hero__meta">
          <span class="hero__chip"><el-icon><Calendar /></el-icon>{{ todayText }}</span>
          <span class="hero__chip"><el-icon><Clock /></el-icon>{{ clockText }}</span>
          <span class="hero__chip hero__chip--ok"><el-icon><CircleCheck /></el-icon>系统运行正常</span>
        </div>
      </div>
      <div class="hero__right">
        <div class="hero__cap">平台概览</div>
        <div class="hero__cap-sub">数据取自业务库实时聚合</div>
        <el-button class="hero__btn" size="small" round @click="loadData">
          <el-icon><Refresh /></el-icon>
          <span>刷新数据</span>
        </el-button>
      </div>
    </div>

    <!-- 指标卡 -->
    <el-row :gutter="16" class="row">
      <el-col v-for="card in statCards" :key="card.key" :xs="24" :sm="12" :lg="6">
        <div class="stat" :class="`stat--${card.tone}`">
          <div class="stat__icon">
            <el-icon :size="26"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat__body">
            <div class="stat__label">{{ card.label }}</div>
            <div class="stat__value">
              {{ card.value }}<span class="stat__unit">{{ card.unit }}</span>
            </div>
            <div class="stat__foot">
              <el-progress
                v-if="card.key === 'completionRate'"
                :percentage="Math.min(100, Math.max(0, Number(card.value) || 0))"
                :stroke-width="6"
                :show-text="false"
                :color="card.color"
                class="stat__bar"
              />
              <span v-else class="stat__hint">{{ card.hint }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16" class="row">
      <el-col :xs="24" :lg="16">
        <div class="panel">
          <div class="panel__head">
            <div class="panel__title"><el-icon><DataLine /></el-icon>订单趋势</div>
            <span class="panel__tip">最近 7 天</span>
          </div>
          <div ref="orderChartRef" class="chart chart--lg"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="panel">
          <div class="panel__head">
            <div class="panel__title"><el-icon><TrendCharts /></el-icon>订单状态分布</div>
          </div>
          <div ref="statusChartRef" class="chart chart--lg"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 待办 + 告警 -->
    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel__head">
            <div class="panel__title"><el-icon><List /></el-icon>待处理事项</div>
            <el-badge :value="todoList.length" :hidden="!todoList.length">
              <el-button size="small" round @click="refreshTodo">
                <el-icon><Refresh /></el-icon>
                <span>刷新</span>
              </el-button>
            </el-badge>
          </div>
          <el-table
            :data="todoList"
            style="width: 100%"
            max-height="280"
            empty-text="暂无待处理事项"
            :header-cell-style="{ background: '#f7f9fc', color: '#5b6a7d', fontWeight: '600' }"
          >
            <el-table-column prop="type" label="类型" width="104">
              <template #default="{ row }">
                <el-tag size="small" effect="light" round>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="内容" show-overflow-tooltip />
            <el-table-column prop="time" label="时间" width="164" />
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleTodo(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel__head">
            <div class="panel__title"><el-icon><Bell /></el-icon>实时告警</div>
            <span class="panel__tip">{{ alerts.length }} 条</span>
          </div>
          <div class="alerts">
            <el-empty v-if="!alerts.length" description="暂无告警" :image-size="76" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="(alert, index) in alerts"
                :key="index"
                :timestamp="alert.time"
                placement="top"
                :color="levelColor(alert.level)"
              >
                <div class="alert-item">
                  <el-tag size="small" effect="dark" round :type="levelTagType(alert.level)">{{ alert.level }}</el-tag>
                  <span class="alert-item__text">{{ alert.content }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { statisticsAPI } from '@/api'
import { useUserStore } from '@/stores/user'

/**
 * 工作台：所有数字/图表/待办/告警都来自后端 GET /api/statistics/dashboard
 * （后端从 users / aircraft / airspace_zones / stations / flight_plans / orders 真实聚合）
 */

const router = useRouter()
const userStore = useUserStore()
const orderChartRef = ref(null)
const statusChartRef = ref(null)

const stats = ref({
  todayOrders: 0,
  completionRate: 0,
  flyingCount: 0,
  alertCount: 0
})

const todoList = ref([])
const alerts = ref([])

let orderChart = null
let statusChart = null
let refreshTimer = null
let clockTimer = null

// ===== 顶部横幅：问候语 / 角色 / 日期时钟 =====
const ROLE_LABELS = {
  ADMIN: '管理员',
  SUPERVISOR: '监管人员',
  AIR_CONTROL: '空管人员',
  OPERATOR: '运营人员',
  PILOT: '飞行员',
  CUSTOMER: '客户',
  MAINTENANCE: '维护人员',
  GUEST: '访客'
}
const WEEK_LABELS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

const username = computed(() => userStore.userInfo?.username || '用户')
const roleLabel = computed(() => ROLE_LABELS[userStore.userInfo?.role] || userStore.userInfo?.role || '未分配角色')
const now = ref(new Date())
const greeting = computed(() => {
  const h = now.value.getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const todayText = computed(() => {
  const d = now.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${WEEK_LABELS[d.getDay()]}`
})
const clockText = computed(() => now.value.toLocaleTimeString('zh-CN', { hour12: false }))

// ===== 指标卡（数值均来自后端统计接口）=====
const statCards = computed(() => [
  { key: 'todayOrders', label: '今日订单', value: stats.value.todayOrders, unit: '单', icon: 'Document', tone: 'blue', color: '#409eff', hint: '今日新建运单' },
  { key: 'completionRate', label: '订单完成率', value: stats.value.completionRate, unit: '%', icon: 'TrendCharts', tone: 'green', color: '#67c23a', hint: '已完成运单占比' },
  { key: 'flyingCount', label: '飞行中', value: stats.value.flyingCount, unit: '架', icon: 'Position', tone: 'orange', color: '#e6a23c', hint: '实时在空中飞行' },
  { key: 'alertCount', label: '告警数', value: stats.value.alertCount, unit: '条', icon: 'Warning', tone: 'red', color: '#f56c6c', hint: '待关注告警' }
])

// 订单状态中文名（orders 表里的原始状态值 → 展示名）
const ORDER_STATUS_LABELS = {
  PENDING: '待接单',
  ACCEPTED: '已接单',
  IN_PROGRESS: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  FAILED: '异常'
}

const levelColor = (level) => (level === '紧急' ? '#f56c6c' : level === '严重' ? '#e6a23c' : '#409eff')
const levelTagType = (level) => (level === '紧急' ? 'danger' : level === '严重' ? 'warning' : 'info')

// 初始化订单趋势图
const initOrderChart = () => {
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  orderChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,.96)',
      borderColor: '#e6ebf5',
      textStyle: { color: '#303133' }
    },
    legend: {
      data: ['订单数', '完成数'],
      right: 0,
      top: 0,
      icon: 'roundRect',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#7a8699' }
    },
    grid: { left: 8, right: 12, top: 46, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: [],
      axisLine: { lineStyle: { color: '#e6ebf5' } },
      axisTick: { show: false },
      axisLabel: { color: '#8a94a6' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f4f9' } },
      axisLabel: { color: '#8a94a6' }
    },
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 3, color: '#409eff' },
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,.30)' },
            { offset: 1, color: 'rgba(64,158,255,0)' }
          ])
        },
        data: []
      },
      {
        name: '完成数',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 3, color: '#67c23a' },
        itemStyle: { color: '#67c23a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103,194,58,.28)' },
            { offset: 1, color: 'rgba(103,194,58,0)' }
          ])
        },
        data: []
      }
    ]
  })
}

// 初始化订单状态分布图（环形，中心显示总数）
const initStatusChart = () => {
  if (!statusChartRef.value) return
  statusChart = echarts.init(statusChartRef.value)
  statusChart.setOption({
    color: ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#8e7cf0'],
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#7a8699' } },
    title: {
      text: '0',
      subtext: '总订单',
      left: 'center',
      top: '34%',
      textStyle: { fontSize: 26, fontWeight: 'bold', color: '#303133' },
      subtextStyle: { fontSize: 12, color: '#909399' }
    },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['52%', '74%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 6 },
        label: { show: false },
        labelLine: { show: false },
        data: [],
        emphasis: {
          scale: true,
          scaleSize: 6,
          itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,.18)' }
        }
      }
    ]
  })
}

const updateCharts = (data) => {
  if (orderChart) {
    orderChart.setOption({
      xAxis: { data: data.trendDates || [] },
      series: [
        { name: '订单数', data: data.trendOrders || [] },
        { name: '完成数', data: data.trendCompleted || [] }
      ]
    })
  }

  if (statusChart) {
    const distribution = data.orderStatusDistribution || {}
    const items = Object.entries(distribution).map(([status, value]) => ({
      name: ORDER_STATUS_LABELS[status] || status,
      value
    }))
    const total = items.reduce((sum, item) => sum + (Number(item.value) || 0), 0)
    statusChart.setOption({
      title: { text: String(total) },
      series: [{ data: items }]
    })
  }
}

// 加载数据
const loadData = async () => {
  try {
    const data = await statisticsAPI.getDashboard()
    stats.value = {
      todayOrders: data.todayOrders ?? 0,
      completionRate: data.completionRate ?? 0,
      flyingCount: data.flyingCount ?? 0,
      alertCount: data.alertCount ?? 0
    }
    todoList.value = data.todos || []
    alerts.value = data.alerts || []
    updateCharts(data)
  } catch (error) {
    console.error('加载工作台数据失败:', error)
    ElMessage.error('加载工作台数据失败')
  }
}

// 刷新待办
const refreshTodo = () => {
  loadData()
}

// 处理待办事项：按业务类型跳转到对应页面
const handleTodo = (row) => {
  if (row.businessType === 'flightPlan') {
    router.push('/airspace/flight-plan/list')
  } else if (row.businessType === 'order') {
    router.push('/order/list')
  } else {
    ElMessage.info('暂不支持跳转')
  }
}

const handleResize = () => {
  orderChart?.resize()
  statusChart?.resize()
}

onMounted(() => {
  initOrderChart()
  initStatusChart()
  loadData()

  // 顶部时钟每秒走一次
  clockTimer = setInterval(() => {
    now.value = new Date()
  }, 1000)

  // 每5分钟自动刷新
  refreshTimer = setInterval(loadData, 5 * 60 * 1000)

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (clockTimer) clearInterval(clockTimer)
  window.removeEventListener('resize', handleResize)
  orderChart?.dispose()
  statusChart?.dispose()
  orderChart = null
  statusChart = null
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.row {
  margin-bottom: 16px;
}

/* ===== 顶部横幅 ===== */
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 26px;
  margin-bottom: 16px;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(120deg, #2f6fed 0%, #4a8bf5 45%, #37b0d6 100%);
  box-shadow: 0 10px 26px rgba(47, 111, 237, 0.28);
  position: relative;
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  right: -60px;
  top: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.13);
}

.hero__left {
  position: relative;
  z-index: 1;
}

.hero__hello {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero__role {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.hero__chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
}

.hero__chip--ok {
  background: rgba(255, 255, 255, 0.26);
}

.hero__right {
  position: relative;
  z-index: 1;
  text-align: right;
}

.hero__cap {
  font-size: 14px;
  font-weight: 600;
}

.hero__cap-sub {
  font-size: 12px;
  opacity: 0.85;
  margin: 4px 0 10px;
}

.hero__btn {
  border: none;
  background: rgba(255, 255, 255, 0.92);
  color: #2f6fed;
  font-weight: 600;
}

.hero__btn:hover {
  background: #fff;
  color: #2f6fed;
}

/* ===== 指标卡 ===== */
.stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #eef2f8;
  border-radius: 14px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px rgba(31, 56, 96, 0.1);
}

.stat__icon {
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat--blue .stat__icon {
  background: linear-gradient(135deg, #4d9bff, #2f6fed);
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.32);
}

.stat--green .stat__icon {
  background: linear-gradient(135deg, #7ed957, #4caf50);
  box-shadow: 0 8px 16px rgba(103, 194, 58, 0.32);
}

.stat--orange .stat__icon {
  background: linear-gradient(135deg, #f7bd5c, #e6a23c);
  box-shadow: 0 8px 16px rgba(230, 162, 60, 0.32);
}

.stat--red .stat__icon {
  background: linear-gradient(135deg, #ff8a80, #f56c6c);
  box-shadow: 0 8px 16px rgba(245, 108, 108, 0.32);
}

.stat__body {
  flex: 1;
  min-width: 0;
}

.stat__label {
  font-size: 13px;
  color: #8a94a6;
}

.stat__value {
  font-size: 28px;
  font-weight: 700;
  color: #21324a;
  line-height: 1.25;
}

.stat__unit {
  font-size: 13px;
  font-weight: 500;
  color: #9aa5b5;
  margin-left: 4px;
}

.stat__foot {
  height: 16px;
  display: flex;
  align-items: center;
}

.stat__hint {
  font-size: 12px;
  color: #a6b0c0;
}

.stat__bar {
  width: 100%;
}

/* ===== 面板 ===== */
.panel {
  background: #fff;
  border: 1px solid #eef2f8;
  border-radius: 14px;
  padding: 16px 18px 12px;
  height: 100%;
  box-sizing: border-box;
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.panel__title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 15px;
  font-weight: 600;
  color: #21324a;
}

.panel__title .el-icon {
  color: #2f6fed;
}

.panel__tip {
  font-size: 12px;
  color: #9aa5b5;
}

.chart {
  width: 100%;
}

.chart--lg {
  height: 300px;
}

.alerts {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 4px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.alert-item__text {
  color: #4a5768;
  font-size: 13px;
}
</style>
