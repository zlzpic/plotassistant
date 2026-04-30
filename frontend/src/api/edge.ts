import request from './request'

export interface StoryEdge {
  id?: number
  sourceId: string
  targetId: string
  label?: string
  conditionExpr?: string
  reason?: string
  onSuccess?: string
  onFailure?: string
  effect?: any
}

export default {
  // 批量保存（包含新建和删除）
  batchSave: (projectId: string | number, data: {
    edges: Array<{
      sourceId: string
      targetId: string
      label: string
      conditionExpr?: string
      reason?: string
      onSuccess?: string
      onFailure?: string
      effect?: string | null
    }>
    deleteIds: string[]
  }) => {
    return request.post(`/project/${projectId}/edge/batch-save`, data)
  },
  // 获取边列表
  getList: (projectId: string | number) => {
    return request.get(`/project/${projectId}/edge/list`)
  },
  getDetail: (projectId: number, edgeId: number) => request.get(`/project/${projectId}/edge/${edgeId}/detail`),
  update: (projectId: number, edgeId: number, data: any) => request.post(`/project/${projectId}/edge/${edgeId}/update`, data),
  delete: (projectId: number, edgeId: number) => request.post(`/project/${projectId}/edge/${edgeId}/delete`),
  // L8 生成选项（之前已添加）
  generateSuggestions: (projectId: string | number, data: {
    sourceId: string
    targetId: string
    prompt: string
  }) => {
    return request.post(`/project/${projectId}/edge/generate-suggestions`, data)
  }
}
