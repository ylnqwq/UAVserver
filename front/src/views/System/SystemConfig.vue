<template>
  <div class="system-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">系统基础配置</span>
          <el-button type="primary" @click="handleSaveAll" :loading="saving">
            <el-icon><Check /></el-icon>
            保存全部配置
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" tab-position="left">
        <!-- 基础设置 -->
        <el-tab-pane label="基础设置" name="basic">
          <el-form :model="basicConfig" label-width="180px" class="config-form">
            <h3>平台信息</h3>
            <el-form-item label="平台名称">
              <el-input v-model="basicConfig.platformName" placeholder="天枢·低空智能运营管控平台" />
            </el-form-item>
            <el-form-item label="平台简称">
              <el-input v-model="basicConfig.platformShortName" placeholder="天枢平台" />
            </el-form-item>
            <el-form-item label="平台Logo">
              <el-upload
                class="logo-uploader"
                action="/api/upload"
                :show-file-list="false"
                :on-success="handleLogoSuccess"
              >
                <img v-if="basicConfig.logoUrl" :src="basicConfig.logoUrl" class="logo" />
                <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="备案号">
              <el-input v-model="basicConfig.icp" placeholder="京ICP备XXXXXXXX号" />
            </el-form-item>
            <el-form-item label="版权信息">
              <el-input v-model="basicConfig.copyright" placeholder="© 2024 天枢科技" />
            </el-form-item>

            <el-divider />
            <h3>联系方式</h3>
            <el-form-item label="客服电话">
              <el-input v-model="basicConfig.servicePhone" placeholder="400-123-4567" />
            </el-form-item>
            <el-form-item label="客服邮箱">
              <el-input v-model="basicConfig.serviceEmail" placeholder="service@uav.com" />
            </el-form-item>
            <el-form-item label="公司地址">
              <el-input v-model="basicConfig.address" placeholder="北京市朝阳区xxx" />
            </el-form-item>

            <el-divider />
            <h3>运营时间</h3>
            <el-form-item label="客服工作时间">
              <el-time-picker
                v-model="basicConfig.serviceTime"
                is-range
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                format="HH:mm"
                value-format="HH:mm"
              />
            </el-form-item>
            <el-form-item label="维护时间窗口">
              <el-time-picker
                v-model="basicConfig.maintenanceWindow"
                is-range
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                format="HH:mm"
                value-format="HH:mm"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 业务参数 -->
        <el-tab-pane label="业务参数" name="business">
          <el-form :model="businessConfig" label-width="180px" class="config-form">
            <h3>订单配置</h3>
            <el-form-item label="订单超时时间(秒)">
              <el-input-number v-model="businessConfig.orderTimeout" :min="300" :max="7200" :step="60" />
              <span class="config-tip">订单未接单自动取消时间</span>
            </el-form-item>
            <el-form-item label="订单自动确认时间(秒)">
              <el-input-number v-model="businessConfig.orderAutoConfirm" :min="0" :max="86400" :step="60" />
              <span class="config-tip">订单完成后自动确认时间，0表示立即确认</span>
            </el-form-item>
            <el-form-item label="允许取消时间(分钟)">
              <el-input-number v-model="businessConfig.cancelAllowTime" :min="5" :max="60" :step="5" />
              <span class="config-tip">订单创建后多长时间内允许取消</span>
            </el-form-item>

            <el-divider />
            <h3>飞行计划配置</h3>
            <el-form-item label="计划提前申报时间(小时)">
              <el-input-number v-model="businessConfig.planAdvanceTime" :min="1" :max="72" :step="1" />
              <span class="config-tip">需要提前多久申报飞行计划</span>
            </el-form-item>
            <el-form-item label="计划审批超时时间(小时)">
              <el-input-number v-model="businessConfig.planApprovalTimeout" :min="1" :max="24" :step="1" />
              <span class="config-tip">审批超时自动驳回</span>
            </el-form-item>
            <el-form-item label="自动审批">
              <el-switch v-model="businessConfig.autoApproval" />
              <span class="config-tip">符合条件的计划自动审批通过</span>
            </el-form-item>

            <el-divider />
            <h3>资源配置</h3>
            <el-form-item label="飞行器低电量阈值(%)">
              <el-input-number v-model="businessConfig.lowBatteryThreshold" :min="10" :max="50" :step="5" />
              <span class="config-tip">低于此值发出告警</span>
            </el-form-item>
            <el-form-item label="维保提醒提前天数">
              <el-input-number v-model="businessConfig.maintenanceRemindDays" :min="1" :max="30" :step="1" />
              <span class="config-tip">维保到期前提醒</span>
            </el-form-item>
            <el-form-item label="资质到期提醒天数">
              <el-input-number v-model="businessConfig.qualificationRemindDays" :min="7" :max="90" :step="7" />
              <span class="config-tip">资质到期前提醒</span>
            </el-form-item>

            <el-divider />
            <h3>空域配置</h3>
            <el-form-item label="轨迹偏离告警距离(米)">
              <el-input-number v-model="businessConfig.trajectoryDeviationDistance" :min="50" :max="500" :step="50" />
              <span class="config-tip">偏离航线超过此距离告警</span>
            </el-form-item>
            <el-form-item label="禁飞区缓冲距离(米)">
              <el-input-number v-model="businessConfig.noFlyZoneBuffer" :min="50" :max="500" :step="50" />
              <span class="config-tip">接近禁飞区多少米开始预警</span>
            </el-form-item>
            <el-form-item label="最大飞行高度(米)">
              <el-input-number v-model="businessConfig.maxFlightAltitude" :min="100" :max="500" :step="10" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 安全设置 -->
        <el-tab-pane label="安全设置" name="security">
          <el-form :model="securityConfig" label-width="180px" class="config-form">
            <h3>登录安全</h3>
            <el-form-item label="密码最小长度">
              <el-input-number v-model="securityConfig.passwordMinLength" :min="6" :max="20" />
            </el-form-item>
            <el-form-item label="密码强度要求">
              <el-checkbox-group v-model="securityConfig.passwordRequirements">
                <el-checkbox label="必须包含数字" value="number" />
                <el-checkbox label="必须包含字母" value="letter" />
                <el-checkbox label="必须包含特殊字符" value="special" />
                <el-checkbox label="必须包含大写字母" value="uppercase" />
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="密码过期天数">
              <el-input-number v-model="securityConfig.passwordExpireDays" :min="0" :max="365" :step="30" />
              <span class="config-tip">0表示永不过期</span>
            </el-form-item>
            <el-form-item label="登录失败锁定次数">
              <el-input-number v-model="securityConfig.loginFailLockCount" :min="3" :max="10" />
            </el-form-item>
            <el-form-item label="账号锁定时长(分钟)">
              <el-input-number v-model="securityConfig.accountLockDuration" :min="5" :max="120" :step="5" />
            </el-form-item>

            <el-divider />
            <h3>会话管理</h3>
            <el-form-item label="会话超时时间(分钟)">
              <el-input-number v-model="securityConfig.sessionTimeout" :min="10" :max="480" :step="10" />
            </el-form-item>
            <el-form-item label="是否允许多端登录">
              <el-switch v-model="securityConfig.allowMultiLogin" />
            </el-form-item>
            <el-form-item label="强制登出所有用户">
              <el-button type="danger" @click="handleForceLogout">强制登出</el-button>
              <span class="config-tip">清除所有用户会话，需要重新登录</span>
            </el-form-item>

            <el-divider />
            <h3>数据安全</h3>
            <el-form-item label="敏感数据脱敏">
              <el-switch v-model="securityConfig.dataMasking" />
              <span class="config-tip">手机号、身份证等敏感信息自动脱敏</span>
            </el-form-item>
            <el-form-item label="操作日志保留天数">
              <el-input-number v-model="securityConfig.auditLogRetentionDays" :min="30" :max="365" :step="30" />
            </el-form-item>
            <el-form-item label="自动备份">
              <el-switch v-model="securityConfig.autoBackup" />
            </el-form-item>
            <el-form-item label="备份频率" v-if="securityConfig.autoBackup">
              <el-radio-group v-model="securityConfig.backupFrequency">
                <el-radio value="daily">每日</el-radio>
                <el-radio value="weekly">每周</el-radio>
                <el-radio value="monthly">每月</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 通知设置 -->
        <el-tab-pane label="通知设置" name="notification">
          <el-form :model="notificationConfig" label-width="180px" class="config-form">
            <h3>短信通知</h3>
            <el-form-item label="启用短信通知">
              <el-switch v-model="notificationConfig.smsEnabled" />
            </el-form-item>
            <el-form-item label="短信服务商" v-if="notificationConfig.smsEnabled">
              <el-select v-model="notificationConfig.smsProvider" style="width: 300px">
                <el-option label="阿里云" value="aliyun" />
                <el-option label="腾讯云" value="tencent" />
                <el-option label="华为云" value="huawei" />
              </el-select>
            </el-form-item>
            <el-form-item label="AccessKey" v-if="notificationConfig.smsEnabled">
              <el-input v-model="notificationConfig.smsAccessKey" type="password" show-password style="width: 400px" />
            </el-form-item>
            <el-form-item label="SecretKey" v-if="notificationConfig.smsEnabled">
              <el-input v-model="notificationConfig.smsSecretKey" type="password" show-password style="width: 400px" />
            </el-form-item>
            <el-form-item label="短信签名" v-if="notificationConfig.smsEnabled">
              <el-input v-model="notificationConfig.smsSignature" style="width: 200px" />
            </el-form-item>

            <el-divider />
            <h3>邮件通知</h3>
            <el-form-item label="启用邮件通知">
              <el-switch v-model="notificationConfig.emailEnabled" />
            </el-form-item>
            <el-form-item label="SMTP服务器" v-if="notificationConfig.emailEnabled">
              <el-input v-model="notificationConfig.smtpHost" placeholder="smtp.example.com" style="width: 300px" />
            </el-form-item>
            <el-form-item label="SMTP端口" v-if="notificationConfig.emailEnabled">
              <el-input-number v-model="notificationConfig.smtpPort" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="发件人邮箱" v-if="notificationConfig.emailEnabled">
              <el-input v-model="notificationConfig.emailFrom" placeholder="noreply@example.com" style="width: 300px" />
            </el-form-item>
            <el-form-item label="邮箱密码" v-if="notificationConfig.emailEnabled">
              <el-input v-model="notificationConfig.emailPassword" type="password" show-password style="width: 300px" />
            </el-form-item>
            <el-form-item label="使用SSL" v-if="notificationConfig.emailEnabled">
              <el-switch v-model="notificationConfig.emailSsl" />
            </el-form-item>

            <el-divider />
            <h3>通知场景</h3>
            <el-form-item label="订单状态变更通知">
              <el-checkbox-group v-model="notificationConfig.orderNotifications">
                <el-checkbox label="短信" value="sms" />
                <el-checkbox label="邮件" value="email" />
                <el-checkbox label="站内信" value="internal" />
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="飞行计划审批通知">
              <el-checkbox-group v-model="notificationConfig.flightNotifications">
                <el-checkbox label="短信" value="sms" />
                <el-checkbox label="邮件" value="email" />
                <el-checkbox label="站内信" value="internal" />
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="告警通知">
              <el-checkbox-group v-model="notificationConfig.alertNotifications">
                <el-checkbox label="短信" value="sms" />
                <el-checkbox label="邮件" value="email" />
                <el-checkbox label="站内信" value="internal" />
              </el-checkbox-group>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 系统性能 -->
        <el-tab-pane label="系统性能" name="performance">
          <el-form :model="performanceConfig" label-width="180px" class="config-form">
            <h3>缓存设置</h3>
            <el-form-item label="启用Redis缓存">
              <el-switch v-model="performanceConfig.redisCacheEnabled" />
            </el-form-item>
            <el-form-item label="缓存过期时间(秒)">
              <el-input-number v-model="performanceConfig.cacheExpireTime" :min="60" :max="86400" :step="60" />
            </el-form-item>
            <el-form-item label="清除全部缓存">
              <el-button type="warning" @click="handleClearCache">清除缓存</el-button>
            </el-form-item>

            <el-divider />
            <h3>并发控制</h3>
            <el-form-item label="API请求限流">
              <el-switch v-model="performanceConfig.rateLimitEnabled" />
            </el-form-item>
            <el-form-item label="每秒最大请求数" v-if="performanceConfig.rateLimitEnabled">
              <el-input-number v-model="performanceConfig.maxRequestPerSecond" :min="10" :max="1000" :step="10" />
            </el-form-item>
            <el-form-item label="每用户并发连接数">
              <el-input-number v-model="performanceConfig.maxConcurrentConnections" :min="1" :max="100" :step="1" />
            </el-form-item>

            <el-divider />
            <h3>日志设置</h3>
            <el-form-item label="日志级别">
              <el-select v-model="performanceConfig.logLevel" style="width: 150px">
                <el-option label="ERROR" value="error" />
                <el-option label="WARN" value="warn" />
                <el-option label="INFO" value="info" />
                <el-option label="DEBUG" value="debug" />
              </el-select>
            </el-form-item>
            <el-form-item label="记录慢查询">
              <el-switch v-model="performanceConfig.slowQueryLog" />
            </el-form-item>
            <el-form-item label="慢查询阈值(毫秒)" v-if="performanceConfig.slowQueryLog">
              <el-input-number v-model="performanceConfig.slowQueryThreshold" :min="100" :max="5000" :step="100" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Plus } from '@element-plus/icons-vue'

// 当前标签页
const activeTab = ref('basic')
const saving = ref(false)

// 基础配置
const basicConfig = reactive({
  platformName: '天枢·低空智能运营管控平台',
  platformShortName: '天枢平台',
  logoUrl: '',
  icp: '京ICP备12345678号',
  copyright: '© 2024 天枢科技',
  servicePhone: '400-123-4567',
  serviceEmail: 'service@uav.com',
  address: '北京市朝阳区建国路88号',
  serviceTime: ['09:00', '22:00'],
  maintenanceWindow: ['02:00', '06:00']
})

// 业务配置
const businessConfig = reactive({
  orderTimeout: 3600,
  orderAutoConfirm: 86400,
  cancelAllowTime: 30,
  planAdvanceTime: 24,
  planApprovalTimeout: 4,
  autoApproval: false,
  lowBatteryThreshold: 30,
  maintenanceRemindDays: 7,
  qualificationRemindDays: 30,
  trajectoryDeviationDistance: 100,
  noFlyZoneBuffer: 200,
  maxFlightAltitude: 120
})

// 安全配置
const securityConfig = reactive({
  passwordMinLength: 8,
  passwordRequirements: ['number', 'letter'],
  passwordExpireDays: 90,
  loginFailLockCount: 5,
  accountLockDuration: 30,
  sessionTimeout: 120,
  allowMultiLogin: false,
  dataMasking: true,
  auditLogRetentionDays: 90,
  autoBackup: true,
  backupFrequency: 'daily'
})

// 通知配置
const notificationConfig = reactive({
  smsEnabled: true,
  smsProvider: 'aliyun',
  smsAccessKey: '',
  smsSecretKey: '',
  smsSignature: '天枢平台',
  emailEnabled: true,
  smtpHost: 'smtp.exmail.qq.com',
  smtpPort: 465,
  emailFrom: 'noreply@uav.com',
  emailPassword: '',
  emailSsl: true,
  orderNotifications: ['sms', 'internal'],
  flightNotifications: ['email', 'internal'],
  alertNotifications: ['sms', 'email', 'internal']
})

// 性能配置
const performanceConfig = reactive({
  redisCacheEnabled: true,
  cacheExpireTime: 3600,
  rateLimitEnabled: true,
  maxRequestPerSecond: 100,
  maxConcurrentConnections: 10,
  logLevel: 'info',
  slowQueryLog: true,
  slowQueryThreshold: 1000
})

// Logo上传成功
const handleLogoSuccess = (response, file) => {
  basicConfig.logoUrl = URL.createObjectURL(file.raw)
  ElMessage.success('Logo上传成功')
}

// 保存全部配置
const handleSaveAll = () => {
  ElMessageBox.confirm('确定要保存全部配置吗？部分配置需要重启系统生效。', '保存确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    saving.value = true
    // TODO: 调用API保存配置
    setTimeout(() => {
      ElMessage.success('配置保存成功')
      saving.value = false
    }, 1500)
  })
}

// 强制登出
const handleForceLogout = () => {
  ElMessageBox.confirm('此操作将强制登出所有用户，确定继续吗？', '强制登出', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用API强制登出
    ElMessage.success('所有用户已强制登出')
  })
}

// 清除缓存
const handleClearCache = () => {
  ElMessageBox.confirm('确定要清除全部缓存吗？', '清除缓存', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用API清除缓存
    ElMessage.success('缓存已清除')
  })
}
</script>

<style scoped>
.system-config {
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

.config-form {
  padding: 20px;
  max-width: 800px;
}

.config-form h3 {
  margin: 20px 0 15px 0;
  font-size: 16px;
  color: #303133;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}

.config-form h3:first-child {
  margin-top: 0;
}

.config-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.logo-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-uploader:hover {
  border-color: #409eff;
}

.logo-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.logo {
  width: 178px;
  height: 178px;
  object-fit: contain;
}

:deep(.el-tabs__content) {
  min-height: 600px;
}
</style>
