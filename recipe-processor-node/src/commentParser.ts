import {Message} from "@ton/core";

const CODE_REGEX = /^[A-Z0-9]{6}$/;

export function extractCodeFromPayload(inMsg?: Message): string | null {
    try {
        if (!inMsg?.body) return null;

        const fullString = inMsg.body.beginParse().loadStringTail().toString();
        const codeMatch = fullString.match(/[A-Z0-9]+$/);

        console.debug("[commentParser] Code match result:", codeMatch);
        return codeMatch?.[0] || null;
    } catch (error) {
        console.error("[commentParser] Error parsing comment:", error);
        return null;
    }
}

export function validateDepositCode(comment: string | null): string | null {
    if (!comment) return null;

    const normalized = comment.trim().toUpperCase();

    if (!CODE_REGEX.test(normalized)) {
        console.warn(`[commentParser] Comment "${normalized}" does not match expected format`);
        return null;
    }

    return normalized;
}

export function parseDepositComment(inMsg?: Message): string | null {
    const comment = extractCodeFromPayload(inMsg);
    return validateDepositCode(comment);
}
