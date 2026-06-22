package com.ftsm.rag.controller;

import com.ftsm.rag.model.BillingSummary;
import com.ftsm.rag.model.InvoiceRecord;
import com.ftsm.rag.model.SmartCloudUserContext;
import com.ftsm.rag.service.SmartCloudAuthService;
import com.ftsm.rag.service.SmartCloudBusinessDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final SmartCloudAuthService authService;
    private final SmartCloudBusinessDataService businessDataService;

    public BillingController(SmartCloudAuthService authService, SmartCloudBusinessDataService businessDataService) {
        this.authService = authService;
        this.businessDataService = businessDataService;
    }

    @GetMapping("/summary")
    public BillingSummary summary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        SmartCloudUserContext user = authService.resolve(authorization);
        return businessDataService.billingSummary(user, "billing dashboard");
    }

    @GetMapping("/invoices")
    public List<InvoiceRecord> invoices(@RequestHeader(value = "Authorization", required = false) String authorization) {
        SmartCloudUserContext user = authService.resolve(authorization);
        return businessDataService.invoices(user);
    }
}
