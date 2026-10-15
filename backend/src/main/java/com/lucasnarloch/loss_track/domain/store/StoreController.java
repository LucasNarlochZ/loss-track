package com.lucasnarloch.loss_track.domain.store;

import com.lucasnarloch.loss_track.domain.store.dtos.StoreRequestDTO;
import com.lucasnarloch.loss_track.domain.store.dtos.StoreResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/stores")
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponseDTO save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StoreRequestDTO storeRequestDto) {
        return storeService.save(tenantId(jwt), storeRequestDto);
    }

    @GetMapping("/{id}")
    public StoreResponseDTO findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return storeService.findById(tenantId(jwt), id);
    }

    @GetMapping
    public List<StoreResponseDTO> findAll(@AuthenticationPrincipal Jwt jwt) {
        return storeService.findAll(tenantId(jwt));
    }

    @PutMapping("/{id}")
    public StoreResponseDTO update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody StoreRequestDTO storeRequestDto) {
        return storeService.update(tenantId(jwt), id, storeRequestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        storeService.delete(tenantId(jwt), id);
    }

    private UUID tenantId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("tenant_id"));
    }
}
