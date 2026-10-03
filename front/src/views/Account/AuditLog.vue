<template>
  <div class="audit-log">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">操作日志</span>
          <div>
            <el-button @click="handleExport" :loading="exporting">
              <el-icon><Download /></el-icon>
              导出日志
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作人">
          <el-input v-model="searchForm.operator" placeholder="用户名或姓名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="操作模块">
          <el-select v-model="searchForm.module" placeholder="全部" clearable style="width: 150px">
            <el-option label="账号管理" value="account" />
            <el-option label="订单管理" value="order" />
            <el-option label="资源管理" value="resource" />
            <el-option label="空域管理" value="airspace" />
            <el-option label="系统配置" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.actionType" placeholder="全部" clearable style="width: 150px">
            <el-option label="新增" value="create" />
            <el-option label="修改" value="update" />
            <el-option label="删除" value="delete" />
            <el-option label="查询" value="query" />
            <el-option label="导出" value="export" />
            <el-option label="登录" value="login" />
            <el-option label="登出" value="logout" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作结果">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failure" />
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
        <el-form-item label="IP地址">
          <el-input v-model="searchForm.ip" placeholder="IP地址" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计信息 -->
      <div class="stats-row">
        <el-statistic title="今日操作" :value="stats.todayCount" />
        <el-statistic title="本周操作" :value="stats.weekCount" />
        <el-statistic title="本月操作" :value="stats.monthCount" />
        <el-statistic title="失败次数" :value="stats.failureCount" />
      </div>

      <!-- 日志列表 -->
      <el-table :data="logList" style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="operator" label="操作人" width="120">
          <template #default="{ row }">
            <div>
              <div>{{ row.operatorName }}</div>
              <div style="font-size: 12px; color: #909399">{{ row.operator }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)" size="small">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="120">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ getModuleName(row.module) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.actionType)" size="small">
              {{ getActionName(row.actionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="location" label="地理位置" width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时(ms)" width="100" />
        <el-table-column prop="timestamp" label="操作时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="日志详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作人">{{ detailData.operatorName }} ({{ detailData.operator }})</el-descriptions-item>
        <el-descriptions-item label="角色">{{ getRoleName(detailData.role) }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ getModuleName(detailData.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ getActionName(detailData.actionType) }}</el-descriptions-item>
        <el-descriptions-item label="操作结果">
          <el-tag :type="detailData.status === 'success' ? 'success' : 'danger'">
            {{ detailData.status === 'success' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailData.duration }} ms</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ detailData.ip }}</el-descriptions-item>
        <el-descriptions-item label="地理位置">{{ detailData.location }}</el-descriptions-item>
        <el-descriptions-item label="浏览器">{{ detailData.browser }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ detailData.os }}</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detailData.timestamp }}</el-descriptions-item>
        <el-descriptions-item label="操作内容" :span="2">{{ detailData.description }}</el-descriptions-item>
      </el-descriptions>

      <!-- 请求参数 -->
      <div v-if="detailData.requestParams" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">请求参数：</div>
        <el-input
          v-model="detailData.requestParams"
          type="textarea"
          :rows="6"
          readonly
          style="font-family: monospace"
        />
      </div>

      <!-- 响应结果 -->
      <div v-if="detailData.responseData" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">响应结果：</div>
        <el-input
          v-model="detailData.responseData"
          type="textarea"
          :rows="6"
          readonly
          style="font-family: monospace"
        />
      </div>

      <!-- 错误信息 -->
      <div v-if="detailData.errorMsg" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold; color: #f56c6c">错误信息：</div>
        <el-alert :title="detailData.errorMsg" type="error" :closable="false" />
      </div>

      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'

// 搜索表单
const searchForm = reactive({
  operator: '',
  module: '',
  actionType: '',
  status: '',
  dateRange: [],
  ip: ''
})

// 统计数据
const stats = reactive({
  todayCount: 1245,
  weekCount: 8632,
  monthCount: 35678,
  failureCount: 23
})

// 列表数据
const logList = ref([])
const loading = ref(false)
const exporting = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 详情对话框
const detailDialogVisible = ref(false)
const detailData = ref({})

// 初始化
onMounted(() => {
  fetchLogList()
})

// 获取日志列表
const fetchLogList = () => {
  loading.value = true

  // TODO: 调用实际API
  setTimeout(() => {
    logList.value = [
      {
        id: 1,
        operator: 'admin',
        operatorName: '系统管理员',
        role: 'ADMIN',
        module: 'order',
        actionType: 'create',
        description: '创建订单 #ORD-2024091700001',
        ip: '192.168.1.100',
        location: '北京市',
        browser: 'Chrome 118.0',
        os: 'Windows 11',
        status: 'success',
        duration: 245,
        timestamp: '2026-09-17 14:32:15',
        requestParams: '{"sendAddr":"北京市朝阳区","recvAddr":"北京市海淀区","weight":2.5}',
        responseData: '{"code":200,"data":{"orderId":"ORD-2024091700001"}}'
      },
      {
        id: 2,
        operator: 'merchant01',
        operatorName: '天翔物流',
        role: 'MERCHANT',
        module: 'airspace',
        actionType: 'update',
        description: '修改飞行计划 #FP-2024091700056',
        ip: '192.168.1.105',
        location: '上海市',
        browser: 'Edge 118.0',
        os: 'Windows 10',
        status: 'success',
        duration: 189,
        timestamp: '2026-09-17 14:28:43',
        requestParams: '{"planId":"FP-2024091700056","altitude":120}',
        responseData: '{"code":200,"message":"修改成功"}'
      },
      {
        id: 3,
        operator: 'user123',
        operatorName: '张三',
        role: 'CUSTOMER',
        module: 'account',
        actionType: 'login',
        description: '用户登录',
        ip: '58.246.123.45',
        location: '广东省深圳市',
        browser: 'Safari 16.0',
        os: 'macOS 13',
        status: 'success',
        duration: 523,
        timestamp: '2026-09-17 14:25:12',
        requestParams: '{"username":"user123"}',
        responseData: '{"code":200,"data":{"token":"eyJhbGc..."}}'
      },
      {
        id: 4,
        operator: 'admin',
        operatorName: '系统管理员',
        role: 'ADMIN',
        module: 'resource',
        actionType: 'delete',
        description: '删除飞行器 #UAV-001',
        ip: '192.168.1.100',
        location: '北京市',
        browser: 'Chrome 118.0',
        os: 'Windows 11',
        status: 'failure',
        duration: 156,
        timestamp: '2026-09-17 14:20:35',
        requestParams: '{"aircraftId":"UAV-001"}',
        responseData: null,
        errorMsg: '该飞行器正在执行任务中，无法删除'
      },
      {
        id: 5,
        operator: 'aircontrol01',
        operatorName: '空管人员',
        role: 'AIR_CONTROL',
        module: 'airspace',
        actionType: 'update',
        description: '审批飞行计划 #FP-2024091700055 - 通过',
        ip: '192.168.1.88',
        location: '北京市',
        browser: 'Firefox 118.0',
        os: 'Windows 10',
        status: 'success',
        duration: 892,
        timestamp: '2026-09-17 14:15:20',
        requestParams: '{"planId":"FP-2024091700055","status":"approved"}',
        responseData: '{"code":200,"message":"审批成功"}'
      },
      {
        id: 6,
        operator: 'merchant01',
        operatorName: '天翔物流',
        role: 'MERCHANT',
        module: 'system',
        actionType: 'export',
        description: '导出订单报表',
        ip: '192.168.1.105',
        location: '上海市',
        browser: 'Edge 118.0',
        os: 'Windows 10',
        status: 'success',
        duration: 3245,
        timestamp: '2026-09-17 14:10:08',
        requestParams: '{"startDate":"2024-09-01","endDate":"2024-09-17","format":"excel"}',
        responseData: '{"code":200,"data":{"fileUrl":"/exports/orders_20240917.xlsx"}}'
      },
      {
        id: 7,
        operator: 'operator01',
        operatorName: '李四',
        role: 'OPERATOR',
        module: 'resource',
        actionType: 'update',
        description: '更新飞行器维保记录 #UAV-005',
        ip: '192.168.1.120',
        location: '浙江省杭州市',
        browser: 'Chrome 118.0',
        os: 'Windows 11',
        status: 'success',
        duration: 678,
        timestamp: '2026-09-17 14:05:52',
        requestParams: '{"aircraftId":"UAV-005","maintenanceType":"routine"}',
        responseData: '{"code":200,"message":"更新成功"}'
      },
      {
        id: 8,
        operator: 'admin',
        operatorName: '系统管理员',
        role: 'ADMIN',
        module: 'system',
        actionType: 'update',
        description: '修改系统配置 - 订单超时时间',
        ip: '192.168.1.100',
        location: '北京市',
        browser: 'Chrome 118.0',
        os: 'Windows 11',
        status: 'success',
        duration: 234,
        timestamp: '2026-09-17 14:00:15',
        requestParams: '{"configKey":"order.timeout","configValue":"7200"}',
        responseData: '{"code":200,"message":"配置更新成功"}'
      }
    ]
    pagination.total = 156
    loading.value = false
  }, 500)
}

// 查看详情
const handleView = (row) => {
  detailData.value = { ...row }
  detailDialogVisible.value = true
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchLogList()
}

// 重置
const handleReset = () => {
  searchForm.operator = ''
  searchForm.module = ''
  searchForm.actionType = ''
  searchForm.status = ''
  searchForm.dateRange = []
  searchForm.ip = ''
  handleSearch()
}

// 导出日志
const handleExport = () => {
  exporting.value = true
  // TODO: 调用导出API
  setTimeout(() => {
    ElMessage.success('导出成功')
    exporting.value = false
  }, 2000)
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchLogList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchLogList()
}

// 辅助函数
const getRoleName = (role) => {
  const roleMap = {
    ADMIN: '管理员',
    MERCHANT: '商家',
    CUSTOMER: '客户',
    AIR_CONTROL: '空管',
    OPERATOR: '运维',
    PUBLIC: '公众'
  }
  return roleMap[role] || role
}

const getRoleTagType = (role) => {
  const typeMap = {
    ADMIN: 'danger',
    MERCHANT: 'warning',
    CUSTOMER: 'success',
    AIR_CONTROL: 'primary',
    OPERATOR: 'info',
    PUBLIC: ''
  }
  return typeMap[role] || ''
}

const getModuleName = (module) => {
  const moduleMap = {
    account: '账号管理',
    order: '订单管理',
    resource: '资源管理',
    airspace: '空域管理',
    system: '系统配置'
  }
  return moduleMap[module] || module
}

const getActionName = (actionType) => {
  const actionMap = {
    create: '新增',
    update: '修改',
    delete: '删除',
    query: '查询',
    export: '导出',
    login: '登录',
    logout: '登出'
  }
  return actionMap[actionType] || actionType
}

const getActionTagType = (actionType) => {
  const typeMap = {
    create: 'success',
    update: 'primary',
    delete: 'danger',
    query: 'info',
    export: 'warning',
    login: '',
    logout: ''
  }
  return typeMap[actionType] || ''
}
</script>

<style scoped>
.audit-log {
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

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin: 20px 0;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
