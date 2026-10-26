/**
 * 主题模式工具：通过给 <html> 添加/移除 `dark` 类实现浅色/深色切换。
 * 选择持久化到 uni storage，key 为 `theme`，取值 'light' | 'dark'。
 * 深色模式以 H5 为目标端；小程序无 html 节点，applyTheme 在小程序端为空操作。
 */

const THEME_KEY = 'theme'
const DARK_CLASS = 'dark'

export type ThemeMode = 'light' | 'dark'

/** 读取当前保存的主题（无保存记录时默认浅色） */
export function getTheme(): ThemeMode {
  const saved = uni.getStorageSync(THEME_KEY)
  return saved === 'dark' ? 'dark' : 'light'
}

/** 应用主题：设置/移除 html.dark 类 */
export function applyTheme(mode: ThemeMode): void {
  // #ifdef H5
  const root = document.documentElement
  if (mode === 'dark') {
    root.classList.add(DARK_CLASS)
  } else {
    root.classList.remove(DARK_CLASS)
  }
  // #endif
  // #ifndef H5
  void mode // 小程序端无 html 节点，仅保持持久化
  // #endif
}

/** 切换并持久化主题，返回新主题 */
export function setTheme(mode: ThemeMode): ThemeMode {
  uni.setStorageSync(THEME_KEY, mode)
  applyTheme(mode)
  return mode
}
