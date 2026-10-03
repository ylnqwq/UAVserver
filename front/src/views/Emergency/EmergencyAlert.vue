<template>
  <div class="emergency-alert">
    <el-card shadow="never">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card urgent">
            <div class="stat-content">
              <div class="stat-label">紧急告警</div>
              <div class="stat-value">{{ stats.urgent }}</div>
            </div>
            <el-icon class="stat-icon"><Warning /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card serious">
            <div class="stat-content">
              <div class="stat-label">严重告警</div>
              <div class="stat-value">{{ stats.serious }}</div>
            </div>
            <el-icon class="stat-icon"><WarningFilled /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card normal">
            <div class="stat-content">
              <div class="stat-label">一般告警</div>
              <div class="stat-value">{{ stats.normal }}</div>
            </div>
            <el-icon class="stat-icon"><InfoFilled /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card handled">
            <div class="stat-content">
              <div class="stat-label">今日处理</div>
              <div class="stat-value">{{ stats.handled }}</div>
            </div>
            <el-icon class="stat-icon"><CircleCheck /></el-icon>
          </el-card>
        </el-col>
      </el-row>

      <!-- 搜索和筛选 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="告警级别">
          <el-select v-model="searchForm.level" placeholder="请选择" clearable style="width: 150px">
            <el-option label="紧急" :value="3" />
            <el-option label="严重" :value="2" />
            <el-option label="一般" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 150px">
            <el-option label="未处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已处理" value="handled" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable style="width: 150px">
            <el-option label="设备故障" value="device_fault" />
            <el-option label="空域冲突" value="airspace_conflict" />
            <el-option label="偏离航线" value="route_deviation" />
            <el-option label="电量告警" value="battery_low" />
            <el-option label="订单异常" value="order_abnormal" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 告警列表 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :row-class-name="getRowClass"
      >
        <el-table-column prop="alertNo" label="告警编号" width="150" />
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" effect="dark">
              {{ getLevelName(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            {{ getTypeName(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="content" label="告警内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="relatedObject" label="涉及对象" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="occurTime" label="发生时间" width="180" />
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              type="primary"
              link
              size="small"
              @click="handleProcess(row)"
            >
              <el-icon><Edit /></el-icon>
              处理
            </el-button>
            <el-button type="info" link size="small" @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button
              v-if="row.status === 'handled'"
              type="success"
              link
              size="small"
              @click="handleClose(row)"
            >
              <el-icon><CircleClose /></el-icon>
              关闭
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

    <!-- 告警详情对话框 -->
    <el-dialog v-model="detailVisible" title="告警详情" width="800px">
      <el-descriptions :column="2" border v-if="currentAlert">
        <el-descriptions-item label="告警编号">{{ currentAlert.alertNo }}</el-descriptions-item>
        <el-descriptions-item label="告警级别">
          <el-tag :type="getLevelType(currentAlert.level)" effect="dark">
            {{ getLevelName(currentAlert.level) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="告警类型">{{ getTypeName(currentAlert.type) }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="getStatusType(currentAlert.status)">
            {{ getStatusName(currentAlert.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="涉及对象">{{ currentAlert.relatedObject }}</el-descriptions-item>
        <el-descriptions-item label="发生位置">{{ currentAlert.location }}</el-descriptions-item>
        <el-descriptions-item label="告警内容" :span="2">{{ currentAlert.content }}</el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ currentAlert.occurTime }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ currentAlert.handler || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ currentAlert.handleTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关闭时间">{{ currentAlert.closeTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>处置记录</el-divider>
      <el-timeline v-if="processRecords.length > 0">
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
      <el-empty v-else description="暂无处置记录" />
    </el-dialog>

    <!-- 处理告警对话框 -->
    <el-dialog v-model="processVisible" title="处理告警" width="600px">
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="告警编号">
          <el-input v-model="processForm.alertNo" disabled />
        </el-form-item>
        <el-form-item label="处理方式" required>
          <el-select v-model="processForm.action" placeholder="请选择">
            <el-option label="现场处置" value="on_site" />
            <el-option label="远程干预" value="remote" />
            <el-option label="协调支援" value="coordinate" />
            <el-option label="升级处理" value="escalate" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人员" required>
          <el-select v-model="processForm.assignTo" placeholder="请选择处理人员" multiple>
            <el-option v-for="h in handlerOptions" :key="h.value" :label="h.label" :value="h.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置措施" required>
          <el-input
            v-model="processForm.measures"
            type="textarea"
            :rows="4"
            placeholder="请详细描述处置措施和步骤"
          />
        </el-form-item>
        <el-form-item label="预计完成时间">
          <el-date-picker
            v-model="processForm.estimatedTime"
            type="datetime"
            placeholder="选择时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmProcess">提交处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { alertAPI, userAPI, roleAPI } from '@/api'

/**
 * 应急告警页面：数据全部来自后端 /api/alerts
 *   列表 GET /api/alerts      处置 POST /{id}/process      关闭 POST /{id}/close
 * level: 1(一般)/2(严重)/3(紧急)；status: pending/processing/handled/closed
 * 「处理人员」下拉来自后端真实用户（监管/空管/运维），不再是写死的 user1/user2/user3
 */

const loading = ref(false)
const allAlerts = ref([])
const handlerOptions = ref([])

const stats = reactive({
  urgent: 0,
  serious: 0,
  normal: 0,
  handled: 0
})

const searchForm = reactive({
  level: null,
  status: '',
  type: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  alertNo: '',
  level: 1,
  type: '',
  content: '',
  relatedObject: '',
  location: '',
  occurTime: ''
})

const detailVisible = ref(false)
const currentAlert = ref(null)
const processRecords = ref([])

const processVisible = ref(false)
const processForm = reactive({
  id: null,
  alertNo: '',
  action: '',
  assignTo: '',
  measures: '',
  estimatedTime: ''
})

const filtered = computed(() =>
  allAlerts.value.filter((a) => {
    const levelOk = searchForm.level == null || a.level === searchForm.level
    const statusOk = !searchForm.status || a.status === searchForm.status
    const typeOk = !searchForm.type || a.type === searchForm.type
    let dateOk = true
    if (searchForm.dateRange && searchForm.dateRange.length === 2 && a.occurTime) {
      const day = (a.occurTime || '').slice(0, 10)
      dateOk = day >= searchForm.dateRange[0] && day <= searchForm.dateRange[1]
    }
    return levelOk && statusOk && typeOk && dateOk
  })
)

const tableData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filtered.value.slice(start, start + pagination.size)
})

const refreshStats = () => {
  stats.urgent = allAlerts.value.filter((a) => a.level === 3).length
  stats.serious = allAlerts.value.filter((a) => a.level === 2).length
  stats.normal = allAlerts.value.filter((a) => a.level === 1).length
  stats.handled = allAlerts.value.filter((a) => a.status === 'handled' || a.status === 'closed').length
}

const loadData = async () => {
  loading.value = true
  try {
    allAlerts.value = (await alertAPI.getList()) || []
    pagination.total = filtered.value.length
    refreshStats()
  } catch (error) {
    console.error('加载告警数据失败:', error)
    ElMessage.error('加载告警数据失败')
  } finally {
    loading.value = false
  }
}

// 处理人员（真实用户：监管/空管/运维）
const loadHandlers = async () => {
  try {
    const roleOptions = (await roleAPI.getOptions()) || []
    const roleName = (code) => roleOptions.find((r) => r.code === code)?.name || code
    const roles = ['SUPERVISOR', 'AIR_CONTROL', 'OPERATOR', 'MAINTENANCE']
    const results = await Promise.all(
      roles.map((r) => userAPI.getByRole(r, { silent: true }).catch(() => []))
    )
    handlerOptions.value = results
      .flat()
      .filter(Boolean)
      .map((u) => ({
        value: u.realName || u.username,
        label: `${u.realName || u.username}（${roleName(u.role)}）`
      }))
  } catch (error) {
    console.error('加载处理人员失败:', error)
  }
}

// ===== 查询 / 重置 =====
const handleSearch = () => {
  pagination.page = 1
  pagination.total = filtered.value.length
}

const handleReset = () => {
  Object.assign(searchForm, { level: null, status: '', type: '', dateRange: [] })
  handleSearch()
}

// ===== 详情 =====
const handleView = (row) => {
  currentAlert.value = row
  processRecords.value = row.handler
    ? [
        {
          time: row.handleTime,
          operator: row.handler,
          action: getMethodName(row.handleMethod),
          description: row.handleMeasure || '无',
          expectedTime: row.expectedTime
        }
      ]
    : []
  detailVisible.value = true
}

// ===== 处置 =====
const handleProcess = (row) => {
  processForm.id = row.id
  processForm.alertNo = row.alertNo
  processForm.action = ''
  processForm.assignTo = row.handler ? row.handler.split('、') : []
  processForm.measures = ''
  processForm.estimatedTime = ''
  processVisible.value = true
}

const confirmProcess = async () => {
  if (!processForm.action) {
    ElMessage.warning('请选择处理方式')
    return
  }
  if (!processForm.measures) {
    ElMessage.warning('请填写处置措施')
    return
  }
  if (!processForm.assignTo || processForm.assignTo.length === 0) {
    ElMessage.warning('请选择处理人员')
    return
  }
  try {
    await alertAPI.process(processForm.id, {
      handleMethod: processForm.action,
      handler: Array.isArray(processForm.assignTo) ? processForm.assignTo.join('、') : processForm.assignTo,
      handleMeasure: processForm.measures,
      expectedTime: processForm.estimatedTime || null
    })
    ElMessage.success('处置已提交')
    processVisible.value = false
    loadData()
  } catch (error) {
    console.error('处置告警失败:', error)
  }
}

// ===== 关闭 =====
const handleClose = async (row) => {
  try {
    await ElMessageBox.confirm(`确定关闭告警「${row.alertNo}」吗？`, '提示', { type: 'warning' })
    await alertAPI.close(row.id)
    ElMessage.success('已关闭')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('关闭告警失败:', error)
    }
  }
}

// ===== 映射 =====
const getLevelType = (level) => {
  const map = { 3: 'danger', 2: 'warning', 1: 'info' }
  return map[level] || ''
}

const getLevelName = (level) => {
  const map = { 3: '紧急', 2: '严重', 1: '一般' }
  return map[level] || '-'
}

const getTypeName = (type) => {
  const map = {
    device_fault: '设备故障',
    airspace_conflict: '空域冲突',
    route_deviation: '偏离航线',
    battery_low: '电量告警',
    order_abnormal: '订单异常',
    other: '其他'
  }
  return map[type] || type
}

const getMethodName = (method) => {
  const map = { on_site: '现场处置', remote: '远程干预', coordinate: '协调支援', escalate: '升级处理' }
  return map[method] || '-'
}

const getStatusType = (status) => {
  const map = { pending: 'danger', processing: 'warning', handled: 'success', closed: 'info' }
  return map[status] || ''
}

const getStatusName = (status) => {
  const map = { pending: '未处理', processing: '处理中', handled: '已处理', closed: '已关闭' }
  return map[status] || status
}

const getRowClass = ({ row }) => {
  if (row.status === 'pending' && row.level === 3) return 'row-danger'
  if (row.status === 'pending' && row.level === 2) return 'row-warning'
  return ''
}

onMounted(() => {
  loadHandlers()
  loadData()
})
</script>

<style scoped>
.emergency-alert {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-card .stat-content {
  position: relative;
  z-index: 1;
}

.stat-card .stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-card .stat-value {
  font-size: 32px;
  font-weight: bold;
}

.stat-card .stat-icon {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 60px;
  opacity: 0.3;
}

.stat-card.urgent .stat-value {
  color: #f56c6c;
}

.stat-card.serious .stat-value {
  color: #e6a23c;
}

.stat-card.normal .stat-value {
  color: #409eff;
}

.stat-card.handled .stat-value {
  color: #67c23a;
}

.search-form {
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
}

:deep(.urgent-row) {
  background-color: #fef0f0;
}

:deep(.serious-row) {
  background-color: #fdf6ec;
}
</style>
