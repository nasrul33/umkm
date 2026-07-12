package com.siaumkm.cost;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/app/cost-analysis")
public class CostAnalysisController {

    private final CostAnalysisService service;

    public CostAnalysisController(CostAnalysisService service) {
        this.service = service;
    }

    @GetMapping("/product-margins")
    public List<CostAnalysisService.ProductMarginRow> productMargins() {
        return service.getProductMargins();
    }
}
