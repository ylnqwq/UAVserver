<template>
  <div class="help-center">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">帮助中心</span>
          <el-button type="primary" @click="handleAddArticle" v-if="isAdmin">
            <el-icon><Plus /></el-icon>
            添加文章
          </el-button>
        </div>
      </template>

      <div class="help-container">
        <!-- 左侧分类导航 -->
        <div class="category-sidebar">
          <el-menu
            :default-active="activeCategory"
            @select="handleCategorySelect"
            class="category-menu"
          >
            <el-menu-item index="all">
              <el-icon><Menu /></el-icon>
              <span>全部文章</span>
            </el-menu-item>
            <el-menu-item index="getting-started">
              <el-icon><Star /></el-icon>
              <span>新手入门</span>
            </el-menu-item>
            <el-menu-item index="order">
              <el-icon><Document /></el-icon>
              <span>订单相关</span>
            </el-menu-item>
            <el-menu-item index="flight">
              <el-icon><Promotion /></el-icon>
              <span>飞行计划</span>
            </el-menu-item>
            <el-menu-item index="resource">
              <el-icon><Goods /></el-icon>
              <span>资源管理</span>
            </el-menu-item>
            <el-menu-item index="airspace">
              <el-icon><MapLocation /></el-icon>
              <span>空域规则</span>
            </el-menu-item>
            <el-menu-item index="payment">
              <el-icon><Wallet /></el-icon>
              <span>支付结算</span>
            </el-menu-item>
            <el-menu-item index="safety">
              <el-icon><Lock /></el-icon>
              <span>安全须知</span>
            </el-menu-item>
            <el-menu-item index="faq">
              <el-icon><QuestionFilled /></el-icon>
              <span>常见问题</span>
            </el-menu-item>
          </el-menu>

          <!-- 快捷联系 -->
          <el-card class="contact-card" shadow="hover">
            <div class="contact-item">
              <el-icon><Phone /></el-icon>
              <div>
                <div class="contact-label">客服热线</div>
                <div class="contact-value">400-123-4567</div>
              </div>
            </div>
            <el-divider />
            <div class="contact-item">
              <el-icon><Message /></el-icon>
              <div>
                <div class="contact-label">在线客服</div>
                <div class="contact-value">9:00-22:00</div>
              </div>
            </div>
            <el-button type="primary" style="width: 100%; margin-top: 15px" @click="handleContactService">
              联系客服
            </el-button>
          </el-card>
        </div>

        <!-- 右侧内容区 -->
        <div class="content-area">
          <!-- 搜索框 -->
          <div class="search-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索帮助文章..."
              size="large"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #append>
                <el-button @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </div>

          <!-- 热门文章 -->
          <div class="hot-articles" v-if="activeCategory === 'all'">
            <h3>
              <el-icon><Sunny /></el-icon>
              热门文章
            </h3>
            <div class="hot-list">
              <div
                class="hot-item"
                v-for="article in hotArticles"
                :key="article.id"
                @click="handleViewArticle(article)"
              >
                <span class="hot-rank">{{ article.rank }}</span>
                <span class="hot-title">{{ article.title }}</span>
                <span class="hot-views">{{ article.views }} 次阅读</span>
              </div>
            </div>
          </div>

          <!-- 文章列表 -->
          <div class="article-list">
            <h3 v-if="activeCategory !== 'all'">
              {{ getCategoryName(activeCategory) }}
            </h3>
            <el-empty v-if="articleList.length === 0" description="暂无文章" />
            <div
              class="article-item"
              v-for="article in articleList"
              :key="article.id"
              @click="handleViewArticle(article)"
            >
              <div class="article-header">
                <h4>{{ article.title }}</h4>
                <el-tag v-if="article.isNew" type="danger" size="small">NEW</el-tag>
              </div>
              <div class="article-summary">{{ article.summary }}</div>
              <div class="article-footer">
                <span class="article-meta">
                  <el-icon><View /></el-icon>
                  {{ article.views }}
                </span>
                <span class="article-meta">
                  <el-icon><Star /></el-icon>
                  {{ article.likes }}
                </span>
                <span class="article-meta">
                  <el-icon><Clock /></el-icon>
                  {{ article.updateTime }}
                </span>
                <div class="article-actions" v-if="isAdmin">
                  <el-button link type="primary" size="small" @click.stop="handleEditArticle(article)">
                    编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click.stop="handleDeleteArticle(article)">
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-if="articleList.length > 0"
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
            style="margin-top: 20px; justify-content: center"
          />
        </div>
      </div>
    </el-card>

    <!-- 文章详情对话框 -->
    <el-dialog
      v-model="articleDetailVisible"
      :title="articleDetail.title"
      width="900px"
      top="5vh"
    >
      <div class="article-detail">
        <div class="article-detail-header">
          <el-tag>{{ getCategoryName(articleDetail.category) }}</el-tag>
          <span class="detail-meta">
            <el-icon><Clock /></el-icon>
            更新于 {{ articleDetail.updateTime }}
          </span>
          <span class="detail-meta">
            <el-icon><View /></el-icon>
            {{ articleDetail.views }} 次阅读
          </span>
        </div>

        <el-divider />

        <div class="article-detail-content" v-html="articleDetail.content"></div>

        <el-divider />

        <div class="article-detail-footer">
          <el-button @click="handleLike" :type="articleDetail.liked ? 'primary' : ''">
            <el-icon><Star /></el-icon>
            {{ articleDetail.liked ? '已赞' : '点赞' }} ({{ articleDetail.likes }})
          </el-button>
          <el-button @click="handleFeedback">
            <el-icon><ChatDotRound /></el-icon>
            反馈
          </el-button>
        </div>

        <!-- 相关文章 -->
        <div class="related-articles" v-if="relatedArticles.length > 0">
          <h4>相关文章</h4>
          <div class="related-list">
            <div
              class="related-item"
              v-for="related in relatedArticles"
              :key="related.id"
              @click="handleViewArticle(related)"
            >
              {{ related.title }}
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="articleDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑文章对话框 -->
    <el-dialog
      v-model="articleFormVisible"
      :title="articleFormTitle"
      width="800px"
      @close="handleArticleFormClose"
    >
      <el-form :model="articleForm" :rules="articleRules" ref="articleFormRef" label-width="100px">
        <el-form-item label="文章标题" prop="title">
          <el-input v-model="articleForm.title" placeholder="请输入文章标题" />
        </el-form-item>
        <el-form-item label="所属分类" prop="category">
          <el-select v-model="articleForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="新手入门" value="getting-started" />
            <el-option label="订单相关" value="order" />
            <el-option label="飞行计划" value="flight" />
            <el-option label="资源管理" value="resource" />
            <el-option label="空域规则" value="airspace" />
            <el-option label="支付结算" value="payment" />
            <el-option label="安全须知" value="safety" />
            <el-option label="常见问题" value="faq" />
          </el-select>
        </el-form-item>
        <el-form-item label="文章摘要" prop="summary">
          <el-input
            v-model="articleForm.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入文章摘要"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="文章内容" prop="content">
          <el-input
            v-model="articleForm.content"
            type="textarea"
            :rows="12"
            placeholder="请输入文章内容（支持HTML）"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-tag
            v-for="tag in articleForm.tags"
            :key="tag"
            closable
            @close="handleRemoveTag(tag)"
            style="margin-right: 10px"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="tagInputVisible"
            ref="tagInputRef"
            v-model="tagInputValue"
            size="small"
            style="width: 100px"
            @blur="handleTagInputConfirm"
            @keyup.enter="handleTagInputConfirm"
          />
          <el-button v-else size="small" @click="showTagInput">+ 添加标签</el-button>
        </el-form-item>
        <el-form-item label="是否新文章">
          <el-switch v-model="articleForm.isNew" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="articleFormVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitArticle" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 反馈对话框 -->
    <el-dialog v-model="feedbackVisible" title="文章反馈" width="500px">
      <el-form :model="feedbackForm" label-width="80px">
        <el-form-item label="反馈类型">
          <el-radio-group v-model="feedbackForm.type">
            <el-radio value="helpful">有帮助</el-radio>
            <el-radio value="not-helpful">没帮助</el-radio>
            <el-radio value="error">内容错误</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入您的反馈"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitFeedback">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Menu,
  Star,
  Document,
  Promotion,
  Goods,
  MapLocation,
  Wallet,
  Lock,
  QuestionFilled,
  Phone,
  Message,
  Search,
  Sunny,
  View,
  Clock,
  ChatDotRound
} from '@element-plus/icons-vue'

// 是否管理员
const isAdmin = ref(true)

// 当前分类
const activeCategory = ref('all')

// 搜索关键词
const searchKeyword = ref('')

// 热门文章
const hotArticles = ref([
  { id: 1, rank: 1, title: '如何创建第一个订单', views: 3256 },
  { id: 2, rank: 2, title: '飞行计划申报流程详解', views: 2845 },
  { id: 3, rank: 3, title: '空域规则和禁飞区说明', views: 2634 },
  { id: 4, rank: 4, title: '如何进行资质认证', views: 2128 },
  { id: 5, rank: 5, title: '订单调度最佳实践', views: 1956 }
])

// 文章列表
const articleList = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 文章详情
const articleDetailVisible = ref(false)
const articleDetail = ref({})
const relatedArticles = ref([])

// 文章表单
const articleFormVisible = ref(false)
const articleFormTitle = ref('添加文章')
const articleForm = reactive({
  id: null,
  title: '',
  category: '',
  summary: '',
  content: '',
  tags: [],
  isNew: false
})
const articleRules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  summary: [{ required: true, message: '请输入文章摘要', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文章内容', trigger: 'blur' }]
}
const articleFormRef = ref(null)
const submitting = ref(false)

// 标签输入
const tagInputVisible = ref(false)
const tagInputValue = ref('')
const tagInputRef = ref(null)

// 反馈
const feedbackVisible = ref(false)
const feedbackForm = reactive({
  type: 'helpful',
  content: ''
})

// 初始化
onMounted(() => {
  fetchArticleList()
})

// 获取文章列表
const fetchArticleList = () => {
  // TODO: 调用实际API
  setTimeout(() => {
    articleList.value = [
      {
        id: 1,
        title: '如何创建第一个订单',
        category: 'getting-started',
        summary: '本文详细介绍了如何在平台上创建您的第一个配送订单，包括填写寄收地址、选择配送时效等步骤。',
        content: '<h3>第一步：登录平台</h3><p>使用您的账号登录天枢平台...</p>',
        views: 3256,
        likes: 245,
        liked: false,
        isNew: true,
        updateTime: '2026-09-15 10:30'
      },
      {
        id: 2,
        title: '飞行计划申报流程详解',
        category: 'flight',
        summary: '详细说明飞行计划的申报流程、所需材料、审批时间以及注意事项。',
        content: '<h3>飞行计划申报</h3><p>申报飞行计划需要提前...</p>',
        views: 2845,
        likes: 198,
        liked: false,
        isNew: false,
        updateTime: '2026-09-14 15:20'
      },
      {
        id: 3,
        title: '空域规则和禁飞区说明',
        category: 'airspace',
        summary: '了解北京地区的空域规则、禁飞区范围、限飞区要求，确保合规飞行。',
        content: '<h3>空域分类</h3><p>空域分为禁飞区、限飞区...</p>',
        views: 2634,
        likes: 175,
        liked: false,
        isNew: false,
        updateTime: '2026-09-13 09:45'
      }
    ]
    pagination.total = 28
  }, 300)
}

// 分类选择
const handleCategorySelect = (key) => {
  activeCategory.value = key
  pagination.page = 1
  fetchArticleList()
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchArticleList()
  ElMessage.success(`搜索：${searchKeyword.value}`)
}

// 查看文章
const handleViewArticle = (article) => {
  articleDetail.value = { ...article }
  relatedArticles.value = [
    { id: 10, title: '订单状态说明' },
    { id: 11, title: '配送时效选择' }
  ]
  articleDetailVisible.value = true

  // 增加阅读量
  article.views++
}

// 点赞
const handleLike = () => {
  articleDetail.value.liked = !articleDetail.value.liked
  if (articleDetail.value.liked) {
    articleDetail.value.likes++
    ElMessage.success('感谢点赞')
  } else {
    articleDetail.value.likes--
  }
}

// 反馈
const handleFeedback = () => {
  feedbackVisible.value = true
}

// 提交反馈
const handleSubmitFeedback = () => {
  ElMessage.success('感谢您的反馈')
  feedbackVisible.value = false
  feedbackForm.type = 'helpful'
  feedbackForm.content = ''
}

// 联系客服
const handleContactService = () => {
  ElMessage.info('客服功能开发中')
}

// 添加文章
const handleAddArticle = () => {
  articleFormTitle.value = '添加文章'
  articleFormVisible.value = true
}

// 编辑文章
const handleEditArticle = (article) => {
  articleFormTitle.value = '编辑文章'
  Object.assign(articleForm, article)
  articleFormVisible.value = true
}

// 删除文章
const handleDeleteArticle = (article) => {
  ElMessageBox.confirm('确定要删除该文章吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    fetchArticleList()
  })
}

// 提交文章
const handleSubmitArticle = () => {
  articleFormRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      setTimeout(() => {
        ElMessage.success('保存成功')
        articleFormVisible.value = false
        submitting.value = false
        fetchArticleList()
      }, 1000)
    }
  })
}

// 关闭表单
const handleArticleFormClose = () => {
  articleFormRef.value?.resetFields()
  Object.assign(articleForm, {
    id: null,
    title: '',
    category: '',
    summary: '',
    content: '',
    tags: [],
    isNew: false
  })
}

// 标签相关
const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => {
    tagInputRef.value?.focus()
  })
}

const handleTagInputConfirm = () => {
  if (tagInputValue.value && !articleForm.tags.includes(tagInputValue.value)) {
    articleForm.tags.push(tagInputValue.value)
  }
  tagInputVisible.value = false
  tagInputValue.value = ''
}

const handleRemoveTag = (tag) => {
  articleForm.tags = articleForm.tags.filter(t => t !== tag)
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  fetchArticleList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchArticleList()
}

// 辅助函数
const getCategoryName = (category) => {
  const map = {
    all: '全部文章',
    'getting-started': '新手入门',
    order: '订单相关',
    flight: '飞行计划',
    resource: '资源管理',
    airspace: '空域规则',
    payment: '支付结算',
    safety: '安全须知',
    faq: '常见问题'
  }
  return map[category] || category
}
</script>

<style scoped>
.help-center {
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

.help-container {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 20px;
}

.category-sidebar {
  position: sticky;
  top: 20px;
  height: fit-content;
}

.category-menu {
  border: none;
  border-radius: 4px;
  overflow: hidden;
}

.contact-card {
  margin-top: 20px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.contact-item .el-icon {
  font-size: 24px;
  color: #409eff;
}

.contact-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.contact-value {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.content-area {
  min-height: 600px;
}

.search-section {
  margin-bottom: 30px;
}

.hot-articles {
  margin-bottom: 30px;
}

.hot-articles h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.hot-list {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 15px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  margin-bottom: 8px;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.hot-item:hover {
  transform: translateX(5px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.hot-rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409eff;
  color: white;
  border-radius: 50%;
  font-weight: bold;
  font-size: 12px;
}

.hot-item:nth-child(1) .hot-rank {
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  color: #303133;
}

.hot-item:nth-child(2) .hot-rank {
  background: linear-gradient(135deg, #c0c0c0, #e8e8e8);
  color: #303133;
}

.hot-item:nth-child(3) .hot-rank {
  background: linear-gradient(135deg, #cd7f32, #e0a868);
}

.hot-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.hot-views {
  font-size: 12px;
  color: #909399;
}

.article-list h3 {
  margin-bottom: 20px;
  font-size: 18px;
}

.article-item {
  padding: 20px;
  margin-bottom: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.article-item:hover {
  background: #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.article-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.article-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.article-summary {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.article-footer {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: #909399;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 5px;
}

.article-actions {
  margin-left: auto;
}

.article-detail-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #909399;
}

.article-detail-content {
  line-height: 1.8;
  color: #303133;
  min-height: 200px;
}

.article-detail-content :deep(h3) {
  margin-top: 20px;
  margin-bottom: 10px;
}

.article-detail-content :deep(p) {
  margin-bottom: 15px;
}

.article-detail-footer {
  display: flex;
  gap: 10px;
}

.related-articles {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #dcdfe6;
}

.related-articles h4 {
  margin-bottom: 15px;
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.related-item {
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.related-item:hover {
  background: #e4e7ed;
}
</style>
