package com.lucasnarloch.loss_track.domain.tenant;

import com.lucasnarloch.loss_track.domain.tenant.dtos.TenantRequestDTO;
import com.lucasnarloch.loss_track.domain.tenant.dtos.TenantResponseDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/me")
    public TenantResponseDTO findCurrent(@AuthenticationPrincipal Jwt jwt) {
        return tenantService.findById(tenantId(jwt));
    }

    @PutMapping("/me")
    public TenantResponseDTO updateCurrent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantRequestDTO tenantRequestDto
    ) {
        return tenantService.update(tenantId(jwt), tenantRequestDto);
    }

    private UUID tenantId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("tenant_id"));
    }
}
