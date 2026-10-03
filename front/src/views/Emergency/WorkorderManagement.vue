<template>
  <div class="workorder-management">
    <el-card shadow="never">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <el-statistic title="待处理工单" :value="stats.pending">
              <template #suffix>
                <el-tag type="danger" size="small">紧急</el-tag>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <el-statistic title="处理中" :value="stats.processing" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <el-statistic title="今日已完成" :value="stats.todayCompleted" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <el-statistic title="平均响应时间" :value="stats.avgResponseTime">
              <template #suffix>分钟</template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="工单编号">
          <el-input v-model="searchForm.workorderNo" placeholder="请输入工单编号" clearable />
        </el-form-item>
        <el-form-item label="工单类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable style="width: 150px">
            <el-option label="投诉" value="complaint" />
            <el-option label="咨询" value="inquiry" />
            <el-option label="故障报修" value="fault" />
            <el-option label="建议" value="suggestion" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="searchForm.priority" placeholder="请选择" clearable style="width: 150px">
            <el-option label="紧急" :value="3" />
            <el-option label="高" :value="2" />
            <el-option label="普通" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 150px">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完成" value="completed" />
            <el-option label="已关闭" value="closed" />
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
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          创建工单
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>

      <!-- 工单列表 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :row-class-name="getRowClass"
      >
        <el-table-column prop="workorderNo" label="工单编号" width="150" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeColor(row.type)" size="small">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="getPriorityColor(row.priority)" size="small">
              {{ getPriorityName(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="success"
              link
              @click="handleAccept(row)"
            >
              接单
            </el-button>
            <el-button
              v-if="row.status === 'processing'"
              type="warning"
              link
              @click="handleComplete(row)"
            >
              完成
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

    <!-- 创建工单对话框 -->
    <el-dialog v-model="createVisible" title="创建工单" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="工单类型" required>
          <el-select v-model="createForm.type" placeholder="请选择" style="width: 100%">
            <el-option label="投诉" value="complaint" />
            <el-option label="咨询" value="inquiry" />
            <el-option label="故障报修" value="fault" />
            <el-option label="建议" value="suggestion" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" required>
          <el-radio-group v-model="createForm.priority">
            <el-radio :value="3">紧急</el-radio>
            <el-radio :value="2">高</el-radio>
            <el-radio :value="1">普通</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="关联订单">
          <el-input v-model="createForm.orderNo" placeholder="请输入订单号（可选）" />
        </el-form-item>
        <el-form-item label="问题描述" required>
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="5"
            placeholder="请详细描述问题"
          />
        </el-form-item>
        <el-form-item label="关联订单号">
          <el-input v-model="createForm.orderNo" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- 工单详情抽屉 -->
    <el-drawer v-model="detailVisible" title="工单详情" size="700px">
      <div v-if="currentWorkorder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号">{{ currentWorkorder.workorderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusColor(currentWorkorder.status)">
              {{ getStatusName(currentWorkorder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="getTypeColor(currentWorkorder.type)">
              {{ getTypeName(currentWorkorder.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityColor(currentWorkorder.priority)">
              {{ getPriorityName(currentWorkorder.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="客户">{{ currentWorkorder.customerName }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ currentWorkorder.phone }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ currentWorkorder.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ currentWorkorder.handler || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ currentWorkorder.title }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">
            <div style="white-space: pre-wrap;">{{ currentWorkorder.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentWorkorder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentWorkorder.updateTime }}</el-descriptions-item>
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

        <el-divider>回复</el-divider>

        <el-input
          v-model="replyContent"
          type="textarea"
          :rows="4"
          placeholder="请输入回复内容"
          style="margin-bottom: 10px"
        />
        <el-button type="primary" @click="handleReply">
          <el-icon><ChatLineRound /></el-icon>
          回复
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { workorderAPI, userAPI, roleAPI } from '@/api'

/**
 * 客服工单页面：数据全部来自后端 /api/workorders
 *   列表 GET /api/workorders       创建 POST /api/workorders
 *   接单 POST /{id}/assign         回复(解决) POST /{id}/reply      关闭 POST /{id}/close
 * type: complaint(投诉)/inquiry(咨询)/fault(故障报修)/suggestion(建议)
 * priority: 1(普通)/2(高)/3(紧急)；status: pending/processing/completed/closed
 */

const loading = ref(false)
const allWorkorders = ref([])
const handlerOptions = ref([])

const stats = reactive({
  pending: 0,
  processing: 0,
  todayCompleted: 0,
  avgResponseTime: 0
})

const searchForm = reactive({
  workorderNo: '',
  type: '',
  priority: null,
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const createVisible = ref(false)
const createForm = reactive({
  type: 'complaint',
  priority: 1,
  title: '',
  content: '',
  customerName: '',
  phone: '',
  orderNo: ''
})

// 兼容模板里的 form 绑定
const form = createForm

const detailVisible = ref(false)
const currentWorkorder = ref(null)
const processRecords = ref([])
const replyContent = ref('')

const filtered = computed(() =>
  allWorkorders.value.filter((w) => {
    const noOk = !searchForm.workorderNo || (w.workorderNo || '').includes(searchForm.workorderNo.trim())
    const typeOk = !searchForm.type || w.type === searchForm.type
    const priorityOk = searchForm.priority == null || w.priority === searchForm.priority
    const statusOk = !searchForm.status || w.status === searchForm.status
    return noOk && typeOk && priorityOk && statusOk
  })
)

const tableData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filtered.value.slice(start, start + pagination.size)
})

const refreshStats = () => {
  const today = new Date().toISOString().slice(0, 10)
  stats.pending = allWorkorders.value.filter((w) => w.status === 'pending').length
  stats.processing = allWorkorders.value.filter((w) => w.status === 'processing').length
  stats.todayCompleted = allWorkorders.value.filter(
    (w) => w.resolvedAt && String(w.resolvedAt).slice(0, 10) === today
  ).length

  // 平均响应时间（小时）：创建 → 解决
  const resolved = allWorkorders.value.filter((w) => w.createdAt && w.resolvedAt)
  if (resolved.length) {
    const totalHours = resolved.reduce(
      (sum, w) => sum + (new Date(w.resolvedAt) - new Date(w.createdAt)) / 3600000,
      0
    )
    stats.avgResponseTime = Math.round((totalHours / resolved.length) * 10) / 10
  } else {
    stats.avgResponseTime = 0
  }
}

const loadData = async () => {
  loading.value = true
  try {
    allWorkorders.value = (await workorderAPI.getList()) || []
    pagination.total = filtered.value.length
    refreshStats()
  } catch (error) {
    console.error('加载工单失败:', error)
    ElMessage.error('加载工单失败')
  } finally {
    loading.value = false
  }
}

// 处理人下拉（真实用户）
const loadHandlers = async () => {
  try {
    const roleOptions = (await roleAPI.getOptions()) || []
    const roleName = (code) => roleOptions.find((r) => r.code === code)?.name || code
    const roles = ['OPERATOR', 'SUPERVISOR', 'AIR_CONTROL']
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
    console.error('加载处理人失败:', error)
  }
}

// ===== 查询 / 重置 / 导出 =====
const handleSearch = () => {
  pagination.page = 1
  pagination.total = filtered.value.length
}

const handleReset = () => {
  Object.assign(searchForm, { workorderNo: '', type: '', priority: null, status: '' })
  handleSearch()
}

const handleExport = () => {
  const header = ['工单编号', '类型', '优先级', '标题', '客户', '状态', '处理人', '创建时间']
  const rows = filtered.value.map((w) => [
    w.workorderNo,
    getTypeName(w.type),
    getPriorityName(w.priority),
    w.title || '',
    w.customerName || '',
    getStatusName(w.status),
    w.handler || '',
    w.createdAt || ''
  ])
  const csv = [header, ...rows].map((r) => r.map((x) => `"${x ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `工单列表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出当前筛选结果')
}

// ===== 创建工单 =====
const handleCreate = () => {
  Object.assign(createForm, {
    type: 'complaint',
    priority: 1,
    title: '',
    content: '',
    customerName: '',
    phone: '',
    orderNo: ''
  })
  createVisible.value = true
}

const confirmCreate = async () => {
  if (!createForm.title || !createForm.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  try {
    await workorderAPI.create({
      type: createForm.type,
      priority: createForm.priority,
      title: createForm.title,
      content: createForm.content,
      customerName: createForm.customerName,
      contact: createForm.phone,
      orderNo: createForm.orderNo || null
    })
    ElMessage.success('工单已创建')
    createVisible.value = false
    loadData()
  } catch (error) {
    console.error('创建工单失败:', error)
  }
}

// ===== 详情 =====
const handleView = (row) => {
  currentWorkorder.value = row
  replyContent.value = row.reply || ''
  processRecords.value = []
  if (row.handler) {
    processRecords.value.push({
      time: row.createdAt,
      operator: row.handler,
      action: '接单处理',
      description: `工单分派给 ${row.handler}`
    })
  }
  if (row.reply) {
    processRecords.value.push({
      time: row.resolvedAt,
      operator: row.handler || '客服',
      action: '回复解决',
      description: row.reply
    })
  }
  detailVisible.value = true
}

// ===== 接单 / 完成 / 回复 =====
const handleAccept = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请选择处理人（可下拉选择真实人员）', '接单处理', {
      inputValue: handlerOptions.value[0]?.value || '',
      inputPlaceholder: '处理人姓名'
    })
    await workorderAPI.assign(row.id, value)
    ElMessage.success('已接单')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('接单失败:', error)
    }
  }
}

const handleComplete = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入处理结果', '完成工单', {
      inputPattern: /.+/,
      inputErrorMessage: '处理结果不能为空'
    })
    await workorderAPI.reply(row.id, { reply: value, handler: row.handler })
    ElMessage.success('工单已完成')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('完成工单失败:', error)
    }
  }
}

const handleReply = async () => {
  if (!replyContent.value) {
    ElMessage.warning('请输入回复内容')
    return
  }
  try {
    await workorderAPI.reply(currentWorkorder.value.id, {
      reply: replyContent.value,
      handler: currentWorkorder.value.handler
    })
    ElMessage.success('已回复并解决')
    detailVisible.value = false
    loadData()
  } catch (error) {
    console.error('回复失败:', error)
  }
}

// ===== 映射 =====
const getTypeColor = (type) => {
  const map = { complaint: 'danger', inquiry: 'primary', fault: 'warning', suggestion: 'success' }
  return map[type] || ''
}

const getTypeName = (type) => {
  const map = { complaint: '投诉', inquiry: '咨询', fault: '故障报修', suggestion: '建议' }
  return map[type] || type
}

const getPriorityColor = (priority) => {
  const map = { 3: 'danger', 2: 'warning', 1: 'info' }
  return map[priority] || ''
}

const getPriorityName = (priority) => {
  const map = { 3: '紧急', 2: '高', 1: '普通' }
  return map[priority] || '-'
}

const getStatusColor = (status) => {
  const map = { pending: 'danger', processing: 'warning', completed: 'success', closed: 'info' }
  return map[status] || ''
}

const getStatusName = (status) => {
  const map = { pending: '待处理', processing: '处理中', completed: '已完成', closed: '已关闭' }
  return map[status] || status
}

const getRowClass = ({ row }) => {
  if (row.status === 'pending' && row.priority === 3) return 'row-danger'
  if (row.status === 'pending' && row.priority === 2) return 'row-warning'
  return ''
}

onMounted(() => {
  loadHandlers()
  loadData()
})
</script>

<style scoped>
.workorder-management {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
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

:deep(.urgent-row) {
  background-color: #fef0f0;
}
</style>
