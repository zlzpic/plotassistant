export type GenerationTask = 'L1' | 'L2' | 'L3' | 'L4' | 'L5' | 'L6' | 'L7' | 'L8' | 'L9'

export interface GenerationParams {
  projectId: number
  task: GenerationTask
  data?: any
}

export interface GenerationResult {
  success: boolean
  data?: any
  error?: string
}
