import {ref} from 'vue'
import {api, refreshToken} from './useApi.ts'

export const accessToken = ref<string | null>(sessionStorage.getItem('access_token'))
export const email = ref<string | null>(null)
export const subject = ref<string | null>(null)
export const userId = ref<number | null>(null)
export const loggingIn = ref(false)
export const authError = ref<string | null>(null)
export const needsVerification = ref(false)
export const verificationEmail = ref<string | null>(null)

export async function login(username: string, password: string) {
    loggingIn.value = true
    authError.value = null
    needsVerification.value = false
    try {
        const resp = await api.post('/auth/login', {username, password})
        const data = resp.data
        const token = data?.access_token as string
        const storedHeader = resp.headers['x-offline-refresh-stored']
        console.debug('[auth] login: offline refresh stored header =', storedHeader)
        if (!token) throw new Error('No access_token in response')
        sessionStorage.setItem('access_token', token)
        accessToken.value = token
        await refreshProfile()
    } catch (e: any) {
        const errorMsg = e?.response?.data?.message || e?.message || 'Login failed'

        // Check if account needs email verification
        if (errorMsg.includes('Account is not fully set up') || errorMsg.includes('not fully set up')) {
            needsVerification.value = true
            verificationEmail.value = username
            authError.value = 'Please verify your email address before logging in'
        } else {
            authError.value = errorMsg
        }
        throw e
    } finally {
        loggingIn.value = false
    }
}

export async function register(emailInput: string, passwordInput: string, displayName?: string) {
    authError.value = null
    try {
        const { data } = await api.post('/auth/register', { email: emailInput, password: passwordInput, displayName }, { headers: { Authorization: undefined } })
        return data
    } catch (e: any) {
        authError.value = e?.response?.data?.message ?? e?.message ?? 'Registration failed'
        throw e
    }
}

export function logout() {
    // call backend to cleanup offline tokens, then clear local state
    try {
        api.post('/auth/logout')
    } catch (e) {
        // ignore
    }

    sessionStorage.removeItem('access_token')
    accessToken.value = null
    email.value = null
    subject.value = null
    userId.value = null
}

export async function refreshProfile() {
    email.value = null;
    subject.value = null;
    userId.value = null;
    if (!accessToken.value) return;

    try {
        const {data} = await api.get('/auth/profile', {
            headers: {
                Authorization: `Bearer ${accessToken.value}`,
                Accept: 'application/json'
            }
        });

        subject.value = (data?.subject ?? data?.sub) ?? null;
        email.value = data?.email ?? null;
        userId.value = typeof data?.userId === 'number' ? data.userId : null;

    } catch (err: any) {
        const status = err?.response?.status ?? err?.status ?? 0;

        if (status === 401 || status === 403) {
            // token invalid/expired or not accepted by backend
            logout();
            return;
        }

        // Fallback: decode JWT locally so app can still show who is logged in
        try {
            const payload = parseJwt(accessToken.value!);
            subject.value = payload?.sub ?? null;
            email.value = payload?.email ?? payload?.preferred_username ?? null;
            // userId stays null (needs backend provisioning)
        } catch {
            // ignore; leave fields null
        }
    }
}

// Attempt to refresh access token using backend stored refresh/offline token
export async function refreshAccessToken(): Promise<boolean> {
    try {
        const newTok = await refreshToken()
        if (!newTok) throw new Error('No access token from refresh')
        sessionStorage.setItem('access_token', newTok)
        accessToken.value = newTok
        await refreshProfile()
        return true
    } catch (e) {
        logout()
        return false
    }
}

// Initialize auth on app start - try to restore session if possible
export async function initAuth() {
    const token = sessionStorage.getItem('access_token')
    if (!token) return
    accessToken.value = token
    // attempt a refresh to ensure token is valid or refresh if expired
    await refreshAccessToken()
}

// small helper to decode JWT safely (base64url -> UTF-8 -> JSON)
function parseJwt(token: string): any {
    const parts = token.split('.')
    if (parts.length < 2) throw new Error('Invalid JWT')
    const payload = parts[1]
    const json = b64urlToUtf8(payload)
    return JSON.parse(json)
}

function b64urlToUtf8(b64url: string): string {
    const pad = '='.repeat((4 - (b64url.length % 4)) % 4)
    const base64 = (b64url + pad).replace(/-/g, '+').replace(/_/g, '/')
    const binary = atob(base64)
    const bytes = Uint8Array.from(binary, c => c.charCodeAt(0))
    return new TextDecoder().decode(bytes)
}

export async function verifyEmail(token: string): Promise<{success: boolean, message: string}> {
    try {
        const { data } = await api.post('/auth/verify-email', { token }, { headers: { Authorization: undefined } })
        return { success: data.success ?? true, message: data.message || 'Email verified successfully' }
    } catch (e: any) {
        return { success: false, message: e?.response?.data?.message || 'Verification failed' }
    }
}

export async function resendVerificationEmail(email?: string): Promise<{success: boolean, message: string}> {
    try {
        const body = email ? { email } : {}
        const { data } = await api.post('/auth/resend-verification', body)
        return { success: data.success ?? true, message: data.message || 'Verification email sent' }
    } catch (e: any) {
        return { success: false, message: e?.response?.data?.message || 'Failed to send' }
    }
}
