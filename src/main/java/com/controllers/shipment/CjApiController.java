package com.controllers.shipment;

import com.services.shipment.CjApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CjApiController {

    private final CjApiService cjApiService;

    public CjApiController(CjApiService cjApiService) {
        this.cjApiService = cjApiService;
    }

    @GetMapping("/api/v1/cj/token/test")
    public Map<String, Object> tokenTest() {
        return cjApiService.requestOneDayToken();
    }
}