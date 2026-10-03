<template>
  <div class="order-list">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="待接单" :value="1" />
            <el-option label="待调度" :value="2" />
            <el-option label="飞行中" :value="3" />
            <el-option label="已送达" :value="4" />
            <el-option label="已完成" :value="5" />
            <el-option label="已取消" :value="6" />
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
          />
        </el-form-item>
        <el-form-item label="是否加急">
          <el-select v-model="searchForm.urgency" placeholder="请选择" clearable style="width: 150px">
            <el-option label="加急" :value="1" />
            <el-option label="普通" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleCreate" v-if="userRole === 'CUSTOMER'">
          <el-icon><Plus /></el-icon>
          创建订单
        </el-button>
        <el-button type="success" @click="handleBatchAccept" v-if="canOperate()" :disabled="selectedIds.length === 0">
          <el-icon><Select /></el-icon>
          批量接单
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 订单列表 -->
      <el-table
        :data="tableData"
        @selection-change="handleSelectionChange"
        v-loading="loading"
        border
        stripe
      >
        <el-table-column type="selection" width="55" v-if="canOperate()" />
        <el-table-column prop="orderNo" label="订单号" width="150" fixed="left" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="urgencyFlag" label="优先级" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.urgencyFlag === 1" type="danger" size="small">加急</el-tag>
            <el-tag v-else type="info" size="small">普通</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sendAddr" label="寄件地址" width="200" show-overflow-tooltip />
        <el-table-column prop="recvAddr" label="收件地址" width="200" show-overflow-tooltip />
        <el-table-column prop="itemType" label="物品类型" width="120" />
        <el-table-column prop="weight" label="重量(kg)" width="100" />
        <el-table-column prop="customerName" label="客户" width="120" v-if="userRole !== 'CUSTOMER'" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button type="success" link @click="handleTrack(row)">
              <el-icon><Position /></el-icon>
              跟踪
            </el-button>
            <el-button
              v-if="canOperate() && row.status === 1"
              type="warning"
              link
              @click="handleAccept(row)"
            >
              接单
            </el-button>
            <el-button
              v-if="canOperate() && row.status === 2"
              type="primary"
              link
              @click="handleDispatch(row)"
            >
              调度
            </el-button>
            <el-button
              v-if="row.status < 3"
              type="danger"
              link
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="800px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusName(currentOrder.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户">{{ currentOrder.customerName }}</el-descriptions-item>
        <el-descriptions-item label="接单人">{{ currentOrder.acceptedBy || '待接单' }}</el-descriptions-item>
        <el-descriptions-item label="寄件地址" :span="2">{{ currentOrder.sendAddr }}</el-descriptions-item>
        <el-descriptions-item label="收件地址" :span="2">{{ currentOrder.recvAddr }}</el-descriptions-item>
        <el-descriptions-item label="物品类型">{{ currentOrder.itemType }}</el-descriptions-item>
        <el-descriptions-item label="重量">{{ currentOrder.weight }} kg</el-descriptions-item>
        <el-descriptions-item label="体积">{{ currentOrder.volume || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否加急">
          <el-tag v-if="currentOrder.urgencyFlag === 1" type="danger" size="small">加急</el-tag>
          <el-tag v-else type="info" size="small">普通</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentOrder.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 接单对话框 -->
    <el-dialog v-model="acceptVisible" title="接单确认" width="500px">
      <el-form :model="acceptForm" label-width="100px">
        <el-form-item label="订单号">
          <el-input v-model="acceptForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="预计配送时间">
          <el-date-picker
            v-model="acceptForm.estimatedTime"
            type="datetime"
            placeholder="选择时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="acceptForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAccept">确认接单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { orderAPI } from '@/api'

/**
 * 订单列表：数据来自后端 /api/orders
 * 客户只看自己的订单，管理员/运营/空管看全部。
 * 页面上用的 status(1-6) 与 urgencyFlag(0/1) 是展示用的编码，由后端状态映射得到。
 */

const router = useRouter()
const userStore = useUserStore()

const userRole = computed(() => userStore.userInfo?.role)

// 平台自营：接单/调度由管理员与运营人员操作
const canOperate = () => ['ADMIN', 'OPERATOR'].includes(userRole.value)
const loading = ref(false)
const allOrders = ref([])
const selectedIds = ref([])

const searchForm = reactive({
  orderNo: '',
  status: null,
  dateRange: [],
  urgency: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const detailVisible = ref(false)
const currentOrder = ref(null)

const acceptVisible = ref(false)
const acceptForm = reactive({
  orderId: null,
  orderNo: '',
  estimatedTime: '',
  remark: ''
})

// 后端状态 → 页面展示编码
const STATUS_CODE = {
  PENDING: 1, // 待接单
  ACCEPTED: 2, // 待调度
  DISPATCHED: 2,
  IN_PROGRESS: 3, // 飞行中
  ARRIVED: 4, // 已送达
  COMPLETED: 5, // 已完成
  CANCELLED: 6 // 已取消
}

const decorate = (order) => ({
  ...order,
  status: STATUS_CODE[order.status] ?? 1,
  statusText: order.status,
  urgencyFlag: order.urgencyFlag ? 1 : 0
})

const filtered = computed(() =>
  allOrders.value.filter((o) => {
    const noOk = !searchForm.orderNo || (o.orderNo || '').includes(searchForm.orderNo.trim())
    const statusOk = searchForm.status == null || o.status === searchForm.status
    const urgencyOk = searchForm.urgency == null || o.urgencyFlag === searchForm.urgency
    let dateOk = true
    if (searchForm.dateRange && searchForm.dateRange.length === 2 && o.createTime) {
      const created = (o.createTime || '').slice(0, 10)
      dateOk = created >= searchForm.dateRange[0] && created <= searchForm.dateRange[1]
    }
    return noOk && statusOk && urgencyOk && dateOk
  })
)

const tableData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filtered.value.slice(start, start + pagination.size)
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const userId = userStore.userInfo?.userId
    let data
    if (userRole.value === 'CUSTOMER' && userId) {
      data = await orderAPI.getByCustomer(userId)
    } else {
      data = await orderAPI.getList()
    }
    allOrders.value = (data || []).map(decorate)
    pagination.total = filtered.value.length
  } catch (error) {
    console.error('加载订单列表失败:', error)
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  pagination.total = filtered.value.length
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    orderNo: '',
    status: null,
    dateRange: [],
    urgency: null
  })
  handleSearch()
}

// 刷新
const refreshData = () => {
  loadData()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map((item) => item.id)
}

// 创建订单
const handleCreate = () => {
  router.push('/order/create')
}

// 查看详情
const handleView = (row) => {
  currentOrder.value = row
  detailVisible.value = true
}

// 跟踪订单
const handleTrack = (row) => {
  router.push(`/order/tracking/${row.id}`)
}

// 接单
const handleAccept = (row) => {
  acceptForm.orderId = row.id
  acceptForm.orderNo = row.orderNo
  acceptForm.estimatedTime = ''
  acceptForm.remark = ''
  acceptVisible.value = true
}

// 确认接单
const confirmAccept = async () => {
  try {
    await orderAPI.accept(acceptForm.orderId)
    if (acceptForm.estimatedTime) {
      await orderAPI.update(acceptForm.orderId, { scheduledTime: acceptForm.estimatedTime })
    }
    ElMessage.success('接单成功')
    acceptVisible.value = false
    loadData()
  } catch (error) {
    console.error('接单失败:', error)
  }
}

// 批量接单
const handleBatchAccept = async () => {
  try {
    await ElMessageBox.confirm(`确定要接收选中的 ${selectedIds.value.length} 个订单吗？`, '提示', {
      type: 'warning'
    })
    await Promise.all(selectedIds.value.map((id) => orderAPI.accept(id)))
    ElMessage.success('批量接单成功')
    selectedIds.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量接单失败:', error)
    }
  }
}

// 调度
const handleDispatch = (row) => {
  router.push(`/order/dispatch?id=${row.id}`)
}

// 取消订单
const handleCancel = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '取消原因不能为空'
    })
    await orderAPI.update(row.id, { notes: `取消原因：${value}` })
    await orderAPI.cancel(row.id)
    ElMessage.success('订单已取消')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
    }
  }
}

// 导出当前筛选结果
const handleExport = () => {
  const header = ['订单号', '状态', '优先级', '寄件地址', '收件地址', '物品类型', '重量(kg)', '客户', '创建时间']
  const rows = filtered.value.map((o) => [
    o.orderNo,
    getStatusName(o.status),
    o.urgencyFlag ? '加急' : '普通',
    o.sendAddr || '',
    o.recvAddr || '',
    o.itemType || '',
    o.weight ?? '',
    o.customerName || '',
    o.createTime || ''
  ])
  const csv = [header, ...rows].map((r) => r.map((v) => `"${v ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `订单列表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出当前筛选结果')
}

// 工具函数
const getStatusType = (status) => {
  const types = {
    1: 'info',
    2: 'warning',
    3: 'primary',
    4: 'success',
    5: 'success',
    6: 'danger'
  }
  return types[status] || ''
}

const getStatusName = (status) => {
  const names = {
    1: '待接单',
    2: '待调度',
    3: '飞行中',
    4: '已送达',
    5: '已完成',
    6: '已取消'
  }
  return names[status] || ''
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.order-list {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.toolbar {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
