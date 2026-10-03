<template>
  <div class="maintenance-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">维保管理</span>
          <el-button type="primary" @click="handleAddMaintenance">
            <el-icon><Plus /></el-icon>
            新建维保记录
          </el-button>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="飞行器">
          <el-select v-model="searchForm.aircraftId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option
              v-for="aircraft in aircraftList"
              :key="aircraft.id"
              :label="aircraft.name"
              :value="aircraft.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="维保类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width: 150px">
            <el-option label="例行保养" value="routine" />
            <el-option label="定期检修" value="periodic" />
            <el-option label="故障维修" value="repair" />
            <el-option label="紧急维护" value="emergency" />
          </el-select>
        </el-form-item>
        <el-form-item label="维保状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width="150px">
            <el-option label="待维保" value="pending" />
            <el-option label="维保中" value="in_progress" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <el-card class="stat-card pending">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待维保</div>
        </el-card>
        <el-card class="stat-card in-progress">
          <div class="stat-value">{{ stats.inProgress }}</div>
          <div class="stat-label">维保中</div>
        </el-card>
        <el-card class="stat-card completed">
          <div class="stat-value">{{ stats.completed }}</div>
          <div class="stat-label">本月完成</div>
        </el-card>
        <el-card class="stat-card overdue">
          <div class="stat-value">{{ stats.overdue }}</div>
          <div class="stat-label">逾期未维保</div>
        </el-card>
      </div>

      <!-- 维保列表 -->
      <el-table :data="maintenanceList" style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="maintenanceNo" label="维保单号" width="160" />
        <el-table-column prop="aircraft" label="飞行器" width="180">
          <template #default="{ row }">
            <div>
              <div>{{ row.aircraftName }}</div>
              <div style="font-size: 12px; color: #909399">{{ row.aircraftNo }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="维保类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="维保内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="technician" label="维保人员" width="100" />
        <el-table-column prop="scheduledDate" label="计划时间" width="160" />
        <el-table-column prop="actualDate" label="实际时间" width="160" />
        <el-table-column prop="duration" label="耗时(h)" width="100" />
        <el-table-column prop="cost" label="费用(元)" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="success" @click="handleStart(row)" v-if="row.status === 'pending'">
              开始维保
            </el-button>
            <el-button link type="success" @click="handleComplete(row)" v-if="row.status === 'in_progress'">
              完成
            </el-button>
            <el-button link type="warning" @click="handleEdit(row)" v-if="row.status === 'pending'">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleCancel(row)" v-if="row.status === 'pending'">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新建/编辑维保记录对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="飞行器" prop="aircraftId">
              <el-select v-model="formData.aircraftId" placeholder="请选择飞行器" filterable style="width: 100%">
                <el-option
                  v-for="aircraft in aircraftList"
                  :key="aircraft.id"
                  :label="aircraft.name"
                  :value="aircraft.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维保类型" prop="type">
              <el-select v-model="formData.type" placeholder="请选择维保类型" style="width: 100%">
                <el-option label="例行保养" value="routine" />
                <el-option label="定期检修" value="periodic" />
                <el-option label="故障维修" value="repair" />
                <el-option label="紧急维护" value="emergency" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="维保人员" prop="technician">
              <el-select v-model="formData.technician" placeholder="请选择维保人员" filterable style="width: 100%">
                <el-option
                  v-for="tech in technicianList"
                  :key="tech.id"
                  :label="tech.name"
                  :value="tech.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划时间" prop="scheduledDate">
              <el-date-picker
                v-model="formData.scheduledDate"
                type="datetime"
                placeholder="选择日期时间"
                style="width: 100%"
                value-format="YYYY-MM-DD HH:mm"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="维保内容" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入维保内容描述"
          />
        </el-form-item>

        <el-form-item label="维保项目" prop="items">
          <el-checkbox-group v-model="formData.items">
            <el-checkbox label="机体检查" value="body_check" />
            <el-checkbox label="电池检测" value="battery_check" />
            <el-checkbox label="电机检修" value="motor_check" />
            <el-checkbox label="螺旋桨更换" value="propeller_replace" />
            <el-checkbox label="传感器校准" value="sensor_calibration" />
            <el-checkbox label="固件升级" value="firmware_upgrade" />
            <el-checkbox label="其他" value="other" />
          </el-checkbox-group>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预估耗时(h)">
              <el-input-number v-model="formData.estimatedDuration" :min="0.5" :max="48" :step="0.5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预估费用(元)">
              <el-input-number v-model="formData.estimatedCost" :min="0" :max="100000" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input
            v-model="formData.remarks"
            type="textarea"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="维保详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="维保单号">{{ viewData.maintenanceNo }}</el-descriptions-item>
        <el-descriptions-item label="维保类型">
          <el-tag :type="getTypeTagType(viewData.type)" size="small">
            {{ getTypeName(viewData.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="飞行器">{{ viewData.aircraftName }}</el-descriptions-item>
        <el-descriptions-item label="飞行器编号">{{ viewData.aircraftNo }}</el-descriptions-item>
        <el-descriptions-item label="维保人员">{{ viewData.technician }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(viewData.status)" size="small">
            {{ getStatusName(viewData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="计划时间">{{ viewData.scheduledDate }}</el-descriptions-item>
        <el-descriptions-item label="实际时间">{{ viewData.actualDate || '未开始' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ viewData.duration ? viewData.duration + ' 小时' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="费用">{{ viewData.cost ? viewData.cost + ' 元' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="维保内容" :span="2">{{ viewData.description }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewData.remarks || '无' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 维保项目 -->
      <div v-if="viewData.items && viewData.items.length" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">维保项目：</div>
        <el-tag v-for="item in viewData.items" :key="item" style="margin-right: 10px">
          {{ item }}
        </el-tag>
      </div>

      <!-- 维保照片 -->
      <div v-if="viewData.images && viewData.images.length" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">维保照片：</div>
        <el-image
          v-for="(img, index) in viewData.images"
          :key="index"
          :src="img"
          :preview-src-list="viewData.images"
          :initial-index="index"
          fit="cover"
          style="width: 100px; height: 100px; margin-right: 10px"
        />
      </div>

      <template #footer>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 完成维保对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完成维保" width="600px">
      <el-form :model="completeForm" :rules="completeRules" ref="completeFormRef" label-width="120px">
        <el-form-item label="实际耗时(h)" prop="duration">
          <el-input-number v-model="completeForm.duration" :min="0.1" :max="48" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实际费用(元)" prop="cost">
          <el-input-number v-model="completeForm.cost" :min="0" :max="100000" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="维保结果" prop="result">
          <el-select v-model="completeForm.result" placeholder="请选择维保结果" style="width: 100%">
            <el-option label="正常" value="normal" />
            <el-option label="需要观察" value="observe" />
            <el-option label="需要更换部件" value="replace" />
            <el-option label="建议停飞检修" value="grounding" />
          </el-select>
        </el-form-item>
        <el-form-item label="维保说明" prop="summary">
          <el-input
            v-model="completeForm.summary"
            type="textarea"
            :rows="4"
            placeholder="请输入维保说明"
          />
        </el-form-item>
        <el-form-item label="上传照片">
          <el-upload
            class="upload-demo"
            action="/api/upload"
            list-type="picture-card"
            :limit="5"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitComplete" :loading="completing">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

// 搜索表单
const searchForm = reactive({
  aircraftId: '',
  type: '',
  status: '',
  dateRange: []
})

// 统计数据
const stats = reactive({
  pending: 12,
  inProgress: 5,
  completed: 78,
  overdue: 3
})

// 飞行器列表
const aircraftList = ref([
  { id: 1, name: 'DJI M300-001' },
  { id: 2, name: 'DJI M300-002' },
  { id: 3, name: 'DJI M350-001' }
])

// 技术人员列表
const technicianList = ref([
  { id: 1, name: '张工' },
  { id: 2, name: '李工' },
  { id: 3, name: '王工' }
])

// 列表数据
const maintenanceList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新建维保记录')
const submitting = ref(false)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  id: null,
  aircraftId: '',
  type: '',
  technician: '',
  scheduledDate: '',
  description: '',
  items: [],
  estimatedDuration: 2,
  estimatedCost: 500,
  remarks: ''
})

// 表单验证规则
const formRules = {
  aircraftId: [{ required: true, message: '请选择飞行器', trigger: 'change' }],
  type: [{ required: true, message: '请选择维保类型', trigger: 'change' }],
  technician: [{ required: true, message: '请选择维保人员', trigger: 'change' }],
  scheduledDate: [{ required: true, message: '请选择计划时间', trigger: 'change' }],
  description: [{ required: true, message: '请输入维保内容', trigger: 'blur' }]
}

// 查看详情
const viewDialogVisible = ref(false)
const viewData = ref({})

// 完成维保
const completeDialogVisible = ref(false)
const completeForm = reactive({
  maintenanceId: null,
  duration: 0,
  cost: 0,
  result: '',
  summary: ''
})
const completeRules = {
  duration: [{ required: true, message: '请输入实际耗时', trigger: 'blur' }],
  cost: [{ required: true, message: '请输入实际费用', trigger: 'blur' }],
  result: [{ required: true, message: '请选择维保结果', trigger: 'change' }],
  summary: [{ required: true, message: '请输入维保说明', trigger: 'blur' }]
}
const completeFormRef = ref(null)
const completing = ref(false)

// 初始化
onMounted(() => {
  fetchMaintenanceList()
})

// 获取维保列表
const fetchMaintenanceList = () => {
  loading.value = true

  // TODO: 调用实际API
  setTimeout(() => {
    maintenanceList.value = [
      {
        id: 1,
        maintenanceNo: 'MNT-2024091700001',
        aircraftName: 'DJI M300-001',
        aircraftNo: 'UAV-M300-001',
        type: 'routine',
        description: '例行保养：机体检查、电池检测、螺旋桨检查',
        technician: '张工',
        scheduledDate: '2026-09-18 09:00',
        actualDate: null,
        duration: null,
        cost: null,
        status: 'pending',
        items: ['机体检查', '电池检测', '螺旋桨更换'],
        remarks: '需要提前准备螺旋桨备件'
      },
      {
        id: 2,
        maintenanceNo: 'MNT-2024091700002',
        aircraftName: 'DJI M350-001',
        aircraftNo: 'UAV-M350-001',
        type: 'repair',
        description: '故障维修：电机异响，需要检修',
        technician: '李工',
        scheduledDate: '2026-09-17 10:00',
        actualDate: '2026-09-17 10:15',
        duration: null,
        cost: null,
        status: 'in_progress',
        items: ['电机检修'],
        remarks: '客户反馈飞行中有异常声音'
      },
      {
        id: 3,
        maintenanceNo: 'MNT-2024091600023',
        aircraftName: 'DJI M300-002',
        aircraftNo: 'UAV-M300-002',
        type: 'periodic',
        description: '定期检修：累计飞行100小时检修',
        technician: '王工',
        scheduledDate: '2026-09-16 14:00',
        actualDate: '2026-09-16 14:00',
        duration: 3.5,
        cost: 800,
        status: 'completed',
        items: ['机体检查', '电池检测', '电机检修', '传感器校准'],
        remarks: '检修完成，飞行器状态良好'
      }
    ]
    pagination.total = 89
    loading.value = false
  }, 500)
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchMaintenanceList()
}

// 重置
const handleReset = () => {
  searchForm.aircraftId = ''
  searchForm.type = ''
  searchForm.status = ''
  searchForm.dateRange = []
  handleSearch()
}

// 新建维保
const handleAddMaintenance = () => {
  dialogTitle.value = '新建维保记录'
  dialogVisible.value = true
}

// 编辑维保
const handleEdit = (row) => {
  dialogTitle.value = '编辑维保记录'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 查看详情
const handleView = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
}

// 开始维保
const handleStart = (row) => {
  ElMessageBox.confirm('确定要开始维保吗？', '开始确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    // TODO: 调用API
    ElMessage.success('维保已开始')
    fetchMaintenanceList()
  })
}

// 完成维保
const handleComplete = (row) => {
  completeForm.maintenanceId = row.id
  completeForm.duration = row.estimatedDuration || 0
  completeForm.cost = row.estimatedCost || 0
  completeForm.result = ''
  completeForm.summary = ''
  completeDialogVisible.value = true
}

// 提交完成
const handleSubmitComplete = () => {
  completeFormRef.value.validate((valid) => {
    if (valid) {
      completing.value = true
      // TODO: 调用API
      setTimeout(() => {
        ElMessage.success('维保已完成')
        completeDialogVisible.value = false
        completing.value = false
        fetchMaintenanceList()
      }, 1000)
    }
  })
}

// 取消维保
const handleCancel = (row) => {
  ElMessageBox.confirm('确定要取消该维保记录吗？', '取消确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用API
    ElMessage.success('维保已取消')
    fetchMaintenanceList()
  })
}

// 提交表单
const handleSubmit = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      // TODO: 调用API保存
      setTimeout(() => {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        submitting.value = false
        fetchMaintenanceList()
      }, 1000)
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: null,
    aircraftId: '',
    type: '',
    technician: '',
    scheduledDate: '',
    description: '',
    items: [],
    estimatedDuration: 2,
    estimatedCost: 500,
    remarks: ''
  })
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchMaintenanceList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchMaintenanceList()
}

// 辅助函数
const getTypeName = (type) => {
  const typeMap = {
    routine: '例行保养',
    periodic: '定期检修',
    repair: '故障维修',
    emergency: '紧急维护'
  }
  return typeMap[type] || type
}

const getTypeTagType = (type) => {
  const typeMap = {
    routine: 'success',
    periodic: 'primary',
    repair: 'warning',
    emergency: 'danger'
  }
  return typeMap[type] || ''
}

const getStatusName = (status) => {
  const statusMap = {
    pending: '待维保',
    in_progress: '维保中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status) => {
  const statusMap = {
    pending: 'warning',
    in_progress: 'primary',
    completed: 'success',
    cancelled: 'info'
  }
  return statusMap[status] || ''
}
</script>

<style scoped>
.maintenance-management {
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

.search-form {
  margin-top: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.stat-card {
  text-align: center;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-card.pending {
  border-left: 4px solid #e6a23c;
}

.stat-card.in-progress {
  border-left: 4px solid #409eff;
}

.stat-card.completed {
  border-left: 4px solid #67c23a;
}

.stat-card.overdue {
  border-left: 4px solid #f56c6c;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}
</style>
