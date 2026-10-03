<template>
  <div class="notification-center">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">消息通知</span>
          <div>
            <el-badge :value="unreadCount" :max="99" type="danger">
              <el-button @click="handleMarkAllRead">全部标记为已读</el-button>
            </el-badge>
            <el-button @click="handleSettings">
              <el-icon><Setting /></el-icon>
              通知设置
            </el-button>
          </div>
        </div>
      </template>

      <div class="notification-container">
        <!-- 左侧分类 -->
        <div class="category-sidebar">
          <el-menu :default-active="activeCategory" @select="handleCategorySelect">
            <el-menu-item index="all">
              <el-badge :value="stats.all" :max="99" type="danger">
                <span>全部消息</span>
              </el-badge>
            </el-menu-item>
            <el-menu-item index="system">
              <el-badge :value="stats.system" :max="99" type="primary">
                <span>
                  <el-icon><Bell /></el-icon>
                  系统通知
                </span>
              </el-badge>
            </el-menu-item>
            <el-menu-item index="order">
              <el-badge :value="stats.order" :max="99" type="success">
                <span>
                  <el-icon><Document /></el-icon>
                  订单消息
                </span>
              </el-badge>
            </el-menu-item>
            <el-menu-item index="flight">
              <el-badge :value="stats.flight" :max="99" type="warning">
                <span>
                  <el-icon><Promotion /></el-icon>
                  飞行计划
                </span>
              </el-badge>
            </el-menu-item>
            <el-menu-item index="alert">
              <el-badge :value="stats.alert" :max="99" type="danger">
                <span>
                  <el-icon><Warning /></el-icon>
                  告警提醒
                </span>
              </el-badge>
            </el-menu-item>
            <el-menu-item index="task">
              <el-badge :value="stats.task" :max="99" type="info">
                <span>
                  <el-icon><Operation /></el-icon>
                  任务提醒
                </span>
              </el-badge>
            </el-menu-item>
          </el-menu>
        </div>

        <!-- 右侧消息列表 -->
        <div class="message-area">
          <!-- 筛选栏 -->
          <div class="filter-bar">
            <el-radio-group v-model="filterStatus" @change="handleFilterChange">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="unread">未读</el-radio-button>
              <el-radio-button value="read">已读</el-radio-button>
            </el-radio-group>
            <div class="filter-actions">
              <el-button link @click="handleBatchDelete" v-if="selectedIds.length > 0">
                删除选中 ({{ selectedIds.length }})
              </el-button>
              <el-button link @click="handleClearRead">清空已读</el-button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="message-list" v-loading="loading">
            <el-empty v-if="messageList.length === 0" description="暂无消息" />

            <div
              v-for="message in messageList"
              :key="message.id"
              class="message-item"
              :class="{ unread: !message.read, selected: selectedIds.includes(message.id) }"
              @click="handleMessageClick(message)"
            >
              <el-checkbox
                v-model="message.selected"
                @click.stop
                @change="handleSelectChange(message)"
                class="message-checkbox"
              />

              <div class="message-icon" :class="message.type">
                <el-icon v-if="message.type === 'system'"><Bell /></el-icon>
                <el-icon v-else-if="message.type === 'order'"><Document /></el-icon>
                <el-icon v-else-if="message.type === 'flight'"><Promotion /></el-icon>
                <el-icon v-else-if="message.type === 'alert'"><Warning /></el-icon>
                <el-icon v-else-if="message.type === 'task'"><Operation /></el-icon>
              </div>

              <div class="message-content">
                <div class="message-header">
                  <span class="message-title">{{ message.title }}</span>
                  <el-tag v-if="message.priority === 'high'" type="danger" size="small">紧急</el-tag>
                  <span class="message-time">{{ message.time }}</span>
                </div>
                <div class="message-body">{{ message.content }}</div>
                <div class="message-footer" v-if="message.actions && message.actions.length">
                  <el-button
                    v-for="action in message.actions"
                    :key="action.type"
                    :type="action.btnType"
                    size="small"
                    @click.stop="handleAction(message, action)"
                  >
                    {{ action.label }}
                  </el-button>
                </div>
              </div>

              <div class="message-operations">
                <el-button link @click.stop="handleMarkRead(message)" v-if="!message.read">
                  标为已读
                </el-button>
                <el-button link type="danger" @click.stop="handleDeleteMessage(message)">
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-if="messageList.length > 0"
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
            style="margin-top: 20px; justify-content: center"
          />
        </div>
      </div>
    </el-card>

    <!-- 消息详情对话框 -->
    <el-dialog v-model="detailVisible" :title="detailData.title" width="600px">
      <div class="message-detail">
        <div class="detail-meta">
          <el-tag :type="getTypeTag(detailData.type)">{{ getTypeName(detailData.type) }}</el-tag>
          <span>{{ detailData.time }}</span>
        </div>
        <el-divider />
        <div class="detail-content">{{ detailData.content }}</div>
        <div v-if="detailData.extra" class="detail-extra">
          <el-descriptions :column="2" border>
            <el-descriptions-item
              v-for="(value, key) in detailData.extra"
              :key="key"
              :label="key"
            >
              {{ value }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-for="action in detailData.actions"
          :key="action.type"
          :type="action.btnType"
          @click="handleAction(detailData, action)"
        >
          {{ action.label }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 通知设置对话框 -->
    <el-dialog v-model="settingsVisible" title="通知设置" width="600px">
      <el-form :model="settingsForm" label-width="120px">
        <el-form-item label="系统通知">
          <el-switch v-model="settingsForm.system" />
          <span style="margin-left: 10px; font-size: 12px; color: #909399">
            系统公告、维护通知等
          </span>
        </el-form-item>
        <el-form-item label="订单消息">
          <el-switch v-model="settingsForm.order" />
          <span style="margin-left: 10px; font-size: 12px; color: #909399">
            订单状态变更、配送通知等
          </span>
        </el-form-item>
        <el-form-item label="飞行计划">
          <el-switch v-model="settingsForm.flight" />
          <span style="margin-left: 10px; font-size: 12px; color: #909399">
            飞行计划审批、变更通知等
          </span>
        </el-form-item>
        <el-form-item label="告警提醒">
          <el-switch v-model="settingsForm.alert" />
          <span style="margin-left: 10px; font-size: 12px; color: #909399">
            紧急告警、异常提醒等
          </span>
        </el-form-item>
        <el-form-item label="任务提醒">
          <el-switch v-model="settingsForm.task" />
          <span style="margin-left: 10px; font-size: 12px; color: #909399">
            待办任务、工单提醒等
          </span>
        </el-form-item>
        <el-divider />
        <el-form-item label="邮件通知">
          <el-switch v-model="settingsForm.email" />
        </el-form-item>
        <el-form-item label="短信通知">
          <el-switch v-model="settingsForm.sms" />
        </el-form-item>
        <el-form-item label="免打扰时段">
          <el-time-picker
            v-model="settingsForm.doNotDisturbTime"
            is-range
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="HH:mm"
            value-format="HH:mm"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Bell, Document, Promotion, Warning, Operation } from '@element-plus/icons-vue'

// 统计数据
const stats = reactive({
  all: 15,
  system: 3,
  order: 5,
  flight: 4,
  alert: 2,
  task: 1
})

const unreadCount = computed(() => stats.all)

// 当前分类
const activeCategory = ref('all')

// 筛选状态
const filterStatus = ref('all')

// 选中的消息ID
const selectedIds = ref([])

// 消息列表
const messageList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 详情对话框
const detailVisible = ref(false)
const detailData = ref({})

// 设置对话框
const settingsVisible = ref(false)
const settingsForm = reactive({
  system: true,
  order: true,
  flight: true,
  alert: true,
  task: true,
  email: true,
  sms: false,
  doNotDisturbTime: []
})

// 初始化
onMounted(() => {
  fetchMessageList()
})

// 获取消息列表
const fetchMessageList = () => {
  loading.value = true

  setTimeout(() => {
    messageList.value = [
      {
        id: 1,
        type: 'system',
        priority: 'high',
        title: '系统升级维护通知',
        content: '平台将于2026年9月20日凌晨2:00-6:00进行系统升级维护，届时将无法访问。',
        time: '2026-09-17 10:00',
        read: false,
        selected: false,
        actions: []
      },
      {
        id: 2,
        type: 'order',
        priority: 'normal',
        title: '订单 #ORD-2024091700056 已完成',
        content: '您的订单已成功送达，感谢使用！',
        time: '2026-09-17 15:30',
        read: false,
        selected: false,
        actions: [
          { type: 'view', label: '查看订单', btnType: 'primary' },
          { type: 'evaluate', label: '评价', btnType: 'success' }
        ]
      },
      {
        id: 3,
        type: 'flight',
        priority: 'normal',
        title: '飞行计划 #FP-2024091700089 已通过',
        content: '您的飞行计划已通过审批，可以按计划执行。',
        time: '2026-09-17 14:15',
        read: false,
        selected: false,
        actions: [
          { type: 'view', label: '查看详情', btnType: 'primary' }
        ]
      },
      {
        id: 4,
        type: 'alert',
        priority: 'high',
        title: '飞行器电量低于30%',
        content: '飞行器 M300-005 电量仅剩28%，请尽快返航充电。',
        time: '2026-09-17 14:05',
        read: false,
        selected: false,
        actions: [
          { type: 'handle', label: '立即处理', btnType: 'danger' }
        ]
      },
      {
        id: 5,
        type: 'task',
        priority: 'normal',
        title: '您有1个待处理工单',
        content: '客户提交的工单 #WO-2024091700023 等待处理。',
        time: '2026-09-17 13:20',
        read: false,
        selected: false,
        actions: [
          { type: 'process', label: '去处理', btnType: 'primary' }
        ]
      },
      {
        id: 6,
        type: 'order',
        priority: 'normal',
        title: '订单 #ORD-2024091700045 配送中',
        content: '您的订单正在配送中，预计30分钟后送达。',
        time: '2026-09-17 12:45',
        read: true,
        selected: false,
        actions: []
      }
    ]
    pagination.total = 56
    loading.value = false
  }, 500)
}

// 分类选择
const handleCategorySelect = (key) => {
  activeCategory.value = key
  pagination.page = 1
  selectedIds.value = []
  fetchMessageList()
}

// 筛选变化
const handleFilterChange = () => {
  pagination.page = 1
  selectedIds.value = []
  fetchMessageList()
}

// 消息点击
const handleMessageClick = (message) => {
  if (!message.read) {
    message.read = true
    stats.all--
    stats[message.type]--
  }
  detailData.value = { ...message }
  detailVisible.value = true
}

// 选择变化
const handleSelectChange = (message) => {
  if (message.selected) {
    selectedIds.value.push(message.id)
  } else {
    selectedIds.value = selectedIds.value.filter(id => id !== message.id)
  }
}

// 标记已读
const handleMarkRead = (message) => {
  message.read = true
  stats.all--
  stats[message.type]--
  ElMessage.success('已标记为已读')
}

// 全部标记已读
const handleMarkAllRead = () => {
  ElMessageBox.confirm('确定将所有消息标记为已读吗？', '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    messageList.value.forEach(msg => {
      msg.read = true
    })
    stats.all = 0
    stats.system = 0
    stats.order = 0
    stats.flight = 0
    stats.alert = 0
    stats.task = 0
    ElMessage.success('全部已标记为已读')
  })
}

// 删除消息
const handleDeleteMessage = (message) => {
  ElMessageBox.confirm('确定要删除该消息吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    if (!message.read) {
      stats.all--
      stats[message.type]--
    }
    const index = messageList.value.findIndex(m => m.id === message.id)
    if (index > -1) {
      messageList.value.splice(index, 1)
    }
    ElMessage.success('删除成功')
  })
}

// 批量删除
const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条消息吗？`, '批量删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    messageList.value = messageList.value.filter(msg => !selectedIds.value.includes(msg.id))
    selectedIds.value = []
    ElMessage.success('批量删除成功')
    // 重新统计未读数
    // TODO: 实际应该调用API重新获取
  })
}

// 清空已读
const handleClearRead = () => {
  ElMessageBox.confirm('确定要清空所有已读消息吗？', '清空确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    messageList.value = messageList.value.filter(msg => !msg.read)
    ElMessage.success('已清空已读消息')
  })
}

// 消息操作
const handleAction = (message, action) => {
  ElMessage.info(`执行操作: ${action.label}`)
  // TODO: 根据action.type执行相应操作
}

// 通知设置
const handleSettings = () => {
  settingsVisible.value = true
}

// 保存设置
const handleSaveSettings = () => {
  ElMessage.success('设置已保存')
  settingsVisible.value = false
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchMessageList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchMessageList()
}

// 辅助函数
const getTypeName = (type) => {
  const map = {
    system: '系统通知',
    order: '订单消息',
    flight: '飞行计划',
    alert: '告警提醒',
    task: '任务提醒'
  }
  return map[type] || type
}

const getTypeTag = (type) => {
  const map = {
    system: 'primary',
    order: 'success',
    flight: 'warning',
    alert: 'danger',
    task: 'info'
  }
  return map[type] || ''
}
</script>

<style scoped>
.notification-center {
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

.notification-container {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
}

.category-sidebar {
  position: sticky;
  top: 20px;
  height: fit-content;
}

.category-sidebar .el-menu {
  border: none;
}

.category-sidebar .el-menu-item {
  border-radius: 4px;
  margin-bottom: 5px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #dcdfe6;
}

.filter-actions {
  display: flex;
  gap: 10px;
}

.message-list {
  min-height: 400px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 15px;
  margin-bottom: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: all 0.3s;
}

.message-item:hover {
  background: #e4e7ed;
  transform: translateX(3px);
}

.message-item.unread {
  background: #ecf5ff;
  border-left-color: #409eff;
}

.message-item.selected {
  background: #f0f9ff;
  border-left-color: #67c23a;
}

.message-checkbox {
  margin-top: 2px;
}

.message-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 20px;
}

.message-icon.system {
  background: #ecf5ff;
  color: #409eff;
}

.message-icon.order {
  background: #f0f9ff;
  color: #67c23a;
}

.message-icon.flight {
  background: #fdf6ec;
  color: #e6a23c;
}

.message-icon.alert {
  background: #fef0f0;
  color: #f56c6c;
}

.message-icon.task {
  background: #f4f4f5;
  color: #909399;
}

.message-content {
  flex: 1;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.message-title {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
}

.message-time {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.message-body {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
}

.message-footer {
  display: flex;
  gap: 10px;
}

.message-operations {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.message-detail {
  padding: 10px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
  font-size: 14px;
  color: #909399;
}

.detail-content {
  line-height: 1.8;
  color: #303133;
  margin-bottom: 20px;
}

.detail-extra {
  margin-top: 20px;
}
</style>
