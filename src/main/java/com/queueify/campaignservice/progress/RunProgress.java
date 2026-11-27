package com.queueify.campaignservice.progress;

import java.util.Set;

public record RunProgress(long campaignId,String runId,String runStatus,Long validRows,Long invalidRows,
                          long totalTasks,long sent,long failed,long unknown,long processing,long pending) {
    public RunProgress {
        if(campaignId<1 || runId==null || !runId.matches("[A-Za-z0-9._:-]{1,128}")
                || runStatus==null || !Set.of("PROCESSING","READY","BLOCKED","FAILED").contains(runStatus)) throw invalid();
        for(long count:new long[]{totalTasks,sent,failed,unknown,processing,pending}) if(count<0 || count>Integer.MAX_VALUE) throw invalid();
        if(sent+failed+unknown+processing+pending!=totalTasks) throw invalid();
        if(validRows!=null && (validRows<0 || validRows>Integer.MAX_VALUE)) throw invalid();
        if(invalidRows!=null && (invalidRows<0 || invalidRows>Integer.MAX_VALUE)) throw invalid();
        boolean prepared=runStatus.equals("READY") || runStatus.equals("BLOCKED");
        if(prepared && (validRows==null || invalidRows==null || validRows+invalidRows>Integer.MAX_VALUE)) throw invalid();
        if(!prepared && (validRows!=null || invalidRows!=null)) throw invalid();
        if(runStatus.equals("READY") && (validRows<1 || invalidRows!=0 || totalTasks!=validRows)) throw invalid();
    }
    private static IllegalArgumentException invalid() {return new IllegalArgumentException("Invalid run progress response.");}
}
