package com.queueify.campaignservice.progress;

public class ProgressLookupException extends RuntimeException {
    public ProgressLookupException() {super("Processor progress is unavailable or invalid.");}
}
