import { defineStore } from 'pinia'
import api from '@/api'

interface UserState {
  userId: number | null
  username: string
  token: string
  profile: any
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userId: null,
    username: '',
    token: localStorage.getItem('token') || '',
    profile: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    getUsername: (state) => state.username || 'User'
  },

  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },

    setUserInfo(userId: number, username: string) {
      this.userId = userId
      this.username = username
    },

    async login(data: { username: string; password: string }) {
      const res: any = await api.user.login(data)
      this.setToken(res.token)
      this.setUserInfo(res.userId, res.username)
      return res
    },

    async fetchProfile() {
      const res = await api.user.getProfile()
      this.profile = res
      return res
    },

    logout() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.profile = null
      localStorage.removeItem('token')
    }
  }
})
