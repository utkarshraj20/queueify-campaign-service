package com.queueify.campaignservice.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1/campaigns")
@ConditionalOnProperty(name="queueify.internal.campaign-api.enabled",havingValue="true")
public class InternalCampaignController {
    private final CampaignContextRepository repository;
    public InternalCampaignController(CampaignContextRepository repository) { this.repository=repository; }
    @GetMapping("/{campaignId}/delivery-context")
    public ResponseEntity<CampaignDeliveryContext> get(@PathVariable long campaignId) {
        if(campaignId<1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var result=repository.find(campaignId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!repository.eligible(result)) throw new ResponseStatusException(HttpStatus.CONFLICT);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result.context());
    }
}
