import request from './request'

export default {
  getDetail: (projectId: number) => request.get(`/project/${projectId}/world/detail`),
  update: (projectId: number, data: any) => request.post(`/project/${projectId}/world/update`, data),
  generateL1: (projectId: number, data: any) => request.post(`/project/${projectId}/world/generate-description`, data)
}
