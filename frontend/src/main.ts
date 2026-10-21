/**
 * 应用入口：挂载 Pinia / Router，注入全局样式。
 * Element Plus 组件与样式由 unplugin 按需自动导入。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import IconSprite from './components/IconSprite.vue'

// 样式引入顺序：设计令牌 → EP 主题覆盖 → 基础样式
import './styles/variables.css'
import './styles/element.css'
import './styles/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 全局注册图标雪碧图（仅此一个全局组件，其余按需 import）
app.component('IconSprite', IconSprite)

app.mount('#app')
