import client, { Connection, Channel, ConsumeMessage } from "amqplib";


export type Rabbit = {
    conn: Connection;
    ch: Channel;
    exchange: string;
    queue: string;
};

export async function setupRabbit(url: string, serviceName = "recipe-processor-node"): Promise<Rabbit> {
    const exchange = "app.events";
    const queue = `${serviceName}.in`;

    const channelModel = await client.connect(url);
    const conn = channelModel.connection
    const ch: Channel = await channelModel.createChannel();

    await ch.assertExchange(exchange, "topic", { durable: true });
    await ch.assertQueue(queue, { durable: true });
    await ch.bindQueue(queue, exchange, "user.*");
    ch.prefetch(10);

    return { conn, ch, exchange, queue };
}

export function publishJson(ch: Channel, exchange: string, routingKey: string, payload: unknown) {
    const ok = ch.publish(
        exchange,
        routingKey,
        Buffer.from(JSON.stringify(payload)),
        {
            contentType: "application/json",
            contentEncoding: "utf-8",
            persistent: true,
            type: routingKey,
            timestamp: Math.floor(Date.now() / 1000)
        }
    );
    if (!ok) console.warn("[rabbit] backpressure: publish returned false");
}

/** Start a consumer with safe JSON parsing + ack/nack */
export async function startConsumer(
    ch: Channel,
    queue: string,
    handler: (msg: ConsumeMessage, body: any) => Promise<void> | void
) {
    await ch.consume(
        queue,
        async (msg) => {
            if (!msg) return;
            try {
                const body = JSON.parse(msg.content.toString());
                await handler(msg, body);
                ch.ack(msg);
            } catch (e) {
                console.error("[rabbit] handler failed:", e);
                // dead-letter if DLX is configured; otherwise drop by rejecting requeue=false
                ch.nack(msg, false, false);
            }
        },
        { noAck: false }
    );
}

/** Graceful shutdown */
export async function shutdown(r: Rabbit) {
    try { await r.ch.close(); } catch {}
    try { await r.ch.close(); } catch {}
}
