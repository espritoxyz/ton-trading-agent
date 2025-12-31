export type StonRouter = {
    address: string;
    major_version: number;
    minor_version: number;
    pton_master_address: string;
    pton_wallet_address: string;
    pton_version: string;
    router_type: "ConstantProduct" | "StableSwap" | "WeightedStableSwap" | "WeightedConstProduct" | string;
    pool_creation_enabled: boolean;
};

export type RouterListResponse = {
    router_list: StonRouter[];
};
