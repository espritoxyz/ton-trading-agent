import { Cell } from "@ton/core";

const CODE_REGEX = /^[A-Z0-9]{6}$/;

export function extractComment(body?: Cell): string | null {
    if (!body) return null;

    try {
        const slice = body.beginParse();

        // Check if there's enough data to read opcode (at least 32 bits)
        if (slice.remainingBits < 32) {
            // No opcode, might be empty transfer
            return null;
        }

        // Read op code (first 32 bits)
        const op = slice.loadUint(32);

        // Op code 0 means text comment
        if (op !== 0) {
            // Non-zero opcode means this is not a text comment
            return null;
        }

        // Check if there's any text after the opcode
        if (slice.remainingBits === 0 && slice.remainingRefs === 0) {
            // Empty comment
            return null;
        }

        // Load the comment string
        // loadStringTail() handles both inline text and text in refs
        const comment = slice.loadStringTail();

        return comment && comment.length > 0 ? comment : null;
    } catch (e) {
        // If parsing fails, return null
        console.error("[commentParser] Error parsing comment:", e);
        return null;
    }
}

export function validateDepositCode(comment: string | null): string | null {
    if (!comment) return null;

    const normalized = comment.trim().toUpperCase();

    if (!CODE_REGEX.test(normalized)) {
        return null;
    }

    return normalized;
}

export function parseDepositComment(body?: Cell): string | null {
    const comment = extractComment(body);
    return validateDepositCode(comment);
}
