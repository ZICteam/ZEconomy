package io.zicteam.zeconomy.api;

public final class ZEconomyApiProvider {
    private static final ZEconomyApi API = new ZEconomyApiImpl();

    private ZEconomyApiProvider() {
    }

    public static ZEconomyApi get() {
        return API;
    }
}
