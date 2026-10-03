<template>
  <div class="airspace-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>空域管理</span>
          <el-button v-if="canEdit()" type="primary" icon="Plus" @click="handleAdd">
            添加空域
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="类型">
          <el-select
            v-model="searchForm.type"
            placeholder="全部类型"
            clearable
            class="filter-select"
            style="width: 150px"
          >
            <el-option label="禁飞区" value="nofly" />
            <el-option label="限飞区" value="restricted" />
            <el-option label="临时管制" value="temporary" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleSearch">
            搜索
          </el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
          <span class="result-hint">
            共 {{ tableData.length }} 条{{ appliedType ? `（${getTypeLabel(appliedType)}）` : '' }}
          </span>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="空域名称" min-width="200" />
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="getTypeColor(row.type)">{{ getTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="中心坐标" width="190">
          <template #default="{ row }">
            {{ row.latitude }}, {{ row.longitude }}
          </template>
        </el-table-column>
        <el-table-column label="半径" width="100">
          <template #default="{ row }">{{ row.radius }} 米</template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button v-if="canEdit()" link type="warning" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button v-if="canDelete()" link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialog" title="空域详情" width="620px">
      <el-descriptions :column="2" border v-if="currentZone">
        <el-descriptions-item label="空域名称" :span="2">{{ currentZone.name }}</el-descriptions-item>
        <el-descriptions-item label="空域类型">
          <el-tag :type="getTypeColor(currentZone.type)">{{ getTypeLabel(currentZone.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentZone.active ? 'success' : 'info'">
            {{ currentZone.active ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="中心纬度">{{ currentZone.latitude }}</el-descriptions-item>
        <el-descriptions-item label="中心经度">{{ currentZone.longitude }}</el-descriptions-item>
        <el-descriptions-item label="半径">{{ currentZone.radius }} 米</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(currentZone.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="生效时间">{{ formatTime(currentZone.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="失效时间">{{ formatTime(currentZone.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="说明" :span="2">{{ currentZone.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="dialogTitle" width="620px" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="空域名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：温州商学院限飞区" />
        </el-form-item>

        <el-form-item label="空域类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option label="禁飞区" value="nofly" />
            <el-option label="限飞区" value="restricted" />
            <el-option label="临时管制" value="temporary" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="中心纬度" prop="latitude">
              <el-input v-model.number="form.latitude" type="number" placeholder="如 27.9165" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="中心经度" prop="longitude">
              <el-input v-model.number="form.longitude" type="number" placeholder="如 120.6890" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="半径(米)" prop="radius">
          <el-input v-model.number="form.radius" type="number" placeholder="如 1500" />
        </el-form-item>

        <el-form-item label="是否启用">
          <el-switch v-model="form.active" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生效时间">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选填（临时管制用）"
                style="width: 100%"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失效时间">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选填（临时管制用）"
                style="width: 100%"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="限制说明等" />
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
/**
 * 空域管理页面。
 *
 * 注意：这里管理的是「地图上画圈的那批空域」，即 airspace_zones 表
 * （禁飞区 nofly / 限飞区 restricted / 临时管制 temporary），
 * 与「空域地图」页面用的是同一份数据，因此在列表中能看到地图上的全部空域。
 *
 * 另有一张 airspace 表（空域合规信息：空域编码、高度范围、管理机构），
 * 由 /api/airspace 提供接口，属于不同的业务对象，本页面不涉及。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { airspaceAPI } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const detailDialog = ref(false)
const formRef = ref(null)
const zoneList = ref([])
const currentZone = ref(null)

const searchForm = reactive({
  type: ''
})
// 已生效的筛选条件（点「搜索」后才更新）
const appliedType = ref('')

const createEmptyForm = () => ({
  id: null,
  name: '',
  type: 'restricted',
  latitude: null,
  longitude: null,
  radius: 1000,
  description: '',
  active: true,
  startTime: null,
  endTime: null
})

const form = reactive(createEmptyForm())

const rules = {
  name: [{ required: true, message: '请输入空域名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择空域类型', trigger: 'change' }],
  latitude: [{ required: true, message: '请输入中心纬度', trigger: 'blur' }],
  longitude: [{ required: true, message: '请输入中心经度', trigger: 'blur' }],
  radius: [{ required: true, message: '请输入半径', trigger: 'blur' }]
}

const dialogTitle = computed(() => (form.id ? '编辑空域' : '添加空域'))

// 类型过滤在前端完成，保证「全部」能列出停用的空域
const tableData = computed(() =>
  appliedType.value ? zoneList.value.filter((z) => z.type === appliedType.value) : zoneList.value
)

const canEdit = () => ['ADMIN', 'SUPERVISOR', 'AIR_CONTROL'].includes(userStore.userInfo?.role)
const canDelete = () => userStore.isAdmin()

const getTypeLabel = (type) => {
  const map = { nofly: '禁飞区', restricted: '限飞区', temporary: '临时管制' }
  return map[type] || type
}

const getTypeColor = (type) => {
  const map = { nofly: 'danger', restricted: 'warning', temporary: 'info' }
  return map[type] || ''
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const loadData = async () => {
  try {
    loading.value = true
    let data
    try {
      // 常规接口：包含已停用的空域
      data = await airspaceAPI.getZones({ silent: true })
    } catch (e) {
      // 兼容尚未重启的旧后端（没有 /airspace/zones/all），退回“仅启用”接口
      data = await airspaceAPI.getActiveZones({ silent: true })
    }
    zoneList.value = data || []
  } catch (error) {
    console.error('加载空域数据失败:', error)
    ElMessage.error('加载空域数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  appliedType.value = searchForm.type
  loadData()
}

const handleReset = () => {
  searchForm.type = ''
  appliedType.value = ''
  loadData()
}

const handleAdd = () => {
  Object.assign(form, createEmptyForm())
  showDialog.value = true
}

const handleView = (row) => {
  currentZone.value = row
  detailDialog.value = true
}

const handleEdit = (row) => {
  Object.assign(form, createEmptyForm(), row)
  showDialog.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除空域「${row.name}」吗？`, '提示', { type: 'warning' })
    await airspaceAPI.deleteZone(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 只提交后端实体需要的字段，避免把 id、createdAt 等回传给接口
const buildPayload = () => ({
  name: form.name,
  type: form.type,
  latitude: form.latitude,
  longitude: form.longitude,
  radius: form.radius,
  description: form.description,
  active: form.active,
  startTime: form.startTime || null,
  endTime: form.endTime || null
})

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    if (form.id) {
      await airspaceAPI.updateZone(form.id, buildPayload())
      ElMessage.success('更新成功')
    } else {
      await airspaceAPI.createZone(buildPayload())
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
  formRef.value?.clearValidate()
  Object.assign(form, createEmptyForm())
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
