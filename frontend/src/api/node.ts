import request from './request'

export interface StoryNode {
  id: string
  nodeName: string
  sceneDescription?: string
  associatedCharIds?: string[]
  initialVariables?: any
  positionX?: number
  positionY?: number
}

export default {
  create: (projectId: number, data: any) => request.post<string>(`/project/${projectId}/node/create`, data),
  getList: (projectId: number) => request.get<StoryNode[]>(`/project/${projectId}/node/list`),
  // getDetail: (projectId: number, nodeId: string) => 
  //   request.get<StoryNode>(`/project/${projectId}/node/${nodeId}/detail`),
  update: (projectId: number, nodeId: string, data: any) => request.post(`/project/${projectId}/node/${nodeId}/update`, data),
  batchSave: (projectId: number, nodes: StoryNode[]) => 
    request.post(`/project/${projectId}/node/batch-save`, { nodes }),
  delete: (projectId: number, nodeId: string) => request.post(`/project/${projectId}/node/${nodeId}/delete`),
  generateL4: (projectId: number, clearExisting: boolean = false) => 
    request.post<string[]>(`/project/${projectId}/node/generate-batch`, { clearExisting }),
  // generateL5: (projectId: number, nodeId: string, prompt: string) => 
  //   request.post(`/project/${projectId}/node/${nodeId}/description`, { prompt }),
  generateL6: (projectId: number, nodeId: string, prompt: string) => 
    request.post<string[]>(`/project/${projectId}/character/nodes/${nodeId}/npcs/generate`, { prompt }),
  // 获取节点详情
  getDetail: (projectId: number, nodeId: string) => 
    request.get(`/project/${projectId}/node/${nodeId}/detail`),
  
  // 生成L5场景描述（修改传参方式）
  generateL5: (projectId: number, nodeId: string, data: { prompt: string }) => 
    request.post(`/project/${projectId}/node/${nodeId}/description`, data),
}
