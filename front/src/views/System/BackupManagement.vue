<template>
  <div class="backup-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">数据备份管理</span>
          <el-button type="primary" @click="handleManualBackup">
            <el-icon><FolderAdd /></el-icon>
            立即备份
          </el-button>
        </div>
      </template>

      <!-- 备份统计 -->
      <div class="backup-stats">
        <el-card class="stat-card">
          <div class="stat-icon"><el-icon><Files /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalBackups }}</div>
            <div class="stat-label">总备份数</div>
          </div>
        </el-card>
        <el-card class="stat-card">
          <div class="stat-icon success"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.successCount }}</div>
            <div class="stat-label">成功备份</div>
          </div>
        </el-card>
        <el-card class="stat-card">
          <div class="stat-icon warning"><el-icon><Upload /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalSize }}</div>
            <div class="stat-label">总大小</div>
          </div>
        </el-card>
        <el-card class="stat-card">
          <div class="stat-icon primary"><el-icon><Clock /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.lastBackupTime }}</div>
            <div class="stat-label">最近备份</div>
          </div>
        </el-card>
      </div>

      <!-- 自动备份配置 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <span>自动备份配置</span>
        </template>
        <el-form :model="autoBackupConfig" label-width="150px" style="max-width: 600px">
          <el-form-item label="启用自动备份">
            <el-switch v-model="autoBackupConfig.enabled" @change="handleAutoBackupChange" />
          </el-form-item>
          <el-form-item label="备份频率" v-if="autoBackupConfig.enabled">
            <el-radio-group v-model="autoBackupConfig.frequency">
              <el-radio value="daily">每日</el-radio>
              <el-radio value="weekly">每周</el-radio>
              <el-radio value="monthly">每月</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备份时间" v-if="autoBackupConfig.enabled">
            <el-time-picker
              v-model="autoBackupConfig.time"
              format="HH:mm"
              value-format="HH:mm"
              placeholder="选择备份时间"
            />
          </el-form-item>
          <el-form-item label="保留份数" v-if="autoBackupConfig.enabled">
            <el-input-number v-model="autoBackupConfig.retentionCount" :min="1" :max="30" />
            <span style="margin-left: 10px; font-size: 12px; color: #909399">
              超出将自动删除最旧备份
            </span>
          </el-form-item>
          <el-form-item label="备份内容" v-if="autoBackupConfig.enabled">
            <el-checkbox-group v-model="autoBackupConfig.backupContent">
              <el-checkbox label="数据库" value="database" />
              <el-checkbox label="配置文件" value="config" />
              <el-checkbox label="上传文件" value="uploads" />
              <el-checkbox label="日志文件" value="logs" />
            </el-checkbox-group>
          </el-form-item>
          <el-form-item v-if="autoBackupConfig.enabled">
            <el-button type="primary" @click="handleSaveAutoBackup">保存配置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 备份列表 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>备份列表</span>
            <el-space>
              <el-select v-model="filterType" placeholder="备份类型" style="width: 120px" clearable>
                <el-option label="手动备份" value="manual" />
                <el-option label="自动备份" value="auto" />
              </el-select>
              <el-button @click="handleRefresh">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </el-space>
          </div>
        </template>

        <el-table :data="backupList" style="width: 100%" v-loading="loading">
          <el-table-column prop="name" label="备份名称" width="300" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'manual' ? 'warning' : 'info'" size="small">
                {{ row.type === 'manual' ? '手动' : '自动' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="size" label="文件大小" width="120" />
          <el-table-column prop="content" label="备份内容" width="200">
            <template #default="{ row }">
              <el-tag v-for="item in row.content" :key="item" size="small" style="margin-right: 5px">
                {{ item }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)" size="small">
                {{ getStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remarks" label="备注" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleDownload(row)" :disabled="row.status !== 'success'">
                下载
              </el-button>
              <el-button link type="success" @click="handleRestore(row)" :disabled="row.status !== 'success'">
                恢复
              </el-button>
              <el-button link type="warning" @click="handleVerify(row)" :disabled="row.status !== 'success'">
                校验
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">
                删除
              </el-button>
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
    </el-card>

    <!-- 手动备份对话框 -->
    <el-dialog v-model="manualBackupVisible" title="手动备份" width="600px">
      <el-form :model="manualBackupForm" :rules="manualBackupRules" ref="manualBackupFormRef" label-width="100px">
        <el-form-item label="备份名称" prop="name">
          <el-input v-model="manualBackupForm.name" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="备份内容" prop="content">
          <el-checkbox-group v-model="manualBackupForm.content">
            <el-checkbox label="数据库" value="database" />
            <el-checkbox label="配置文件" value="config" />
            <el-checkbox label="上传文件" value="uploads" />
            <el-checkbox label="日志文件" value="logs" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="压缩格式">
          <el-radio-group v-model="manualBackupForm.compression">
            <el-radio value="zip">ZIP</el-radio>
            <el-radio value="tar.gz">TAR.GZ</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="manualBackupForm.remarks"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <!-- 备份进度 -->
      <div v-if="backupProgress.show" style="margin-top: 20px">
        <el-progress :percentage="backupProgress.percentage" :status="backupProgress.status" />
        <div style="margin-top: 10px; text-align: center; color: #606266">
          {{ backupProgress.message }}
        </div>
      </div>

      <template #footer>
        <el-button @click="manualBackupVisible = false" :disabled="backing">取消</el-button>
        <el-button type="primary" @click="handleStartBackup" :loading="backing" :disabled="backupProgress.show">
          开始备份
        </el-button>
      </template>
    </el-dialog>

    <!-- 恢复对话框 -->
    <el-dialog v-model="restoreDialogVisible" title="恢复数据" width="500px">
      <el-alert
        title="警告"
        type="warning"
        :closable="false"
        style="margin-bottom: 20px"
      >
        恢复操作将覆盖当前数据，建议先备份当前数据。确定继续吗？
      </el-alert>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="备份名称">{{ restoreData.name }}</el-descriptions-item>
        <el-descriptions-item label="备份时间">{{ restoreData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备份大小">{{ restoreData.size }}</el-descriptions-item>
      </el-descriptions>

      <!-- 恢复进度 -->
      <div v-if="restoreProgress.show" style="margin-top: 20px">
        <el-progress :percentage="restoreProgress.percentage" :status="restoreProgress.status" />
        <div style="margin-top: 10px; text-align: center; color: #606266">
          {{ restoreProgress.message }}
        </div>
      </div>

      <template #footer>
        <el-button @click="restoreDialogVisible = false" :disabled="restoring">取消</el-button>
        <el-button type="danger" @click="handleStartRestore" :loading="restoring" :disabled="restoreProgress.show">
          确认恢复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderAdd,
  Files,
  CircleCheck,
  Upload,
  Clock,
  Refresh
} from '@element-plus/icons-vue'

// 统计数据
const stats = reactive({
  totalBackups: 28,
  successCount: 27,
  totalSize: '15.6 GB',
  lastBackupTime: '2026-09-17 02:00'
})

// 自动备份配置
const autoBackupConfig = reactive({
  enabled: true,
  frequency: 'daily',
  time: '02:00',
  retentionCount: 7,
  backupContent: ['database', 'config']
})

// 备份列表
const backupList = ref([])
const loading = ref(false)
const filterType = ref('')

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 手动备份
const manualBackupVisible = ref(false)
const backing = ref(false)
const manualBackupFormRef = ref(null)
const manualBackupForm = reactive({
  name: '',
  content: ['database', 'config'],
  compression: 'zip',
  remarks: ''
})
const manualBackupRules = {
  content: [{ required: true, message: '请选择备份内容', trigger: 'change' }]
}

// 备份进度
const backupProgress = reactive({
  show: false,
  percentage: 0,
  status: '',
  message: ''
})

// 恢复对话框
const restoreDialogVisible = ref(false)
const restoring = ref(false)
const restoreData = ref({})

// 恢复进度
const restoreProgress = reactive({
  show: false,
  percentage: 0,
  status: '',
  message: ''
})

// 初始化
onMounted(() => {
  fetchBackupList()
})

// 获取备份列表
const fetchBackupList = () => {
  loading.value = true

  setTimeout(() => {
    backupList.value = [
      {
        id: 1,
        name: 'backup_2026-09-17_02-00-00.zip',
        type: 'auto',
        size: '856 MB',
        content: ['数据库', '配置文件'],
        createTime: '2026-09-17 02:00:00',
        status: 'success',
        remarks: '每日自动备份'
      },
      {
        id: 2,
        name: 'backup_manual_2026-09-16_15-30-00.zip',
        type: 'manual',
        size: '1.2 GB',
        content: ['数据库', '配置文件', '上传文件'],
        createTime: '2026-09-16 15:30:00',
        status: 'success',
        remarks: '系统升级前备份'
      },
      {
        id: 3,
        name: 'backup_2026-09-16_02-00-00.zip',
        type: 'auto',
        size: '845 MB',
        content: ['数据库', '配置文件'],
        createTime: '2026-09-16 02:00:00',
        status: 'success',
        remarks: '每日自动备份'
      },
      {
        id: 4,
        name: 'backup_2026-09-15_02-00-00.zip',
        type: 'auto',
        size: '832 MB',
        content: ['数据库', '配置文件'],
        createTime: '2026-09-15 02:00:00',
        status: 'success',
        remarks: '每日自动备份'
      },
      {
        id: 5,
        name: 'backup_2026-09-14_02-00-00.zip',
        type: 'auto',
        size: '0 MB',
        content: ['数据库', '配置文件'],
        createTime: '2026-09-14 02:00:00',
        status: 'failed',
        remarks: '磁盘空间不足'
      }
    ]
    pagination.total = 28
    loading.value = false
  }, 500)
}

// 自动备份开关变化
const handleAutoBackupChange = (value) => {
  if (value) {
    ElMessage.success('自动备份已启用')
  } else {
    ElMessage.warning('自动备份已禁用')
  }
}

// 保存自动备份配置
const handleSaveAutoBackup = () => {
  ElMessage.success('自动备份配置已保存')
}

// 手动备份
const handleManualBackup = () => {
  manualBackupVisible.value = true
}

// 开始备份
const handleStartBackup = () => {
  manualBackupFormRef.value.validate((valid) => {
    if (valid) {
      backing.value = true
      backupProgress.show = true
      backupProgress.percentage = 0
      backupProgress.status = ''
      backupProgress.message = '准备备份...'

      // 模拟备份进度
      const interval = setInterval(() => {
        backupProgress.percentage += 10
        if (backupProgress.percentage === 30) {
          backupProgress.message = '正在备份数据库...'
        } else if (backupProgress.percentage === 60) {
          backupProgress.message = '正在备份配置文件...'
        } else if (backupProgress.percentage === 90) {
          backupProgress.message = '正在压缩文件...'
        } else if (backupProgress.percentage >= 100) {
          clearInterval(interval)
          backupProgress.percentage = 100
          backupProgress.status = 'success'
          backupProgress.message = '备份完成'
          backing.value = false

          setTimeout(() => {
            ElMessage.success('备份成功')
            manualBackupVisible.value = false
            backupProgress.show = false
            fetchBackupList()
          }, 1500)
        }
      }, 300)
    }
  })
}

// 下载备份
const handleDownload = (row) => {
  ElMessage.success(`正在下载: ${row.name}`)
  // TODO: 调用下载API
}

// 恢复数据
const handleRestore = (row) => {
  restoreData.value = { ...row }
  restoreDialogVisible.value = true
}

// 开始恢复
const handleStartRestore = () => {
  restoring.value = true
  restoreProgress.show = true
  restoreProgress.percentage = 0
  restoreProgress.status = ''
  restoreProgress.message = '准备恢复...'

  // 模拟恢复进度
  const interval = setInterval(() => {
    restoreProgress.percentage += 10
    if (restoreProgress.percentage === 30) {
      restoreProgress.message = '正在解压备份文件...'
    } else if (restoreProgress.percentage === 60) {
      restoreProgress.message = '正在恢复数据库...'
    } else if (restoreProgress.percentage === 90) {
      restoreProgress.message = '正在恢复配置文件...'
    } else if (restoreProgress.percentage >= 100) {
      clearInterval(interval)
      restoreProgress.percentage = 100
      restoreProgress.status = 'success'
      restoreProgress.message = '恢复完成'
      restoring.value = false

      setTimeout(() => {
        ElMessage.success('数据恢复成功，请重启系统')
        restoreDialogVisible.value = false
        restoreProgress.show = false
      }, 1500)
    }
  }, 300)
}

// 校验备份
const handleVerify = (row) => {
  ElMessage.info('正在校验备份文件...')
  // TODO: 调用校验API
  setTimeout(() => {
    ElMessage.success('校验通过，备份文件完整')
  }, 2000)
}

// 删除备份
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该备份吗？删除后无法恢复', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    fetchBackupList()
  })
}

// 刷新列表
const handleRefresh = () => {
  fetchBackupList()
  ElMessage.success('列表已刷新')
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchBackupList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchBackupList()
}

// 辅助函数
const getStatusName = (status) => {
  const map = {
    success: '成功',
    failed: '失败',
    processing: '进行中'
  }
  return map[status] || status
}

const getStatusTag = (status) => {
  const map = {
    success: 'success',
    failed: 'danger',
    processing: 'warning'
  }
  return map[status] || ''
}
</script>

<style scoped>
.backup-management {
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

.backup-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  font-size: 48px;
  margin-right: 20px;
  color: #409eff;
}

.stat-icon.success {
  color: #67c23a;
}

.stat-icon.warning {
  color: #e6a23c;
}

.stat-icon.primary {
  color: #409eff;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
