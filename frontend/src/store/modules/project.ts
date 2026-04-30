import { defineStore } from 'pinia'

interface ProjectState {
  currentProjectId: number | null
  currentProject: any | null
}

export const useProjectStore = defineStore('project', {
  state: (): ProjectState => ({
    currentProjectId: Number(localStorage.getItem('currentProjectId')) || null,
    currentProject: null
  }),

  getters: {
    hasProject: (state) => !!state.currentProjectId,
    getProjectId: (state) => state.currentProjectId
  },

  actions: {
    setProjectId(id: number) {
      this.currentProjectId = id
      localStorage.setItem('currentProjectId', String(id))
    },

    setProject(project: any) {
      this.currentProject = project
    },

    selectProject(id: number, project?: any) {
      this.setProjectId(id)
      if (project) {
        this.setProject(project)
      }
    },

    clearProject() {
      this.currentProjectId = null
      this.currentProject = null
      localStorage.removeItem('currentProjectId')
    }
  }
})
