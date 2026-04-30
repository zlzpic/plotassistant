<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header"><span>个人中心</span></div>
      </template>
      <div class="profile-content" v-loading="loading">
        <div class="avatar-section">
          <el-avatar :size="100" :icon="UserFilled" />
          <h3>{{ userStore.username }}</h3>
        </div>
        <el-divider />
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="profile-form">
          <el-form-item label="用户名"><el-input v-model="form.username" disabled /></el-form-item>
          <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updateProfile" :loading="updating">保存修改</el-button>
          </el-form-item>
        </el-form>
        <el-divider />
        <h4>修改密码</h4>
        <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" class="password-form">
          <el-form-item label="原密码" prop="oldPassword"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="changePassword" :loading="changing">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import api from '@/api'

const userStore = useUserStore()
const loading = ref(false)
const updating = ref(false)
const changing = ref(false)
const formRef = ref()
const pwdFormRef = ref()

const form = reactive({ username: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const rules = { email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }] }

const validateConfirmPwd = (rule: any, value: string, callback: any) => {
  if (value !== pwdForm.newPassword) callback(new Error('两次输入密码不一致'))
  else callback()
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }, { validator: validateConfirmPwd, trigger: 'blur' }]
}

const fetchProfile = async () => {
  loading.value = true
  try {
    const res: any = await api.user.getProfile()
    form.username = res.username
    form.email = res.email
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const updateProfile = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  updating.value = true
  try {
    await api.user.updateProfile({ email: form.email })
    ElMessage.success('更新成功')
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    updating.value = false
  }
}

const changePassword = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  changing.value = true
  try {
    await api.user.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (error: any) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    changing.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.profile-page { max-width: 600px; margin: 0 auto; }
.profile-card { margin-bottom: 20px; }
.card-header { font-size: 16px; font-weight: bold; }
.avatar-section { text-align: center; padding: 20px 0; }
.avatar-section h3 { margin-top: 15px; color: #333; }
.profile-form, .password-form { padding: 20px 0; }
h4 { margin: 20px 0; color: #333; }
</style>
