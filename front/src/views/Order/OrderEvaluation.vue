<template>
  <div class="order-evaluation">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">评价与投诉管理</span>
        </div>
      </template>

      <!-- 标签页切换 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 订单评价 -->
        <el-tab-pane label="订单评价" name="evaluation">
          <!-- 搜索筛选 -->
          <el-form :inline="true" :model="evaluationSearch" class="search-form">
            <el-form-item label="订单号">
              <el-input v-model="evaluationSearch.orderNo" placeholder="订单号" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="评价等级">
              <el-select v-model="evaluationSearch.rating" placeholder="全部" clearable style="width: 120px">
                <el-option label="五星" :value="5" />
                <el-option label="四星" :value="4" />
                <el-option label="三星" :value="3" />
                <el-option label="二星" :value="2" />
                <el-option label="一星" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="评价时间">
              <el-date-picker
                v-model="evaluationSearch.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 240px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleEvaluationSearch">查询</el-button>
              <el-button @click="handleEvaluationReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 评价统计 -->
          <div class="rating-stats">
            <el-card class="stat-item">
              <div class="stat-label">平均评分</div>
              <div class="stat-value">{{ evaluationStats.avgRating }}</div>
              <el-rate v-model="evaluationStats.avgRating" disabled show-score text-color="#ff9900" />
            </el-card>
            <el-card class="stat-item">
              <div class="stat-label">总评价数</div>
              <div class="stat-value">{{ evaluationStats.total }}</div>
            </el-card>
            <el-card class="stat-item">
              <div class="stat-label">好评率</div>
              <div class="stat-value">{{ evaluationStats.goodRate }}%</div>
            </el-card>
            <el-card class="stat-item">
              <div class="stat-label">本月评价</div>
              <div class="stat-value">{{ evaluationStats.monthCount }}</div>
            </el-card>
          </div>

          <!-- 评价列表 -->
          <el-table :data="evaluationList" style="width: 100%; margin-top: 20px" v-loading="evaluationLoading">
            <el-table-column prop="orderNo" label="订单号" width="160" />
            <el-table-column prop="customer" label="客户" width="120" />
            <el-table-column prop="merchant" label="商家" width="150" />
            <el-table-column prop="rating" label="评分" width="180">
              <template #default="{ row }">
                <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" />
              </template>
            </el-table-column>
            <el-table-column label="评价维度" width="300">
              <template #default="{ row }">
                <div class="rating-details">
                  <div>时效: <el-rate v-model="row.timeRating" disabled size="small" /></div>
                  <div>服务: <el-rate v-model="row.serviceRating" disabled size="small" /></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="images" label="图片" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.images && row.images.length">{{ row.images.length }}张</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="评价时间" width="160" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewEvaluation(row)">查看</el-button>
                <el-button link type="success" @click="handleReply(row)" v-if="!row.reply">回复</el-button>
                <el-button link type="info" @click="handleViewReply(row)" v-else>查看回复</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="evaluationPagination.page"
            v-model:page-size="evaluationPagination.size"
            :total="evaluationPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleEvaluationSizeChange"
            @current-change="handleEvaluationPageChange"
            style="margin-top: 20px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- 投诉管理 -->
        <el-tab-pane label="投诉管理" name="complaint">
          <!-- 搜索筛选 -->
          <el-form :inline="true" :model="complaintSearch" class="search-form">
            <el-form-item label="投诉单号">
              <el-input v-model="complaintSearch.complaintNo" placeholder="投诉单号" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="投诉类型">
              <el-select v-model="complaintSearch.type" placeholder="全部" clearable style="width: 150px">
                <el-option label="服务态度" value="service" />
                <el-option label="延误超时" value="delay" />
                <el-option label="货物损坏" value="damage" />
                <el-option label="收费问题" value="fee" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select v-model="complaintSearch.status" placeholder="全部" clearable style="width: 120px">
                <el-option label="待处理" value="pending" />
                <el-option label="处理中" value="processing" />
                <el-option label="已完成" value="completed" />
                <el-option label="已关闭" value="closed" />
              </el-select>
            </el-form-item>
            <el-form-item label="紧急程度">
              <el-select v-model="complaintSearch.urgency" placeholder="全部" clearable style="width: 120px">
                <el-option label="紧急" value="urgent" />
                <el-option label="一般" value="normal" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleComplaintSearch">查询</el-button>
              <el-button @click="handleComplaintReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 投诉统计 -->
          <div class="complaint-stats">
            <el-card class="stat-card pending">
              <div class="stat-value">{{ complaintStats.pending }}</div>
              <div class="stat-label">待处理</div>
            </el-card>
            <el-card class="stat-card processing">
              <div class="stat-value">{{ complaintStats.processing }}</div>
              <div class="stat-label">处理中</div>
            </el-card>
            <el-card class="stat-card completed">
              <div class="stat-value">{{ complaintStats.completed }}</div>
              <div class="stat-label">已完成</div>
            </el-card>
            <el-card class="stat-card urgent">
              <div class="stat-value">{{ complaintStats.urgent }}</div>
              <div class="stat-label">紧急投诉</div>
            </el-card>
          </div>

          <!-- 投诉列表 -->
          <el-table :data="complaintList" style="width: 100%; margin-top: 20px" v-loading="complaintLoading">
            <el-table-column prop="complaintNo" label="投诉单号" width="160" />
            <el-table-column prop="orderNo" label="关联订单" width="160" />
            <el-table-column prop="customer" label="投诉人" width="120" />
            <el-table-column prop="merchant" label="被投诉商家" width="150" />
            <el-table-column prop="type" label="投诉类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getComplaintTypeTag(row.type)" size="small">
                  {{ getComplaintTypeName(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="urgency" label="紧急程度" width="100">
              <template #default="{ row }">
                <el-tag :type="row.urgency === 'urgent' ? 'danger' : 'info'" size="small">
                  {{ row.urgency === 'urgent' ? '紧急' : '一般' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="投诉内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="处理状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getComplaintStatusTag(row.status)" size="small">
                  {{ getComplaintStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="投诉时间" width="160" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewComplaint(row)">查看</el-button>
                <el-button link type="success" @click="handleProcessComplaint(row)" v-if="row.status === 'pending'">
                  处理
                </el-button>
                <el-button link type="warning" @click="handleCloseComplaint(row)" v-if="row.status !== 'closed'">
                  关闭
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="complaintPagination.page"
            v-model:page-size="complaintPagination.size"
            :total="complaintPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleComplaintSizeChange"
            @current-change="handleComplaintPageChange"
            style="margin-top: 20px; justify-content: flex-end"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 查看评价详情对话框 -->
    <el-dialog v-model="evaluationDetailVisible" title="评价详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ currentEvaluation.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ currentEvaluation.customer }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ currentEvaluation.merchant }}</el-descriptions-item>
        <el-descriptions-item label="综合评分">
          <el-rate v-model="currentEvaluation.rating" disabled show-score text-color="#ff9900" />
        </el-descriptions-item>
        <el-descriptions-item label="时效评分">
          <el-rate v-model="currentEvaluation.timeRating" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="服务评分">
          <el-rate v-model="currentEvaluation.serviceRating" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="评价内容">{{ currentEvaluation.content }}</el-descriptions-item>
        <el-descriptions-item label="评价时间">{{ currentEvaluation.createTime }}</el-descriptions-item>
      </el-descriptions>

      <!-- 评价图片 -->
      <div v-if="currentEvaluation.images && currentEvaluation.images.length" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">评价图片：</div>
        <el-image
          v-for="(img, index) in currentEvaluation.images"
          :key="index"
          :src="img"
          :preview-src-list="currentEvaluation.images"
          :initial-index="index"
          fit="cover"
          style="width: 100px; height: 100px; margin-right: 10px"
        />
      </div>

      <!-- 商家回复 -->
      <div v-if="currentEvaluation.reply" style="margin-top: 20px">
        <el-divider content-position="left">商家回复</el-divider>
        <el-alert :title="currentEvaluation.reply.content" type="info" :closable="false">
          <template #default>
            <div style="margin-top: 10px; font-size: 12px; color: #909399">
              回复时间：{{ currentEvaluation.reply.time }}
            </div>
          </template>
        </el-alert>
      </div>

      <template #footer>
        <el-button type="primary" @click="evaluationDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 商家回复对话框 -->
    <el-dialog v-model="replyDialogVisible" title="回复评价" width="500px">
      <el-form :model="replyForm" :rules="replyRules" ref="replyFormRef" label-width="80px">
        <el-form-item label="回复内容" prop="content">
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入回复内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReply" :loading="replying">提交</el-button>
      </template>
    </el-dialog>

    <!-- 查看投诉详情对话框 -->
    <el-dialog v-model="complaintDetailVisible" title="投诉详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="投诉单号">{{ currentComplaint.complaintNo }}</el-descriptions-item>
        <el-descriptions-item label="关联订单">{{ currentComplaint.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="投诉人">{{ currentComplaint.customer }}</el-descriptions-item>
        <el-descriptions-item label="被投诉商家">{{ currentComplaint.merchant }}</el-descriptions-item>
        <el-descriptions-item label="投诉类型">
          {{ getComplaintTypeName(currentComplaint.type) }}
        </el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="currentComplaint.urgency === 'urgent' ? 'danger' : 'info'" size="small">
            {{ currentComplaint.urgency === 'urgent' ? '紧急' : '一般' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="getComplaintStatusTag(currentComplaint.status)" size="small">
            {{ getComplaintStatusName(currentComplaint.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="投诉时间">{{ currentComplaint.createTime }}</el-descriptions-item>
        <el-descriptions-item label="投诉内容" :span="2">
          {{ currentComplaint.content }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 处理记录 -->
      <div v-if="currentComplaint.processRecords && currentComplaint.processRecords.length" style="margin-top: 20px">
        <el-divider content-position="left">处理记录</el-divider>
        <el-timeline>
          <el-timeline-item
            v-for="record in currentComplaint.processRecords"
            :key="record.id"
            :timestamp="record.time"
            placement="top"
          >
            <el-card>
              <div><strong>{{ record.operator }}</strong> - {{ record.action }}</div>
              <div style="margin-top: 5px; color: #606266">{{ record.content }}</div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <template #footer>
        <el-button type="primary" @click="complaintDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 处理投诉对话框 -->
    <el-dialog v-model="processDialogVisible" title="处理投诉" width="500px">
      <el-form :model="processForm" :rules="processRules" ref="processFormRef" label-width="100px">
        <el-form-item label="处理方式" prop="action">
          <el-select v-model="processForm.action" placeholder="请选择处理方式" style="width: 100%">
            <el-option label="商家回复" value="reply" />
            <el-option label="协商解决" value="negotiate" />
            <el-option label="平台介入" value="intervene" />
            <el-option label="退款处理" value="refund" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明" prop="content">
          <el-input
            v-model="processForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入处理说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitProcess" :loading="processing">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 当前标签页
const activeTab = ref('evaluation')

// ========== 评价相关 ==========
const evaluationSearch = reactive({
  orderNo: '',
  rating: null,
  dateRange: []
})

const evaluationStats = reactive({
  avgRating: 4.8,
  total: 1256,
  goodRate: 96.5,
  monthCount: 342
})

const evaluationList = ref([])
const evaluationLoading = ref(false)
const evaluationPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const evaluationDetailVisible = ref(false)
const currentEvaluation = ref({})

const replyDialogVisible = ref(false)
const replyForm = reactive({
  evaluationId: null,
  content: ''
})
const replyRules = {
  content: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}
const replyFormRef = ref(null)
const replying = ref(false)

// ========== 投诉相关 ==========
const complaintSearch = reactive({
  complaintNo: '',
  type: '',
  status: '',
  urgency: ''
})

const complaintStats = reactive({
  pending: 8,
  processing: 15,
  completed: 234,
  urgent: 3
})

const complaintList = ref([])
const complaintLoading = ref(false)
const complaintPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const complaintDetailVisible = ref(false)
const currentComplaint = ref({})

const processDialogVisible = ref(false)
const processForm = reactive({
  complaintId: null,
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
  fetchEvaluationList()
})

// 标签页切换
const handleTabChange = (tab) => {
  if (tab === 'evaluation') {
    fetchEvaluationList()
  } else {
    fetchComplaintList()
  }
}

// ========== 评价相关方法 ==========
const fetchEvaluationList = () => {
  evaluationLoading.value = true

  // TODO: 调用实际API
  setTimeout(() => {
    evaluationList.value = [
      {
        id: 1,
        orderNo: 'ORD-2024091700001',
        customer: '张三',
        merchant: '天翔物流',
        rating: 5,
        timeRating: 5,
        serviceRating: 5,
        content: '服务非常好，配送及时，飞行器很稳定，强烈推荐！',
        images: [],
        createTime: '2026-09-17 14:30:25',
        reply: null
      },
      {
        id: 2,
        orderNo: 'ORD-2024091700002',
        customer: '李四',
        merchant: '顺丰速运',
        rating: 4,
        timeRating: 4,
        serviceRating: 5,
        content: '整体不错，就是稍微晚了一点点，但服务态度很好。',
        images: [],
        createTime: '2026-09-17 13:15:10',
        reply: {
          content: '感谢您的反馈，我们会继续努力提升服务质量！',
          time: '2026-09-17 15:20:00'
        }
      },
      {
        id: 3,
        orderNo: 'ORD-2024091700003',
        customer: '王五',
        merchant: '京东物流',
        rating: 3,
        timeRating: 3,
        serviceRating: 3,
        content: '一般般，时间有点久，期待改进。',
        images: [],
        createTime: '2026-09-17 12:45:33',
        reply: null
      }
    ]
    evaluationPagination.total = 156
    evaluationLoading.value = false
  }, 500)
}

const handleEvaluationSearch = () => {
  evaluationPagination.page = 1
  fetchEvaluationList()
}

const handleEvaluationReset = () => {
  evaluationSearch.orderNo = ''
  evaluationSearch.rating = null
  evaluationSearch.dateRange = []
  handleEvaluationSearch()
}

const handleViewEvaluation = (row) => {
  currentEvaluation.value = { ...row }
  evaluationDetailVisible.value = true
}

const handleReply = (row) => {
  replyForm.evaluationId = row.id
  replyForm.content = ''
  replyDialogVisible.value = true
}

const handleViewReply = (row) => {
  currentEvaluation.value = { ...row }
  evaluationDetailVisible.value = true
}

const handleSubmitReply = () => {
  replyFormRef.value.validate((valid) => {
    if (valid) {
      replying.value = true
      // TODO: 调用API提交回复
      setTimeout(() => {
        ElMessage.success('回复成功')
        replyDialogVisible.value = false
        replying.value = false
        fetchEvaluationList()
      }, 1000)
    }
  })
}

const handleEvaluationSizeChange = (size) => {
  evaluationPagination.size = size
  fetchEvaluationList()
}

const handleEvaluationPageChange = (page) => {
  evaluationPagination.page = page
  fetchEvaluationList()
}

// ========== 投诉相关方法 ==========
const fetchComplaintList = () => {
  complaintLoading.value = true

  // TODO: 调用实际API
  setTimeout(() => {
    complaintList.value = [
      {
        id: 1,
        complaintNo: 'CPT-2024091700001',
        orderNo: 'ORD-2024091700056',
        customer: '赵六',
        merchant: '天翔物流',
        type: 'delay',
        urgency: 'urgent',
        content: '订单严重延误，已经超过约定时间2小时，影响重要会议',
        status: 'pending',
        createTime: '2026-09-17 14:25:12',
        processRecords: []
      },
      {
        id: 2,
        complaintNo: 'CPT-2024091700002',
        orderNo: 'ORD-2024091700045',
        customer: '孙七',
        merchant: '顺丰速运',
        type: 'damage',
        urgency: 'normal',
        content: '货物有轻微破损，包装不够牢固',
        status: 'processing',
        createTime: '2026-09-17 13:10:33',
        processRecords: [
          {
            id: 1,
            operator: '客服人员',
            action: '已联系商家',
            content: '商家正在核实情况',
            time: '2026-09-17 13:30:00'
          }
        ]
      },
      {
        id: 3,
        complaintNo: 'CPT-2024091700003',
        orderNo: 'ORD-2024091700038',
        customer: '周八',
        merchant: '京东物流',
        type: 'service',
        urgency: 'normal',
        content: '机长态度不好，沟通不畅',
        status: 'completed',
        createTime: '2026-09-17 11:45:20',
        processRecords: [
          {
            id: 1,
            operator: '客服人员',
            action: '商家回复',
            content: '已对机长进行批评教育，向客户致歉',
            time: '2026-09-17 12:00:00'
          },
          {
            id: 2,
            operator: '平台管理员',
            action: '投诉处理完成',
            content: '客户表示满意，投诉已解决',
            time: '2026-09-17 12:30:00'
          }
        ]
      }
    ]
    complaintPagination.total = 89
    complaintLoading.value = false
  }, 500)
}

const handleComplaintSearch = () => {
  complaintPagination.page = 1
  fetchComplaintList()
}

const handleComplaintReset = () => {
  complaintSearch.complaintNo = ''
  complaintSearch.type = ''
  complaintSearch.status = ''
  complaintSearch.urgency = ''
  handleComplaintSearch()
}

const handleViewComplaint = (row) => {
  currentComplaint.value = { ...row }
  complaintDetailVisible.value = true
}

const handleProcessComplaint = (row) => {
  processForm.complaintId = row.id
  processForm.action = ''
  processForm.content = ''
  processDialogVisible.value = true
}

const handleSubmitProcess = () => {
  processFormRef.value.validate((valid) => {
    if (valid) {
      processing.value = true
      // TODO: 调用API提交处理
      setTimeout(() => {
        ElMessage.success('处理成功')
        processDialogVisible.value = false
        processing.value = false
        fetchComplaintList()
      }, 1000)
    }
  })
}

const handleCloseComplaint = (row) => {
  ElMessageBox.confirm('确定要关闭该投诉吗？', '关闭确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用API关闭投诉
    ElMessage.success('投诉已关闭')
    fetchComplaintList()
  })
}

const handleComplaintSizeChange = (size) => {
  complaintPagination.size = size
  fetchComplaintList()
}

const handleComplaintPageChange = (page) => {
  complaintPagination.page = page
  fetchComplaintList()
}

// 辅助函数
const getComplaintTypeName = (type) => {
  const typeMap = {
    service: '服务态度',
    delay: '延误超时',
    damage: '货物损坏',
    fee: '收费问题',
    other: '其他'
  }
  return typeMap[type] || type
}

const getComplaintTypeTag = (type) => {
  const tagMap = {
    service: 'warning',
    delay: 'danger',
    damage: 'danger',
    fee: 'warning',
    other: 'info'
  }
  return tagMap[type] || ''
}

const getComplaintStatusName = (status) => {
  const statusMap = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    closed: '已关闭'
  }
  return statusMap[status] || status
}

const getComplaintStatusTag = (status) => {
  const tagMap = {
    pending: 'warning',
    processing: 'primary',
    completed: 'success',
    closed: 'info'
  }
  return tagMap[status] || ''
}
</script>

<style scoped>
.order-evaluation {
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

.rating-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #409eff;
}

.rating-details {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.rating-details > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.complaint-stats {
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

.stat-card.processing {
  border-left: 4px solid #409eff;
}

.stat-card.completed {
  border-left: 4px solid #67c23a;
}

.stat-card.urgent {
  border-left: 4px solid #f56c6c;
}
</style>
