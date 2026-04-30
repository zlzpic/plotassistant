import request from './request'

export default {
  getDetail: (projectId: number, contentType: string) => 
    request.get(`/project/${projectId}/generate/${contentType}/detail`),
  save: (projectId: number, contentType: string, contentJson: string) => 
    request.post(`/project/${projectId}/generate/${contentType}/save`, { contentJson }),
  //L3 生成故事大纲（90s超时）
  generateOutline: (projectId: number, data: { darkness: number; complexity: number; prompt: string }) =>
    request.post(`/project/${projectId}/generate/outline`, data),
  //L7 生成角色对话
  generateDialogue: (projectId: number, data: any) =>
    request.post(`/project/${projectId}/generate/dialogue`, data)
  
}
