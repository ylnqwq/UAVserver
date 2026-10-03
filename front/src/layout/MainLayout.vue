<template>
  <el-container class="main-layout">
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <h2>天枢·UAV</h2>
      </div>

      <el-menu
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
      >
        <!-- 工作台 -->
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <!-- 账号管理 -->
        <el-sub-menu index="account" v-if="userStore.userInfo?.role === 'ADMIN'">
          <template #title>
            <el-icon><User /></el-icon>
            <span>账号管理</span>
          </template>
          <el-menu-item index="/account/users">用户管理</el-menu-item>
          <el-menu-item index="/account/roles">角色权限</el-menu-item>
        </el-sub-menu>

        <!-- 订单管理 -->
        <el-sub-menu index="order">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>订单管理</span>
          </template>
          <el-menu-item index="/order/list">订单列表</el-menu-item>
          <el-menu-item index="/order/create" v-if="userStore.userInfo?.role === 'CUSTOMER'">创建订单</el-menu-item>
        </el-sub-menu>

        <!-- 资源管理 -->
        <el-sub-menu index="resource">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>资源管理</span>
          </template>
          <el-menu-item index="/resource/aircraft">飞行器管理</el-menu-item>
          <el-menu-item index="/resource/station">场站管理</el-menu-item>
          <el-menu-item index="/resource/personnel">人员管理</el-menu-item>
        </el-sub-menu>

        <!-- 空域合规 -->
        <el-sub-menu index="airspace">
          <template #title>
            <el-icon><MapLocation /></el-icon>
            <span>空域合规</span>
          </template>
          <el-menu-item index="/airspace/map">空域地图</el-menu-item>
          <el-menu-item index="/airspace/list">空域管理</el-menu-item>
          <el-menu-item index="/airspace/flight-plan/list">飞行计划</el-menu-item>
          <el-menu-item index="/airspace/flight-plan/create" v-if="['ADMIN', 'PILOT'].includes(userStore.userInfo?.role)">申报计划</el-menu-item>
          <el-menu-item index="/airspace/flight-plan/approval" v-if="['ADMIN', 'AIR_CONTROL'].includes(userStore.userInfo?.role)">计划审批</el-menu-item>
          <el-menu-item index="/airspace/violation">违规处理</el-menu-item>
        </el-sub-menu>

        <!-- 应急客服 -->
        <el-sub-menu index="emergency">
          <template #title>
            <el-icon><Bell /></el-icon>
            <span>应急客服</span>
          </template>
          <el-menu-item index="/emergency/alert">应急告警</el-menu-item>
          <el-menu-item index="/emergency/workorder">客服工单</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ currentPageTitle }}</span>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.username }}</span>
              <el-tag size="small" :type="getRoleType()">{{ getRoleLabel() }}</el-tag>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="changePwd">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => route.meta.title || '工作台')

// 默认展开的子菜单
const defaultOpeneds = ['account', 'order', 'resource', 'airspace', 'statistics', 'emergency']

const getRoleLabel = () => {
  const roleMap = {
    ADMIN: '管理员',
    CUSTOMER: '客户',
    AIR_CONTROL: '空管',
    OPERATOR: '运维',
    MAINTENANCE: '维护',
    PUBLIC: '公众'
  }
  return roleMap[userStore.userInfo?.role] || '未知'
}

const getRoleType = () => {
  const typeMap = {
    ADMIN: 'danger',
    CUSTOMER: 'success',
    AIR_CONTROL: 'primary',
    OPERATOR: 'info',
    MAINTENANCE: 'warning',
    PUBLIC: ''
  }
  return typeMap[userStore.userInfo?.role] || ''
}

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人信息功能开发中')
      break
    case 'changePwd':
      ElMessage.info('修改密码功能开发中')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        userStore.logout()
        router.push('/login')
        ElMessage.success('已退出登录')
      } catch {
        // 取消操作
      }
      break
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background: #001529;
  overflow-y: auto;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  margin: 0;
  font-size: 20px;
}

.header {
  background: white;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.page-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}

.main-content {
  background: #f0f2f5;
  padding: 24px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
