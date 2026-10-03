<template>
  <div class="report-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">报表管理</span>
          <el-button type="primary" @click="handleCreateReport">
            <el-icon><Plus /></el-icon>
            生成报表
          </el-button>
        </div>
      </template>

      <!-- 报表类型快捷入口 -->
      <div class="report-categories">
        <el-card
          class="category-card"
          v-for="category in reportCategories"
          :key="category.type"
          shadow="hover"
          @click="handleSelectCategory(category.type)"
        >
          <div class="category-icon" :style="{ color: category.color }">
            <el-icon :size="40"><component :is="category.icon" /></el-icon>
          </div>
          <div class="category-name">{{ category.name }}</div>
          <div class="category-count">{{ category.count }} 份</div>
        </el-card>
      </div>

      <!-- 搜索筛选 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="报表类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width: 180px">
            <el-option label="订单统计报表" value="order" />
            <el-option label="飞行数据报表" value="flight" />
            <el-option label="资源利用报表" value="resource" />
            <el-option label="空域分析报表" value="airspace" />
            <el-option label="财务对账报表" value="finance" />
            <el-option label="违规统计报表" value="violation" />
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
        <el-form-item label="生成状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="生成中" value="generating" />
            <el-option label="已完成" value="completed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="报表名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 报表列表 -->
      <el-table :data="reportList" style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="reportNo" label="报表编号" width="160" />
        <el-table-column prop="name" label="报表名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="报表类型" width="140">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="period" label="统计周期" width="180" />
        <el-table-column prop="format" label="格式" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.format.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="100" />
        <el-table-column prop="creator" label="生成人" width="100" />
        <el-table-column prop="createTime" label="生成时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">预览</el-button>
            <el-button link type="success" @click="handleDownload(row)" :disabled="row.status !== 'completed'">
              下载
            </el-button>
            <el-button link type="warning" @click="handleShare(row)" :disabled="row.status !== 'completed'">
              分享
            </el-button>
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

    <!-- 生成报表对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="生成报表"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form :model="reportForm" :rules="reportRules" ref="reportFormRef" label-width="120px">
        <el-form-item label="报表类型" prop="type">
          <el-select v-model="reportForm.type" placeholder="请选择报表类型" style="width: 100%">
            <el-option label="订单统计报表" value="order" />
            <el-option label="飞行数据报表" value="flight" />
            <el-option label="资源利用报表" value="resource" />
            <el-option label="空域分析报表" value="airspace" />
            <el-option label="财务对账报表" value="finance" />
            <el-option label="违规统计报表" value="violation" />
          </el-select>
        </el-form-item>

        <el-form-item label="报表名称" prop="name">
          <el-input v-model="reportForm.name" placeholder="请输入报表名称" />
        </el-form-item>

        <el-form-item label="统计周期" prop="periodType">
          <el-radio-group v-model="reportForm.periodType">
            <el-radio value="day">按日</el-radio>
            <el-radio value="week">按周</el-radio>
            <el-radio value="month">按月</el-radio>
            <el-radio value="quarter">按季度</el-radio>
            <el-radio value="year">按年</el-radio>
            <el-radio value="custom">自定义</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="时间范围" prop="dateRange" v-if="reportForm.periodType === 'custom'">
          <el-date-picker
            v-model="reportForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="具体日期" prop="specificDate" v-if="reportForm.periodType === 'day'">
          <el-date-picker
            v-model="reportForm.specificDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="数据维度" prop="dimensions">
          <el-checkbox-group v-model="reportForm.dimensions">
            <el-checkbox label="总体概况" value="overview" />
            <el-checkbox label="趋势分析" value="trend" />
            <el-checkbox label="对比分析" value="comparison" />
            <el-checkbox label="明细数据" value="detail" />
            <el-checkbox label="图表展示" value="chart" />
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="筛选条件">
          <el-select v-model="reportForm.filters" multiple placeholder="可选，用于数据过滤" style="width: 100%">
            <el-option label="仅包含正常订单" value="normal_orders" />
            <el-option label="仅包含异常订单" value="abnormal_orders" />
            <el-option label="排除测试数据" value="exclude_test" />
            <el-option label="仅包含已完成" value="completed_only" />
          </el-select>
        </el-form-item>

        <el-form-item label="导出格式" prop="format">
          <el-radio-group v-model="reportForm.format">
            <el-radio value="excel">Excel (.xlsx)</el-radio>
            <el-radio value="pdf">PDF (.pdf)</el-radio>
            <el-radio value="csv">CSV (.csv)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="发送邮件">
          <el-switch v-model="reportForm.sendEmail" />
          <span style="margin-left: 10px; font-size: 12px; color: #909399">
            生成后发送到邮箱
          </span>
        </el-form-item>

        <el-form-item label="邮箱地址" prop="email" v-if="reportForm.sendEmail">
          <el-input v-model="reportForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="reportForm.remarks"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="generating">生成</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="报表预览" width="80%" top="5vh">
      <div class="preview-container">
        <div class="preview-header">
          <h2>{{ previewData.name }}</h2>
          <div class="preview-info">
            <span>报表编号: {{ previewData.reportNo }}</span>
            <span>统计周期: {{ previewData.period }}</span>
            <span>生成时间: {{ previewData.createTime }}</span>
          </div>
        </div>

        <el-divider />

        <div class="preview-content">
          <el-descriptions title="报表摘要" :column="3" border>
            <el-descriptions-item label="数据总量">1,256</el-descriptions-item>
            <el-descriptions-item label="有效数据">1,198</el-descriptions-item>
            <el-descriptions-item label="异常数据">58</el-descriptions-item>
            <el-descriptions-item label="增长率" :span="3">+12.5%</el-descriptions-item>
          </el-descriptions>

          <div style="margin-top: 20px">
            <h3>数据预览（前10条）</h3>
            <el-table :data="previewTableData" border style="margin-top: 10px">
              <el-table-column prop="date" label="日期" width="120" />
              <el-table-column prop="orders" label="订单数" width="100" />
              <el-table-column prop="flights" label="飞行架次" width="100" />
              <el-table-column prop="revenue" label="营收(元)" width="120" />
              <el-table-column prop="rate" label="完成率" width="100" />
            </el-table>
          </div>

          <el-alert
            title="提示"
            type="info"
            :closable="false"
            style="margin-top: 20px"
          >
            完整数据请下载报表文件查看
          </el-alert>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleDownload(previewData)">下载报表</el-button>
      </template>
    </el-dialog>

    <!-- 分享对话框 -->
    <el-dialog v-model="shareDialogVisible" title="分享报表" width="500px">
      <el-form :model="shareForm" label-width="100px">
        <el-form-item label="分享链接">
          <el-input v-model="shareForm.link" readonly>
            <template #append>
              <el-button @click="handleCopyLink">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="shareForm.expiry" style="width: 100%">
            <el-option label="1小时" value="1h" />
            <el-option label="24小时" value="24h" />
            <el-option label="7天" value="7d" />
            <el-option label="永久" value="forever" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问密码">
          <el-input v-model="shareForm.password" placeholder="留空则无需密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shareDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleGenerateShareLink">生成链接</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Document, DataAnalysis, Goods, Folder, Wallet, Warning } from '@element-plus/icons-vue'

// 报表类型
const reportCategories = ref([
  { type: 'order', name: '订单统计', icon: 'Document', color: '#409eff', count: 45 },
  { type: 'flight', name: '飞行数据', icon: 'DataAnalysis', color: '#67c23a', count: 32 },
  { type: 'resource', name: '资源利用', icon: 'Goods', color: '#e6a23c', count: 28 },
  { type: 'airspace', name: '空域分析', icon: 'Folder', color: '#909399', count: 18 },
  { type: 'finance', name: '财务对账', icon: 'Wallet', color: '#f56c6c', count: 12 },
  { type: 'violation', name: '违规统计', icon: 'Warning', color: '#f56c6c', count: 8 }
])

// 搜索表单
const searchForm = reactive({
  type: '',
  dateRange: [],
  status: '',
  keyword: ''
})

// 列表数据
const reportList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 生成报表对话框
const createDialogVisible = ref(false)
const reportForm = reactive({
  type: '',
  name: '',
  periodType: 'month',
  dateRange: [],
  specificDate: '',
  dimensions: ['overview', 'trend'],
  filters: [],
  format: 'excel',
  sendEmail: false,
  email: '',
  remarks: ''
})

const reportRules = {
  type: [{ required: true, message: '请选择报表类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
  periodType: [{ required: true, message: '请选择统计周期', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择时间范围', trigger: 'change' }],
  specificDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  format: [{ required: true, message: '请选择导出格式', trigger: 'change' }],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const reportFormRef = ref(null)
const generating = ref(false)

// 预览对话框
const previewDialogVisible = ref(false)
const previewData = ref({})
const previewTableData = ref([
  { date: '2026-09-01', orders: 125, flights: 98, revenue: 45600, rate: '98.5%' },
  { date: '2026-09-02', orders: 132, flights: 105, revenue: 48200, rate: '97.8%' },
  { date: '2026-09-03', orders: 118, flights: 92, revenue: 42300, rate: '99.1%' }
])

// 分享对话框
const shareDialogVisible = ref(false)
const shareForm = reactive({
  link: '',
  expiry: '24h',
  password: ''
})

// 初始化
onMounted(() => {
  fetchReportList()
})

// 获取报表列表
const fetchReportList = () => {
  loading.value = true

  setTimeout(() => {
    reportList.value = [
      {
        id: 1,
        reportNo: 'RPT-2024091700001',
        name: '9月订单统计月报',
        type: 'order',
        period: '2026-09-01 至 2026-09-17',
        format: 'excel',
        fileSize: '2.5 MB',
        creator: '系统管理员',
        createTime: '2026-09-17 10:30:25',
        status: 'completed'
      },
      {
        id: 2,
        reportNo: 'RPT-2024091700002',
        name: '飞行数据周报',
        type: 'flight',
        period: '2026-09-11 至 2026-09-17',
        format: 'pdf',
        fileSize: '1.8 MB',
        creator: '张三',
        createTime: '2026-09-17 09:15:10',
        status: 'completed'
      },
      {
        id: 3,
        reportNo: 'RPT-2024091700003',
        name: '资源利用率分析报表',
        type: 'resource',
        period: '2026-09-01 至 2026-09-17',
        format: 'excel',
        fileSize: '3.2 MB',
        creator: '李四',
        createTime: '2026-09-17 08:45:33',
        status: 'generating'
      },
      {
        id: 4,
        reportNo: 'RPT-2024091600089',
        name: '空域利用统计报表',
        type: 'airspace',
        period: '2026-09-01 至 2026-09-16',
        format: 'pdf',
        fileSize: '1.5 MB',
        creator: '王五',
        createTime: '2026-09-16 16:20:15',
        status: 'completed'
      },
      {
        id: 5,
        reportNo: 'RPT-2024091600088',
        name: '财务对账月报',
        type: 'finance',
        period: '2026-08-01 至 2026-08-31',
        format: 'excel',
        fileSize: '4.1 MB',
        creator: '财务部',
        createTime: '2026-09-01 14:10:05',
        status: 'completed'
      }
    ]
    pagination.total = 67
    loading.value = false
  }, 500)
}

// 选择报表类型
const handleSelectCategory = (type) => {
  searchForm.type = type
  handleSearch()
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchReportList()
}

// 重置
const handleReset = () => {
  searchForm.type = ''
  searchForm.dateRange = []
  searchForm.status = ''
  searchForm.keyword = ''
  handleSearch()
}

// 生成报表
const handleCreateReport = () => {
  createDialogVisible.value = true
}

// 提交生成
const handleSubmit = () => {
  reportFormRef.value.validate((valid) => {
    if (valid) {
      generating.value = true

      setTimeout(() => {
        ElMessage.success('报表生成任务已提交，请稍后查看')
        createDialogVisible.value = false
        generating.value = false
        fetchReportList()
      }, 2000)
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  reportFormRef.value?.resetFields()
}

// 预览
const handleView = (row) => {
  previewData.value = { ...row }
  previewDialogVisible.value = true
}

// 下载
const handleDownload = (row) => {
  ElMessage.success(`正在下载报表: ${row.name}`)
  // TODO: 调用下载API
}

// 分享
const handleShare = (row) => {
  shareForm.link = `https://uav.example.com/report/share/${row.reportNo}`
  shareForm.expiry = '24h'
  shareForm.password = ''
  shareDialogVisible.value = true
}

// 生成分享链接
const handleGenerateShareLink = () => {
  ElMessage.success('分享链接已生成')
}

// 复制链接
const handleCopyLink = () => {
  navigator.clipboard.writeText(shareForm.link)
  ElMessage.success('链接已复制到剪贴板')
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该报表吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    fetchReportList()
  })
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchReportList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchReportList()
}

// 辅助函数
const getTypeName = (type) => {
  const map = {
    order: '订单统计',
    flight: '飞行数据',
    resource: '资源利用',
    airspace: '空域分析',
    finance: '财务对账',
    violation: '违规统计'
  }
  return map[type] || type
}

const getTypeTagType = (type) => {
  const map = {
    order: 'primary',
    flight: 'success',
    resource: 'warning',
    airspace: 'info',
    finance: 'danger',
    violation: 'danger'
  }
  return map[type] || ''
}

const getStatusName = (status) => {
  const map = {
    generating: '生成中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = {
    generating: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || ''
}
</script>

<style scoped>
.report-management {
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

.report-categories {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.category-card {
  text-align: center;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.category-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.category-icon {
  margin-bottom: 10px;
}

.category-name {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
}

.category-count {
  font-size: 14px;
  color: #909399;
}

.search-form {
  margin-top: 20px;
}

.preview-container {
  padding: 20px;
}

.preview-header h2 {
  margin: 0 0 10px 0;
}

.preview-info {
  display: flex;
  gap: 30px;
  font-size: 14px;
  color: #606266;
}

.preview-content h3 {
  margin: 20px 0 10px 0;
}
</style>
