<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
import { applyTheme, getTheme } from "@/utils/theme";

onLaunch(() => {
  console.log("App Launch");
  // 启动即应用上次保存的主题（避免刷新后闪烁回浅色）
  applyTheme(getTheme());
});
onShow(() => {
  console.log("App Show");
});
onHide(() => {
  console.log("App Hide");
});
</script>
<style lang="scss">
/* ============================================================
   全局主题变量：浅色为默认，html.dark 覆盖为深色。
   仅替换「中性色」（背景/卡片/文字/边框），品牌蓝、状态色、渐变保持不变。
   同时覆盖 wot-ui 的语义 token（--wot-*），保证 navbar/popup/picker/
   dialog/input/textarea 等组件在深色模式下同步变深，不再出现白块或黑底黑字。
   ============================================================ */
:root {
  /* 项目自定义变量 */
  --bg-page: #f4f7f9;         /* 页面背景 */
  --bg-card: #ffffff;         /* 卡片/白底 */
  --bg-input: #f5f7fa;        /* 输入框/浅灰块 */
  --bg-hover: #f5f9ff;        /* 浅蓝灰悬停/高亮块 */

  --text-main: #1f2d3d;       /* 主文字 */
  --text-sub: #606266;        /* 次级文字 */
  --text-muted: #909399;      /* 弱化文字 */
  --text-placeholder: #b0b8c4;/* 占位符 */

  --line: #f0f3f7;            /* 分隔线 */
  --line-strong: #dcdfe6;     /* 强边框 */
}

html.dark {
  /* 项目自定义变量 */
  --bg-page: #121212;
  --bg-card: #1e1e1e;
  --bg-input: #2a2a2a;
  --bg-hover: #24272c;

  --text-main: #e5eaf3;
  --text-sub: #c9d6e4;
  --text-muted: #93a7bb;
  --text-placeholder: #7a90a8;

  --line: #2a343f;
  --line-strong: #36424f;

  /* ============================================================
     wot-ui 语义 token 覆盖（组件默认值多为 --wot-filled-oppo 白色）
     深色下统一映射为深色面，文字映射为浅色，避免「白块/黑底黑字」。
     ============================================================ */
  --wot-filled-oppo: #1e1e1e;        /* 反向填充（navbar/popup/picker/dialog/input/textarea 默认背景） */
  --wot-filled-bottom: #2a2a2a;
  --wot-filled-content: #2a2a2a;
  --wot-filled-strong: #36424f;
  --wot-filled-extra-strong: #36424f;

  --wot-text-main: #e5eaf3;
  --wot-text-secondary: #c9d6e4;
  --wot-text-auxiliary: #93a7bb;
  --wot-text-placeholder: #93a7bb;
  --wot-text-disabled: #7a90a8;

  --wot-icon-main: #e5eaf3;
  --wot-icon-secondary: #c9d6e4;
  --wot-icon-auxiliary: #93a7bb;
  --wot-icon-placeholder: #93a7bb;

  --wot-border-main: #2a343f;
  --wot-border-light: #2a343f;
  --wot-border-strong: #36424f;
  --wot-border-extra-strong: #36424f;

  --wot-divider-main: #2a343f;
  --wot-divider-light: #2a343f;
  --wot-divider-strong: #36424f;

  /* 组件级 token（显式指定，避免依赖 filled-oppo 回退链） */
  --wot-navbar-bg: #1e1e1e;
  --wot-navbar-color: #e5eaf3;
  --wot-navbar-desc-color: #93a7bb;

  --wot-popup-bg: #1e1e1e;

  --wot-picker-bg: #1e1e1e;
  --wot-picker-title-color: #e5eaf3;
  --wot-picker-action-color-cancel: #93a7bb;

  --wot-picker-view-bg: #1e1e1e;
  --wot-picker-view-roller-bg: #2a2a2a;
  --wot-picker-view-column-item-color: #e5eaf3;
  --wot-picker-view-column-item-disabled-color: #7a90a8;
  --wot-picker-view-mask-start-color: rgba(18, 18, 18, 0.85);
  --wot-picker-view-mask-end-color: rgba(18, 18, 18, 0.2);

  --wot-dialog-bg: #1e1e1e;
  --wot-dialog-title-color: #e5eaf3;
  --wot-dialog-content-color: #c9d6e4;
  --wot-dialog-content-input-bg: #2a2a2a;

  --wot-input-bg: #1e1e1e;
  --wot-input-inner-color: #e5eaf3;
  --wot-input-inner-placeholder-color: #93a7bb;
  --wot-input-count-color: #93a7bb;

  --wot-textarea-bg: #1e1e1e;
  --wot-textarea-inner-color: #e5eaf3;
  --wot-textarea-inner-placeholder-color: #93a7bb;
  --wot-textarea-count-color: #93a7bb;

  --wot-cell-bg: #1e1e1e;
  --wot-cell-bg-active: #24272c;
  --wot-cell-title-color: #e5eaf3;
  --wot-cell-value-color: #c9d6e4;
  --wot-cell-label-color: #93a7bb;
  --wot-cell-border-color: #2a343f;
  --wot-cell-placeholder-color: #93a7bb;

  --wot-tabs-nav-bg: #1e1e1e;
  --wot-tabs-nav-color: #c9d6e4;
  --wot-tabs-map-body-bg: #1e1e1e;
  --wot-tabs-map-color: #e5eaf3;
  --wot-tabs-map-nav-btn-bg: #2a2a2a;
  --wot-tabs-map-nav-btn-color: #e5eaf3;
  --wot-tabs-map-btn-before-bg: linear-gradient(270deg, #1e1e1e 0%, transparent 100%);
}

/* 全局基础样式 */
page {
  background-color: var(--bg-page);
  font-size: 28rpx;
  color: var(--text-main);
}

/* 深色模式下，强制原生与 wot 输入组件文字/光标为浅色，杜绝黑底黑字、光标不可见 */
html.dark input,
html.dark textarea,
html.dark .wd-input__inner,
html.dark .wd-textarea__inner {
  color: #e5eaf3 !important;
  caret-color: #ffffff !important;
  -webkit-text-fill-color: #e5eaf3 !important;
}
html.dark input::placeholder,
html.dark textarea::placeholder,
html.dark .wd-input__placeholder,
html.dark .wd-textarea__placeholder {
  color: #93a7bb !important;
}
</style>
