/**
 * 故障分类接口。
 * - GET    /api/categories              ：启用分类列表（公共，报修端选择）
 * - GET    /api/admin/categories        ：全部分类（含停用，管理端）
 * - POST   /api/admin/categories        ：新增分类
 * - PUT    /api/admin/categories/{id}   ：编辑分类
 * - DELETE /api/admin/categories/{id}   ：删除分类
 */
import { http } from './request'
import type { Category } from '@/types/api'

/** 启用的分类列表（按 sort 升序，报修端选择） */
export function listCategories(): Promise<Category[]> {
  return http.get<Category[]>('/api/categories')
}

/** 全部分类列表（含停用，管理端表格展示） */
export function listAllCategories(): Promise<Category[]> {
  return http.get<Category[]>('/api/admin/categories')
}

/** 新增分类 */
export function createCategory(data: { name: string; sort?: number }): Promise<void> {
  return http.post<void>('/api/admin/categories', data)
}

/** 编辑分类 */
export function updateCategory(id: number, data: { name: string; sort?: number }): Promise<void> {
  return http.put<void>(`/api/admin/categories/${id}`, data)
}

/** 删除分类 */
export function deleteCategory(id: number): Promise<void> {
  return http.delete<void>(`/api/admin/categories/${id}`)
}
