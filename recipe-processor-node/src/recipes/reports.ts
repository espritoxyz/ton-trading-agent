export interface SuccessReport {
    ok: true;
    txId: string;
    totalFee?: number;
    router?: string;
    pool?: string; 
    pTon?: string;
    jettonMinter?: string;
    offerNanotons?: string;
    minAskNano?: string;
    askNano?: number;
}

export interface ErrorReport {
    ok: false;
    txId?: string;
    totalFee?: number;
    exitCode?: number;
    error: string;
}
