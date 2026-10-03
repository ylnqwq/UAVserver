<template>
  <div class="aircraft-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>飞行器管理</span>
          <el-button type="primary" icon="Plus" @click="showDialog = true">
            添加飞行器
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部状态"
            clearable
            style="width: 150px"
          >
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="飞行中" value="IN_FLIGHT" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="已退役" value="RETIRED" />
            <el-option label="损坏" value="DAMAGED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleSearch">
            搜索
          </el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
          <span class="result-hint">
            共 {{ tableData.length }} 条{{ appliedStatus ? `（${getStatusLabel(appliedStatus)}）` : '' }}
          </span>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="registrationNumber" label="注册编号" width="140" />
        <el-table-column prop="name" label="飞行器名称" />
        <el-table-column prop="model" label="型号" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            {{ getTypeLabel(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="运营者" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="注册编号" prop="registrationNumber">
          <el-input v-model="form.registrationNumber" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="飞行器名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="制造商">
          <el-input v-model="form.manufacturer" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option label="固定翼" value="FIXED_WING" />
            <el-option label="旋翼" value="ROTARY_WING" />
            <el-option label="多旋翼" value="MULTI_ROTOR" />
            <el-option label="混合型" value="HYBRID" />
            <el-option label="垂直起降" value="VTOL" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="飞行中" value="IN_FLIGHT" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="已退役" value="RETIRED" />
            <el-option label="损坏" value="DAMAGED" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大起飞重量">
          <el-input v-model.number="form.maxTakeoffWeight" type="number">
            <template #append>kg</template>
          </el-input>
        </el-form-item>
        <el-form-item label="最大速度">
          <el-input v-model.number="form.maxSpeed" type="number">
            <template #append>m/s</template>
          </el-input>
        </el-form-item>
        <el-form-item label="运营者">
          <el-input v-model="form.operator" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aircraftAPI } from '@/api'

const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const formRef = ref(null)
const tableData = ref([])

const searchForm = reactive({
  status: ''
})
// 已生效的筛选条件（点「搜索」后才更新，用于结果提示）
const appliedStatus = ref('')

const form = reactive({
  id: null,
  registrationNumber: '',
  name: '',
  model: '',
  manufacturer: '',
  type: '',
  maxTakeoffWeight: null,
  maxSpeed: null,
  operator: '',
  description: ''
})

const rules = {
  registrationNumber: [{ required: true, message: '请输入注册编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入飞行器名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择飞行器类型', trigger: 'change' }]
}

const dialogTitle = computed(() => (form.id ? '编辑飞行器' : '添加飞行器'))

const getTypeLabel = (type) => {
  const map = {
    FIXED_WING: '固定翼',
    ROTARY_WING: '旋翼',
    MULTI_ROTOR: '多旋翼',
    HYBRID: '混合型',
    VTOL: '垂直起降',
    OTHER: '其他'
  }
  return map[type] || type
}

const getStatusType = (status) => {
  const map = {
    AVAILABLE: 'success',
    IN_FLIGHT: 'warning',
    MAINTENANCE: 'info',
    RETIRED: '',
    DAMAGED: 'danger'
  }
  return map[status] || ''
}

const getStatusLabel = (status) => {
  const map = {
    AVAILABLE: '可用',
    IN_FLIGHT: '飞行中',
    MAINTENANCE: '维护中',
    RETIRED: '已退役',
    DAMAGED: '损坏'
  }
  return map[status] || status
}

const loadData = async () => {
  try {
    loading.value = true
    const data = searchForm.status
      ? await aircraftAPI.getByStatus(searchForm.status)
      : await aircraftAPI.getList()
    tableData.value = data
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  appliedStatus.value = searchForm.status
  loadData()
}

const handleReset = () => {
  searchForm.status = ''
  appliedStatus.value = ''
  loadData()
}

const handleEdit = (row) => {
  Object.assign(form, row)
  showDialog.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该飞行器吗？', '提示', {
      type: 'warning'
    })
    await aircraftAPI.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    if (form.id) {
      await aircraftAPI.update(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await aircraftAPI.create(form)
      ElMessage.success('创建成功')
    }

    showDialog.value = false
    loadData()
  } catch (error) {
    if (error !== false) {
      console.error('保存失败:', error)
    }
  } finally {
    saving.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.keys(form).forEach(key => {
    form[key] = null
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 16px;
}

.search-form :deep(.el-select) {
  width: 150px;
}

.result-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}
</style>
