<template>
  <div class="order-batch-import">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">批量导入订单</span>
          <el-button type="primary" @click="handleDownloadTemplate">
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
        </div>
      </template>

      <!-- 导入步骤 -->
      <el-steps :active="activeStep" align-center finish-status="success" style="margin-bottom: 30px">
        <el-step title="上传文件" description="选择并上传Excel文件" />
        <el-step title="数据校验" description="自动校验数据格式" />
        <el-step title="确认导入" description="查看并确认导入结果" />
        <el-step title="导入完成" description="查看导入统计" />
      </el-steps>

      <!-- 步骤1: 上传文件 -->
      <div v-show="activeStep === 0" class="step-content">
        <div class="upload-section">
          <el-upload
            ref="uploadRef"
            class="upload-demo"
            drag
            :action="uploadAction"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :file-list="fileList"
            :limit="1"
            :auto-upload="false"
            accept=".xlsx,.xls"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 xlsx/xls 文件，且不超过 5MB
              </div>
            </template>
          </el-upload>

          <el-alert
            title="导入说明"
            type="info"
            :closable="false"
            style="margin-top: 20px"
          >
            <template #default>
              <div class="import-tips">
                <p><strong>1. 下载模板：</strong>点击右上角"下载模板"按钮获取标准Excel模板</p>
                <p><strong>2. 填写数据：</strong>按模板格式填写订单信息，必填字段不能为空</p>
                <p><strong>3. 数据要求：</strong></p>
                <ul>
                  <li>寄收地址必须在服务空域内</li>
                  <li>物品重量不能超过可用飞行器载重上限</li>
                  <li>物品类型不能为禁运品</li>
                  <li>预约时间必须在当前时间之后</li>
                </ul>
                <p><strong>4. 批量限制：</strong>单次最多导入 1000 条订单</p>
              </div>
            </template>
          </el-alert>
        </div>

        <div class="step-actions">
          <el-button type="primary" @click="handleNextStep" :disabled="fileList.length === 0">
            下一步
          </el-button>
        </div>
      </div>

      <!-- 步骤2: 数据校验 -->
      <div v-show="activeStep === 1" class="step-content">
        <el-alert
          v-if="validationResult.hasError"
          title="数据校验发现问题"
          type="warning"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            共 {{ validationResult.total }} 条数据，其中 {{ validationResult.errorCount }} 条存在错误，
            {{ validationResult.successCount }} 条校验通过
          </template>
        </el-alert>

        <el-alert
          v-else
          title="数据校验通过"
          type="success"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            共 {{ validationResult.total }} 条数据，全部校验通过
          </template>
        </el-alert>

        <!-- 校验结果表格 -->
        <el-table
          :data="validationResult.data"
          style="width: 100%"
          v-loading="validating"
          max-height="500"
        >
          <el-table-column type="index" label="行号" width="60" />
          <el-table-column prop="sendAddr" label="寄件地址" width="150" show-overflow-tooltip />
          <el-table-column prop="recvAddr" label="收件地址" width="150" show-overflow-tooltip />
          <el-table-column prop="itemType" label="物品类型" width="100" />
          <el-table-column prop="weight" label="重量(kg)" width="100" />
          <el-table-column prop="urgency" label="时效" width="80">
            <template #default="{ row }">
              {{ row.urgency ? '加急' : '普通' }}
            </template>
          </el-table-column>
          <el-table-column prop="scheduledTime" label="预约时间" width="160" />
          <el-table-column prop="status" label="校验状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.hasError ? 'danger' : 'success'" size="small">
                {{ row.hasError ? '校验失败' : '校验通过' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span style="color: #f56c6c">{{ row.errorMsg }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="step-actions">
          <el-button @click="handlePrevStep">上一步</el-button>
          <el-button
            type="primary"
            @click="handleNextStep"
            :disabled="validationResult.errorCount > 0"
          >
            下一步
          </el-button>
        </div>
      </div>

      <!-- 步骤3: 确认导入 -->
      <div v-show="activeStep === 2" class="step-content">
        <el-alert
          title="确认导入信息"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            即将导入 {{ validationResult.successCount }} 条订单，请确认无误后点击"开始导入"
          </template>
        </el-alert>

        <!-- 导入配置 -->
        <el-form :model="importConfig" label-width="120px" style="max-width: 600px">
          <el-form-item label="导入模式">
            <el-radio-group v-model="importConfig.mode">
              <el-radio value="normal">正常导入</el-radio>
              <el-radio value="skip">跳过重复订单</el-radio>
              <el-radio value="update">更新已存在订单</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="自动接单">
            <el-switch v-model="importConfig.autoAccept" />
            <span style="margin-left: 10px; font-size: 12px; color: #909399">
              启用后将自动分配给对应商家
            </span>
          </el-form-item>
          <el-form-item label="发送通知">
            <el-switch v-model="importConfig.sendNotification" />
            <span style="margin-left: 10px; font-size: 12px; color: #909399">
              通知客户订单已创建
            </span>
          </el-form-item>
        </el-form>

        <!-- 导入预览统计 -->
        <div class="import-summary">
          <el-descriptions title="导入统计" :column="2" border>
            <el-descriptions-item label="总订单数">{{ validationResult.successCount }}</el-descriptions-item>
            <el-descriptions-item label="预计耗时">约 {{ Math.ceil(validationResult.successCount / 10) }} 秒</el-descriptions-item>
            <el-descriptions-item label="普通订单">{{ importStats.normal }}</el-descriptions-item>
            <el-descriptions-item label="加急订单">{{ importStats.urgent }}</el-descriptions-item>
            <el-descriptions-item label="预计总重量">{{ importStats.totalWeight }} kg</el-descriptions-item>
            <el-descriptions-item label="预约订单">{{ importStats.scheduled }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="step-actions">
          <el-button @click="handlePrevStep">上一步</el-button>
          <el-button type="primary" @click="handleStartImport" :loading="importing">
            开始导入
          </el-button>
        </div>
      </div>

      <!-- 步骤4: 导入完成 -->
      <div v-show="activeStep === 3" class="step-content">
        <el-result
          :icon="importResult.success ? 'success' : 'warning'"
          :title="importResult.success ? '导入成功' : '导入完成（部分失败）'"
        >
          <template #sub-title>
            <div class="import-result">
              <p>总计导入：{{ importResult.total }} 条</p>
              <p style="color: #67c23a">成功：{{ importResult.successCount }} 条</p>
              <p v-if="importResult.failureCount > 0" style="color: #f56c6c">
                失败：{{ importResult.failureCount }} 条
              </p>
              <p style="color: #909399">耗时：{{ importResult.duration }} 秒</p>
            </div>
          </template>
          <template #extra>
            <el-button type="primary" @click="handleViewOrders">查看订单列表</el-button>
            <el-button @click="handleReset">继续导入</el-button>
            <el-button
              v-if="importResult.failureCount > 0"
              @click="handleExportErrors"
            >
              导出失败记录
            </el-button>
          </template>
        </el-result>

        <!-- 失败详情 -->
        <div v-if="importResult.failureCount > 0" style="margin-top: 20px">
          <el-divider content-position="left">失败详情</el-divider>
          <el-table :data="importResult.failureList" style="width: 100%">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="rowNumber" label="行号" width="80" />
            <el-table-column prop="sendAddr" label="寄件地址" width="150" show-overflow-tooltip />
            <el-table-column prop="recvAddr" label="收件地址" width="150" show-overflow-tooltip />
            <el-table-column prop="errorMsg" label="失败原因" min-width="200" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Download, UploadFilled } from '@element-plus/icons-vue'

const router = useRouter()

// 当前步骤
const activeStep = ref(0)

// 文件上传
const uploadRef = ref(null)
const fileList = ref([])
const uploadAction = '/api/upload'
const validating = ref(false)
const importing = ref(false)

// 校验结果
const validationResult = reactive({
  total: 0,
  successCount: 0,
  errorCount: 0,
  hasError: false,
  data: []
})

// 导入配置
const importConfig = reactive({
  mode: 'normal',
  autoAccept: false,
  sendNotification: true
})

// 导入统计
const importStats = computed(() => {
  const data = validationResult.data.filter(item => !item.hasError)
  return {
    normal: data.filter(item => !item.urgency).length,
    urgent: data.filter(item => item.urgency).length,
    scheduled: data.filter(item => item.scheduledTime).length,
    totalWeight: data.reduce((sum, item) => sum + (parseFloat(item.weight) || 0), 0).toFixed(2)
  }
})

// 导入结果
const importResult = reactive({
  success: false,
  total: 0,
  successCount: 0,
  failureCount: 0,
  duration: 0,
  failureList: []
})

// 下载模板
const handleDownloadTemplate = () => {
  // TODO: 调用API下载模板
  const link = document.createElement('a')
  link.href = '/templates/order_import_template.xlsx'
  link.download = '订单导入模板.xlsx'
  link.click()
  ElMessage.success('模板下载中...')
}

// 上传前校验
const beforeUpload = (file) => {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
                  file.type === 'application/vnd.ms-excel'
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('文件大小不能超过 5MB!')
    return false
  }
  return true
}

// 文件变化
const handleFileChange = (file, files) => {
  fileList.value = files
}

// 上传成功
const handleUploadSuccess = (response, file, files) => {
  ElMessage.success('文件上传成功')
}

// 上传失败
const handleUploadError = (error, file, files) => {
  ElMessage.error('文件上传失败')
}

// 下一步
const handleNextStep = () => {
  if (activeStep.value === 0) {
    // 开始校验数据
    validateData()
  } else if (activeStep.value === 1) {
    // 进入确认导入
    activeStep.value++
  } else if (activeStep.value === 2) {
    // 已在 handleStartImport 中处理
  }
}

// 上一步
const handlePrevStep = () => {
  if (activeStep.value > 0) {
    activeStep.value--
  }
}

// 校验数据
const validateData = () => {
  validating.value = true

  // TODO: 调用实际API进行数据校验
  setTimeout(() => {
    // 模拟校验结果
    const mockData = [
      {
        sendAddr: '北京市朝阳区建国路88号',
        recvAddr: '北京市海淀区中关村大街1号',
        itemType: '电子产品',
        weight: 2.5,
        urgency: false,
        scheduledTime: '',
        hasError: false,
        errorMsg: ''
      },
      {
        sendAddr: '上海市浦东新区陆家嘴环路1000号',
        recvAddr: '上海市静安区南京西路1000号',
        itemType: '文件',
        weight: 0.5,
        urgency: true,
        scheduledTime: '2026-09-18 10:00',
        hasError: false,
        errorMsg: ''
      },
      {
        sendAddr: '广州市天河区天河路208号',
        recvAddr: '深圳市南山区科技园',
        itemType: '危险品',
        weight: 5.0,
        urgency: false,
        scheduledTime: '',
        hasError: true,
        errorMsg: '物品类型为禁运品'
      },
      {
        sendAddr: '杭州市西湖区文三路',
        recvAddr: '杭州市滨江区江南大道',
        itemType: '食品',
        weight: 25.0,
        urgency: false,
        scheduledTime: '',
        hasError: true,
        errorMsg: '重量超过飞行器载重上限（20kg）'
      },
      {
        sendAddr: '成都市武侯区人民南路',
        recvAddr: '成都市高新区天府大道',
        itemType: '医疗用品',
        weight: 1.0,
        urgency: true,
        scheduledTime: '2026-09-17 16:00',
        hasError: false,
        errorMsg: ''
      }
    ]

    validationResult.data = mockData
    validationResult.total = mockData.length
    validationResult.errorCount = mockData.filter(item => item.hasError).length
    validationResult.successCount = mockData.filter(item => !item.hasError).length
    validationResult.hasError = validationResult.errorCount > 0

    validating.value = false
    activeStep.value = 1
  }, 1500)
}

// 开始导入
const handleStartImport = () => {
  importing.value = true

  const startTime = Date.now()

  // TODO: 调用实际API进行导入
  setTimeout(() => {
    const endTime = Date.now()
    const duration = ((endTime - startTime) / 1000).toFixed(2)

    // 模拟导入结果
    importResult.success = validationResult.errorCount === 0
    importResult.total = validationResult.successCount
    importResult.successCount = validationResult.successCount - 1 // 模拟1条失败
    importResult.failureCount = 1
    importResult.duration = duration
    importResult.failureList = [
      {
        rowNumber: 2,
        sendAddr: '上海市浦东新区陆家嘴环路1000号',
        recvAddr: '上海市静安区南京西路1000号',
        errorMsg: '系统错误：数据库连接超时'
      }
    ]

    importing.value = false
    activeStep.value = 3
    ElMessage.success('导入完成')
  }, 3000)
}

// 查看订单列表
const handleViewOrders = () => {
  router.push('/orders/list')
}

// 重置导入
const handleReset = () => {
  activeStep.value = 0
  fileList.value = []
  validationResult.data = []
  validationResult.total = 0
  validationResult.successCount = 0
  validationResult.errorCount = 0
  validationResult.hasError = false
  importResult.success = false
  importResult.total = 0
  importResult.successCount = 0
  importResult.failureCount = 0
  importResult.failureList = []
}

// 导出失败记录
const handleExportErrors = () => {
  // TODO: 导出失败记录
  ElMessage.success('失败记录导出中...')
}
</script>

<style scoped>
.order-batch-import {
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

.step-content {
  padding: 20px 0;
  min-height: 400px;
}

.upload-section {
  max-width: 800px;
  margin: 0 auto;
}

.import-tips {
  line-height: 1.8;
}

.import-tips p {
  margin: 10px 0;
}

.import-tips ul {
  margin: 5px 0 10px 20px;
}

.import-tips li {
  margin: 5px 0;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.import-summary {
  margin-top: 30px;
}

.import-result {
  font-size: 16px;
  line-height: 2;
}

.import-result p {
  margin: 5px 0;
}

:deep(.el-upload-dragger) {
  padding: 40px;
}
</style>
