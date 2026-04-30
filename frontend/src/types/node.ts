export interface StoryNode {
  id: string
  nodeName: string
  sceneDescription?: string
  associatedCharIds?: string[]
  initialVariables?: any
  positionX?: number
  positionY?: number
}

export interface FlowNode {
  id: string
  position: { x: number; y: number }
  data: {
    label: string
    [key: string]: any
  }
  type?: string
}
