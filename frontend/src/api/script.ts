import request from './request'

export default {
  generateL9: (projectId: number, branchPath: string, style?: string) => 
    request.post(`/project/${projectId}/scripts/generate-whole`, { branchPath, style }),
  generateL7: (projectId: number, data: any) => 
    request.post(`/project/${projectId}/generate/dialogue`, data)
}
