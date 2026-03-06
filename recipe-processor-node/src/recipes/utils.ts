import {bufToHex} from "../utils.js";
import {fromNano, Transaction, TransactionActionPhase, TransactionComputePhase} from "@ton/core";
import {ErrorReport, SuccessReport} from "./reports.js";

function explainExitCode(code: number, phase: "compute" | "action"): string {
    if (phase === "compute" && (code === 0 || code === 1)) return "Success (compute phase)";
    if (phase === "action" && code === 0) return "Success (action phase)";

    // Common compute errors (0..127 reserved by TON)
    if (phase === "compute") {
        switch (code) {
            case 2: return "Stack underflow (TVM)";
            case 3: return "Stack overflow (TVM)";
            case 4: return "Integer overflow (TVM)";
            case 5: return "Range check error (integer out of expected range)";
            case 6: return "Invalid TVM opcode";
            case 7: return "Type check error";
            case 8: return "Cell overflow";
            case 9: return "Cell underflow";
            case 10: return "Dictionary error";
            case 11: return "Unknown error (may be thrown by user code)";
            case 12: return "Fatal TVM error";
            case 13: return "Out of gas (may be displayed as -14)";
            case -14: return "Out of gas (display form of 13)";
            // Seen often in contracts; treated as "unknown opcode" to this contract
            case 65535: return "Unknown opcode / no handler (often treated as 130)";
            default: break;
        }
    }

    // Common action errors
    if (phase === "action") {
        switch (code) {
            case 32: return "Action list is invalid";
            case 33: return "Action list is too long";
            case 34: return "Invalid or unsupported action";
            case 35: return "Invalid source address in outbound message";
            case 36: return "Invalid destination address in outbound message";
            case 37: return "Not enough Toncoin to complete actions / pay forward fees";
            case 38: return "Not enough extra currencies";
            case 39: return "Outbound message cannot be packed into a cell";
            case 40: return "Cannot process outbound message (too large/deep or not enough funds)";
            case 50: return "Account state size exceeded protocol limits";
            default: break;
        }
    }

    // Reserved vs developer-defined range
    if (code >= 256 && code <= 65535) {
        return `Developer-defined exit code (${code}). Consult the contract's source/spec.`;
    }
    if (code < 0) {
        return `Negative exit code (${code}). Often indicates a protected/derived code (e.g., -14).`;
    }
    return `Exit code ${code} (see TON TVM exit codes)`;
}

export function interpretTransaction(tx: Transaction): {
    ok: boolean;
    phase: "compute" | "action" | "unknown";
    exitCode?: number;
    reason?: string;
    desc: any;
    txId: string;
} {
    const desc = tx.description as any;
    const computePhase: any = desc?.computePhase as TransactionComputePhase | undefined;
    const actionPhase: TransactionActionPhase | undefined = desc?.actionPhase as TransactionActionPhase | undefined;

    let ok = false;
    let phase: "compute" | "action" | "unknown" = "unknown";
    let exitCode: number | undefined;
    let reason: string | undefined;

    if (!desc || desc.type !== "generic") {
        reason = `Unsupported transaction description type: ${desc?.type ?? "unknown"}`;
    } else if (!computePhase) {
        reason = "Compute phase is missing";
    } else if (computePhase.type === "skipped") {
        phase = "compute";
        reason = `Compute phase skipped: ${computePhase.reason || "unknown reason"}`;
    } else if (computePhase.type !== "vm") {
        reason = `Unsupported compute phase type: ${computePhase.type}`;
    } else {
        const computeOk = computePhase.success && (computePhase.exitCode === 0 || computePhase.exitCode === 1);
        if (!computeOk) {
            phase = "compute";
            exitCode = computePhase.exitCode as number;
            reason = explainExitCode(exitCode, "compute");
        } else if (!actionPhase) {
            phase = "action";
            reason = "Action phase is missing";
        } else if (actionPhase.skippedActions === actionPhase.totalActions) {
            phase = "action";
            reason = "Action phase has all actions skipped";
        } else {
            const actionCode = actionPhase.resultCode;
            const actionOk = actionPhase.success && actionCode === 0;

            if (!actionOk) {
                phase = "action";
                exitCode = actionCode;
                reason = explainExitCode(exitCode, "action");
            } else if (desc.aborted) {
                phase = "action";
                exitCode = actionCode;
                reason = "Transaction was aborted";
            } else {
                ok = true;
            }
        }
    }

    const raw = typeof tx.hash === "function" ? tx.hash() : tx.hash;
    const txId = bufToHex(raw);

    return { ok, phase, exitCode, reason, desc, txId };
}

export function buildReport(
    tx: Transaction,
    context: {
        router: string;
        pool: string;
        pTon: string;
        jettonMinter: string;
        offerNanotons: string;
        minAskNano: string;
        askNano: number;
        logPrefix: string;
    },
): SuccessReport | ErrorReport {
    const { ok, phase, exitCode, reason, desc, txId } = interpretTransaction(tx);
    const totalFee = Number(fromNano(tx.totalFees.coins));

    if (!ok) {
        const error = reason || "Unknown transaction failure";
        console.error(`[swap] ${context.logPrefix} failed`, { txId, phase, exitCode, error, desc });
        return {
            ok: false,
            txId,
            totalFee,
            exitCode,
            error,
        };
    }

    console.log(`[swap] ${context.logPrefix} succeeded with txId`, txId);

    return {
        ok: true,
        txId,
        totalFee,
        router: context.router,
        pool: context.pool,
        pTon: context.pTon,
        jettonMinter: context.jettonMinter,
        offerNanotons: context.offerNanotons,
        minAskNano: context.minAskNano,
        askNano: context.askNano,
    };
}

