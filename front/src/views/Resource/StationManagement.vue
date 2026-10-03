<template>
  <div class="station-management">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="场站名称">
          <el-input v-model="searchForm.name" placeholder="请输入场站名称" clearable />
        </el-form-item>
        <el-form-item label="场站类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" clearable style="width: 150px">
            <el-option label="起降场" value="landing" />
            <el-option label="换电站" value="charging" />
            <el-option label="维修站" value="maintenance" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="空闲" value="idle" />
            <el-option label="使用中" value="occupied" />
            <el-option label="维护中" value="maintenance" />
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
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增场站
        </el-button>
        <el-button @click="handleMapView">
          <el-icon><MapLocation /></el-icon>
          地图查看
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

      <!-- 场站列表 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
      >
        <el-table-column type="index" width="55" />
        <el-table-column prop="name" label="场站名称" width="180" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeColor(row.type)">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="capacity" label="容量" width="80" />
        <el-table-column prop="available" label="可用" width="80">
          <template #default="{ row }">
            <span :style="{ color: row.available > 0 ? '#67c23a' : '#f56c6c' }">
              {{ row.available }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="todayUsage" label="今日使用" width="100" />
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系方式" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button type="warning" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="filtered.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="场站名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入场站名称" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="场站类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
                <el-option label="起降场" value="landing" />
                <el-option label="换电站" value="charging" />
                <el-option label="维修站" value="maintenance" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input v-model.number="form.latitude" type="number" placeholder="如 27.9165" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input v-model.number="form.longitude" type="number" placeholder="如 120.6890" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人" prop="manager">
              <el-input v-model="form.manager" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系方式" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系方式" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="运营时间">
          <el-time-picker
            v-model="form.operationTime"
            is-range
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="设施说明">
          <el-input
            v-model="form.facilities"
            type="textarea"
            :rows="3"
            placeholder="请描述场站设施情况"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="其他说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 场站详情对话框 -->
    <el-dialog v-model="detailVisible" title="场站详情" width="900px">
      <el-row :gutter="20" v-if="currentStation">
        <el-col :span="14">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="场站名称" :span="2">{{ currentStation.name }}</el-descriptions-item>
            <el-descriptions-item label="类型">
              <el-tag :type="getTypeColor(currentStation.type)">{{ getTypeName(currentStation.type) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusColor(currentStation.status)">{{ getStatusName(currentStation.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="地址" :span="2">{{ currentStation.address }}</el-descriptions-item>
            <el-descriptions-item label="容量">{{ currentStation.capacity }}</el-descriptions-item>
            <el-descriptions-item label="可用">{{ currentStation.available }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ currentStation.manager }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ currentStation.phone }}</el-descriptions-item>
            <el-descriptions-item label="运营时间" :span="2">
              {{ currentStation.operationTime || '全天' }}
            </el-descriptions-item>
            <el-descriptions-item label="设施说明" :span="2">
              {{ currentStation.facilities || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider>使用统计</el-divider>
          <div ref="usageChartRef" style="height: 250px"></div>
        </el-col>

        <el-col :span="10">
          <el-card shadow="hover">
            <template #header>
              <span>运营信息</span>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="负责人">{{ currentStation.manager || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系方式">{{ currentStation.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="运营时间">{{ currentStation.operationTime || '全天' }}</el-descriptions-item>
              <el-descriptions-item label="今日使用">{{ currentStation.todayUsage || 0 }} 次</el-descriptions-item>
              <el-descriptions-item label="备注">{{ currentStation.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { airspaceAPI } from '@/api'

/**
 * 场站管理页面：数据全部来自后端 /api/airspace/stations
 *   列表     GET    /api/airspace/stations/all  （含维护中/停用）
 *   新增     POST   /api/airspace/stations
 *   修改     PUT    /api/airspace/stations/{id}
 *   删除     DELETE /api/airspace/stations/{id}
 */

const router = useRouter()

const loading = ref(false)
const allStations = ref([])

const searchForm = reactive({
  name: '',
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增场站')
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  type: '',
  latitude: null,
  longitude: null,
  capacity: 10,
  currentUsage: 0,
  todayUsage: 0,
  address: '',
  manager: '',
  phone: '',
  operationTime: [],
  facilities: '',
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入场站名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择场站类型', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }],
  latitude: [{ required: true, message: '请输入纬度', trigger: 'blur' }],
  longitude: [{ required: true, message: '请输入经度', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  manager: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const detailVisible = ref(false)
const currentStation = ref(null)
const usageChartRef = ref(null)
let usageChart = null

// ===== 展示用的派生字段 =====
const displayStatus = (station) => {
  if (station.status === 'maintenance') return 'maintenance'
  if (station.status === 'closed') return 'closed'
  const capacity = station.capacity || 0
  const used = station.currentUsage || 0
  return used >= capacity ? 'occupied' : 'idle'
}

const rows = computed(() =>
  allStations.value.map((s) => ({
    ...s,
    status: displayStatus(s),
    available: Math.max((s.capacity || 0) - (s.currentUsage || 0), 0)
  }))
)

const filtered = computed(() =>
  rows.value.filter((s) => {
    const nameOk = !searchForm.name || (s.name || '').includes(searchForm.name.trim())
    const typeOk = !searchForm.type || s.type === searchForm.type
    const statusOk = !searchForm.status || s.status === searchForm.status
    return nameOk && typeOk && statusOk
  })
)

const tableData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filtered.value.slice(start, start + pagination.size)
})

// ===== 数据加载 =====
const loadData = async () => {
  loading.value = true
  try {
    let data
    try {
      data = await airspaceAPI.getStations({ silent: true })
    } catch (e) {
      // 兼容尚未重启的旧后端（没有 /stations/all）
      data = await airspaceAPI.getActiveStations({ silent: true })
    }
    allStations.value = data || []
    pagination.total = filtered.value.length
  } catch (error) {
    console.error('加载场站数据失败:', error)
    ElMessage.error('加载场站数据失败')
  } finally {
    loading.value = false
  }
}

// ===== 工具函数 =====
const getTypeColor = (type) => {
  const colors = { landing: 'primary', charging: 'success', maintenance: 'warning', emergency: 'danger', transfer: 'info' }
  return colors[type] || ''
}

const getTypeName = (type) => {
  const names = { landing: '起降场', charging: '换电站', maintenance: '维修站', emergency: '应急站', transfer: '中转站' }
  return names[type] || type
}

const getStatusColor = (status) => {
  const colors = { idle: 'success', occupied: 'warning', maintenance: 'info', closed: 'danger' }
  return colors[status] || ''
}

const getStatusName = (status) => {
  const names = { idle: '空闲', occupied: '已满', maintenance: '维护中', closed: '已停用' }
  return names[status] || status
}

// 后端存的是 "06:00-22:00"，表单用时间区间数组
const toRange = (value) => {
  if (!value || !value.includes('-')) return []
  const parts = value.split('-')
  return parts.length === 2 ? parts : []
}

// ===== 查看详情 =====
const handleView = async (row) => {
  currentStation.value = row
  detailVisible.value = true

  await nextTick()
  if (usageChartRef.value) {
    if (!usageChart) {
      usageChart = echarts.init(usageChartRef.value)
    }
    const used = row.currentUsage || 0
    const free = Math.max((row.capacity || 0) - used, 0)
    usageChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['已占用', '空闲'] },
      xAxis: { type: 'category', data: ['停机位'] },
      yAxis: { type: 'value' },
      series: [
        { name: '已占用', type: 'bar', stack: 'total', data: [used], itemStyle: { color: '#e6a23c' } },
        { name: '空闲', type: 'bar', stack: 'total', data: [free], itemStyle: { color: '#67c23a' } }
      ]
    })
    usageChart.resize()
  }
}

// ===== 新增 / 编辑 =====
const handleAdd = () => {
  dialogTitle.value = '新增场站'
  Object.assign(form, {
    id: null,
    name: '',
    type: '',
    latitude: 27.9165,
    longitude: 120.689,
    capacity: 10,
    currentUsage: 0,
    todayUsage: 0,
    address: '',
    manager: '',
    phone: '',
    operationTime: [],
    facilities: '',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑场站'
  Object.assign(form, {
    ...row,
    operationTime: toRange(row.operationTime)
  })
  dialogVisible.value = true
}

// ===== 删除 =====
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除场站「${row.name}」吗？`, '提示', { type: 'warning' })
    await airspaceAPI.deleteStation(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// ===== 提交 =====
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    const payload = {
      name: form.name,
      type: form.type,
      latitude: form.latitude,
      longitude: form.longitude,
      capacity: form.capacity,
      currentUsage: form.currentUsage ?? 0,
      todayUsage: form.todayUsage ?? 0,
      address: form.address,
      manager: form.manager,
      phone: form.phone,
      operationTime: Array.isArray(form.operationTime) && form.operationTime.length === 2
        ? form.operationTime.join('-')
        : null,
      facilities: form.facilities,
      remark: form.remark,
      status: form.status === 'maintenance' || form.status === 'closed' ? form.status : 'active'
    }

    if (form.id) {
      await airspaceAPI.updateStation(form.id, payload)
      ElMessage.success('更新成功')
    } else {
      await airspaceAPI.createStation(payload)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    loadData()
  } catch (error) {
    if (error !== false) {
      console.error('保存失败:', error)
    }
  }
}

const handleDialogClose = () => {
  formRef.value?.clearValidate()
}

// ===== 查询 / 重置 / 导出 / 刷新 =====
const handleSearch = () => {
  pagination.page = 1
  pagination.total = filtered.value.length
}

const handleReset = () => {
  Object.assign(searchForm, { name: '', type: '', status: '' })
  pagination.page = 1
  pagination.total = filtered.value.length
}

const handleMapView = () => {
  router.push('/airspace/map')
}

const handleExport = () => {
  const header = ['名称', '类型', '状态', '地址', '容量', '已占用', '可用', '负责人', '联系方式', '运营时间']
  const list = filtered.value.map((s) => [
    s.name,
    getTypeName(s.type),
    getStatusName(s.status),
    s.address || '',
    s.capacity,
    s.currentUsage ?? 0,
    s.available,
    s.manager || '',
    s.phone || '',
    s.operationTime || ''
  ])
  const csv = [header, ...list].map((r) => r.map((v) => `"${v ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `场站列表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出当前筛选结果')
}

const refreshData = () => {
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.station-management {
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
