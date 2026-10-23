/**
 * 主题模式工具：通过给 <html> 添加/移除 `dark` 类实现浅色/深色切换。
 * 选择持久化到 localStorage，key 为 `theme`，取值 'light' | 'dark'。
 */

const THEME_KEY = 'theme'
const DARK_CLASS = 'dark'

export type ThemeMode = 'light' | 'dark'

/** 读取当前保存的主题（无保存记录时默认浅色） */
export function getTheme(): ThemeMode {
  return localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light'
}

/** 应用主题：设置/移除 html.dark 类 */
export function applyTheme(mode: ThemeMode): void {
  const root = document.documentElement
  if (mode === 'dark') {
    root.classList.add(DARK_CLASS)
  } else {
    root.classList.remove(DARK_CLASS)
  }
}

/** 切换并持久化主题，返回新主题 */
export function setTheme(mode: ThemeMode): ThemeMode {
  localStorage.setItem(THEME_KEY, mode)
  applyTheme(mode)
  return mode
}
