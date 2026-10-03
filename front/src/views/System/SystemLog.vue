<template>
  <div class="system-log">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">系统日志</span>
          <div>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon>
              导出日志
            </el-button>
            <el-button @click="handleClearLogs" type="danger">
              <el-icon><Delete /></el-icon>
              清空日志
            </el-button>
          </div>
        </div>
      </template>

      <!-- 日志类型标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="应用日志" name="application">
          <template #label>
            <el-badge :value="logStats.application" :max="999">
              <span>应用日志</span>
            </el-badge>
          </template>
        </el-tab-pane>
        <el-tab-pane label="错误日志" name="error">
          <template #label>
            <el-badge :value="logStats.error" :max="999" type="danger">
              <span>错误日志</span>
            </el-badge>
          </template>
        </el-tab-pane>
        <el-tab-pane label="访问日志" name="access">
          <template #label>
            <el-badge :value="logStats.access" :max="999" type="success">
              <span>访问日志</span>
            </el-badge>
          </template>
        </el-tab-pane>
        <el-tab-pane label="SQL日志" name="sql">
          <template #label>
            <el-badge :value="logStats.sql" :max="999" type="warning">
              <span>SQL日志</span>
            </el-badge>
          </template>
        </el-tab-pane>
        <el-tab-pane label="定时任务" name="cron">
          <template #label>
            <el-badge :value="logStats.cron" :max="999" type="info">
              <span>定时任务</span>
            </el-badge>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 搜索筛选 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="日志级别">
          <el-select v-model="searchForm.level" placeholder="全部" clearable style="width: 120px">
            <el-option label="DEBUG" value="debug" />
            <el-option label="INFO" value="info" />
            <el-option label="WARN" value="warn" />
            <el-option label="ERROR" value="error" />
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
            style="width: 380px"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索日志内容" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 日志列表 -->
      <el-table
        :data="logList"
        style="width: 100%; margin-top: 20px"
        v-loading="loading"
        :row-class-name="getRowClassName"
        @row-click="handleRowClick"
      >
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelTag(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="150" show-overflow-tooltip />
        <el-table-column prop="message" label="日志内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="user" label="用户" width="120" v-if="activeTab === 'access'" />
        <el-table-column prop="ip" label="IP地址" width="140" v-if="activeTab === 'access'" />
        <el-table-column prop="duration" label="耗时(ms)" width="100" v-if="activeTab === 'sql'" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="时间">{{ detailData.time }}</el-descriptions-item>
        <el-descriptions-item label="级别">
          <el-tag :type="getLevelTag(detailData.level)">{{ detailData.level }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="来源" :span="2">{{ detailData.source }}</el-descriptions-item>
        <el-descriptions-item label="用户" v-if="detailData.user">{{ detailData.user }}</el-descriptions-item>
        <el-descriptions-item label="IP地址" v-if="detailData.ip">{{ detailData.ip }}</el-descriptions-item>
        <el-descriptions-item label="请求方法" v-if="detailData.method">{{ detailData.method }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" v-if="detailData.url" :span="2">{{ detailData.url }}</el-descriptions-item>
        <el-descriptions-item label="日志内容" :span="2">{{ detailData.message }}</el-descriptions-item>
      </el-descriptions>

      <!-- 堆栈信息 -->
      <div v-if="detailData.stack" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">堆栈信息：</div>
        <el-input
          v-model="detailData.stack"
          type="textarea"
          :rows="10"
          readonly
          style="font-family: monospace; font-size: 12px"
        />
      </div>

      <!-- 请求参数 -->
      <div v-if="detailData.params" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">请求参数：</div>
        <el-input
          v-model="detailData.params"
          type="textarea"
          :rows="5"
          readonly
          style="font-family: monospace; font-size: 12px"
        />
      </div>

      <!-- SQL语句 -->
      <div v-if="detailData.sql" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">SQL语句：</div>
        <el-input
          v-model="detailData.sql"
          type="textarea"
          :rows="5"
          readonly
          style="font-family: monospace; font-size: 12px"
        />
      </div>

      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Delete, Refresh } from '@element-plus/icons-vue'

// 当前标签页
const activeTab = ref('application')

// 日志统计
const logStats = reactive({
  application: 1256,
  error: 23,
  access: 8965,
  sql: 5432,
  cron: 156
})

// 搜索表单
const searchForm = reactive({
  level: '',
  dateRange: [],
  keyword: ''
})

// 日志列表
const logList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 详情对话框
const detailVisible = ref(false)
const detailData = ref({})

// 初始化
onMounted(() => {
  fetchLogList()
})

// 获取日志列表
const fetchLogList = () => {
  loading.value = true

  setTimeout(() => {
    // 根据不同类型返回不同数据
    if (activeTab.value === 'application') {
      logList.value = [
        {
          id: 1,
          time: '2026-09-17 14:32:15',
          level: 'INFO',
          source: 'OrderController',
          message: '订单创建成功 - ORD-2024091700001',
          user: 'admin',
          stack: null
        },
        {
          id: 2,
          time: '2026-09-17 14:31:58',
          level: 'INFO',
          source: 'FlightPlanService',
          message: '飞行计划审批通过 - FP-2024091700089',
          user: 'aircontrol01',
          stack: null
        },
        {
          id: 3,
          time: '2026-09-17 14:31:42',
          level: 'WARN',
          source: 'AuthenticationFilter',
          message: '用户登录失败，密码错误 - user123',
          user: 'user123',
          stack: null
        }
      ]
    } else if (activeTab.value === 'error') {
      logList.value = [
        {
          id: 1,
          time: '2026-09-17 14:30:25',
          level: 'ERROR',
          source: 'DatabaseConnection',
          message: 'Connection timeout: unable to connect to database',
          stack: 'java.sql.SQLException: Connection timeout\n  at com.mysql.jdbc.Driver.connect(Driver.java:281)\n  at java.sql.DriverManager.getConnection(DriverManager.java:664)'
        },
        {
          id: 2,
          time: '2026-09-17 14:28:10',
          level: 'ERROR',
          source: 'OrderService',
          message: 'NullPointerException in processOrder method',
          stack: 'java.lang.NullPointerException\n  at com.uav.service.OrderService.processOrder(OrderService.java:125)'
        }
      ]
    } else if (activeTab.value === 'access') {
      logList.value = [
        {
          id: 1,
          time: '2026-09-17 14:32:15',
          level: 'INFO',
          source: 'AccessLog',
          message: 'GET /api/orders/list',
          user: 'admin',
          ip: '192.168.1.100',
          method: 'GET',
          url: '/api/orders/list'
        },
        {
          id: 2,
          time: '2026-09-17 14:32:10',
          level: 'INFO',
          source: 'AccessLog',
          message: 'POST /api/flight-plan/create',
          user: 'merchant01',
          ip: '192.168.1.105',
          method: 'POST',
          url: '/api/flight-plan/create'
        }
      ]
    } else if (activeTab.value === 'sql') {
      logList.value = [
        {
          id: 1,
          time: '2026-09-17 14:32:15',
          level: 'DEBUG',
          source: 'MyBatis',
          message: 'SELECT * FROM orders WHERE status = ?',
          duration: 235,
          sql: 'SELECT * FROM orders WHERE status = ? AND create_time > ?'
        },
        {
          id: 2,
          time: '2026-09-17 14:32:10',
          level: 'DEBUG',
          source: 'MyBatis',
          message: 'INSERT INTO flight_plan VALUES (?, ?, ?)',
          duration: 45,
          sql: 'INSERT INTO flight_plan (id, plan_no, status) VALUES (?, ?, ?)'
        }
      ]
    } else if (activeTab.value === 'cron') {
      logList.value = [
        {
          id: 1,
          time: '2026-09-17 14:00:00',
          level: 'INFO',
          source: 'ScheduledTask',
          message: '定时任务执行：订单状态同步',
          stack: null
        },
        {
          id: 2,
          time: '2026-09-17 13:00:00',
          level: 'INFO',
          source: 'ScheduledTask',
          message: '定时任务执行：数据统计',
          stack: null
        }
      ]
    }

    pagination.total = logStats[activeTab.value]
    loading.value = false
  }, 500)
}

// 标签页切换
const handleTabChange = () => {
  pagination.page = 1
  searchForm.level = ''
  searchForm.dateRange = []
  searchForm.keyword = ''
  fetchLogList()
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchLogList()
}

// 重置
const handleReset = () => {
  searchForm.level = ''
  searchForm.dateRange = []
  searchForm.keyword = ''
  handleSearch()
}

// 刷新
const handleRefresh = () => {
  fetchLogList()
  ElMessage.success('日志已刷新')
}

// 行点击
const handleRowClick = (row) => {
  // 可以在这里实现点击行的逻辑
}

// 查看详情
const handleViewDetail = (row) => {
  detailData.value = { ...row }
  detailVisible.value = true
}

// 导出日志
const handleExport = () => {
  ElMessage.success('日志导出中...')
  // TODO: 调用导出API
}

// 清空日志
const handleClearLogs = () => {
  ElMessageBox.confirm(
    '确定要清空当前类型的所有日志吗？此操作不可恢复',
    '清空日志',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // TODO: 调用清空API
    ElMessage.success('日志已清空')
    fetchLogList()
  })
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
const getLevelTag = (level) => {
  const map = {
    DEBUG: 'info',
    INFO: '',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return map[level] || ''
}

const getRowClassName = ({ row }) => {
  if (row.level === 'ERROR') {
    return 'error-row'
  } else if (row.level === 'WARN') {
    return 'warn-row'
  }
  return ''
}
</script>

<style scoped>
.system-log {
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

:deep(.error-row) {
  background-color: #fef0f0;
}

:deep(.warn-row) {
  background-color: #fdf6ec;
}

:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}
</style>
