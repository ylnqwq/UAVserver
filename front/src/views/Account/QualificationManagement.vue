<template>
  <div class="qualification-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">资质管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加资质
          </el-button>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="资质类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width: 180px">
            <el-option label="运营许可证" value="operating_license" />
            <el-option label="适航证" value="airworthiness" />
            <el-option label="机长执照" value="pilot_license" />
            <el-option label="维修许可" value="maintenance_license" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 150px">
            <el-option label="有效" value="valid" />
            <el-option label="即将过期" value="expiring" />
            <el-option label="已过期" value="expired" />
            <el-option label="待审核" value="pending" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="证书编号/名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <el-card class="stat-card valid">
          <div class="stat-value">{{ stats.valid }}</div>
          <div class="stat-label">有效资质</div>
        </el-card>
        <el-card class="stat-card expiring">
          <div class="stat-value">{{ stats.expiring }}</div>
          <div class="stat-label">即将过期</div>
        </el-card>
        <el-card class="stat-card expired">
          <div class="stat-value">{{ stats.expired }}</div>
          <div class="stat-label">已过期</div>
        </el-card>
        <el-card class="stat-card pending">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待审核</div>
        </el-card>
      </div>

      <!-- 资质列表 -->
      <el-table :data="qualificationList" style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="certificateNo" label="证书编号" width="180" />
        <el-table-column prop="name" label="资质名称" width="200" />
        <el-table-column prop="type" label="资质类型" width="150">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="holder" label="持有人/机构" width="180" />
        <el-table-column prop="issueDate" label="颁发日期" width="120" />
        <el-table-column prop="expiryDate" label="到期日期" width="120">
          <template #default="{ row }">
            <span :class="getExpiryClass(row.expiryDate)">
              {{ row.expiryDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="daysToExpiry" label="剩余天数" width="100">
          <template #default="{ row }">
            <span :class="getDaysClass(row.daysToExpiry)">
              {{ row.daysToExpiry > 0 ? row.daysToExpiry + ' 天' : '已过期' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleRenew(row)" v-if="row.daysToExpiry < 90">续期</el-button>
            <el-button link type="warning" @click="handleDownload(row)">下载</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 添加/编辑资质对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="资质类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择资质类型" style="width: 100%">
            <el-option label="运营许可证" value="operating_license" />
            <el-option label="适航证" value="airworthiness" />
            <el-option label="机长执照" value="pilot_license" />
            <el-option label="维修许可" value="maintenance_license" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="资质名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入资质名称" />
        </el-form-item>
        <el-form-item label="证书编号" prop="certificateNo">
          <el-input v-model="formData.certificateNo" placeholder="请输入证书编号" />
        </el-form-item>
        <el-form-item label="持有人/机构" prop="holder">
          <el-input v-model="formData.holder" placeholder="请输入持有人或机构名称" />
        </el-form-item>
        <el-form-item label="颁发机构" prop="issuer">
          <el-input v-model="formData.issuer" placeholder="请输入颁发机构" />
        </el-form-item>
        <el-form-item label="颁发日期" prop="issueDate">
          <el-date-picker
            v-model="formData.issueDate"
            type="date"
            placeholder="选择颁发日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="到期日期" prop="expiryDate">
          <el-date-picker
            v-model="formData.expiryDate"
            type="date"
            placeholder="选择到期日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="证书文件" prop="attachments">
          <el-upload
            class="upload-demo"
            action="/api/upload"
            :on-success="handleUploadSuccess"
            :file-list="formData.attachments"
            :limit="5"
          >
            <el-button type="primary">上传文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/pdf文件，单个文件不超过10MB，最多5个
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="formData.remarks"
            type="textarea"
            :rows="3"
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
    <el-dialog v-model="viewDialogVisible" title="资质详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="证书编号">{{ viewData.certificateNo }}</el-descriptions-item>
        <el-descriptions-item label="资质名称">{{ viewData.name }}</el-descriptions-item>
        <el-descriptions-item label="资质类型">{{ getTypeName(viewData.type) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(viewData.status)">{{ getStatusName(viewData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="持有人/机构">{{ viewData.holder }}</el-descriptions-item>
        <el-descriptions-item label="颁发机构">{{ viewData.issuer }}</el-descriptions-item>
        <el-descriptions-item label="颁发日期">{{ viewData.issueDate }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ viewData.expiryDate }}</el-descriptions-item>
        <el-descriptions-item label="剩余天数" :span="2">
          <span :class="getDaysClass(viewData.daysToExpiry)">
            {{ viewData.daysToExpiry > 0 ? viewData.daysToExpiry + ' 天' : '已过期' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewData.remarks || '无' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="viewData.attachments && viewData.attachments.length" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">附件列表：</div>
        <el-tag v-for="file in viewData.attachments" :key="file.name" style="margin-right: 10px; margin-bottom: 10px">
          {{ file.name }}
        </el-tag>
      </div>
      <template #footer>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
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
  type: '',
  status: '',
  keyword: ''
})

// 统计数据
const stats = reactive({
  valid: 45,
  expiring: 8,
  expired: 3,
  pending: 2
})

// 列表数据
const qualificationList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('添加资质')
const submitting = ref(false)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  id: null,
  type: '',
  name: '',
  certificateNo: '',
  holder: '',
  issuer: '',
  issueDate: '',
  expiryDate: '',
  attachments: [],
  remarks: ''
})

// 表单验证规则
const formRules = {
  type: [{ required: true, message: '请选择资质类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入资质名称', trigger: 'blur' }],
  certificateNo: [{ required: true, message: '请输入证书编号', trigger: 'blur' }],
  holder: [{ required: true, message: '请输入持有人或机构', trigger: 'blur' }],
  issuer: [{ required: true, message: '请输入颁发机构', trigger: 'blur' }],
  issueDate: [{ required: true, message: '请选择颁发日期', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }]
}

// 查看详情
const viewDialogVisible = ref(false)
const viewData = ref({})

// 初始化数据
onMounted(() => {
  fetchQualificationList()
})

// 获取资质列表
const fetchQualificationList = () => {
  loading.value = true

  // TODO: 调用实际API
  setTimeout(() => {
    qualificationList.value = [
      {
        id: 1,
        certificateNo: 'OL-2024-001',
        name: '无人机运营许可证',
        type: 'operating_license',
        holder: '天翔物流科技有限公司',
        issuer: '民航局',
        issueDate: '2024-01-15',
        expiryDate: '2025-01-14',
        daysToExpiry: 120,
        status: 'valid',
        remarks: '适用于城市物流配送'
      },
      {
        id: 2,
        certificateNo: 'AW-2023-056',
        name: 'DJI M300 适航证',
        type: 'airworthiness',
        holder: 'DJI-M300-001',
        issuer: '中国民用航空局',
        issueDate: '2023-06-20',
        expiryDate: '2024-12-20',
        daysToExpiry: 45,
        status: 'expiring',
        remarks: '需要定期检查'
      },
      {
        id: 3,
        certificateNo: 'PL-2022-189',
        name: '无人机机长执照',
        type: 'pilot_license',
        holder: '张三',
        issuer: 'AOPA',
        issueDate: '2022-08-10',
        expiryDate: '2024-08-09',
        daysToExpiry: -30,
        status: 'expired',
        remarks: '需要续期'
      },
      {
        id: 4,
        certificateNo: 'ML-2024-012',
        name: '维修许可证',
        type: 'maintenance_license',
        holder: '李四',
        issuer: 'CAAC',
        issueDate: '2024-03-01',
        expiryDate: '2026-02-28',
        daysToExpiry: 520,
        status: 'valid',
        remarks: ''
      },
      {
        id: 5,
        certificateNo: 'OL-2024-025',
        name: '特种作业许可',
        type: 'other',
        holder: '天翔物流科技有限公司',
        issuer: '应急管理局',
        issueDate: '2024-09-01',
        expiryDate: '2025-08-31',
        daysToExpiry: 348,
        status: 'pending',
        remarks: '待审核'
      }
    ]
    pagination.total = 5
    loading.value = false
  }, 500)
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchQualificationList()
}

// 重置
const handleReset = () => {
  searchForm.type = ''
  searchForm.status = ''
  searchForm.keyword = ''
  handleSearch()
}

// 添加资质
const handleAdd = () => {
  dialogTitle.value = '添加资质'
  dialogVisible.value = true
}

// 编辑资质
const handleEdit = (row) => {
  dialogTitle.value = '编辑资质'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 查看详情
const handleView = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
}

// 续期
const handleRenew = (row) => {
  ElMessageBox.confirm('确定要为该资质申请续期吗？', '续期确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用续期API
    ElMessage.success('续期申请已提交')
  })
}

// 下载证书
const handleDownload = (row) => {
  // TODO: 调用下载API
  ElMessage.success('正在下载证书文件...')
}

// 删除资质
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该资质吗？删除后无法恢复', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用删除API
    ElMessage.success('删除成功')
    fetchQualificationList()
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
        fetchQualificationList()
      }, 1000)
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: null,
    type: '',
    name: '',
    certificateNo: '',
    holder: '',
    issuer: '',
    issueDate: '',
    expiryDate: '',
    attachments: [],
    remarks: ''
  })
}

// 文件上传成功
const handleUploadSuccess = (response, file, fileList) => {
  formData.attachments = fileList
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchQualificationList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchQualificationList()
}

// 辅助函数
const getTypeName = (type) => {
  const typeMap = {
    operating_license: '运营许可证',
    airworthiness: '适航证',
    pilot_license: '机长执照',
    maintenance_license: '维修许可',
    other: '其他'
  }
  return typeMap[type] || type
}

const getTypeTagType = (type) => {
  const typeMap = {
    operating_license: 'success',
    airworthiness: 'primary',
    pilot_license: 'warning',
    maintenance_license: 'info',
    other: ''
  }
  return typeMap[type] || ''
}

const getStatusName = (status) => {
  const statusMap = {
    valid: '有效',
    expiring: '即将过期',
    expired: '已过期',
    pending: '待审核'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status) => {
  const statusMap = {
    valid: 'success',
    expiring: 'warning',
    expired: 'danger',
    pending: 'info'
  }
  return statusMap[status] || ''
}

const getExpiryClass = (date) => {
  const days = calculateDaysToExpiry(date)
  if (days < 0) return 'expired-date'
  if (days < 30) return 'expiring-soon-date'
  if (days < 90) return 'expiring-date'
  return ''
}

const getDaysClass = (days) => {
  if (days < 0) return 'expired-days'
  if (days < 30) return 'expiring-soon-days'
  if (days < 90) return 'expiring-days'
  return 'valid-days'
}

const calculateDaysToExpiry = (expiryDate) => {
  const today = new Date()
  const expiry = new Date(expiryDate)
  const diff = expiry - today
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
}
</script>

<style scoped>
.qualification-management {
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

.stat-card.valid {
  border-left: 4px solid #67c23a;
}

.stat-card.expiring {
  border-left: 4px solid #e6a23c;
}

.stat-card.expired {
  border-left: 4px solid #f56c6c;
}

.stat-card.pending {
  border-left: 4px solid #909399;
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

.expired-date {
  color: #f56c6c;
  font-weight: bold;
}

.expiring-soon-date {
  color: #e6a23c;
  font-weight: bold;
}

.expiring-date {
  color: #e6a23c;
}

.expired-days {
  color: #f56c6c;
  font-weight: bold;
}

.expiring-soon-days {
  color: #e6a23c;
  font-weight: bold;
}

.expiring-days {
  color: #e6a23c;
}

.valid-days {
  color: #67c23a;
}
</style>
