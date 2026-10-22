import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入组件本体。
    // 注意：样式统一由 main.ts 引入的 element-plus/dist/index.css 提供，
    // 这里关闭 resolver 的样式注入，避免同一份组件样式被打包两次；
    // 更重要的是——ElMessage / ElMessageBox 这类服务式组件由 JS 调用渲染，
    // resolver 不会为它们注入样式，只有全量样式入口能覆盖（曾导致弹窗无样式）。
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: 'src/types/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: 'src/types/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    host: true,
  },
})
