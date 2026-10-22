/**
 * 应用入口：挂载 Pinia / Router，注入全局样式。
 * Element Plus 组件由 unplugin 按需自动导入；
 * 但其「服务式组件」（ElMessage / ElMessageBox / ElNotification / ElLoading）
 * 是 JS 调用渲染的，unplugin 不会为其注入样式，必须在此显式引入完整样式表，
 * 否则弹窗会退化成无 CSS 的裸 DOM（退出登录确认框样式崩坏的根因）。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import IconSprite from './components/IconSprite.vue'

// 样式引入顺序：EP 基础样式 → 设计令牌 → EP 主题覆盖 → 项目基础样式
// 顺序保证：先有 EP 默认样式，再用 variables/element.css 覆盖主色与圆角
import 'element-plus/dist/index.css'
import './styles/variables.css'
import './styles/element.css'
import './styles/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 全局注册图标雪碧图（仅此一个全局组件，其余按需 import）
app.component('IconSprite', IconSprite)

app.mount('#app')
