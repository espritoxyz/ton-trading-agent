import {TonClient, WalletContractV5R1} from "@ton/ton";
import {Address, internal, SendMode, toNano} from "@ton/core";
import {mnemonicToPrivateKey} from "@ton/crypto";
import {dexFactory} from "@ston-fi/sdk";
import {StonApiClient} from "@ston-fi/api";
import {waitSeqno} from "../utils.js";
import {buildReport, findOurTransaction} from "./utils.js";
import type {SuccessReport, ErrorReport} from "./reports.js";


const endpoint = process.env.TONCENTER_ENDPOINT || "https://toncenter.com/api/v2/jsonRPC";
const apiKey = process.env.TONCENTER_API_KEY || "";
const tonJettonMaster = "EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c";

export async function swapTokenToToken(
    offerJettonMaster: Address,
    askJettonMaster: Address,
    minimalAskTokenAmount: number,
    swapOfferTokenAmount: number,
    preferredPoolAddress: string,
    userMnemonic: string[],
): Promise<SuccessReport | ErrorReport> {
    console.log("[swap] swapTokenToToken called", {
        offerJettonMaster: offerJettonMaster.toString({bounceable: false}),
        askJettonMaster: askJettonMaster.toString({bounceable: false}),
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

        const { router: routerInfo, offerUnits, askUnits, minAskUnits } = simulationResult;

        if (!routerInfo) {
            return {
                ok: false,
                error: "STON.fi simulateSwap did not return router info",
            };
        }

        const minAsk = String(minAskUnits ?? "0");
        const askAmount = Number(askUnits ?? "0")
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

        // findOurTransaction picks the external-in tx (our swap), not the Ston.fi excess-TON refund
        const tx: any = findOurTransaction(txs);

        return buildReport(tx, {
            router: routerInfo.address,
            pool: routerInfo.address,
            pTon: routerInfo.ptonMasterAddress,
            jettonMinter: askAddrStr,
            offerNanotons: String(offerUnits ?? offerJettons.toString()),
            minAskNano: minAsk,
            askNano: askAmount,
            logPrefix: "Swap (jetton -> jetton)",
            allTxs: txs,
        });
    } catch (e: any) {
        console.error("[swap] swapTokenToToken failed", e);
        return {
            ok: false,
            error: e?.message || "Swap failed",
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

        const offerJettons = BigInt(swapTokenAmount);
        console.log("[swap] Computed offerJettons (nanojettons)", offerJettons.toString());

        const apiClient = new StonApiClient();
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

        const { router: routerInfo, offerUnits, askUnits, minAskUnits } = simulationResult;

        if (!routerInfo) {
            return {
                ok: false,
                error: "STON.fi simulateSwap did not return router info",
            };
        }

        const minAsk = String(minAskUnits ?? "0");
        const askAmount = Number(askUnits ?? "0")
        console.log("[swap] Using simulation-based offerUnits/minAskUnits (jetton -> TON)", { offerUnits, minAsk });

        const dexContracts = dexFactory(routerInfo);
        const routerWrapper = dexContracts.Router.create(routerInfo.address);
        const routerOC = client.open(routerWrapper);

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

        const txs = await client.getTransactions(wallet.address, {limit: 5});
        console.log("[swap] Got transactions", txs?.length);

        // findOurTransaction picks the external-in tx (our swap), not the Ston.fi excess-TON refund
        const tx: any = findOurTransaction(txs);

        return buildReport(tx, {
            router: routerInfo.address,
            pool: routerInfo.address,
            pTon: routerInfo.ptonMasterAddress,
            jettonMinter: jettonAddrStr,
            offerNanotons: String(offerUnits ?? offerJettons.toString()),
            askNano: askAmount,
            minAskNano: minAsk,
            logPrefix: "Swap (jetton -> TON)",
            allTxs: txs,
            // Jetton→TON: wallet receives swap-result TON — exclude it from "refund"
            swapTonReceivedNano: String(askUnits ?? "0"),
        });
    } catch (e: any) {
        console.error("[swap] swapTokenToTon failed", e);
        return {
            ok: false,
            error: e?.message || "Swap failed",
        };
    }
}

export async function swapTonToToken(
    jettonMaster: Address,
    minimalTokenAmount: number,
    swapTonAmount: number,
    preferredPoolAddress: string,
    userMnemonic: string[],
): Promise<SuccessReport | ErrorReport> {

    console.log("[swap] swapTonToToken called", {
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

        const apiClient = new StonApiClient();
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

        const { router: routerInfo, offerUnits, askUnits, minAskUnits, askAddress } = simulationResult;

        if (!routerInfo) {
            return {
                ok: false,
                error: "STON.fi simulateSwap did not return router info",
            };
        }

        const minAsk = String(minAskUnits ?? "0");
        const askAmount = Number(askUnits ?? "0")
        console.log("[swap] Using simulation-based offerUnits/minAskUnits", { offerUnits, minAsk });

        const dexContracts = dexFactory(routerInfo);
        const routerWrapper = dexContracts.Router.create(routerInfo.address);
        const routerOC = client.open(routerWrapper);

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

        // findOurTransaction picks the external-in tx (our swap), not the Ston.fi excess-TON refund
        const tx: any = findOurTransaction(txs);

        return buildReport(tx, {
            router: routerInfo.address,
            pool: routerInfo.address,
            pTon: routerInfo.ptonMasterAddress,
            jettonMinter: jettonAddrStr,
            offerNanotons: String(offerUnits ?? offerTON.toString()),
            askNano: askAmount,
            minAskNano: minAsk,
            logPrefix: "Swap (TON -> jetton)",
            allTxs: txs,
            // TON→Jetton: outgoing message includes the swap amount — exclude it from "gas sent"
            swapTonSentNano: String(offerUnits ?? offerTON.toString()),
        });
    } catch (e: any) {
        console.error("[swap] swapTonToToken failed", e);
        return {
            ok: false,
            error: e?.message || "Swap failed",
        };
    }
}
