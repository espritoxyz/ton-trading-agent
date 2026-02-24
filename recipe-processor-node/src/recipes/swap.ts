import {TonClient, WalletContractV5R1} from "@ton/ton";
import {Address, internal, SendMode, toNano} from "@ton/core";
import {mnemonicToPrivateKey} from "@ton/crypto";
import {dexFactory} from "@ston-fi/sdk";
import {StonApiClient} from "@ston-fi/api";
import {bufToHex, waitSeqno} from "../utils.js";


export interface SuccessReport {
    ok: true;
    userId: number;
    txId: string;
    router: string;     // friendly address
    pool: string;       // friendly address
    pTon: string;       // friendly address
    jettonMinter: string; // friendly address of requested jetton
    offerNanotons: string; // string to avoid bigint JSON issues
    minAskNano: string;    // string
}

export interface ErrorReport {
    ok: false;
    userId: number;
    error: string;
    details?: any;
}

const endpoint = process.env.TONCENTER_ENDPOINT || "https://toncenter.com/api/v2/jsonRPC";
const apiKey = process.env.TONCENTER_API_KEY || "";
const tonJettonMaster = "EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c";

export async function swapTokenToToken(
    userId: number,
    offerJettonMaster: Address,
    askJettonMaster: Address,
    minimalAskTokenAmount: number,
    swapOfferTokenAmount: number,
    preferredPoolAddress: string,
    userMnemonic: string[],
): Promise<SuccessReport | ErrorReport> {
    console.log("[swap] swapTokenToToken called", {
        userId,
        offerJettonMaster: offerJettonMaster.toString({ bounceable: false }),
        askJettonMaster: askJettonMaster.toString({ bounceable: false }),
        minimalAskTokenAmount,
        swapOfferTokenAmount,
        preferredPoolAddress,
    });

    try {
        const client = new TonClient({ endpoint, apiKey });

        const { publicKey, secretKey } = await mnemonicToPrivateKey(userMnemonic);
        const wallet = WalletContractV5R1.create({ publicKey, workchain: 0 });
        const provider = client.open(wallet);

        // swapOfferTokenAmount is expected to be in smallest units (nanojettons) of offer token
        const offerJettons = BigInt(swapOfferTokenAmount);
        console.log("[swap] Computed offerJettons (nanojettons) for jetton->jetton", offerJettons.toString());

        const apiClient = new StonApiClient();
        const offerAddrStr = offerJettonMaster.toString({ bounceable: true });
        const askAddrStr = askJettonMaster.toString({ bounceable: true });

        console.log("[swap] Calling STON.fi simulateSwap (jetton -> jetton)", {
            offerAddress: offerAddrStr,
            askAddress: askAddrStr,
            offerUnits: offerJettons.toString(),
            slippageTolerance: "0.05",
        });

        const simulationResult: any = await apiClient.simulateSwap({
            offerAddress: offerAddrStr,
            askAddress: askAddrStr,
            offerUnits: offerJettons.toString(),
            slippageTolerance: "0.05",
        });

        console.log("[swap] simulationResult (jetton -> jetton)", simulationResult);

        const { router: routerInfo, offerUnits, minAskUnits } = simulationResult;

        if (!routerInfo) {
            return {
                ok: false,
                userId,
                error: "STON.fi simulateSwap did not return router info",
                details: simulationResult,
            };
        }

        const minAsk = String(minAskUnits ?? "0");
        console.log("[swap] Using simulation-based offerUnits/minAskUnits (jetton -> jetton)", { offerUnits, minAsk });

        const dexContracts = dexFactory(routerInfo);
        const routerWrapper = dexContracts.Router.create(routerInfo.address);
        const routerOC = client.open(routerWrapper);

        const pTon = dexContracts.pTON.create(routerInfo.ptonMasterAddress);

        const txParams = await (routerOC as any).getSwapJettonToJettonTxParams({
            userWalletAddress: wallet.address,
            proxyTon: pTon,
            offerJettonAddress: Address.parse(offerAddrStr),
            askJettonAddress: Address.parse(askAddrStr),
            offerAmount: BigInt(offerUnits ?? offerJettons.toString()),
            minAskAmount: BigInt(minAsk),
        });

        console.log("[swap] Built txParams (jetton -> jetton)", txParams);

        const before = await provider.getSeqno();

        await provider.sendTransfer({
            seqno: before,
            secretKey,
            messages: [internal(txParams)],
            sendMode: SendMode.PAY_GAS_SEPARATELY,
        });

        await waitSeqno(provider, before);

        const txs = await client.getTransactions(wallet.address, { limit: 5 });
        console.log("[swap] Got transactions", txs?.length);

        const raw = typeof (txs[0] as any).hash === "function" ? (txs[0] as any).hash() : (txs[0] as any).hash;
        const txId = bufToHex(raw);
        console.log("[swap] Swap (jetton -> jetton) succeeded with txId", txId);

        return {
            ok: true,
            userId,
            txId,
            router: routerInfo.address,
            pool: routerInfo.address,
            pTon: routerInfo.ptonMasterAddress,
            jettonMinter: askAddrStr,
            offerNanotons: String(offerUnits ?? offerJettons.toString()),
            minAskNano: minAsk,
        };

    } catch (e: any) {
        console.error("[swap] swapTokenToToken failed", e);
        return {
            ok: false,
            userId,
            error: e?.message || "Swap failed",
            details: e?.response?.data ?? (e?.stack || String(e)),
        };
    }
}

export async function swapTokenToTon(

    userId: number,
    jettonMaster: Address,
    minimalTonAmount: number,
    swapTokenAmount: number,
    preferredPoolAddress: string,
    userMnemonic: string[],
): Promise<SuccessReport | ErrorReport> {

    console.log("[swap] swapTokenToTon called", {
        userId,
        jettonMaster: jettonMaster.toString({ bounceable: false }),
        minimalTonAmount,
        swapTokenAmount,
        preferredPoolAddress,
    });

    try {
        const client = new TonClient({ endpoint, apiKey });

        const { publicKey, secretKey } = await mnemonicToPrivateKey(userMnemonic);
        const wallet = WalletContractV5R1.create({ publicKey, workchain: 0 });
        const provider = client.open(wallet);

        // Here swapTokenAmount is already expected to be in smallest jetton units (nanojettons)
        const offerJettons = BigInt(swapTokenAmount);
        console.log("[swap] Computed offerJettons (nanojettons)", offerJettons.toString());

        // 1. Simulate the swap with STON.fi API (mainnet-first workflow)
        const apiClient = new StonApiClient();
        // STON.fi API expects standard bounceable (EQ...) jetton addresses
        const jettonAddrStr = jettonMaster.toString({ bounceable: true });

        console.log("[swap] Calling STON.fi simulateSwap (jetton -> TON)", {
            offerAddress: jettonAddrStr,
            askAddress: tonJettonMaster,
            offerUnits: offerJettons.toString(),
            slippageTolerance: "0.05",
        });

        const simulationResult: any = await apiClient.simulateSwap({
            offerAddress: jettonAddrStr,
            askAddress: tonJettonMaster,
            offerUnits: offerJettons.toString(),
            slippageTolerance: "0.05",
        });

        console.log("[swap] simulationResult (jetton -> TON)", simulationResult);

        const { router: routerInfo, offerUnits, minAskUnits } = simulationResult;

        if (!routerInfo) {
            return {
                ok: false,
                userId,
                error: "STON.fi simulateSwap did not return router info",
                details: simulationResult,
            };
        }

        // Recommended by STON.fi: reuse offerUnits and minAskUnits from simulation
        const minAsk = String(minAskUnits ?? "0");
        console.log("[swap] Using simulation-based offerUnits/minAskUnits (jetton -> TON)", { offerUnits, minAsk });

        // 2. Build DEX contracts from router metadata
        const dexContracts = dexFactory(routerInfo);
        const routerWrapper = dexContracts.Router.create(routerInfo.address);
        const routerOC = client.open(routerWrapper);

        // Optional pTON helper when TON is in the route
        const pTon = dexContracts.pTON.create(routerInfo.ptonMasterAddress);

        const txParams = await routerOC.getSwapJettonToTonTxParams({

            userWalletAddress: wallet.address,
            proxyTon: pTon,
            offerJettonAddress: Address.parse(jettonAddrStr),
            offerAmount: BigInt(offerUnits ?? offerJettons.toString()),
            minAskAmount: BigInt(minAsk),
        });

        console.log("[swap] Built txParams (jetton -> TON)", txParams);

        const before = await provider.getSeqno();

        await provider.sendTransfer({
            seqno: before,
            secretKey,
            messages: [internal(txParams)],
            sendMode: SendMode.PAY_GAS_SEPARATELY,
        });

        await waitSeqno(provider, before);

        const txs = await client.getTransactions(wallet.address, { limit: 5 });
        console.log("[swap] Got transactions", txs?.length);

        const raw = typeof (txs[0] as any).hash === "function" ? (txs[0] as any).hash() : (txs[0] as any).hash;
        const txId = bufToHex(raw);
        console.log("[swap] Swap (jetton -> TON) succeeded with txId", txId);

        return {
            ok: true,
            userId,
            txId,
            router: routerInfo.address,
            pool: routerInfo.address,
            pTon: routerInfo.ptonMasterAddress,
            jettonMinter: jettonAddrStr,
            offerNanotons: String(offerUnits ?? offerJettons.toString()),
            minAskNano: minAsk,
        };

    } catch (e: any) {
        console.error("[swap] swapTokenToTon failed", e);
        return {
            ok: false,
            userId,
            error: e?.message || "Swap failed",
            details: e?.response?.data ?? (e?.stack || String(e)),
        };
    }
}

export async function swapTonToToken(
    userId: number,
    jettonMaster: Address,
    minimalTokenAmount: number,
    swapTonAmount: number,
    preferredPoolAddress: string,
    userMnemonic: string[],
): Promise<SuccessReport | ErrorReport> {

    console.log("[swap] swapTonToToken called", {
        userId,
        jettonMaster: jettonMaster.toString({ bounceable: false }),
        minimalTokenAmount,
        swapTonAmount,
        preferredPoolAddress,
    });

    try {
        const client = new TonClient({ endpoint, apiKey });

        const { publicKey, secretKey } = await mnemonicToPrivateKey(userMnemonic);
        const wallet = WalletContractV5R1.create({ publicKey, workchain: 0 });
        const provider = client.open(wallet);

        const offerTON = toNano(swapTonAmount);
        console.log("[swap] Computed offerTON (nanotons)", offerTON.toString());

        // 1. Simulate the swap with STON.fi API (mainnet-first workflow)
        const apiClient = new StonApiClient();
        // STON.fi API expects standard bounceable (EQ...) jetton addresses
        const jettonAddrStr = jettonMaster.toString({ bounceable: true });

        console.log("[swap] Calling STON.fi simulateSwap", {
            offerAddress: tonJettonMaster,
            askAddress: jettonAddrStr,
            offerUnits: offerTON.toString(),
            slippageTolerance: "0.05",
        });

        const simulationResult: any = await apiClient.simulateSwap({
            offerAddress: tonJettonMaster,
            askAddress: jettonAddrStr,
            offerUnits: offerTON.toString(),
            slippageTolerance: "0.05",
        });

        console.log("[swap] simulationResult", simulationResult);

        const { router: routerInfo, offerUnits, minAskUnits, askAddress } = simulationResult;

        if (!routerInfo) {
            return {
                ok: false,
                userId,
                error: "STON.fi simulateSwap did not return router info",
                details: simulationResult,
            };
        }

        // Recommended by STON.fi: reuse offerUnits and minAskUnits from simulation
        const minAsk = String(minAskUnits ?? "0");
        console.log("[swap] Using simulation-based offerUnits/minAskUnits", { offerUnits, minAsk });

        // 2. Build DEX contracts from router metadata
        const dexContracts = dexFactory(routerInfo);
        const routerWrapper = dexContracts.Router.create(routerInfo.address);
        const routerOC = client.open(routerWrapper);

        // Optional pTON helper when TON is in the route
        const pTon = dexContracts.pTON.create(routerInfo.ptonMasterAddress);

        const txParams = await routerOC.getSwapTonToJettonTxParams({
            userWalletAddress: wallet.address,
            proxyTon: pTon,
            offerAmount: BigInt(offerUnits ?? offerTON.toString()),
            // Use the same askAddress that was simulated (fallback to our jetton master if absent)
            askJettonAddress: Address.parse(askAddress ?? jettonAddrStr),
            minAskAmount: BigInt(minAsk),

        });


        console.log("[swap] Built txParams", txParams);

        const before = await provider.getSeqno();

        await provider.sendTransfer({
            seqno: before,
            secretKey,
            messages: [internal(txParams)],
            sendMode: SendMode.PAY_GAS_SEPARATELY,
        });

        await waitSeqno(provider, before);

        const txs = await client.getTransactions(wallet.address, { limit: 5 });
        console.log("[swap] Got transactions", txs?.length);

        const raw = typeof (txs[0] as any).hash === "function" ? (txs[0] as any).hash() : (txs[0] as any).hash;
        const txId = bufToHex(raw);
        console.log("[swap] Swap succeeded with txId", txId);

        return {
            ok: true,
            userId,
            txId,
            router: routerInfo.address,
            pool: routerInfo.address,
            pTon: routerInfo.ptonMasterAddress,
            jettonMinter: jettonAddrStr,
            offerNanotons: String(offerUnits ?? offerTON.toString()),
            minAskNano: minAsk,
        };

    } catch (e: any) {
        console.error("[swap] swapTonToToken failed", e);
        return {

            ok: false,
            userId,
            error: e?.message || "Swap failed",
            details: e?.response?.data ?? (e?.stack || String(e)),
        };
    }
}
