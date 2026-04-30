import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginData {
  userId: number
  token: string
  username: string
}

export interface UserProfile {
  id: number
  username: string
  email: string
  status: string
  createdAt: string
}

export default {
  login: (data: LoginParams) => request.post<LoginData>('/user/login', data),
  register: (data: any) => request.post<number>('/user/register', data),
  getProfile: () => request.get<UserProfile>('/user/profile'),
  updateProfile: (data: any) => request.post('/user/profile/update', data),
  changePassword: (data: any) => request.post('/user/password/change', data)
}
