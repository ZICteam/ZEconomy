package net.sixik.zeconomy.utils;

public enum ErrorCodes {
    SUCCESS,
    FAIL,
    NOT_FOUND,
    NOT_ACCESS;

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isFail() {
        return this == FAIL;
    }

    public boolean isNotFound() {
        return this == NOT_FOUND;
    }

    public boolean isNotAccess() {
        return this == NOT_ACCESS;
    }
}
