<template>
  <div class="flight-plan-create">
    <el-card>
      <template #header>
        <span>创建飞行计划</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计划名称" prop="planName">
              <el-input v-model="form.planName" placeholder="请输入计划名称" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="飞行目的" prop="purpose">
              <el-select v-model="form.purpose" placeholder="请选择飞行目的">
                <el-option label="商业飞行" value="COMMERCIAL" />
                <el-option label="测绘勘察" value="SURVEY" />
                <el-option label="巡检" value="INSPECTION" />
                <el-option label="配送" value="DELIVERY" />
                <el-option label="航拍" value="PHOTOGRAPHY" />
                <el-option label="农业植保" value="AGRICULTURE" />
                <el-option label="应急救援" value="EMERGENCY" />
                <el-option label="训练" value="TRAINING" />
                <el-option label="科研" value="RESEARCH" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="飞行器" prop="aircraftId">
              <el-select v-model="form.aircraftId" placeholder="请选择飞行器" @focus="loadAircraft">
                <el-option
                  v-for="item in aircraftList"
                  :key="item.id"
                  :label="`${item.name} (${item.registrationNumber})`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="飞行员" prop="pilotId">
              <el-select v-model="form.pilotId" placeholder="请选择飞行员" @focus="loadPilots">
                <el-option
                  v-for="item in pilotList"
                  :key="item.id"
                  :label="item.realName || item.username"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计划起飞时间" prop="plannedStartTime">
              <el-date-picker
                v-model="form.plannedStartTime"
                type="datetime"
                placeholder="选择起飞时间"
                style="width: 100%"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="计划降落时间" prop="plannedEndTime">
              <el-date-picker
                v-model="form.plannedEndTime"
                type="datetime"
                placeholder="选择降落时间"
                style="width: 100%"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="起飞地点" prop="departureLocation">
              <el-input v-model="form.departureLocation" placeholder="请输入起飞地点" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="降落地点" prop="arrivalLocation">
              <el-input v-model="form.arrivalLocation" placeholder="请输入降落地点" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计划飞行高度">
              <el-input v-model.number="form.plannedAltitude" type="number">
                <template #append>米</template>
              </el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="天气条件">
              <el-input v-model="form.weatherConditions" placeholder="晴天、多云等" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="form.emergencyContact" placeholder="请输入联系人姓名" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.emergencyPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="航线描述">
          <el-input
            v-model="form.routeDescription"
            type="textarea"
            :rows="3"
            placeholder="请描述飞行航线"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="3" placeholder="其他备注信息" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { flightPlanAPI, aircraftAPI, userAPI } from '@/api'

const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const aircraftList = ref([])
const pilotList = ref([])

const form = reactive({
  planName: '',
  aircraftId: null,
  pilotId: null,
  purpose: '',
  plannedStartTime: '',
  plannedEndTime: '',
  departureLocation: '',
  arrivalLocation: '',
  plannedAltitude: null,
  routeDescription: '',
  remarks: '',
  weatherConditions: '',
  emergencyContact: '',
  emergencyPhone: ''
})

const rules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  aircraftId: [{ required: true, message: '请选择飞行器', trigger: 'change' }],
  pilotId: [{ required: true, message: '请选择飞行员', trigger: 'change' }],
  purpose: [{ required: true, message: '请选择飞行目的', trigger: 'change' }],
  plannedStartTime: [{ required: true, message: '请选择起飞时间', trigger: 'change' }],
  plannedEndTime: [{ required: true, message: '请选择降落时间', trigger: 'change' }],
  departureLocation: [{ required: true, message: '请输入起飞地点', trigger: 'blur' }],
  arrivalLocation: [{ required: true, message: '请输入降落地点', trigger: 'blur' }]
}

const loadAircraft = async () => {
  if (aircraftList.value.length) return
  try {
    const data = await aircraftAPI.getByStatus('AVAILABLE')
    aircraftList.value = data
  } catch (error) {
    console.error('加载飞行器列表失败:', error)
  }
}

const loadPilots = async () => {
  if (pilotList.value.length) return
  try {
    pilotList.value = await userAPI.getByRole('PILOT')
  } catch (error) {
    console.error('加载飞行员列表失败:', error)
  }
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    await flightPlanAPI.create(form)
    ElMessage.success('创建成功')
    router.push('/airspace/flight-plan/list')
  } catch (error) {
    if (error !== false) {
      console.error('保存失败:', error)
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.flight-plan-create {
  max-width: 1200px;
}
</style>
