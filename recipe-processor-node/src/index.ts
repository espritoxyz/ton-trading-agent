import { setupRabbit, publishJson, startConsumer, shutdown } from "./rabbit.js";
import crypto from "node:crypto";

const RABBIT_URL = process.env.RABBIT_URL || "amqp://guest:guest@localhost:5672/";
const SERVICE = "recipe-processor-node";

const { conn, ch, exchange, queue } = await setupRabbit(RABBIT_URL, SERVICE);

// Consume all user.* events
await startConsumer(ch, queue, async (_msg, body) => {
    console.log(`[${SERVICE}] consumed:`, body);
    // ...do work...
});

// Publish a sample event every 5s
setInterval(() => {
    publishJson(ch, exchange, "user.created", {
        type: "user.created",
        messageId: crypto.randomUUID(),
        occurredAt: new Date().toISOString(),
        data: { id: crypto.randomUUID(), email: "user@example.com" }
    });
    console.log(`[${SERVICE}] published user.created`);
}, 5000);

// Graceful stop
process.on("SIGINT", async () => { await shutdown({ conn, ch, exchange, queue }); process.exit(0); });
process.on("SIGTERM", async () => { await shutdown({ conn, ch, exchange, queue }); process.exit(0); });
