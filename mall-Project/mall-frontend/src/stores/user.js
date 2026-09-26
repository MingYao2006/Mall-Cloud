import {defineStore} from 'pinia'
import {ref, computed} from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')

    // 安全解析 userInfo，避免 JSON.parse 错误
    const getUserInfo = () => {
        const stored = localStorage.getItem('userInfo')
        if (!stored || stored === 'undefined' || stored === 'null') {
            return null
        }
        try {
            return JSON.parse(stored)
        } catch {
            return null
        }
    }
    const userInfo = ref(getUserInfo())

    // 供导航栏等组件使用
    const isLogin = computed(() => !!token.value)
    const nickname = computed(
        () => userInfo.value?.nickname || userInfo.value?.username || '用户'
    )

    function setToken(newToken) {
        token.value = newToken
        localStorage.setItem('token', newToken)
    }

    function setUserInfo(info) {
        userInfo.value = info
        localStorage.setItem('userInfo', JSON.stringify(info))
    }

    async function login(loginForm) {
        const {userApi} = await import('@/api')
        const res = await userApi.login(loginForm)
        setToken(res.data.token)
        setUserInfo(res.data)
        return res
    }

    async function loginByCode(phone, code) {
        const {userApi} = await import('@/api')
        const res = await userApi.loginByCode(phone, code)
        setToken(res.data.token)
        setUserInfo(res.data)
        return res
    }

    async function bindPhone(phone, code) {
        const {userApi} = await import('@/api')
        const res = await userApi.bindPhone(phone, code)
        // 绑定成功后刷新本地用户信息（手机号已变化）
        await refreshUserInfo()
        return res
    }

    async function logout() {
        const {userApi} = await import('@/api')
        await userApi.logout()
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    async function updateUserInfo(data) {
        const {userApi} = await import('@/api')
        const res = await userApi.updateInfo(data)
        // 更新成功后，刷新本地用户信息
        await refreshUserInfo()
        return res
    }

    async function refreshUserInfo() {
        const {userApi} = await import('@/api')
        const infoRes = await userApi.getInfo()
        setUserInfo(infoRes.data)
        return infoRes
    }

    async function updateAvatar(file) {
        const {userApi} = await import('@/api')
        const res = await userApi.uploadAvatar(file)
        // 更新成功后，刷新本地用户信息
        await refreshUserInfo()
        return res
    }

    return {
        token,
        userInfo,
        isLogin,
        nickname,
        setToken,
        setUserInfo,
        login,
        loginByCode,
        bindPhone,
        logout,
        updateUserInfo,
        refreshUserInfo,
        updateAvatar
    }
})
