import axios from 'axios'

// Use environment-configured base URL, defaulting to '/api' which works with nginx and Vite proxy
const apiBase = (import.meta as any).env?.VITE_BACKEND_URL || '/api'

export const api = axios.create({
    baseURL: apiBase,
    withCredentials: false
})

api.interceptors.request.use((config) => {
    const token = sessionStorage.getItem('access_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// --- response interceptor: on 401 attempt refresh via backend and retry original request ---
let isRefreshing = false
let refreshPromise: Promise<string | null> | null = null

async function doRefresh(): Promise<string | null> {
    try {
        const resp = await api.post('/auth/refresh')
        const token = resp?.data?.access_token ?? resp?.data?.accessToken
        if (token) {
            sessionStorage.setItem('access_token', token)
            return token
        }
    } catch (e) {
        // refresh failed
    }
    // ensure token cleared on failure
    sessionStorage.removeItem('access_token')
    return null
}

api.interceptors.response.use(
    r => r,
    async (error) => {
        const originalRequest = error.config
        if (!originalRequest) return Promise.reject(error)
        const status = error?.response?.status
        if (status === 401 && !originalRequest._retry) {
            originalRequest._retry = true
            if (!isRefreshing) {
                isRefreshing = true
                refreshPromise = doRefresh()
            }

            try {
                const newToken = await refreshPromise
                isRefreshing = false
                refreshPromise = null

                if (newToken) {
                    originalRequest.headers.Authorization = `Bearer ${newToken}`
                    return api(originalRequest)
                }
            } catch (e) {
                isRefreshing = false
                refreshPromise = null
            }
        }
        return Promise.reject(error)
    }
)
