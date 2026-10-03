<template>
  <div class="user-management">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable style="width: 150px">
            <el-option v-for="r in roleOptions" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
            <el-option label="已锁定" value="LOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
        <el-button @click="handleBatchDelete" :disabled="selectedIds.length === 0">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>

      <!-- 用户列表 -->
      <el-table
        :data="tableData"
        @selection-change="handleSelectionChange"
        v-loading="loading"
        border
        stripe
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="150">
          <template #default="{ row }">
            {{ maskPhone(row.phone) }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="ACTIVE"
              inactive-value="INACTIVE"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最后登录" width="180">
          <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
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
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option v-for="r in roleOptions" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userAPI, roleAPI } from '@/api'

const loading = ref(false)
const allUsers = ref([])
const selectedIds = ref([])
const roleOptions = ref([])

const searchForm = reactive({
  username: '',
  role: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  realName: '',
  phone: '',
  role: '',
  password: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

// 过滤 + 分页都在前端完成（后端返回全量用户）
const filtered = computed(() =>
  allUsers.value.filter((u) => {
    const nameOk = !searchForm.username || (u.username || '').includes(searchForm.username.trim())
    const roleOk = !searchForm.role || u.role === searchForm.role
    const statusOk = !searchForm.status || u.status === searchForm.status
    return nameOk && roleOk && statusOk
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
    allUsers.value = (await userAPI.getList()) || []
    pagination.total = filtered.value.length
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    roleOptions.value = (await roleAPI.getOptions()) || []
  } catch (error) {
    console.error('加载角色选项失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  pagination.total = filtered.value.length
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, { username: '', role: '', status: '' })
  pagination.page = 1
  pagination.total = filtered.value.length
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map((item) => item.id)
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增用户'
  Object.assign(form, {
    id: null,
    username: '',
    realName: '',
    phone: '',
    role: '',
    password: '',
    email: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
    await userAPI.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个用户吗？`, '提示', {
      type: 'warning'
    })
    await Promise.all(selectedIds.value.map((id) => userAPI.remove(id)))
    ElMessage.success('删除成功')
    selectedIds.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

// 状态切换（v-model 已经改了 row.status，这里同步到后端）
const handleStatusChange = async (row) => {
  try {
    await userAPI.update(row.id, { status: row.status })
    ElMessage.success(row.status === 'ACTIVE' ? '用户已启用' : '用户已禁用')
  } catch (error) {
    row.status = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    console.error('更新状态失败:', error)
  }
}

// 重置密码
const handleResetPassword = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(`为用户「${row.username}」设置新密码`, '重置密码', {
      inputValue: '123456',
      inputPattern: /^.{6,100}$/,
      inputErrorMessage: '密码长度需在 6-100 位之间'
    })
    await userAPI.resetPassword(row.id, value)
    ElMessage.success('密码已重置')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
    }
  }
}

// 导出（前端直接把当前筛选结果导出为 CSV）
const handleExport = () => {
  const header = ['ID', '用户名', '真实姓名', '手机号', '角色', '状态', '邮箱']
  const rows = filtered.value.map((u) => [
    u.id,
    u.username,
    u.realName || '',
    u.phone || '',
    getRoleName(u.role),
    u.status,
    u.email || ''
  ])
  const csv = [header, ...rows].map((r) => r.map((v) => `"${v ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `用户列表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出当前筛选结果')
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    if (form.id) {
      await userAPI.update(form.id, {
        realName: form.realName,
        phone: form.phone,
        email: form.email,
        role: form.role
      })
      ElMessage.success('更新成功')
    } else {
      await userAPI.create({
        username: form.username,
        password: form.password,
        realName: form.realName,
        phone: form.phone,
        email: form.email,
        role: form.role
      })
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

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.clearValidate()
}

// 工具函数
const maskPhone = (phone) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const getRoleType = (role) => {
  const types = {
    ADMIN: 'danger',
    SUPERVISOR: 'warning',
    AIR_CONTROL: 'info',
    CUSTOMER: 'success',
    OPERATOR: 'primary',
    PILOT: 'success',
    MAINTENANCE: 'info',
    GUEST: 'info'
  }
  return types[role] || ''
}

// 角色名称以后端返回为准
const getRoleName = (role) => {
  const found = roleOptions.value.find((r) => r.code === role)
  return found ? found.name : role
}

onMounted(() => {
  loadRoles()
  loadData()
})
</script>

<style scoped>
.user-management {
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
