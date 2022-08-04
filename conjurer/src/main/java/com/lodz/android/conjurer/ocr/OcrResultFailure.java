package com.lodz.android.conjurer.ocr;

/**
 * Class to hold metadata for failed OCR results.
 */
public final class OcrResultFailure {
    private final long timeRequired;
    private final long timestamp;

    OcrResultFailure(long timeRequired) {
        this.timeRequired = timeRequired;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimeRequired() {
        return timeRequired;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return timeRequired + " " + timestamp;
    }
}
