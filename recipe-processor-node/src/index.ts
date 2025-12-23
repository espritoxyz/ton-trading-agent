import { setupRabbit, startConsumer, shutdown, publishJson } from "./rabbit.js";
import {mockSendTon, sendTon} from "./transactions.js";

const RABBIT_URL = process.env.RABBIT_URL || "amqp://guest:guest@localhost:5672/";
const SERVICE = "recipe-processor-node";

// Subscribe to agent-llm.# events
const { conn, ch, exchange, queue } = await setupRabbit(RABBIT_URL, SERVICE, ["agent-llm.#"]);

await startConsumer(ch, queue, async (_msg, body) => {
    try {
        if (!body || typeof body !== "object") return;
        const { type, data, occurredAt } = body;
        if (type === "agent-llm.send-ton") {
            const messageId = data?.messageId;
            const userId = data?.userId;
            const amount = data?.tonAmount;
            const receiver = data?.receiverAddress;
            console.log(`[${SERVICE}] send-ton requested:`, { messageId, userId, amount, receiver });

            try {
                const txId = await mockSendTon(amount, receiver);
                console.log(`[${SERVICE}] send-ton done: txId=${txId}`);
                publishJson(ch, exchange, "agent-llm.send-ton.result", {
                    type: "agent-llm.send-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        tonAmount: amount,
                        receiverAddress: receiver,
                        success: true,
                        txId,
                    },
                });
            } catch (err: any) {
                console.error(`[${SERVICE}] send-ton error:`, err);
                publishJson(ch, exchange, "agent-llm.send-ton.result", {
                    type: "agent-llm.send-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        tonAmount: amount,
                        receiverAddress: receiver,
                        success: false,
                        error: String(err?.message || err),
                    },
                });
            }
        }
    } catch (e) {
        console.error(`[${SERVICE}] error handling message:`, e);
        throw e; // let startConsumer nack
    }
});

// Graceful stop
process.on("SIGINT", async () => { await shutdown({ conn, ch, exchange, queue }); process.exit(0); });
process.on("SIGTERM", async () => { await shutdown({ conn, ch, exchange, queue }); process.exit(0); });
