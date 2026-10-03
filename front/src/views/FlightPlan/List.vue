<template>
  <div class="flight-plan-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>飞行计划管理</span>
          <el-button type="primary" icon="Plus" @click="$router.push('/flight-plans/create')">
            创建飞行计划
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部状态"
            clearable
            class="filter-select"
            style="width: 150px"
          >
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="审核中" value="UNDER_REVIEW" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleSearch">
            搜索
          </el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
          <span class="result-hint">
            共 {{ tableData.length }} 条{{ appliedStatus ? `（${getStatusLabel(appliedStatus)}）` : '' }}
          </span>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="planNumber" label="计划编号" width="180" />
        <el-table-column prop="planName" label="计划名称" width="200" />
        <el-table-column prop="aircraftName" label="飞行器" width="150" />
        <el-table-column prop="pilotName" label="飞行员" width="100" />
        <el-table-column prop="plannedStartTime" label="计划起飞时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.plannedStartTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="success"
              size="small"
              @click="handleSubmit(row)"
            >
              提交
            </el-button>
            <el-button
              v-if="['SUBMITTED', 'UNDER_REVIEW'].includes(row.status) && userStore.isSupervisor()"
              link
              type="warning"
              size="small"
              @click="handleApprove(row)"
            >
              审批
            </el-button>
            <el-button
              v-if="!['COMPLETED', 'CANCELLED'].includes(row.status)"
              link
              type="danger"
              size="small"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog v-model="approvalDialog" title="审批飞行计划" width="500px">
      <el-form :model="approvalForm" label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approvalForm.approved">
            <el-radio :value="true">批准</el-radio>
            <el-radio :value="false">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="approvalForm.comments"
            type="textarea"
            :rows="4"
            placeholder="请输入审批意见"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="approvalDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmApproval">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="detailDialog" title="飞行计划详情" width="700px">
      <el-descriptions :column="2" border v-if="currentPlan">
        <el-descriptions-item label="计划编号">{{ currentPlan.planNumber }}</el-descriptions-item>
        <el-descriptions-item label="计划名称">{{ currentPlan.planName }}</el-descriptions-item>
        <el-descriptions-item label="飞行器">{{ currentPlan.aircraftName }}</el-descriptions-item>
        <el-descriptions-item label="飞行员">{{ currentPlan.pilotName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentPlan.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="飞行目的">{{ getPurposeLabel(currentPlan.purpose) }}</el-descriptions-item>
        <el-descriptions-item label="起飞地点">{{ currentPlan.departureLocation }}</el-descriptions-item>
        <el-descriptions-item label="降落地点">{{ currentPlan.arrivalLocation }}</el-descriptions-item>
        <el-descriptions-item label="计划起飞时间">
          {{ formatTime(currentPlan.plannedStartTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="计划降落时间">
          {{ formatTime(currentPlan.plannedEndTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="飞行时长">
          {{ currentPlan.plannedDuration }} 分钟
        </el-descriptions-item>
        <el-descriptions-item label="飞行高度">
          {{ currentPlan.plannedAltitude }} 米
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentPlan.status)">
            {{ getStatusLabel(currentPlan.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatTime(currentPlan.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentPlan.approvedByName" label="审批人">
          {{ currentPlan.approvedByName }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentPlan.approvedAt" label="审批时间">
          {{ formatTime(currentPlan.approvedAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentPlan.approvalComments" label="审批意见" :span="2">
          {{ currentPlan.approvalComments }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { flightPlanAPI } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const approvalDialog = ref(false)
const detailDialog = ref(false)
const currentPlan = ref(null)

// 与后端 @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')") 保持一致
const canApprove = () => ['ADMIN', 'SUPERVISOR'].includes(userStore.userInfo?.role)

const searchForm = reactive({
  status: ''
})
// 已生效的筛选条件（点「搜索」后才更新，用于结果提示）
const appliedStatus = ref('')

const approvalForm = reactive({
  approved: true,
  comments: ''
})

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    SUBMITTED: 'warning',
    UNDER_REVIEW: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info',
    COMPLETED: ''
  }
  return map[status] || ''
}

const getStatusLabel = (status) => {
  const map = {
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    UNDER_REVIEW: '审核中',
    APPROVED: '已批准',
    REJECTED: '已拒绝',
    CANCELLED: '已取消',
    COMPLETED: '已完成'
  }
  return map[status] || status
}

const getPurposeLabel = (purpose) => {
  const map = {
    COMMERCIAL: '商业飞行',
    SURVEY: '测绘勘察',
    INSPECTION: '巡检',
    DELIVERY: '配送',
    PHOTOGRAPHY: '航拍',
    AGRICULTURE: '农业植保',
    EMERGENCY: '应急救援',
    TRAINING: '训练',
    RESEARCH: '科研',
    OTHER: '其他'
  }
  return map[purpose] || purpose
}

const loadData = async () => {
  try {
    loading.value = true
    const data = searchForm.status
      ? await flightPlanAPI.getByStatus(searchForm.status)
      : await flightPlanAPI.getList()
    tableData.value = data
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  appliedStatus.value = searchForm.status
  loadData()
}

const handleReset = () => {
  searchForm.status = ''
  appliedStatus.value = ''
  loadData()
}

const handleView = (row) => {
  currentPlan.value = row
  detailDialog.value = true
}

const handleSubmit = async (row) => {
  try {
    await ElMessageBox.confirm('确定要提交该飞行计划吗？', '提示', {
      type: 'warning'
    })
    await flightPlanAPI.submit(row.id)
    ElMessage.success('提交成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
    }
  }
}

const handleApprove = (row) => {
  currentPlan.value = row
  approvalForm.approved = true
  approvalForm.comments = ''
  approvalDialog.value = true
}

const confirmApproval = async () => {
  try {
    await flightPlanAPI.approve(currentPlan.value.id, approvalForm)
    ElMessage.success('审批成功')
    approvalDialog.value = false
    loadData()
  } catch (error) {
    console.error('审批失败:', error)
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定要取消该飞行计划吗？', '提示', {
      type: 'warning'
    })
    await flightPlanAPI.cancel(row.id)
    ElMessage.success('取消成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消失败:', error)
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 16px;
}

.search-form :deep(.el-select) {
  width: 150px;
}

.result-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}
</style>
