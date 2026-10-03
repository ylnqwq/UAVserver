<template>
  <div class="resource-schedule">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">资源调度</span>
          <div>
            <el-button type="primary" @click="handleAutoSchedule">
              <el-icon><MagicStick /></el-icon>
              智能调度
            </el-button>
            <el-button @click="handleRefresh">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="日期选择">
          <el-date-picker
            v-model="filterForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="filterForm.resourceType" placeholder="全部" clearable style="width: 150px">
            <el-option label="飞行器" value="aircraft" />
            <el-option label="机长" value="pilot" />
            <el-option label="场站" value="station" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="空闲" value="idle" />
            <el-option label="使用中" value="busy" />
            <el-option label="维护中" value="maintenance" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">查询</el-button>
          <el-button @click="handleResetFilter">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 资源概览 -->
      <div class="resource-overview">
        <el-card class="overview-card aircraft">
          <div class="card-icon"><el-icon><Promotion /></el-icon></div>
          <div class="card-content">
            <div class="card-title">飞行器</div>
            <div class="card-stats">
              <span class="total">总数: {{ resourceStats.aircraft.total }}</span>
              <span class="available">可用: {{ resourceStats.aircraft.available }}</span>
              <span class="busy">使用中: {{ resourceStats.aircraft.busy }}</span>
            </div>
          </div>
        </el-card>
        <el-card class="overview-card pilot">
          <div class="card-icon"><el-icon><User /></el-icon></div>
          <div class="card-content">
            <div class="card-title">机长</div>
            <div class="card-stats">
              <span class="total">总数: {{ resourceStats.pilot.total }}</span>
              <span class="available">可用: {{ resourceStats.pilot.available }}</span>
              <span class="busy">任务中: {{ resourceStats.pilot.busy }}</span>
            </div>
          </div>
        </el-card>
        <el-card class="overview-card station">
          <div class="card-icon"><el-icon><House /></el-icon></div>
          <div class="card-content">
            <div class="card-title">场站</div>
            <div class="card-stats">
              <span class="total">总数: {{ resourceStats.station.total }}</span>
              <span class="available">可用: {{ resourceStats.station.available }}</span>
              <span class="busy">占用中: {{ resourceStats.station.busy }}</span>
            </div>
          </div>
        </el-card>
        <el-card class="overview-card utilization">
          <div class="card-icon"><el-icon><PieChart /></el-icon></div>
          <div class="card-content">
            <div class="card-title">资源利用率</div>
            <div class="card-value">{{ resourceStats.utilization }}%</div>
          </div>
        </el-card>
      </div>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" style="margin-top: 20px">
        <!-- 飞行器调度 -->
        <el-tab-pane label="飞行器调度" name="aircraft">
          <el-table :data="aircraftScheduleList" style="width: 100%" v-loading="loading">
            <el-table-column prop="aircraftName" label="飞行器" width="150" />
            <el-table-column prop="model" label="型号" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)" size="small">
                  {{ getStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="battery" label="电量" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.battery" :color="getBatteryColor(row.battery)" />
              </template>
            </el-table-column>
            <el-table-column prop="currentTask" label="当前任务" min-width="200" show-overflow-tooltip />
            <el-table-column prop="location" label="当前位置" width="150" />
            <el-table-column prop="nextAvailableTime" label="下次可用时间" width="160" />
            <el-table-column label="今日任务" width="250">
              <template #default="{ row }">
                <el-timeline>
                  <el-timeline-item
                    v-for="task in row.todayTasks"
                    :key="task.id"
                    :timestamp="task.time"
                    size="small"
                    :color="task.status === 'completed' ? '#67c23a' : task.status === 'in_progress' ? '#409eff' : '#909399'"
                  >
                    {{ task.orderNo }}
                  </el-timeline-item>
                </el-timeline>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewSchedule(row, 'aircraft')">查看详情</el-button>
                <el-button link type="success" @click="handleAssignTask(row)" :disabled="row.status !== 'idle'">
                  分配任务
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 机长调度 -->
        <el-tab-pane label="机长调度" name="pilot">
          <el-table :data="pilotScheduleList" style="width: 100%" v-loading="loading">
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="license" label="执照编号" width="150" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)" size="small">
                  {{ getStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="workHours" label="工作时长" width="120">
              <template #default="{ row }">
                {{ row.workHours }} / 8 小时
              </template>
            </el-table-column>
            <el-table-column prop="currentTask" label="当前任务" min-width="200" show-overflow-tooltip />
            <el-table-column prop="experience" label="飞行经验" width="120">
              <template #default="{ row }">
                {{ row.experience }} 小时
              </template>
            </el-table-column>
            <el-table-column label="今日排班" width="300">
              <template #default="{ row }">
                <div class="schedule-bar">
                  <div
                    v-for="schedule in row.todaySchedule"
                    :key="schedule.id"
                    class="schedule-block"
                    :class="schedule.type"
                    :style="{ left: schedule.left + '%', width: schedule.width + '%' }"
                    @click="handleViewScheduleBlock(schedule)"
                  >
                    {{ schedule.label }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewSchedule(row, 'pilot')">查看详情</el-button>
                <el-button link type="success" @click="handleAssignPilot(row)" :disabled="row.status !== 'idle'">
                  分配任务
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 场站调度 -->
        <el-tab-pane label="场站调度" name="station">
          <el-table :data="stationScheduleList" style="width: 100%" v-loading="loading">
            <el-table-column prop="name" label="场站名称" width="180" />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getStationTypeTag(row.type)" size="small">
                  {{ getStationTypeName(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="capacity" label="容量" width="100" />
            <el-table-column prop="usage" label="使用率" width="150">
              <template #default="{ row }">
                <el-progress :percentage="row.usage" :color="getUsageColor(row.usage)" />
              </template>
            </el-table-column>
            <el-table-column prop="available" label="可用数" width="100">
              <template #default="{ row }">
                {{ row.capacity - row.occupied }} / {{ row.capacity }}
              </template>
            </el-table-column>
            <el-table-column label="今日预约" width="400">
              <template #default="{ row }">
                <el-timeline>
                  <el-timeline-item
                    v-for="booking in row.todayBookings"
                    :key="booking.id"
                    :timestamp="booking.time"
                    size="small"
                  >
                    {{ booking.aircraft }} - {{ booking.purpose }}
                  </el-timeline-item>
                </el-timeline>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewSchedule(row, 'station')">查看详情</el-button>
                <el-button link type="success" @click="handleBookStation(row)">预约</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 智能调度对话框 -->
    <el-dialog v-model="autoScheduleVisible" title="智能调度" width="800px">
      <el-alert title="智能调度说明" type="info" :closable="false" style="margin-bottom: 20px">
        系统将根据订单需求、资源状态、空域情况等因素，自动生成最优调度方案
      </el-alert>

      <el-form :model="scheduleForm" label-width="120px">
        <el-form-item label="调度范围">
          <el-radio-group v-model="scheduleForm.scope">
            <el-radio value="pending">仅待分配订单</el-radio>
            <el-radio value="today">今日全部订单</el-radio>
            <el-radio value="week">本周订单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优化目标">
          <el-checkbox-group v-model="scheduleForm.objectives">
            <el-checkbox label="时效优先" value="time" />
            <el-checkbox label="成本优先" value="cost" />
            <el-checkbox label="负载均衡" value="balance" />
            <el-checkbox label="能耗最低" value="energy" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="约束条件">
          <el-checkbox-group v-model="scheduleForm.constraints">
            <el-checkbox label="考虑空域限制" value="airspace" />
            <el-checkbox label="考虑气象条件" value="weather" />
            <el-checkbox label="考虑维保计划" value="maintenance" />
            <el-checkbox label="考虑人员工时" value="workhours" />
          </el-checkbox-group>
        </el-form-item>
      </el-form>

      <!-- 调度结果 -->
      <div v-if="scheduleResult.show" style="margin-top: 20px">
        <el-divider content-position="left">调度结果</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="可调度订单">{{ scheduleResult.totalOrders }}</el-descriptions-item>
          <el-descriptions-item label="成功分配">{{ scheduleResult.successCount }}</el-descriptions-item>
          <el-descriptions-item label="预计完成时间">{{ scheduleResult.estimatedTime }}</el-descriptions-item>
          <el-descriptions-item label="资源利用率">{{ scheduleResult.utilization }}%</el-descriptions-item>
        </el-descriptions>

        <el-table :data="scheduleResult.details" style="width: 100%; margin-top: 20px" max-height="300">
          <el-table-column prop="orderNo" label="订单号" width="150" />
          <el-table-column prop="aircraft" label="分配飞行器" width="120" />
          <el-table-column prop="pilot" label="分配机长" width="100" />
          <el-table-column prop="estimatedTime" label="预计时长" width="100" />
          <el-table-column prop="score" label="匹配度" width="100">
            <template #default="{ row }">
              <el-progress :percentage="row.score" :color="getScoreColor(row.score)" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="autoScheduleVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRunSchedule" :loading="scheduling" v-if="!scheduleResult.show">
          运行调度
        </el-button>
        <el-button type="success" @click="handleApplySchedule" v-if="scheduleResult.show">
          应用方案
        </el-button>
      </template>
    </el-dialog>

    <!-- 分配任务对话框 -->
    <el-dialog v-model="assignTaskVisible" title="分配任务" width="600px">
      <el-form :model="assignForm" :rules="assignRules" ref="assignFormRef" label-width="100px">
        <el-form-item label="选择订单" prop="orderId">
          <el-select v-model="assignForm.orderId" placeholder="请选择待分配订单" filterable style="width: 100%">
            <el-option
              v-for="order in pendingOrders"
              :key="order.id"
              :label="order.orderNo + ' - ' + order.destination"
              :value="order.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="机长" prop="pilotId" v-if="assignForm.resourceType === 'aircraft'">
          <el-select v-model="assignForm.pilotId" placeholder="请选择机长" filterable style="width: 100%">
            <el-option
              v-for="pilot in availablePilots"
              :key="pilot.id"
              :label="pilot.name"
              :value="pilot.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划时间" prop="scheduledTime">
          <el-date-picker
            v-model="assignForm.scheduledTime"
            type="datetime"
            placeholder="选择执行时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.remarks" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignTaskVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitAssign" :loading="assigning">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh, Promotion, User, House, PieChart } from '@element-plus/icons-vue'

// 筛选表单
const filterForm = reactive({
  date: new Date().toISOString().split('T')[0],
  resourceType: '',
  status: ''
})

// 资源统计
const resourceStats = reactive({
  aircraft: { total: 25, available: 18, busy: 7 },
  pilot: { total: 30, available: 22, busy: 8 },
  station: { total: 15, available: 12, busy: 3 },
  utilization: 72.5
})

// 当前标签页
const activeTab = ref('aircraft')

// 列表数据
const aircraftScheduleList = ref([])
const pilotScheduleList = ref([])
const stationScheduleList = ref([])
const loading = ref(false)

// 智能调度
const autoScheduleVisible = ref(false)
const scheduleForm = reactive({
  scope: 'pending',
  objectives: ['time'],
  constraints: ['airspace', 'weather']
})
const scheduleResult = reactive({
  show: false,
  totalOrders: 0,
  successCount: 0,
  estimatedTime: '',
  utilization: 0,
  details: []
})
const scheduling = ref(false)

// 分配任务
const assignTaskVisible = ref(false)
const assignForm = reactive({
  resourceId: null,
  resourceType: '',
  orderId: null,
  pilotId: null,
  scheduledTime: '',
  remarks: ''
})
const assignRules = {
  orderId: [{ required: true, message: '请选择订单', trigger: 'change' }],
  scheduledTime: [{ required: true, message: '请选择执行时间', trigger: 'change' }]
}
const assignFormRef = ref(null)
const assigning = ref(false)

// 待分配订单
const pendingOrders = ref([])
const availablePilots = ref([])

// 初始化
onMounted(() => {
  fetchScheduleData()
  fetchPendingOrders()
  fetchAvailablePilots()
})

// 获取调度数据
const fetchScheduleData = () => {
  loading.value = true

  setTimeout(() => {
    // 飞行器调度数据
    aircraftScheduleList.value = [
      {
        id: 1,
        aircraftName: 'DJI M300-001',
        model: 'M300 RTK',
        status: 'idle',
        battery: 95,
        currentTask: null,
        location: '北京场站',
        nextAvailableTime: '立即可用',
        todayTasks: [
          { id: 1, orderNo: 'ORD-001', time: '09:00', status: 'completed' },
          { id: 2, orderNo: 'ORD-002', time: '11:30', status: 'completed' }
        ]
      },
      {
        id: 2,
        aircraftName: 'DJI M350-001',
        model: 'M350 RTK',
        status: 'busy',
        battery: 68,
        currentTask: 'ORD-2024091700056',
        location: '飞行中',
        nextAvailableTime: '2026-09-17 16:30',
        todayTasks: [
          { id: 1, orderNo: 'ORD-005', time: '08:00', status: 'completed' },
          { id: 2, orderNo: 'ORD-056', time: '14:00', status: 'in_progress' }
        ]
      }
    ]

    // 机长调度数据
    pilotScheduleList.value = [
      {
        id: 1,
        name: '张三',
        license: 'PL-2023-001',
        status: 'idle',
        workHours: 4.5,
        currentTask: null,
        experience: 1200,
        todaySchedule: [
          { id: 1, label: 'ORD-001', type: 'completed', left: 12.5, width: 20 },
          { id: 2, label: 'ORD-002', type: 'completed', left: 47.9, width: 15 }
        ]
      },
      {
        id: 2,
        name: '李四',
        license: 'PL-2023-002',
        status: 'busy',
        workHours: 6.0,
        currentTask: 'ORD-2024091700056',
        experience: 850,
        todaySchedule: [
          { id: 1, label: 'ORD-005', type: 'completed', left: 0, width: 25 },
          { id: 2, label: 'ORD-056', type: 'in_progress', left: 58.3, width: 25 }
        ]
      }
    ]

    // 场站调度数据
    stationScheduleList.value = [
      {
        id: 1,
        name: '北京朝阳起降场',
        type: 'takeoff',
        capacity: 10,
        occupied: 3,
        usage: 30,
        todayBookings: [
          { id: 1, aircraft: 'M300-001', purpose: '物流配送', time: '09:00-10:00' },
          { id: 2, aircraft: 'M350-001', purpose: '物流配送', time: '14:00-15:30' }
        ]
      },
      {
        id: 2,
        name: '海淀换电站A',
        type: 'charging',
        capacity: 8,
        occupied: 5,
        usage: 62.5,
        todayBookings: [
          { id: 1, aircraft: 'M300-002', purpose: '快速换电', time: '10:30' },
          { id: 2, aircraft: 'M300-003', purpose: '快速换电', time: '12:00' }
        ]
      }
    ]

    loading.value = false
  }, 500)
}

// 待分配订单
const fetchPendingOrders = () => {
  pendingOrders.value = [
    { id: 1, orderNo: 'ORD-2024091700100', destination: '海淀区中关村' },
    { id: 2, orderNo: 'ORD-2024091700101', destination: '朝阳区CBD' }
  ]
}

// 可用机长
const fetchAvailablePilots = () => {
  availablePilots.value = [
    { id: 1, name: '张三' },
    { id: 2, name: '王五' }
  ]
}

// 标签页切换
const handleTabChange = (tab) => {
  // 切换时可以重新加载数据
}

// 筛选
const handleFilter = () => {
  fetchScheduleData()
}

// 重置筛选
const handleResetFilter = () => {
  filterForm.date = new Date().toISOString().split('T')[0]
  filterForm.resourceType = ''
  filterForm.status = ''
  handleFilter()
}

// 刷新
const handleRefresh = () => {
  ElMessage.success('数据已刷新')
  fetchScheduleData()
}

// 智能调度
const handleAutoSchedule = () => {
  scheduleResult.show = false
  autoScheduleVisible.value = true
}

// 运行调度
const handleRunSchedule = () => {
  scheduling.value = true

  setTimeout(() => {
    scheduleResult.show = true
    scheduleResult.totalOrders = 15
    scheduleResult.successCount = 13
    scheduleResult.estimatedTime = '2026-09-17 18:00'
    scheduleResult.utilization = 85.6
    scheduleResult.details = [
      { orderNo: 'ORD-100', aircraft: 'M300-001', pilot: '张三', estimatedTime: '45min', score: 95 },
      { orderNo: 'ORD-101', aircraft: 'M350-001', pilot: '李四', estimatedTime: '38min', score: 92 }
    ]
    scheduling.value = false
  }, 2000)
}

// 应用方案
const handleApplySchedule = () => {
  ElMessage.success('调度方案已应用')
  autoScheduleVisible.value = false
  fetchScheduleData()
}

// 查看详情
const handleViewSchedule = (row, type) => {
  ElMessage.info(`查看${type}详情`)
}

// 查看排班块
const handleViewScheduleBlock = (schedule) => {
  ElMessage.info(`查看任务: ${schedule.label}`)
}

// 分配任务
const handleAssignTask = (row) => {
  assignForm.resourceId = row.id
  assignForm.resourceType = 'aircraft'
  assignForm.orderId = null
  assignForm.pilotId = null
  assignForm.scheduledTime = ''
  assignForm.remarks = ''
  assignTaskVisible.value = true
}

// 分配机长
const handleAssignPilot = (row) => {
  assignForm.resourceId = row.id
  assignForm.resourceType = 'pilot'
  assignForm.orderId = null
  assignForm.scheduledTime = ''
  assignForm.remarks = ''
  assignTaskVisible.value = true
}

// 提交分配
const handleSubmitAssign = () => {
  assignFormRef.value.validate((valid) => {
    if (valid) {
      assigning.value = true
      setTimeout(() => {
        ElMessage.success('任务分配成功')
        assignTaskVisible.value = false
        assigning.value = false
        fetchScheduleData()
      }, 1000)
    }
  })
}

// 预约场站
const handleBookStation = (row) => {
  ElMessage.info('预约场站功能')
}

// 辅助函数
const getStatusName = (status) => {
  const map = { idle: '空闲', busy: '使用中', maintenance: '维护中' }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = { idle: 'success', busy: 'warning', maintenance: 'info' }
  return map[status] || ''
}

const getBatteryColor = (battery) => {
  if (battery >= 60) return '#67c23a'
  if (battery >= 30) return '#e6a23c'
  return '#f56c6c'
}

const getUsageColor = (usage) => {
  if (usage < 60) return '#67c23a'
  if (usage < 80) return '#e6a23c'
  return '#f56c6c'
}

const getScoreColor = (score) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getStationTypeName = (type) => {
  const map = { takeoff: '起降场', charging: '换电站', maintenance: '维修站' }
  return map[type] || type
}

const getStationTypeTag = (type) => {
  const map = { takeoff: 'primary', charging: 'success', maintenance: 'warning' }
  return map[type] || ''
}
</script>

<style scoped>
.resource-schedule {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.filter-form {
  margin-top: 20px;
}

.resource-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.overview-card {
  display: flex;
  align-items: center;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.overview-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.overview-card.aircraft {
  border-left: 4px solid #409eff;
}

.overview-card.pilot {
  border-left: 4px solid #67c23a;
}

.overview-card.station {
  border-left: 4px solid #e6a23c;
}

.overview-card.utilization {
  border-left: 4px solid #f56c6c;
}

.card-icon {
  font-size: 48px;
  margin-right: 20px;
  opacity: 0.8;
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.card-stats {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 13px;
}

.card-stats .total {
  font-weight: bold;
}

.card-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.schedule-bar {
  position: relative;
  height: 40px;
  background: #f5f7fa;
  border-radius: 4px;
}

.schedule-block {
  position: absolute;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  font-size: 12px;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.schedule-block:hover {
  filter: brightness(1.1);
}

.schedule-block.completed {
  background: #67c23a;
}

.schedule-block.in_progress {
  background: #409eff;
}

.schedule-block.pending {
  background: #909399;
}
</style>
