import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [uni()],
  // H5 开发服务器：固定 5174，避免与 Web 端（frontend）的 5173 冲突。
  // Web 端占 5173（frontend/vite.config.ts），移动端 H5 用 5174，可并行启动互不干扰。
  server: {
    port: 5174
  },
  // Wot UI 依赖 sass，消除 legacy-js-api 弃用警告（官方快速上手建议）
  css: {
    preprocessorOptions: {
      scss: {
        api: "modern-compiler",
        silenceDeprecations: ["legacy-js-api"]
      }
    }
  }
});
