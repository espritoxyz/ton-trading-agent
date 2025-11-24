import { ref } from 'vue';
import { api } from './useApi';
export function useChat() {
    const sessionId = ref(null);
    const messages = ref([]);
    const loading = ref(false);
    async function ensureSession() {
        if (sessionId.value)
            return;
        const { data } = await api.post('/agent/sessions');
        sessionId.value = data.sessionId;
    }
    async function sendUserMessage(text) {
        await ensureSession();
        if (!sessionId.value)
            return;
        // Optimistic UI
        const tempId = `tmp_${Date.now()}`;
        messages.value.push({
            id: tempId,
            role: 'user',
            content: text,
            ts: new Date().toISOString()
        });
        loading.value = true;
        await api.post(`/agent/sessions/${sessionId.value}/messages`, {
            role: 'user',
            content: text
        });
        // Immediately poll for new messages (simple version)
        await pollNew(tempId);
        loading.value = false;
    }
    async function pollNew(afterId) {
        if (!sessionId.value)
            return;
        const { data } = await api.get(`/agent/sessions/${sessionId.value}/messages`, { params: { after: afterId } });
        if (Array.isArray(data.items)) {
            for (const m of data.items)
                messages.value.push(m);
        }
    }
    return { sessionId, messages, loading, sendUserMessage, pollNew };
}
