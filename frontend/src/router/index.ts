import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { useGenerationStore } from '@/store/modules/generation'
import { ElMessage } from 'element-plus'

const routes: any = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/components/Layout/AppLayout.vue'),
    redirect: '/projects',
    children: [
      {
        path: 'projects',
        name: 'ProjectList',
        component: () => import('@/views/ProjectList.vue'),
        meta: { title: '项目列表', icon: 'Folder' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', icon: 'User' }
      },
      {
        path: 'project/:id',
        component: () => import('@/views/ProjectDetail/Index.vue'),
        redirect: (to: any) => ({ path: `/project/${to.params.id}/world` }),
        meta: { requiresProject: true },
        children: [
          {
            path: 'world',
            name: 'ProjectWorld',
            component: () => import('@/views/ProjectDetail/World.vue'),
            meta: { title: '世界观(L1/L3)', icon: 'MapLocation' }
          },
          {
            path: 'characters',
            name: 'ProjectCharacters',
            component: () => import('@/views/ProjectDetail/Characters.vue'),
            meta: { title: '角色(L2)', icon: 'UserFilled' }
          },
          {
            path: 'canvas',
            name: 'ProjectCanvas',
            component: () => import('@/views/ProjectDetail/Canvas.vue'),
            meta: { title: '剧情树(L4/L5/L6/L7/L8)', icon: 'Connection', unsavedCheck: true }
          },
          {
            path: 'generation',
            name: 'ProjectGeneration',
            component: () => import('@/views/ProjectDetail/Generation.vue'),
            meta: { title: '生成预览(L9)', icon: 'View' }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const generationStore = useGenerationStore()
  const token = localStorage.getItem('token')

  if (!to.meta.public && !token) {
    return next('/login')
  }

  if (to.path === '/login' && token) {
    return next('/')
  }

  if (to.meta.requiresProject) {
    const projectId = to.params.id
    if (!projectId) {
      return next('/projects')
    }
  }

  if (generationStore.isGenerating && to.name !== 'ProjectGeneration') {
    ElMessage.warning('AI生成中，请等待完成')
    return false
  }

  next()
})

export default router
