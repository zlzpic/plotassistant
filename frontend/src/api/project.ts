import request from './request'
import type { PageParams, PageResult } from './types'

export interface ProjectItem {
  id: number
  name: string
  description?: string
  status: string
  updatedAt: string
}

export interface ProjectDetail extends ProjectItem {
  description?: string
  worldSetting?: any
}

export default {
  create: (data: any) => request.post<number>('/project/create', data),
  getList: (params?: PageParams) => request.get<PageResult<ProjectItem>>('/project/list', { params }),
  getDetail: (id: number) => request.get<ProjectDetail>(`/project/${id}/detail`),
  update: (id: number, data: any) => request.post(`/project/${id}/update`, data),
  complete: (id: number) => request.post(`/project/${id}/complete`),
  delete: (id: number) => request.post(`/project/${id}/delete`)
}
