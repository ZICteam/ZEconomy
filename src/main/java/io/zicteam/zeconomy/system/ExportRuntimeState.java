package io.zicteam.zeconomy.system;

import net.minecraft.nbt.CompoundTag;

final class ExportRuntimeState {
    private long lastExportEpochSec;

    long getLastExportEpochSec() {
        return lastExportEpochSec;
    }

    boolean shouldExport(long nowEpochSec, long intervalSeconds) {
        return nowEpochSec - lastExportEpochSec >= intervalSeconds;
    }

    void markExported(long nowEpochSec) {
        lastExportEpochSec = nowEpochSec;
    }

    void writeTo(CompoundTag root) {
        root.putLong("lastExportEpochSec", lastExportEpochSec);
    }

    void readFrom(CompoundTag root) {
        lastExportEpochSec = root.getLong("lastExportEpochSec");
    }
}
