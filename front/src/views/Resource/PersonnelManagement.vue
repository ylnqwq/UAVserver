<template>
  <div class="personnel-management">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable style="width: 150px">
            <el-option v-for="r in roleOptions" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="在职" value="ACTIVE" />
            <el-option label="休假" value="INACTIVE" />
            <el-option label="离职" value="SUSPENDED" />
            <el-option label="已锁定" value="LOCKED" />
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
        <el-button v-if="isAdmin()" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增人员
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>

      <!-- 人员列表 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
      >
        <el-table-column type="index" width="55" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="role" label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="120">
          <template #default="{ row }">
            {{ maskPhone(row.phone) }}
          </template>
        </el-table-column>
        <el-table-column prop="certNo" label="证件号" width="150" />
        <el-table-column prop="certExpiry" label="证件到期" width="120">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isExpiringSoon(row.certExpiry) }">
              {{ row.certExpiry }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="flightHours" label="飞行时长(h)" width="120" v-if="searchForm.role === 'pilot' || !searchForm.role" />
        <el-table-column prop="todayTasks" label="今日任务" width="100" />
        <el-table-column prop="totalTasks" label="累计任务" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button v-if="isAdmin()" type="warning" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="isAdmin()" type="danger" link @click="handleDelete(row)">删除</el-button>
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
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-select v-model="form.role" placeholder="请选择" style="width: 100%">
                <el-option v-for="r in roleOptions" :key="r.code" :label="r.name" :value="r.code" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="证件号" prop="certNo">
              <el-input v-model="form.certNo" placeholder="请输入驾驶证号或资格证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件到期日期" prop="certExpiry">
              <el-date-picker
                v-model="form.certExpiry"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="培训记录">
          <el-input
            v-model="form.training"
            type="textarea"
            :rows="2"
            placeholder="请输入培训记录"
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

    <!-- 人员详情对话框 -->
    <el-dialog v-model="detailVisible" title="人员详情" width="900px">
      <el-row :gutter="20" v-if="currentPerson">
        <el-col :span="14">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="姓名">{{ currentPerson.name }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag :type="currentPerson.role === 'pilot' ? 'primary' : 'success'">
                {{ currentPerson.role === 'pilot' ? '机长' : '运维' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="手机号">{{ currentPerson.phone }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{ maskIdCard(currentPerson.idCard) }}</el-descriptions-item>
            <el-descriptions-item label="证件号">{{ currentPerson.certNo }}</el-descriptions-item>
            <el-descriptions-item label="证件到期">
              <span :class="{ 'text-danger': isExpiringSoon(currentPerson.certExpiry) }">
                {{ currentPerson.certExpiry }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="累计飞行时长" v-if="currentPerson.role === 'pilot'">
              {{ currentPerson.flightHours }} 小时
            </el-descriptions-item>
            <el-descriptions-item label="累计任务">{{ currentPerson.totalTasks }} 单</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentPerson.status)">{{ getStatusName(currentPerson.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="入职日期">{{ currentPerson.joinDate }}</el-descriptions-item>
            <el-descriptions-item label="培训记录" :span="2">{{ currentPerson.training || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider>工作统计</el-divider>
          <div ref="workChartRef" style="height: 250px"></div>
        </el-col>

        <el-col :span="10">
          <el-card shadow="hover">
            <template #header>
              <span>档案信息</span>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="入职日期">{{ currentPerson.joinDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="今日任务">{{ currentPerson.todayTasks || 0 }} 单</el-descriptions-item>
              <el-descriptions-item label="培训记录">{{ currentPerson.training || '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ currentPerson.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { userAPI, roleAPI } from '@/api'
import { useUserStore } from '@/stores/user'

/**
 * 人员管理页面：数据来自后端 users 表（按业务角色过滤）
 *   列表  GET    /api/users/role/{PILOT|OPERATOR|MAINTENANCE}
 *   新增  POST   /api/users         （仅管理员，用户名取手机号，初始密码 123456）
 *   修改  PUT    /api/users/{id}    （仅管理员）
 *   删除  DELETE /api/users/{id}    （仅管理员）
 *
 * 说明：人员与「账号管理」里的用户是同一批人，只是这里按业务角色（飞行员/运维/维护）
 * 查看和维护人员档案字段（执照、入职日期、飞行时长等）。
 */

const userStore = useUserStore()
const isAdmin = () => userStore.isAdmin()

const BUSINESS_ROLES = ['PILOT', 'OPERATOR', 'MAINTENANCE']

const loading = ref(false)
const allPeople = ref([])
const roleOptions = ref([])

const searchForm = reactive({
  name: '',
  role: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增人员')
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  name: '',
  role: '',
  phone: '',
  idCard: '',
  certNo: '',
  certExpiry: '',
  training: '',
  remark: '',
  status: 'ACTIVE'
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  idCard: [{ pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }]
}

const detailVisible = ref(false)
const currentPerson = ref(null)
const workChartRef = ref(null)
let workChart = null

// 状态：后端 UserStatus → 展示文案
const STATUS_LABELS = { ACTIVE: '在职', INACTIVE: '休假', SUSPENDED: '离职', LOCKED: '已锁定' }

const filtered = computed(() =>
  allPeople.value.filter((p) => {
    const nameOk = !searchForm.name || (p.name || '').includes(searchForm.name.trim())
    const roleOk = !searchForm.role || p.role === searchForm.role
    const statusOk = !searchForm.status || p.status === searchForm.status
    return nameOk && roleOk && statusOk
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
    const results = await Promise.all(
      BUSINESS_ROLES.map((role) => userAPI.getByRole(role, { silent: true }).catch(() => []))
    )
    allPeople.value = results
      .flat()
      .filter(Boolean)
      .map((u) => ({ ...u, name: u.realName || u.username }))
    pagination.total = filtered.value.length
  } catch (error) {
    console.error('加载人员数据失败:', error)
    ElMessage.error('加载人员数据失败')
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    const options = (await roleAPI.getOptions()) || []
    roleOptions.value = options.filter((r) => BUSINESS_ROLES.includes(r.code))
  } catch (error) {
    console.error('加载角色选项失败:', error)
  }
}

// ===== 工具函数 =====
const getRoleName = (role) => {
  const found = roleOptions.value.find((r) => r.code === role)
  return found ? found.name : role
}

const getRoleType = (role) => {
  const types = { PILOT: 'primary', OPERATOR: 'success', MAINTENANCE: 'warning' }
  return types[role] || ''
}

const getStatusType = (status) => {
  const types = { ACTIVE: 'success', INACTIVE: 'info', SUSPENDED: 'danger', LOCKED: 'warning' }
  return types[status] || ''
}

const getStatusName = (status) => STATUS_LABELS[status] || status

const maskPhone = (phone) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

const maskIdCard = (idCard) => {
  if (!idCard || idCard.length < 8) return idCard || ''
  return idCard.replace(/^(.{4}).*(.{4})$/, '$1**********$2')
}

// 执照 30 天内到期
const isExpiringSoon = (certExpiry) => {
  if (!certExpiry) return false
  const now = new Date()
  const days = (new Date(certExpiry) - now) / (24 * 3600 * 1000)
  return days <= 30
}

// ===== 查看 =====
const handleView = async (row) => {
  currentPerson.value = row
  detailVisible.value = true

  await nextTick()
  if (workChartRef.value) {
    if (!workChart) workChart = echarts.init(workChartRef.value)
    workChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['今日任务', '累计任务'] },
      xAxis: { type: 'category', data: [row.name] },
      yAxis: { type: 'value' },
      series: [
        { name: '今日任务', type: 'bar', data: [row.todayTasks || 0], itemStyle: { color: '#409eff' } },
        { name: '累计任务', type: 'bar', data: [row.totalTasks || 0], itemStyle: { color: '#67c23a' } }
      ]
    })
    workChart.resize()
  }
}

// ===== 新增 / 编辑 =====
const handleAdd = () => {
  dialogTitle.value = '新增人员'
  Object.assign(form, {
    id: null,
    username: '',
    name: '',
    role: 'PILOT',
    phone: '',
    idCard: '',
    certNo: '',
    certExpiry: '',
    training: '',
    remark: '',
    status: 'ACTIVE'
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑人员'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

// ===== 删除 =====
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除人员「${row.name}」的账号吗？`, '提示', { type: 'warning' })
    await userAPI.remove(row.id)
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
      realName: form.name,
      role: form.role,
      phone: form.phone,
      idCard: form.idCard || null,
      certNo: form.certNo || null,
      certExpiry: form.certExpiry || null,
      training: form.training || null,
      remark: form.remark || null,
      status: form.status || 'ACTIVE'
    }

    if (form.id) {
      await userAPI.update(form.id, payload)
      ElMessage.success('更新成功')
    } else {
      // 人员新建时后端需要账号信息：用户名取手机号，初始密码 123456
      await userAPI.create({
        ...payload,
        username: form.phone,
        password: '123456'
      })
      ElMessage.success(`新增成功，登录账号：${form.phone}，初始密码：123456`)
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

// ===== 查询 / 重置 / 导出 =====
const handleSearch = () => {
  pagination.page = 1
  pagination.total = filtered.value.length
}

const handleReset = () => {
  Object.assign(searchForm, { name: '', role: '', status: '' })
  pagination.page = 1
  pagination.total = filtered.value.length
}

const handleExport = () => {
  const header = ['姓名', '角色', '手机号', '身份证号', '执照号', '执照到期', '飞行时长(h)', '累计任务', '状态']
  const list = filtered.value.map((p) => [
    p.name,
    getRoleName(p.role),
    p.phone || '',
    p.idCard || '',
    p.certNo || '',
    p.certExpiry || '',
    p.flightHours ?? 0,
    p.totalTasks ?? 0,
    getStatusName(p.status)
  ])
  const csv = [header, ...list].map((r) => r.map((v) => `"${v ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `人员列表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出当前筛选结果')
}

onMounted(() => {
  loadRoles()
  loadData()
})
</script>

<style scoped>
.personnel-management {
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

.text-danger {
  color: #f56c6c;
  font-weight: bold;
}

.calendar-cell,
.schedule-cell {
  height: 80px;
  text-align: center;
  padding: 5px;
}

.schedule-cell {
  cursor: pointer;
}

.schedule-cell:hover {
  background: #f5f7fa;
}

.schedule-cell.has-schedule {
  background: #ecf5ff;
}

.date {
  font-size: 18px;
  margin-bottom: 5px;
}

.schedule-info {
  font-size: 12px;
}
</style>
