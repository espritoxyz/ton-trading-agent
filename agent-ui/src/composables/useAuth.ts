import { ref } from 'vue'
import { api } from './useApi.ts'

export const accessToken = ref<string | null>(sessionStorage.getItem('access_token'))
export const email = ref<string | null>(null)
export const subject = ref<string | null>(null)
export const userId = ref<number | null>(null)
export const loggingIn = ref(false)
export const authError = ref<string | null>(null)

export async function login(username: string, password: string) {
    loggingIn.value = true
    authError.value = null
    try {
        const { data } = await api.post('/auth/login', { username, password })
        const token = data?.access_token as string
        if (!token) throw new Error('No access_token in response')
        sessionStorage.setItem('access_token', token)
        accessToken.value = token
        await refreshProfile()
    } catch (e: any) {
        authError.value = e?.message ?? 'Login failed'
        throw e
    } finally {
        loggingIn.value = false
    }
}

export function logout() {
    sessionStorage.removeItem('access_token')
    accessToken.value = null
    email.value = null
    subject.value = null
    userId.value = null
}

export async function refreshProfile() {
    email.value = null; subject.value = null; userId.value = null;
    if (!accessToken.value) return;

    try {
        const { data } = await api.get('/auth/profile', {
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

// small helper to decode JWT safely (base64url -> UTF-8 -> JSON)
function parseJwt(token: string): any {
    const parts = token.split('.');
    if (parts.length < 2) throw new Error('Invalid JWT');
    const payload = parts[1];
    const json = b64urlToUtf8(payload);
    return JSON.parse(json);
}

function b64urlToUtf8(b64url: string): string {
    const pad = '='.repeat((4 - (b64url.length % 4)) % 4);
    const base64 = (b64url + pad).replace(/-/g, '+').replace(/_/g, '/');
    const binary = atob(base64);
    const bytes = Uint8Array.from(binary, c => c.charCodeAt(0));
    return new TextDecoder().decode(bytes);
}
