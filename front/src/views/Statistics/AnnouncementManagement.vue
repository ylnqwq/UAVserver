<template>
  <div class="announcement-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">信息发布管理</span>
          <el-button type="primary" @click="handleCreateAnnouncement">
            <el-icon><Plus /></el-icon>
            发布信息
          </el-button>
        </div>
      </template>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 公告管理 -->
        <el-tab-pane label="系统公告" name="announcement">
          <!-- 搜索筛选 -->
          <el-form :inline="true" :model="announcementSearch" class="search-form">
            <el-form-item label="公告类型">
              <el-select v-model="announcementSearch.type" placeholder="全部" clearable style="width: 150px">
                <el-option label="系统通知" value="system" />
                <el-option label="功能更新" value="update" />
                <el-option label="维护公告" value="maintenance" />
                <el-option label="活动公告" value="activity" />
              </el-select>
            </el-form-item>
            <el-form-item label="发布状态">
              <el-select v-model="announcementSearch.status" placeholder="全部" clearable style="width: 120px">
                <el-option label="已发布" value="published" />
                <el-option label="草稿" value="draft" />
                <el-option label="已下线" value="offline" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="announcementSearch.keyword" placeholder="标题或内容" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAnnouncementSearch">查询</el-button>
              <el-button @click="handleAnnouncementReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 公告列表 -->
          <el-table :data="announcementList" style="width: 100%; margin-top: 20px" v-loading="announcementLoading">
            <el-table-column prop="title" label="公告标题" min-width="250" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getAnnouncementTypeTag(row.type)" size="small">
                  {{ getAnnouncementTypeName(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="100">
              <template #default="{ row }">
                <el-tag :type="getPriorityTag(row.priority)" size="small">
                  {{ getPriorityName(row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="targetRole" label="目标用户" width="150">
              <template #default="{ row }">
                <el-tag v-for="role in row.targetRoles" :key="role" size="small" style="margin-right: 5px">
                  {{ role }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="阅读量" width="100" />
            <el-table-column prop="publishTime" label="发布时间" width="160" />
            <el-table-column prop="expireTime" label="过期时间" width="160" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)" size="small">
                  {{ getStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewAnnouncement(row)">查看</el-button>
                <el-button link type="warning" @click="handleEditAnnouncement(row)">编辑</el-button>
                <el-button link type="success" @click="handlePublish(row)" v-if="row.status === 'draft'">
                  发布
                </el-button>
                <el-button link type="info" @click="handleOffline(row)" v-if="row.status === 'published'">
                  下线
                </el-button>
                <el-button link type="danger" @click="handleDeleteAnnouncement(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="announcementPagination.page"
            v-model:page-size="announcementPagination.size"
            :total="announcementPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleAnnouncementSizeChange"
            @current-change="handleAnnouncementPageChange"
            style="margin-top: 20px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- 服务需求 -->
        <el-tab-pane label="服务需求" name="service">
          <!-- 搜索筛选 -->
          <el-form :inline="true" :model="serviceSearch" class="search-form">
            <el-form-item label="需求类型">
              <el-select v-model="serviceSearch.type" placeholder="全部" clearable style="width: 150px">
                <el-option label="物流配送" value="logistics" />
                <el-option label="应急救援" value="emergency" />
                <el-option label="航拍服务" value="aerial" />
                <el-option label="巡检服务" value="inspection" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select v-model="serviceSearch.status" placeholder="全部" clearable style="width: 120px">
                <el-option label="待处理" value="pending" />
                <el-option label="处理中" value="processing" />
                <el-option label="已完成" value="completed" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleServiceSearch">查询</el-button>
              <el-button @click="handleServiceReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 需求列表 -->
          <el-table :data="serviceList" style="width: 100%; margin-top: 20px" v-loading="serviceLoading">
            <el-table-column prop="requestNo" label="需求编号" width="160" />
            <el-table-column prop="title" label="需求标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getServiceTypeTag(row.type)" size="small">
                  {{ getServiceTypeName(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitter" label="提交人" width="120" />
            <el-table-column prop="contact" label="联系方式" width="150" />
            <el-table-column prop="submitTime" label="提交时间" width="160" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)" size="small">
                  {{ getStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewService(row)">查看</el-button>
                <el-button link type="success" @click="handleProcessService(row)" v-if="row.status === 'pending'">
                  处理
                </el-button>
                <el-button link type="info" @click="handleCompleteService(row)" v-if="row.status === 'processing'">
                  完成
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="servicePagination.page"
            v-model:page-size="servicePagination.size"
            :total="servicePagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleServiceSizeChange"
            @current-change="handleServicePageChange"
            style="margin-top: 20px; justify-content: flex-end"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新建/编辑公告对话框 -->
    <el-dialog
      v-model="announcementDialogVisible"
      :title="announcementDialogTitle"
      width="800px"
      @close="handleAnnouncementDialogClose"
    >
      <el-form :model="announcementForm" :rules="announcementRules" ref="announcementFormRef" label-width="120px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="公告类型" prop="type">
          <el-select v-model="announcementForm.type" placeholder="请选择公告类型" style="width: 100%">
            <el-option label="系统通知" value="system" />
            <el-option label="功能更新" value="update" />
            <el-option label="维护公告" value="maintenance" />
            <el-option label="活动公告" value="activity" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="announcementForm.priority">
            <el-radio value="high">高</el-radio>
            <el-radio value="normal">普通</el-radio>
            <el-radio value="low">低</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="目标用户" prop="targetRoles">
          <el-checkbox-group v-model="announcementForm.targetRoles">
            <el-checkbox label="全部用户" value="all" />
            <el-checkbox label="管理员" value="admin" />
            <el-checkbox label="商家" value="merchant" />
            <el-checkbox label="客户" value="customer" />
            <el-checkbox label="空管" value="aircontrol" />
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="过期时间" prop="expireTime">
          <el-date-picker
            v-model="announcementForm.expireTime"
            type="datetime"
            placeholder="选择过期时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="是否置顶">
          <el-switch v-model="announcementForm.pinned" />
        </el-form-item>

        <el-form-item label="立即发布">
          <el-switch v-model="announcementForm.publishNow" />
          <span style="margin-left: 10px; font-size: 12px; color: #909399">
            关闭则保存为草稿
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitAnnouncement" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看公告对话框 -->
    <el-dialog v-model="viewAnnouncementVisible" title="公告详情" width="700px">
      <div class="announcement-detail">
        <h2>{{ viewAnnouncementData.title }}</h2>
        <div class="announcement-meta">
          <el-tag :type="getAnnouncementTypeTag(viewAnnouncementData.type)" size="small">
            {{ getAnnouncementTypeName(viewAnnouncementData.type) }}
          </el-tag>
          <el-tag :type="getPriorityTag(viewAnnouncementData.priority)" size="small">
            {{ getPriorityName(viewAnnouncementData.priority) }}
          </el-tag>
          <span>发布时间: {{ viewAnnouncementData.publishTime }}</span>
          <span>阅读量: {{ viewAnnouncementData.viewCount }}</span>
        </div>
        <el-divider />
        <div class="announcement-content" v-html="viewAnnouncementData.content"></div>
      </div>
      <template #footer>
        <el-button type="primary" @click="viewAnnouncementVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 查看服务需求对话框 -->
    <el-dialog v-model="viewServiceVisible" title="需求详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="需求编号">{{ viewServiceData.requestNo }}</el-descriptions-item>
        <el-descriptions-item label="需求类型">
          <el-tag :type="getServiceTypeTag(viewServiceData.type)" size="small">
            {{ getServiceTypeName(viewServiceData.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="需求标题" :span="2">{{ viewServiceData.title }}</el-descriptions-item>
        <el-descriptions-item label="提交人">{{ viewServiceData.submitter }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ viewServiceData.contact }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ viewServiceData.submitTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTag(viewServiceData.status)" size="small">
            {{ getStatusName(viewServiceData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="需求描述" :span="2">
          {{ viewServiceData.description }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 处理记录 -->
      <div v-if="viewServiceData.processRecords && viewServiceData.processRecords.length" style="margin-top: 20px">
        <el-divider content-position="left">处理记录</el-divider>
        <el-timeline>
          <el-timeline-item
            v-for="record in viewServiceData.processRecords"
            :key="record.id"
            :timestamp="record.time"
            placement="top"
          >
            <div><strong>{{ record.operator }}</strong> - {{ record.action }}</div>
            <div style="color: #606266; margin-top: 5px">{{ record.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <template #footer>
        <el-button type="primary" @click="viewServiceVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 处理服务需求对话框 -->
    <el-dialog v-model="processServiceVisible" title="处理需求" width="500px">
      <el-form :model="processForm" :rules="processRules" ref="processFormRef" label-width="100px">
        <el-form-item label="处理方式" prop="action">
          <el-select v-model="processForm.action" placeholder="请选择处理方式" style="width: 100%">
            <el-option label="安排对接" value="contact" />
            <el-option label="转发相关部门" value="forward" />
            <el-option label="暂不处理" value="postpone" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明" prop="content">
          <el-input
            v-model="processForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入处理说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processServiceVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitProcess" :loading="processing">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

// 当前标签页
const activeTab = ref('announcement')

// ========== 公告相关 ==========
const announcementSearch = reactive({
  type: '',
  status: '',
  keyword: ''
})

const announcementList = ref([])
const announcementLoading = ref(false)
const announcementPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const announcementDialogVisible = ref(false)
const announcementDialogTitle = ref('发布公告')
const announcementForm = reactive({
  id: null,
  title: '',
  type: '',
  priority: 'normal',
  targetRoles: ['all'],
  content: '',
  expireTime: '',
  pinned: false,
  publishNow: true
})

const announcementRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const announcementFormRef = ref(null)
const submitting = ref(false)

const viewAnnouncementVisible = ref(false)
const viewAnnouncementData = ref({})

// ========== 服务需求相关 ==========
const serviceSearch = reactive({
  type: '',
  status: ''
})

const serviceList = ref([])
const serviceLoading = ref(false)
const servicePagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const viewServiceVisible = ref(false)
const viewServiceData = ref({})

const processServiceVisible = ref(false)
const processForm = reactive({
  serviceId: null,
  action: '',
  content: ''
})
const processRules = {
  action: [{ required: true, message: '请选择处理方式', trigger: 'change' }],
  content: [{ required: true, message: '请输入处理说明', trigger: 'blur' }]
}
const processFormRef = ref(null)
const processing = ref(false)

// 初始化
onMounted(() => {
  fetchAnnouncementList()
})

// 标签页切换
const handleTabChange = (tab) => {
  if (tab === 'announcement') {
    fetchAnnouncementList()
  } else {
    fetchServiceList()
  }
}

// ========== 公告相关方法 ==========
const fetchAnnouncementList = () => {
  announcementLoading.value = true

  setTimeout(() => {
    announcementList.value = [
      {
        id: 1,
        title: '系统升级维护通知',
        type: 'maintenance',
        priority: 'high',
        targetRoles: ['全部用户'],
        content: '为提升系统性能，将于2026年9月20日凌晨2:00-6:00进行系统升级维护，届时将无法访问系统，请提前做好安排。',
        viewCount: 1256,
        publishTime: '2026-09-17 10:00:00',
        expireTime: '2026-09-20 23:59:59',
        status: 'published'
      },
      {
        id: 2,
        title: '新功能上线：智能调度系统',
        type: 'update',
        priority: 'normal',
        targetRoles: ['商家', '管理员'],
        content: '平台新增智能调度功能，可根据订单需求、资源状态等自动生成最优调度方案。',
        viewCount: 856,
        publishTime: '2026-09-16 14:30:00',
        expireTime: '2026-10-16 23:59:59',
        status: 'published'
      },
      {
        id: 3,
        title: '中秋节假期服务安排',
        type: 'activity',
        priority: 'normal',
        targetRoles: ['全部用户'],
        content: '中秋节期间（9月29日-10月1日）客服工作时间调整为9:00-18:00。',
        viewCount: 0,
        publishTime: null,
        expireTime: '2026-10-01 23:59:59',
        status: 'draft'
      }
    ]
    announcementPagination.total = 23
    announcementLoading.value = false
  }, 500)
}

const handleAnnouncementSearch = () => {
  announcementPagination.page = 1
  fetchAnnouncementList()
}

const handleAnnouncementReset = () => {
  announcementSearch.type = ''
  announcementSearch.status = ''
  announcementSearch.keyword = ''
  handleAnnouncementSearch()
}

const handleCreateAnnouncement = () => {
  announcementDialogTitle.value = '发布公告'
  announcementDialogVisible.value = true
}

const handleEditAnnouncement = (row) => {
  announcementDialogTitle.value = '编辑公告'
  Object.assign(announcementForm, row)
  announcementDialogVisible.value = true
}

const handleViewAnnouncement = (row) => {
  viewAnnouncementData.value = { ...row }
  viewAnnouncementVisible.value = true
}

const handlePublish = (row) => {
  ElMessageBox.confirm('确定要发布该公告吗？', '发布确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    ElMessage.success('公告已发布')
    fetchAnnouncementList()
  })
}

const handleOffline = (row) => {
  ElMessageBox.confirm('确定要下线该公告吗？', '下线确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('公告已下线')
    fetchAnnouncementList()
  })
}

const handleDeleteAnnouncement = (row) => {
  ElMessageBox.confirm('确定要删除该公告吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    fetchAnnouncementList()
  })
}

const handleSubmitAnnouncement = () => {
  announcementFormRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      setTimeout(() => {
        ElMessage.success(announcementForm.publishNow ? '公告已发布' : '公告已保存为草稿')
        announcementDialogVisible.value = false
        submitting.value = false
        fetchAnnouncementList()
      }, 1000)
    }
  })
}

const handleAnnouncementDialogClose = () => {
  announcementFormRef.value?.resetFields()
  Object.assign(announcementForm, {
    id: null,
    title: '',
    type: '',
    priority: 'normal',
    targetRoles: ['all'],
    content: '',
    expireTime: '',
    pinned: false,
    publishNow: true
  })
}

const handleAnnouncementSizeChange = (size) => {
  announcementPagination.size = size
  fetchAnnouncementList()
}

const handleAnnouncementPageChange = (page) => {
  announcementPagination.page = page
  fetchAnnouncementList()
}

// ========== 服务需求相关方法 ==========
const fetchServiceList = () => {
  serviceLoading.value = true

  setTimeout(() => {
    serviceList.value = [
      {
        id: 1,
        requestNo: 'SRQ-2024091700001',
        title: '大型活动航拍需求',
        type: 'aerial',
        submitter: '北京文化公司',
        contact: '13800138000',
        description: '需要在10月1日进行活动航拍，预计3小时。',
        submitTime: '2026-09-17 14:25:00',
        status: 'pending',
        processRecords: []
      },
      {
        id: 2,
        requestNo: 'SRQ-2024091700002',
        title: '紧急医疗物资配送',
        type: 'emergency',
        submitter: '北京医院',
        contact: '13900139000',
        description: '需要紧急配送医疗物资到郊区医院。',
        submitTime: '2026-09-17 12:10:00',
        status: 'processing',
        processRecords: [
          {
            id: 1,
            operator: '客服人员',
            action: '已安排对接',
            content: '已联系天翔物流处理',
            time: '2026-09-17 12:20:00'
          }
        ]
      }
    ]
    servicePagination.total = 15
    serviceLoading.value = false
  }, 500)
}

const handleServiceSearch = () => {
  servicePagination.page = 1
  fetchServiceList()
}

const handleServiceReset = () => {
  serviceSearch.type = ''
  serviceSearch.status = ''
  handleServiceSearch()
}

const handleViewService = (row) => {
  viewServiceData.value = { ...row }
  viewServiceVisible.value = true
}

const handleProcessService = (row) => {
  processForm.serviceId = row.id
  processForm.action = ''
  processForm.content = ''
  processServiceVisible.value = true
}

const handleCompleteService = (row) => {
  ElMessageBox.confirm('确定要标记为已完成吗？', '完成确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    ElMessage.success('已标记为完成')
    fetchServiceList()
  })
}

const handleSubmitProcess = () => {
  processFormRef.value.validate((valid) => {
    if (valid) {
      processing.value = true
      setTimeout(() => {
        ElMessage.success('处理成功')
        processServiceVisible.value = false
        processing.value = false
        fetchServiceList()
      }, 1000)
    }
  })
}

const handleServiceSizeChange = (size) => {
  servicePagination.size = size
  fetchServiceList()
}

const handleServicePageChange = (page) => {
  servicePagination.page = page
  fetchServiceList()
}

// 辅助函数
const getAnnouncementTypeName = (type) => {
  const map = {
    system: '系统通知',
    update: '功能更新',
    maintenance: '维护公告',
    activity: '活动公告'
  }
  return map[type] || type
}

const getAnnouncementTypeTag = (type) => {
  const map = {
    system: 'primary',
    update: 'success',
    maintenance: 'warning',
    activity: 'info'
  }
  return map[type] || ''
}

const getPriorityName = (priority) => {
  const map = { high: '高', normal: '普通', low: '低' }
  return map[priority] || priority
}

const getPriorityTag = (priority) => {
  const map = { high: 'danger', normal: '', low: 'info' }
  return map[priority] || ''
}

const getStatusName = (status) => {
  const map = {
    published: '已发布',
    draft: '草稿',
    offline: '已下线',
    pending: '待处理',
    processing: '处理中',
    completed: '已完成'
  }
  return map[status] || status
}

const getStatusTag = (status) => {
  const map = {
    published: 'success',
    draft: 'info',
    offline: 'info',
    pending: 'warning',
    processing: 'primary',
    completed: 'success'
  }
  return map[status] || ''
}

const getServiceTypeName = (type) => {
  const map = {
    logistics: '物流配送',
    emergency: '应急救援',
    aerial: '航拍服务',
    inspection: '巡检服务',
    other: '其他'
  }
  return map[type] || type
}

const getServiceTypeTag = (type) => {
  const map = {
    logistics: 'primary',
    emergency: 'danger',
    aerial: 'success',
    inspection: 'warning',
    other: 'info'
  }
  return map[type] || ''
}
</script>

<style scoped>
.announcement-management {
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

.announcement-detail h2 {
  margin: 0 0 15px 0;
}

.announcement-meta {
  display: flex;
  gap: 15px;
  align-items: center;
  font-size: 14px;
  color: #909399;
}

.announcement-content {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}
</style>
