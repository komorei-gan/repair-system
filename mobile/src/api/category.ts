/**
 * 故障分类接口（design.md §3.2 GET /api/categories）。
 */
import { http } from './request'
import type { Category } from '@/types/api'

/** 启用的分类列表 */
export function listCategories(): Promise<Category[]> {
  return http.get<Category[]>('/api/categories')
}
