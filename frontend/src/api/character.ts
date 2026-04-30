import request from './request'

export default {
  create: (projectId: number, data: any) => request.post<string>(`/project/${projectId}/character/create`, data),
  getList: (projectId: number) => request.get<any[]>(`/project/${projectId}/character/list`),
  getDetail: (projectId: number, charId: string) => request.get(`/project/${projectId}/character/${charId}/detail`),
  update: (projectId: number, charId: string, data: any) => request.post(`/project/${projectId}/character/${charId}/update`, data),
  addInsight: (projectId: number, charId: string, insight: string) => 
    request.post(`/project/${projectId}/character/${charId}/add-insight`, { insight }),
  delete: (projectId: number, charId: string) => request.post(`/project/${projectId}/character/${charId}/delete`),
  generateNPC: (projectId: number, nodeId: string, data: { prompt: string, count: number }) => {
    return request.post(`/project/${projectId}/character/nodes/${nodeId}/npcs/generate`, data)
  },
  generateL2: (projectId: number, data: any) => request.post<string[]>(`/project/${projectId}/character/characters/generate-important`, data)
}
