<template>
  <div class="service-area-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">服务空域配置</span>
          <el-button type="primary" @click="handleAddArea">
            <el-icon><Plus /></el-icon>
            添加空域
          </el-button>
        </div>
      </template>

      <!-- 空域列表 -->
      <el-table :data="areaList" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="空域名称" width="200" />
        <el-table-column prop="code" label="空域编号" width="150" />
        <el-table-column prop="type" label="空域类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="所属区域" width="150" />
        <el-table-column prop="altitudeRange" label="高度范围(m)" width="150">
          <template #default="{ row }">
            {{ row.minAltitude }} - {{ row.maxAltitude }}
          </template>
        </el-table-column>
        <el-table-column prop="area" label="面积(km²)" width="120" />
        <el-table-column prop="capacity" label="容量(架次/小时)" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="warning" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleEnable(row)" v-if="row.status === 'disabled'">
              启用
            </el-button>
            <el-button link type="info" @click="handleDisable(row)" v-if="row.status === 'enabled'">
              禁用
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 添加/编辑空域对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="900px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="空域名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入空域名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="空域编号" prop="code">
              <el-input v-model="formData.code" placeholder="如：BJ-CY-A01" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="空域类型" prop="type">
              <el-select v-model="formData.type" placeholder="请选择空域类型" style="width: 100%">
                <el-option label="普通空域" value="normal" />
                <el-option label="限制空域" value="restricted" />
                <el-option label="禁飞区" value="no-fly" />
                <el-option label="临时管制区" value="temporary" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属区域" prop="region">
              <el-cascader
                v-model="formData.region"
                :options="regionOptions"
                placeholder="请选择区域"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最低高度(m)" prop="minAltitude">
              <el-input-number v-model="formData.minAltitude" :min="0" :max="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高高度(m)" prop="maxAltitude">
              <el-input-number v-model="formData.maxAltitude" :min="0" :max="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="容量限制" prop="capacity">
              <el-input-number v-model="formData.capacity" :min="1" :max="100" style="width: 100%" />
              <span style="margin-left: 10px; font-size: 12px; color: #909399">架次/小时</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="面积(km²)" prop="area">
              <el-input-number v-model="formData.area" :min="0.1" :max="1000" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="空域范围" prop="boundary">
          <div class="map-container">
            <div class="map-placeholder">
              <el-icon style="font-size: 48px; color: #909399"><MapLocation /></el-icon>
              <p>点击地图标注空域边界</p>
              <p style="font-size: 12px; color: #909399">集成高德/百度地图后可绘制</p>
              <el-button type="primary" style="margin-top: 10px" @click="handleDrawBoundary">
                绘制边界
              </el-button>
            </div>
          </div>
          <el-input
            v-model="formData.boundary"
            type="textarea"
            :rows="3"
            placeholder="经纬度坐标（GeoJSON格式）"
            style="margin-top: 10px"
          />
        </el-form-item>

        <el-form-item label="服务时间">
          <el-time-picker
            v-model="formData.serviceTime"
            is-range
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="HH:mm"
            value-format="HH:mm"
          />
        </el-form-item>

        <el-form-item label="管制要求">
          <el-checkbox-group v-model="formData.requirements">
            <el-checkbox label="需要提前申报" value="pre-declaration" />
            <el-checkbox label="需要实时监控" value="real-time-monitoring" />
            <el-checkbox label="限制飞行器类型" value="aircraft-type-limit" />
            <el-checkbox label="需要专业资质" value="qualification-required" />
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="formData.remarks"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="空域详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="空域名称">{{ viewData.name }}</el-descriptions-item>
        <el-descriptions-item label="空域编号">{{ viewData.code }}</el-descriptions-item>
        <el-descriptions-item label="空域类型">
          <el-tag :type="getTypeTag(viewData.type)">{{ getTypeName(viewData.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属区域">{{ viewData.region }}</el-descriptions-item>
        <el-descriptions-item label="高度范围">
          {{ viewData.minAltitude }} - {{ viewData.maxAltitude }} m
        </el-descriptions-item>
        <el-descriptions-item label="面积">{{ viewData.area }} km²</el-descriptions-item>
        <el-descriptions-item label="容量">{{ viewData.capacity }} 架次/小时</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTag(viewData.status)">{{ getStatusName(viewData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="服务时间" :span="2">
          {{ viewData.serviceTime ? viewData.serviceTime.join(' - ') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewData.remarks || '无' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="viewData.requirements && viewData.requirements.length" style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">管制要求：</div>
        <el-tag v-for="req in viewData.requirements" :key="req" style="margin-right: 10px">
          {{ req }}
        </el-tag>
      </div>

      <div style="margin-top: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">空域地图：</div>
        <div class="map-placeholder" style="height: 300px">
          <el-icon style="font-size: 48px; color: #909399"><MapLocation /></el-icon>
          <p>空域边界显示</p>
        </div>
      </div>

      <template #footer>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MapLocation } from '@element-plus/icons-vue'

// 空域列表
const areaList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 区域选项
const regionOptions = [
  {
    value: 'beijing',
    label: '北京市',
    children: [
      { value: 'chaoyang', label: '朝阳区' },
      { value: 'haidian', label: '海淀区' },
      { value: 'fengtai', label: '丰台区' }
    ]
  },
  {
    value: 'shanghai',
    label: '上海市',
    children: [
      { value: 'pudong', label: '浦东新区' },
      { value: 'huangpu', label: '黄浦区' }
    ]
  }
]

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('添加空域')
const submitting = ref(false)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  id: null,
  name: '',
  code: '',
  type: '',
  region: [],
  minAltitude: 0,
  maxAltitude: 120,
  capacity: 10,
  area: 1.0,
  boundary: '',
  serviceTime: ['00:00', '23:59'],
  requirements: [],
  remarks: ''
})

// 表单验证
const formRules = {
  name: [{ required: true, message: '请输入空域名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入空域编号', trigger: 'blur' }],
  type: [{ required: true, message: '请选择空域类型', trigger: 'change' }],
  region: [{ required: true, message: '请选择所属区域', trigger: 'change' }],
  minAltitude: [{ required: true, message: '请输入最低高度', trigger: 'blur' }],
  maxAltitude: [{ required: true, message: '请输入最高高度', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量限制', trigger: 'blur' }],
  area: [{ required: true, message: '请输入面积', trigger: 'blur' }]
}

// 查看详情
const viewDialogVisible = ref(false)
const viewData = ref({})

// 初始化
onMounted(() => {
  fetchAreaList()
})

// 获取空域列表
const fetchAreaList = () => {
  loading.value = true

  setTimeout(() => {
    areaList.value = [
      {
        id: 1,
        name: '朝阳区核心空域A区',
        code: 'BJ-CY-A01',
        type: 'normal',
        region: '北京市-朝阳区',
        minAltitude: 0,
        maxAltitude: 120,
        area: 5.2,
        capacity: 20,
        status: 'enabled',
        serviceTime: ['06:00', '22:00'],
        requirements: ['需要提前申报', '需要实时监控'],
        remarks: '核心商务区空域'
      },
      {
        id: 2,
        name: '海淀区限制空域B区',
        code: 'BJ-HD-B01',
        type: 'restricted',
        region: '北京市-海淀区',
        minAltitude: 0,
        maxAltitude: 100,
        area: 3.8,
        capacity: 10,
        status: 'enabled',
        serviceTime: ['08:00', '20:00'],
        requirements: ['需要提前申报', '限制飞行器类型', '需要专业资质'],
        remarks: '科研院所密集区'
      },
      {
        id: 3,
        name: '首都机场禁飞区',
        code: 'BJ-SY-NFZ',
        type: 'no-fly',
        region: '北京市-朝阳区',
        minAltitude: 0,
        maxAltitude: 1000,
        area: 15.6,
        capacity: 0,
        status: 'enabled',
        serviceTime: ['00:00', '23:59'],
        requirements: ['禁止飞行'],
        remarks: '首都机场周边永久禁飞区'
      }
    ]
    pagination.total = 15
    loading.value = false
  }, 500)
}

// 添加空域
const handleAddArea = () => {
  dialogTitle.value = '添加空域'
  dialogVisible.value = true
}

// 编辑空域
const handleEdit = (row) => {
  dialogTitle.value = '编辑空域'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 查看详情
const handleView = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
}

// 启用
const handleEnable = (row) => {
  ElMessageBox.confirm('确定要启用该空域吗？', '启用确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    row.status = 'enabled'
    ElMessage.success('空域已启用')
  })
}

// 禁用
const handleDisable = (row) => {
  ElMessageBox.confirm('禁用后该空域将无法使用，确定继续吗？', '禁用确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    row.status = 'disabled'
    ElMessage.success('空域已禁用')
  })
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该空域吗？删除后无法恢复', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    fetchAreaList()
  })
}

// 绘制边界
const handleDrawBoundary = () => {
  ElMessage.info('地图绘制功能待集成')
}

// 提交表单
const handleSubmit = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      setTimeout(() => {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        submitting.value = false
        fetchAreaList()
      }, 1000)
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: null,
    name: '',
    code: '',
    type: '',
    region: [],
    minAltitude: 0,
    maxAltitude: 120,
    capacity: 10,
    area: 1.0,
    boundary: '',
    serviceTime: ['00:00', '23:59'],
    requirements: [],
    remarks: ''
  })
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchAreaList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchAreaList()
}

// 辅助函数
const getTypeName = (type) => {
  const map = {
    normal: '普通空域',
    restricted: '限制空域',
    'no-fly': '禁飞区',
    temporary: '临时管制区'
  }
  return map[type] || type
}

const getTypeTag = (type) => {
  const map = {
    normal: 'success',
    restricted: 'warning',
    'no-fly': 'danger',
    temporary: 'info'
  }
  return map[type] || ''
}

const getStatusName = (status) => {
  const map = { enabled: '已启用', disabled: '已禁用' }
  return map[status] || status
}

const getStatusTag = (status) => {
  const map = { enabled: 'success', disabled: 'info' }
  return map[status] || ''
}
</script>

<style scoped>
.service-area-config {
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

.map-container {
  width: 100%;
  height: 300px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.map-placeholder p {
  margin: 5px 0;
  color: #909399;
}
</style>
