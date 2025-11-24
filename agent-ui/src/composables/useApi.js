import axios from 'axios';
const apiBase = '/api';
export const api = axios.create({
    baseURL: apiBase,
    withCredentials: true
});
// Example auth injection (adapt to your Keycloak flow)
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('access_token');
    if (token)
        config.headers.Authorization = `Bearer ${token}`;
    return config;
});
