<template>
  <div class="interface-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">外部接口管理</span>
          <el-button type="primary" @click="handleAddInterface">
            <el-icon><Plus /></el-icon>
            添加接口
          </el-button>
        </div>
      </template>

      <!-- 接口分类 -->
      <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
        <el-tab-pane label="全部接口" name="all" />
        <el-tab-pane label="空管系统" name="air-control" />
        <el-tab-pane label="气象服务" name="weather" />
        <el-tab-pane label="地图服务" name="map" />
        <el-tab-pane label="短信服务" name="sms" />
        <el-tab-pane label="支付服务" name="payment" />
        <el-tab-pane label="其他" name="other" />
      </el-tabs>

      <!-- 接口列表 -->
      <el-table :data="interfaceList" style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="name" label="接口名称" width="200" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryTag(row.category)">{{ getCategoryName(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="服务商" width="150" />
        <el-table-column prop="url" label="接口地址" min-width="250" show-overflow-tooltip />
        <el-table-column prop="callCount" label="调用次数" width="120">
          <template #default="{ row }">
            {{ row.callCount.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="successRate" label="成功率" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.successRate >= 95 ? '#67c23a' : '#f56c6c' }">
              {{ row.successRate }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="avgResponseTime" label="平均响应(ms)" width="130" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'enabled' ? 'success' : 'info'">
              {{ row.status === 'enabled' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTest(row)">测试</el-button>
            <el-button link type="success" @click="handleViewStats(row)">统计</el-button>
            <el-button link type="warning" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 添加/编辑接口对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="接口名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入接口名称" />
        </el-form-item>

        <el-form-item label="接口分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="空管系统" value="air-control" />
            <el-option label="气象服务" value="weather" />
            <el-option label="地图服务" value="map" />
            <el-option label="短信服务" value="sms" />
            <el-option label="支付服务" value="payment" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="服务商" prop="provider">
          <el-input v-model="formData.provider" placeholder="如：阿里云、腾讯云" />
        </el-form-item>

        <el-form-item label="接口地址" prop="url">
          <el-input v-model="formData.url" placeholder="https://api.example.com" />
        </el-form-item>

        <el-form-item label="请求方式" prop="method">
          <el-radio-group v-model="formData.method">
            <el-radio value="GET">GET</el-radio>
            <el-radio value="POST">POST</el-radio>
            <el-radio value="PUT">PUT</el-radio>
            <el-radio value="DELETE">DELETE</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="认证方式" prop="authType">
          <el-select v-model="formData.authType" placeholder="请选择认证方式" style="width: 100%">
            <el-option label="无需认证" value="none" />
            <el-option label="API Key" value="api-key" />
            <el-option label="OAuth 2.0" value="oauth" />
            <el-option label="Basic Auth" value="basic" />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key" v-if="formData.authType === 'api-key'">
          <el-input v-model="formData.apiKey" type="password" show-password placeholder="请输入API Key" />
        </el-form-item>

        <el-form-item label="API Secret" v-if="formData.authType === 'api-key'">
          <el-input v-model="formData.apiSecret" type="password" show-password placeholder="请输入API Secret" />
        </el-form-item>

        <el-form-item label="超时时间(秒)" prop="timeout">
          <el-input-number v-model="formData.timeout" :min="1" :max="60" />
        </el-form-item>

        <el-form-item label="重试次数" prop="retryCount">
          <el-input-number v-model="formData.retryCount" :min="0" :max="5" />
        </el-form-item>

        <el-form-item label="启用接口">
          <el-switch v-model="formData.enabled" />
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

    <!-- 测试接口对话框 -->
    <el-dialog v-model="testDialogVisible" title="测试接口" width="800px">
      <div class="test-container">
        <el-form label-width="100px">
          <el-form-item label="接口地址">
            <el-input v-model="testData.url" readonly />
          </el-form-item>
          <el-form-item label="请求方式">
            <el-tag>{{ testData.method }}</el-tag>
          </el-form-item>
          <el-form-item label="请求参数">
            <el-input
              v-model="testData.params"
              type="textarea"
              :rows="5"
              placeholder='{"key": "value"}'
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleRunTest" :loading="testing">
              <el-icon><VideoPlay /></el-icon>
              执行测试
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider content-position="left">测试结果</el-divider>

        <div v-if="testResult.tested" class="test-result">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="状态码">
              <el-tag :type="testResult.statusCode === 200 ? 'success' : 'danger'">
                {{ testResult.statusCode }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="响应时间">{{ testResult.responseTime }} ms</el-descriptions-item>
            <el-descriptions-item label="响应结果" :span="2">
              <el-input
                v-model="testResult.response"
                type="textarea"
                :rows="8"
                readonly
              />
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-empty v-else description="点击执行测试按钮开始测试" />
      </div>
      <template #footer>
        <el-button type="primary" @click="testDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 统计信息对话框 -->
    <el-dialog v-model="statsDialogVisible" title="接口调用统计" width="900px">
      <el-descriptions :column="3" border style="margin-bottom: 20px">
        <el-descriptions-item label="总调用次数">{{ statsData.totalCalls }}</el-descriptions-item>
        <el-descriptions-item label="成功次数">{{ statsData.successCalls }}</el-descriptions-item>
        <el-descriptions-item label="失败次数">{{ statsData.failedCalls }}</el-descriptions-item>
        <el-descriptions-item label="成功率">{{ statsData.successRate }}%</el-descriptions-item>
        <el-descriptions-item label="平均响应时间">{{ statsData.avgResponseTime }} ms</el-descriptions-item>
        <el-descriptions-item label="最大响应时间">{{ statsData.maxResponseTime }} ms</el-descriptions-item>
      </el-descriptions>

      <div ref="statsChartRef" style="height: 300px"></div>

      <el-divider content-position="left">最近调用记录</el-divider>

      <el-table :data="statsData.recentCalls" style="width: 100%" max-height="300">
        <el-table-column prop="time" label="时间" width="160" />
        <el-table-column prop="statusCode" label="状态码" width="100">
          <template #default="{ row }">
            <el-tag :type="row.statusCode === 200 ? 'success' : 'danger'" size="small">
              {{ row.statusCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseTime" label="响应时间(ms)" width="130" />
        <el-table-column prop="error" label="错误信息" show-overflow-tooltip />
      </el-table>

      <template #footer>
        <el-button type="primary" @click="statsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoPlay } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 当前分类
const activeCategory = ref('all')

// 接口列表
const interfaceList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('添加接口')
const submitting = ref(false)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  id: null,
  name: '',
  category: '',
  provider: '',
  url: '',
  method: 'GET',
  authType: 'none',
  apiKey: '',
  apiSecret: '',
  timeout: 10,
  retryCount: 3,
  enabled: true,
  remarks: ''
})

// 表单验证
const formRules = {
  name: [{ required: true, message: '请输入接口名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  provider: [{ required: true, message: '请输入服务商', trigger: 'blur' }],
  url: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
  method: [{ required: true, message: '请选择请求方式', trigger: 'change' }],
  authType: [{ required: true, message: '请选择认证方式', trigger: 'change' }]
}

// 测试对话框
const testDialogVisible = ref(false)
const testing = ref(false)
const testData = reactive({
  url: '',
  method: '',
  params: ''
})
const testResult = reactive({
  tested: false,
  statusCode: 0,
  responseTime: 0,
  response: ''
})

// 统计对话框
const statsDialogVisible = ref(false)
const statsData = reactive({
  totalCalls: 0,
  successCalls: 0,
  failedCalls: 0,
  successRate: 0,
  avgResponseTime: 0,
  maxResponseTime: 0,
  recentCalls: []
})
const statsChartRef = ref(null)

// 初始化
onMounted(() => {
  fetchInterfaceList()
})

// 获取接口列表
const fetchInterfaceList = () => {
  loading.value = true

  setTimeout(() => {
    interfaceList.value = [
      {
        id: 1,
        name: '空管系统-飞行计划查询',
        category: 'air-control',
        provider: '民航局',
        url: 'https://api.caac.gov.cn/flight/plan/query',
        method: 'POST',
        callCount: 15678,
        successRate: 98.5,
        avgResponseTime: 235,
        status: 'enabled'
      },
      {
        id: 2,
        name: '天气查询接口',
        category: 'weather',
        provider: '中国气象局',
        url: 'https://api.weather.gov.cn/query',
        method: 'GET',
        callCount: 45632,
        successRate: 99.2,
        avgResponseTime: 128,
        status: 'enabled'
      },
      {
        id: 3,
        name: '高德地图-路径规划',
        category: 'map',
        provider: '高德地图',
        url: 'https://restapi.amap.com/v3/direction/driving',
        method: 'GET',
        callCount: 28956,
        successRate: 97.8,
        avgResponseTime: 186,
        status: 'enabled'
      },
      {
        id: 4,
        name: '阿里云短信服务',
        category: 'sms',
        provider: '阿里云',
        url: 'https://dysmsapi.aliyuncs.com',
        method: 'POST',
        callCount: 8965,
        successRate: 99.5,
        avgResponseTime: 342,
        status: 'enabled'
      }
    ]
    pagination.total = 12
    loading.value = false
  }, 500)
}

// 分类切换
const handleCategoryChange = () => {
  pagination.page = 1
  fetchInterfaceList()
}

// 添加接口
const handleAddInterface = () => {
  dialogTitle.value = '添加接口'
  dialogVisible.value = true
}

// 编辑接口
const handleEdit = (row) => {
  dialogTitle.value = '编辑接口'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 测试接口
const handleTest = (row) => {
  testData.url = row.url
  testData.method = row.method
  testData.params = ''
  testResult.tested = false
  testDialogVisible.value = true
}

// 执行测试
const handleRunTest = () => {
  testing.value = true

  setTimeout(() => {
    testResult.tested = true
    testResult.statusCode = 200
    testResult.responseTime = Math.floor(Math.random() * 500) + 100
    testResult.response = JSON.stringify({
      code: 200,
      message: 'success',
      data: {
        result: 'Test data'
      }
    }, null, 2)
    testing.value = false
    ElMessage.success('测试完成')
  }, 1500)
}

// 查看统计
const handleViewStats = (row) => {
  statsData.totalCalls = row.callCount
  statsData.successCalls = Math.floor(row.callCount * row.successRate / 100)
  statsData.failedCalls = row.callCount - statsData.successCalls
  statsData.successRate = row.successRate
  statsData.avgResponseTime = row.avgResponseTime
  statsData.maxResponseTime = row.avgResponseTime * 3
  statsData.recentCalls = [
    { time: '2026-09-17 14:32:15', statusCode: 200, responseTime: 235, error: '' },
    { time: '2026-09-17 14:31:58', statusCode: 200, responseTime: 198, error: '' },
    { time: '2026-09-17 14:31:42', statusCode: 500, responseTime: 1235, error: 'Connection timeout' }
  ]

  statsDialogVisible.value = true

  // 绘制图表
  setTimeout(() => {
    if (statsChartRef.value) {
      const chart = echarts.init(statsChartRef.value)
      chart.setOption({
        title: { text: '最近24小时调用趋势' },
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
        },
        yAxis: { type: 'value', name: '调用次数' },
        series: [{
          data: [120, 85, 256, 458, 652, 389, 156],
          type: 'line',
          smooth: true,
          itemStyle: { color: '#409eff' }
        }]
      })
    }
  }, 100)
}

// 删除接口
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该接口配置吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    fetchInterfaceList()
  })
}

// 提交表单
const handleSubmit = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      setTimeout(() => {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        submitting.value = false
        fetchInterfaceList()
      }, 1000)
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: null,
    name: '',
    category: '',
    provider: '',
    url: '',
    method: 'GET',
    authType: 'none',
    apiKey: '',
    apiSecret: '',
    timeout: 10,
    retryCount: 3,
    enabled: true,
    remarks: ''
  })
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchInterfaceList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchInterfaceList()
}

// 辅助函数
const getCategoryName = (category) => {
  const map = {
    'air-control': '空管系统',
    weather: '气象服务',
    map: '地图服务',
    sms: '短信服务',
    payment: '支付服务',
    other: '其他'
  }
  return map[category] || category
}

const getCategoryTag = (category) => {
  const map = {
    'air-control': 'primary',
    weather: 'success',
    map: 'warning',
    sms: 'info',
    payment: 'danger',
    other: ''
  }
  return map[category] || ''
}
</script>

<style scoped>
.interface-management {
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

.test-container {
  padding: 10px;
}

.test-result {
  margin-top: 20px;
}
</style>
