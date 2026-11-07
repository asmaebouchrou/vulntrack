package com.vulntrack.backend.scan;

import com.vulntrack.backend.scan.ScanDtos.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Local demonstration history shared by all authenticated users. */
@RestController
@RequestMapping("/api/v1/scans")
public class ScanController {
    private final ScanService service;
    public ScanController(ScanService service) { this.service = service; }

    @GetMapping
    public List<ScanSummary> history() { return service.history(); }

    @GetMapping("/{id}")
    public ScanDetail detail(@PathVariable Long id) { return service.detail(id); }
}
