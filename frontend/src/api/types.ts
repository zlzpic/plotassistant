export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

export interface PageParams {
  page?: number
  size?: number
}

export interface PageResult<T> {
  data: T[]
  total: number
  page: number
  size: number
  pages: number
}
