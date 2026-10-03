<template>
  <div class="register">
    <!-- 背景装饰 -->
    <div class="bg">
      <span class="bg__orb bg__orb--1"></span>
      <span class="bg__orb bg__orb--2"></span>
      <span class="bg__orb bg__orb--3"></span>
      <span class="bg__grid"></span>
    </div>

    <div class="card">
      <!-- 左侧品牌区 -->
      <aside class="brand">
        <div class="brand__logo">
          <el-icon :size="24"><Promotion /></el-icon>
        </div>
        <h1 class="brand__name">天枢·低空智能运营管控平台</h1>
        <p class="brand__en">Tianshu Low-Altitude Intelligent Operation Control Platform</p>

        <ul class="brand__list">
          <li><el-icon><Position /></el-icon>空域网格与禁飞 / 限飞区可视化管理</li>
          <li><el-icon><Van /></el-icon>无人机订单调度与航线自动避障</li>
          <li><el-icon><Monitor /></el-icon>飞行监管、违规处置与应急告警</li>
          <li><el-icon><DataLine /></el-icon>运营数据统计与多维分析</li>
        </ul>

        <div class="brand__foot">© 2026 Tianshu UAV Platform</div>
      </aside>

      <!-- 右侧注册表单 -->
      <section class="form-area">
        <h2 class="form-area__title">用户注册</h2>
        <p class="form-area__sub">创建新账号以使用平台服务</p>

        <el-form ref="formRef" :model="form" :rules="rules" class="register-form">
          <el-form-item prop="role">
            <el-select v-model="form.role" placeholder="请选择角色" size="large" style="width: 100%">
              <el-option label="客户" value="CUSTOMER" />
              <el-option label="机长" value="PILOT" />
              <el-option label="运营人员" value="OPERATOR" />
              <el-option label="空管人员" value="ATC" />
              <el-option label="监管人员" value="SUPERVISOR" />
            </el-select>
          </el-form-item>

          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" prefix-icon="User" clearable />
          </el-form-item>

          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" size="large" prefix-icon="Message" clearable />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请确认密码"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="submit" @click="handleRegister">
              注 册
            </el-button>
          </el-form-item>

          <div class="link-area">
            已有账号？<router-link to="/login" class="link">立即登录</router-link>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authAPI } from '@/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  role: '',
  password: '',
  confirmPassword: ''
})

const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度不能少于6位'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const handleRegister = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    await authAPI.register({
      username: form.username,
      email: form.email,
      role: form.role,
      password: form.password
    })

    ElMessage.success('注册成功！请登录')
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (error) {
    if (error !== false) {
      console.error('注册失败:', error)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: radial-gradient(1200px 600px at 15% 15%, #2c5fd0 0%, #1b3a86 45%, #0d1b3e 100%);
}

/* ===== 背景装饰 ===== */
.bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.55;
  animation: float 14s ease-in-out infinite;
}

.bg__orb--1 {
  width: 320px;
  height: 320px;
  left: -80px;
  top: -60px;
  background: radial-gradient(circle at 30% 30%, rgba(93, 169, 255, 0.85), rgba(93, 169, 255, 0));
}

.bg__orb--2 {
  right: -60px;
  bottom: -90px;
  width: 420px;
  height: 420px;
  animation-delay: -4s;
  background: radial-gradient(circle at 60% 40%, rgba(56, 214, 197, 0.5), rgba(56, 214, 197, 0));
}

.bg__orb--3 {
  right: 18%;
  top: 8%;
  width: 200px;
  height: 200px;
  animation-delay: -8s;
  background: radial-gradient(circle at 50% 50%, rgba(142, 124, 240, 0.55), rgba(142, 124, 240, 0));
}

.bg__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(circle at 50% 50%, #000 35%, transparent 78%);
  -webkit-mask-image: radial-gradient(circle at 50% 50%, #000 35%, transparent 78%);
}

@keyframes float {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, -18px, 0) scale(1.05);
  }
}

/* ===== 卡片 ===== */
.card {
  position: relative;
  z-index: 1;
  display: flex;
  width: 900px;
  max-width: calc(100vw - 40px);
  min-height: 520px;
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 24px 60px rgba(6, 18, 45, 0.42);
  animation: rise 0.55s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 左侧品牌区 ===== */
.brand {
  position: relative;
  width: 46%;
  padding: 40px 34px;
  color: #fff;
  background: linear-gradient(150deg, #2f6fed 0%, #2b56c4 55%, #1f3f9c 100%);
  display: flex;
  flex-direction: column;
}

.brand::after {
  content: '';
  position: absolute;
  right: -70px;
  bottom: -90px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.brand__logo {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.32);
  margin-bottom: 22px;
}

.brand__name {
  font-size: 21px;
  font-weight: 700;
  line-height: 1.45;
  margin: 0 0 8px;
  letter-spacing: 0.4px;
}

.brand__en {
  font-size: 12px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 26px;
}

.brand__list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex: 1;
}

.brand__list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.92);
  padding: 9px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.16);
}

.brand__list li:last-child {
  border-bottom: none;
}

.brand__list .el-icon {
  color: #a9d4ff;
}

.brand__foot {
  position: relative;
  z-index: 1;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 18px;
}

/* ===== 右侧表单 ===== */
.form-area {
  flex: 1;
  padding: 44px 42px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-area__title {
  font-size: 24px;
  font-weight: 700;
  color: #1c2b45;
  margin: 0 0 6px;
}

.form-area__sub {
  font-size: 13px;
  color: #93a0b4;
  margin: 0 0 20px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.register-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e3eaf5 inset;
  padding: 4px 12px;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2f6fed inset, 0 4px 12px rgba(47, 111, 237, 0.14);
}

.submit {
  width: 100%;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(120deg, #2f6fed, #4a8bf5);
  box-shadow: 0 10px 20px rgba(47, 111, 237, 0.28);
}

.submit:hover {
  background: linear-gradient(120deg, #2a63d6, #3f7fe8);
}

.link-area {
  text-align: center;
  font-size: 13px;
  color: #93a0b4;
  margin-top: 16px;
}

.link {
  color: #2f6fed;
  text-decoration: none;
  font-weight: 500;
}

.link:hover {
  text-decoration: underline;
}

/* ===== 窄屏：隐藏左侧品牌区 ===== */
@media (max-width: 820px) {
  .card {
    width: 94vw;
    flex-direction: column;
  }

  .brand {
    width: auto;
    padding: 26px 24px;
  }

  .brand__list,
  .brand__foot {
    display: none;
  }

  .form-area {
    padding: 30px 24px 26px;
  }
}
</style>
