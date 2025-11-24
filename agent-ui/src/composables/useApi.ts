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
