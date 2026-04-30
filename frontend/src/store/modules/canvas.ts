import { defineStore } from 'pinia'

interface CanvasState {
  hasUnsavedChanges: boolean
  nodes: any[]
  edges: any[]
}

export const useCanvasStore = defineStore('canvas', {
  state: (): CanvasState => ({
    hasUnsavedChanges: false,
    nodes: [],
    edges: []
  }),

  getters: {
    getUnsavedStatus: (state) => state.hasUnsavedChanges
  },

  actions: {
    setUnsaved(flag: boolean) {
      this.hasUnsavedChanges = flag
    },

    setNodes(nodes: any[]) {
      this.nodes = nodes
    },

    setEdges(edges: any[]) {
      this.edges = edges
    }
  }
})
