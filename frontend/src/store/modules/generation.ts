import { defineStore } from 'pinia'

interface GenerationState {
  isGenerating: boolean
  currentTask: string
  progress: number
}

export const useGenerationStore = defineStore('generation', {
  state: (): GenerationState => ({
    isGenerating: false,
    currentTask: '',
    progress: 0
  }),

  getters: {
    getGeneratingStatus: (state) => state.isGenerating,
    getCurrentTask: (state) => state.currentTask
  },

  actions: {
    startGeneration(task: string) {
      this.isGenerating = true
      this.currentTask = task
      this.progress = 0
    },

    endGeneration() {
      this.isGenerating = false
      this.currentTask = ''
      this.progress = 0
    },

    setProgress(progress: number) {
      this.progress = progress
    }
  }
})
