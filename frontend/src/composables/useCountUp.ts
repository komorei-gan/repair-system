/**
 * 轻量数字滚动 Hook（Count-up）。
 * 用 requestAnimationFrame + easeOutCubic 缓动实现，
 * 页面加载与手动刷新时，数字从 0 平滑滚动到目标值。
 * 零依赖、体积小，组件卸载时自动清理动画帧。
 *
 * 可访问性：URL 带 ?noanim 或系统开启「减弱动态效果」时，直接显示终值。
 */
import { computed, onBeforeUnmount, ref } from 'vue'

export interface UseCountUpOptions {
  /** 动画时长（ms） */
  duration?: number
  /** 小数位数（0 = 整数） */
  decimals?: number
  /** 开始前延迟（ms），用于与卡片入场动画错峰 */
  delay?: number
  /** 缓动函数（默认 easeOutCubic：先快后慢） */
  easing?: (t: number) => number
}

function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3)
}

/** 是否跳过动画（截图/录屏辅助 + 尊重系统减弱动态设置） */
function shouldSkipAnimation(): boolean {
  if (typeof window === 'undefined') return false
  return (
    new URLSearchParams(window.location.search).has('noanim') ||
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
}

export function useCountUp(getTarget: () => number, options: UseCountUpOptions = {}) {
  const { duration = 1000, decimals = 0, delay = 0, easing = easeOutCubic } = options

  /** 当前滚动到的数值 */
  const display = ref(0)
  /** 按 decimals 格式化后的展示字符串（如 92.6 / 18） */
  const formatted = computed(() => display.value.toFixed(decimals))

  let rafId = 0

  /** 从 0 滚动到目标值；可重复调用（刷新时重滚） */
  function start(): void {
    cancelAnimationFrame(rafId)
    const target = getTarget()

    // 跳过动画：直接落位终值
    if (shouldSkipAnimation()) {
      display.value = Number(target.toFixed(decimals))
      return
    }

    const beginAt = performance.now() + delay

    const step = (now: number): void => {
      const elapsed = now - beginAt
      if (elapsed < 0) {
        rafId = requestAnimationFrame(step)
        return
      }
      const progress = Math.min(elapsed / duration, 1)
      const value = target * easing(progress)
      display.value = decimals > 0 ? Number(value.toFixed(decimals)) : Math.round(value)

      if (progress < 1) {
        rafId = requestAnimationFrame(step)
      } else {
        // 终值精确落位，避免浮点误差
        display.value = Number(target.toFixed(decimals))
      }
    }

    rafId = requestAnimationFrame(step)
  }

  onBeforeUnmount(() => cancelAnimationFrame(rafId))

  return { display, formatted, start }
}
