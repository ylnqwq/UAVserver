<template>
  <div class="role-management">
    <el-card shadow="never">
      <el-row :gutter="20">
        <!-- 角色列表 -->
        <el-col :span="9">
          <div class="section-header">
            <span>角色列表</span>
            <el-tag size="small" type="info">角色由后端枚举定义</el-tag>
          </div>
          <el-table
            :data="roles"
            highlight-current-row
            @current-change="handleRoleSelect"
            v-loading="loading"
            border
            style="margin-top: 10px"
          >
            <el-table-column prop="name" label="角色名称" />
            <el-table-column prop="code" label="角色代码" width="130" />
            <el-table-column prop="userCount" label="用户数" width="80" />
          </el-table>
        </el-col>

        <!-- 权限查看 -->
        <el-col :span="15">
          <div class="section-header">
            <span>权限配置 {{ currentRole ? `- ${currentRole.name}` : '' }}</span>
            <el-tag v-if="currentRole" size="small" type="success">
              权限 {{ (currentRole.permissions || []).length }} 项
            </el-tag>
          </div>

          <el-alert
            v-if="!currentRole"
            title="请在左侧选择一个角色查看其权限"
            type="info"
            :closable="false"
            style="margin-top: 10px"
          />

          <template v-else>
            <el-alert
              :title="currentRole.description || '权限由后端角色定义（内置），前端仅做展示'"
              type="warning"
              :closable="false"
              style="margin: 10px 0"
            />

            <el-tree
              ref="permissionTreeRef"
              :data="permissionTree"
              :props="treeProps"
              show-checkbox
              node-key="id"
              :default-checked-keys="currentRole.permissions || []"
              :default-expand-all="true"
            >
              <template #default="{ node, data }">
                <span class="tree-node">{{ node.label }}</span>
              </template>
            </el-tree>
          </template>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 角色权限页面。
 *
 * 角色不是数据库表，而是后端的 UserRole 枚举；权限也是后端内置定义。
 * 本页面的角色列表、用户数、权限目录全部来自后端接口：
 *   GET /api/roles             角色（含用户数、内置权限）
 *   GET /api/roles/permissions 权限目录树
 * 由于角色/权限改不了（改代码才能改），这里只做展示，不再提供"新增/编辑/保存"这类假操作。
 */
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { roleAPI } from '@/api'

const loading = ref(false)
const roles = ref([])
const permissionTree = ref([])
const currentRole = ref(null)
const permissionTreeRef = ref(null)

const treeProps = { children: 'children', label: 'label' }

// 权限节点全部置为 disabled：权限由后端定义，前端不可编辑
const disableNodes = (nodes) =>
  (nodes || []).map((node) => ({
    ...node,
    disabled: true,
    children: node.children ? disableNodes(node.children) : undefined
  }))

const loadPermissions = async () => {
  try {
    const tree = (await roleAPI.getPermissions()) || []
    permissionTree.value = disableNodes(tree)
  } catch (error) {
    console.error('加载权限目录失败:', error)
  }
}

const loadRoles = async () => {
  loading.value = true
  try {
    roles.value = (await roleAPI.getList()) || []
    if (roles.value.length) {
      handleRoleSelect(roles.value[0])
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

const handleRoleSelect = async (row) => {
  currentRole.value = row
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(row?.permissions || [])
}

onMounted(() => {
  loadPermissions()
  loadRoles()
})
</script>

<style scoped>
.role-management {
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.tree-node {
  font-size: 13px;
}
</style>
