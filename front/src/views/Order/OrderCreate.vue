<template>
  <div class="order-create">
    <el-card shadow="never">
      <template #header>
        <span>创建订单</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 800px"
      >
        <!-- 基本信息 -->
        <el-divider content-position="left">基本信息</el-divider>

        <el-form-item label="寄件地址" prop="sendAddr">
          <el-input
            v-model="form.sendAddr"
            placeholder="请输入详细地址或选择常用地址"
          >
            <template #append>
              <el-button @click="showAddressSelector('send')">
                <el-icon><Location /></el-icon>
                选择地址
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="收件地址" prop="recvAddr">
          <el-input
            v-model="form.recvAddr"
            placeholder="请输入详细地址或选择常用地址"
          >
            <template #append>
              <el-button @click="showAddressSelector('recv')">
                <el-icon><Location /></el-icon>
                选择地址
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="收件人" prop="recvName">
          <el-input v-model="form.recvName" placeholder="请输入收件人姓名" />
        </el-form-item>

        <el-form-item label="收件人电话" prop="recvPhone">
          <el-input v-model="form.recvPhone" placeholder="请输入收件人电话" />
        </el-form-item>

        <!-- 物品信息 -->
        <el-divider content-position="left">物品信息</el-divider>

        <el-form-item label="物品类型" prop="itemType">
          <el-select v-model="form.itemType" placeholder="请选择物品类型" style="width: 100%">
            <el-option label="医疗物资" value="medical" />
            <el-option label="电子产品" value="electronics" />
            <el-option label="日用品" value="daily" />
            <el-option label="食品" value="food" />
            <el-option label="文件资料" value="document" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="重量(kg)" prop="weight">
              <el-input-number
                v-model="form.weight"
                :min="0.1"
                :max="50"
                :step="0.1"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体积(cm³)" prop="volume">
              <el-input v-model="form.volume" placeholder="例如: 30x20x15" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="物品描述">
          <el-input
            v-model="form.itemDesc"
            type="textarea"
            :rows="3"
            placeholder="请描述物品内容、注意事项等"
          />
        </el-form-item>

        <!-- 配送要求 -->
        <el-divider content-position="left">配送要求</el-divider>

        <el-form-item label="配送时效" prop="timeRequirement">
          <el-radio-group v-model="form.timeRequirement">
            <el-radio value="normal">普通配送（2-4小时）</el-radio>
            <el-radio value="urgent">加急配送（1小时内）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="预约配送" v-if="form.timeRequirement === 'normal'">
          <el-date-picker
            v-model="form.appointmentTime"
            type="datetime"
            placeholder="选择预约时间（可选）"
            :disabled-date="disabledDate"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="特殊要求">
          <el-checkbox-group v-model="form.specialRequirements">
            <el-checkbox value="fragile">易碎品</el-checkbox>
            <el-checkbox value="cold">冷链运输</el-checkbox>
            <el-checkbox value="sign">需要签收</el-checkbox>
            <el-checkbox value="photo">送达拍照</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="其他说明"
          />
        </el-form-item>

        <!-- 费用预估 -->
        <el-divider content-position="left">费用预估</el-divider>

        <el-alert
          :title="`预计配送距离：${estimatedDistance} km`"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            <div>预计配送时间：{{ estimatedTime }}</div>
            <div style="color: #909399; font-size: 12px; margin-top: 5px;">
              * 实际配送时间以平台调度后为准
            </div>
          </template>
        </el-alert>

        <!-- 操作按钮 -->
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            <el-icon><Check /></el-icon>
            提交订单
          </el-button>
          <el-button @click="handleSaveDraft">
            <el-icon><Document /></el-icon>
            保存草稿
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
          <el-button @click="handleCancel">
            <el-icon><Close /></el-icon>
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 地址选择对话框 -->
    <el-dialog v-model="addressDialogVisible" title="选择地址" width="600px">
      <el-tabs v-model="addressTab">
        <el-tab-pane label="常用地址" name="saved">
          <el-radio-group v-model="selectedAddress" style="width: 100%">
            <el-radio
              v-for="addr in savedAddresses"
              :key="addr.id"
              :value="addr.address"
              style="display: block; margin: 10px 0; line-height: 30px;"
              border
            >
              <div>
                <strong>{{ addr.label }}</strong>
                <span style="margin-left: 10px; color: #909399;">{{ addr.address }}</span>
              </div>
            </el-radio>
          </el-radio-group>
        </el-tab-pane>
        <el-tab-pane label="地图选点" name="map">
          <div style="height: 400px; background: #f5f7fa; display: flex; align-items: center; justify-content: center;">
            <div style="text-align: center; color: #909399;">
              <el-icon :size="48"><MapLocation /></el-icon>
              <div style="margin-top: 10px;">地图组件待集成</div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddress">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderAPI, airspaceAPI } from '@/api'

/**
 * 下单页面。
 * - 常用地址来自后端场站数据（/api/airspace/stations），带经纬度，可直接用于算距离
 * - 提交走 POST /api/orders，距离/时长/运费由后端按坐标实时计算
 */

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  sendAddr: '',
  sendLat: null,
  sendLng: null,
  recvAddr: '',
  recvLat: null,
  recvLng: null,
  recvName: '',
  recvPhone: '',
  itemType: '',
  weight: 1.0,
  volume: '',
  itemDesc: '',
  timeRequirement: 'normal',
  appointmentTime: '',
  specialRequirements: [],
  remark: ''
})

const rules = {
  sendAddr: [{ required: true, message: '请输入寄件地址', trigger: 'blur' }],
  recvAddr: [{ required: true, message: '请输入收件地址', trigger: 'blur' }],
  recvName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  recvPhone: [
    { required: true, message: '请输入收件人电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  itemType: [{ required: true, message: '请选择物品类型', trigger: 'change' }],
  weight: [{ required: true, message: '请输入重量', trigger: 'blur' }],
  timeRequirement: [{ required: true, message: '请选择配送时效', trigger: 'change' }]
}

// ===== 常用地址（后端场站数据）=====
const addressDialogVisible = ref(false)
const addressTab = ref('saved')
const addressType = ref('send')
const selectedAddress = ref('')
const savedAddresses = ref([])

const loadSavedAddresses = async () => {
  try {
    const stations = (await airspaceAPI.getActiveStations({ silent: true })) || []
    savedAddresses.value = stations.map((s) => ({
      id: s.id,
      label: s.name,
      address: s.address || s.name,
      latitude: s.latitude,
      longitude: s.longitude
    }))
  } catch (error) {
    console.error('加载常用地址失败:', error)
  }
}

const showAddressSelector = (type) => {
  addressType.value = type
  selectedAddress.value = type === 'send' ? form.sendAddr : form.recvAddr
  addressDialogVisible.value = true
}

const confirmAddress = () => {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择地址')
    return
  }
  const matched = savedAddresses.value.find((a) => a.address === selectedAddress.value)
  if (addressType.value === 'send') {
    form.sendAddr = selectedAddress.value
    form.sendLat = matched?.latitude ?? null
    form.sendLng = matched?.longitude ?? null
  } else {
    form.recvAddr = selectedAddress.value
    form.recvLat = matched?.latitude ?? null
    form.recvLng = matched?.longitude ?? null
  }
  addressDialogVisible.value = false
}

// ===== 费用/距离预估（本地按经纬度算，选中场站地址时可用）=====
const haversineKm = (lat1, lng1, lat2, lng2) => {
  const r = 6371
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return Math.round(2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 100) / 100
}

const estimatedDistance = computed(() => {
  if (form.sendLat == null || form.recvLat == null) return '—'
  return haversineKm(form.sendLat, form.sendLng, form.recvLat, form.recvLng)
})

const estimatedFee = computed(() => {
  if (form.sendLat == null || form.recvLat == null) return '—'
  const km = haversineKm(form.sendLat, form.sendLng, form.recvLat, form.recvLng)
  return (5 + km * 12).toFixed(2)
})

const estimatedTime = computed(() => {
  if (form.timeRequirement === 'urgent') return '约30-60分钟'
  return '约2-4小时'
})

// 禁用日期：只能选择未来7天
const disabledDate = (time) => {
  const now = new Date()
  const maxDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)
  return time.getTime() < now.getTime() || time.getTime() > maxDate.getTime()
}

// ===== 提交 =====
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    if (form.sendAddr === form.recvAddr) {
      ElMessage.warning('寄件地址和收件地址不能相同')
      return
    }

    submitting.value = true

    const notes = [
      form.itemDesc ? `物品描述：${form.itemDesc}` : '',
      form.remark ? `备注：${form.remark}` : ''
    ]
      .filter(Boolean)
      .join('；')

    await orderAPI.create({
      sendAddr: form.sendAddr,
      sendLat: form.sendLat,
      sendLng: form.sendLng,
      recvAddr: form.recvAddr,
      recvLat: form.recvLat,
      recvLng: form.recvLng,
      recvName: form.recvName,
      recvPhone: form.recvPhone,
      itemType: form.itemType,
      weight: form.weight,
      volume: Number(form.volume) || null,
      timeRequirement: form.timeRequirement,
      specialRequirements: (form.specialRequirements || []).join(','),
      scheduledTime: form.appointmentTime || null,
      notes
    })

    localStorage.removeItem('orderDraft')
    ElMessage.success('订单提交成功！平台将尽快调度')
    router.push('/order/list')
  } catch (error) {
    if (error !== false) {
      console.error('订单提交失败:', error)
    }
  } finally {
    submitting.value = false
  }
}

// 保存草稿
const handleSaveDraft = () => {
  localStorage.setItem('orderDraft', JSON.stringify(form))
  ElMessage.success('草稿已保存')
}

// 重置表单
const handleReset = () => {
  formRef.value?.resetFields()
  ElMessage.info('表单已重置')
}

// 取消
const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消创建订单吗？未保存的信息将丢失', '提示', {
      type: 'warning'
    })
    router.back()
  } catch (error) {
    // 用户取消
  }
}

// 加载草稿
const loadDraft = () => {
  const draft = localStorage.getItem('orderDraft')
  if (draft) {
    try {
      Object.assign(form, JSON.parse(draft))
      ElMessage.info('已加载草稿')
    } catch (error) {
      console.error('加载草稿失败', error)
    }
  }
}

onMounted(() => {
  loadSavedAddresses()
  loadDraft()
})
</script>

<style scoped>
.order-create {
  padding: 20px;
}

.el-divider {
  margin: 30px 0 20px 0;
}
</style>
