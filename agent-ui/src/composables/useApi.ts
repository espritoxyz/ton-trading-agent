import axios from 'axios'

// Use environment-configured base URL, defaulting to '/api' which works with nginx and Vite proxy
const apiBase = (import.meta as any).env?.VITE_BACKEND_URL || '/api'

export const api = axios.create({
    baseURL: apiBase,
    withCredentials: false
})

// rawApi: axios instance WITHOUT interceptors — used to call refresh endpoint directly
export const rawApi = axios.create({baseURL: apiBase, withCredentials: false})

// helper: decode JWT exp
function getJwtExpMs(token: string | null): number | null {
    if (!token) return null
    try {
        const parts = token.split('.')
        if (parts.length < 2) return null
        const payload = parts[1]
        const pad = '='.repeat((4 - (payload.length % 4)) % 4)
        const base64 = (payload + pad).replace(/-/g, '+').replace(/_/g, '/')
        const json = atob(base64)
        const obj = JSON.parse(json)
        if (!obj.exp) return null
        return obj.exp * 1000
    } catch (e) {
        return null
    }
}

// --- token refresh helpers (centralized) ---
let isRefreshing = false
let refreshPromise: Promise<string | null> | null = null
const REFRESH_MARGIN_MS = 60 * 1000 // refresh if token expires within 60s

export async function refreshToken(): Promise<string | null> {
    // single flight
    if (isRefreshing && refreshPromise) return refreshPromise
    isRefreshing = true
    refreshPromise = (async () => {
        try {
            const token = sessionStorage.getItem('access_token')
            if (!token) {
                return null
            }
            // call refresh endpoint directly using rawApi to avoid interceptors
            // send token in X-Access-Token header to prevent ResourceServer from rejecting expired token
            const resp = await rawApi.post('/auth/refresh', null, {
                headers: {'X-Access-Token': token}
            })
            const newToken = resp?.data?.access_token ?? resp?.data?.accessToken
            if (newToken) {
                sessionStorage.setItem('access_token', newToken)
                return newToken
            }
        } catch (e) {
            // swallow error and clear token
        } finally {
            isRefreshing = false
        }
        sessionStorage.removeItem('access_token')
        return null
    })()
    return refreshPromise
}

// request interceptor: pre-refresh if token near expiry
api.interceptors.request.use(async (config) => {
    // allow skipping via custom flag
    if ((config as any)._skipRefresh) return config

    const token = sessionStorage.getItem('access_token')
    if (token) {
        const expMs = getJwtExpMs(token)
        if (expMs && Date.now() + REFRESH_MARGIN_MS >= expMs) {
            // token near expiry -> refresh before proceeding
            try {
                const newTok = await refreshToken()
                if (newTok) {
                    config.headers = config.headers || {}
                    config.headers.Authorization = `Bearer ${newTok}`
                    return config
                }
            } catch (e) {
                // ignore and proceed; request will likely fail
            }
        }
        // otherwise attach current token
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// --- response interceptor: on 401 attempt refresh via backend and retry original request ---

async function doRefresh(): Promise<string | null> {
    return refreshToken()
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
