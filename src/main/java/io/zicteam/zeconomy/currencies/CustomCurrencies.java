package io.zicteam.zeconomy.currencies;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class CustomCurrencies {
    public static Map<String, Supplier<BaseCurrency>> CURRENCIES = new ConcurrentHashMap<>();

    public CustomCurrencies() {
    }
}
