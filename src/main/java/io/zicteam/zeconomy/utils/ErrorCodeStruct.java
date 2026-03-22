package io.zicteam.zeconomy.utils;

public class ErrorCodeStruct<T> {
    public T value;
    public ErrorCodes codes;

    public ErrorCodeStruct(T value) {
        this(value, ErrorCodes.SUCCESS);
    }

    public ErrorCodeStruct(T value, ErrorCodes codes) {
        this.value = value;
        this.codes = codes;
    }
}
