package com.lucasnarloch.loss_track.domain.product;

import com.lucasnarloch.loss_track.domain.product.dtos.ProductRequestDTO;
import com.lucasnarloch.loss_track.domain.product.dtos.ProductResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProductRequestDTO productRequestDto) {
        return productService.save(tenantId(jwt), productRequestDto);
    }

    @GetMapping("/{id}")
    public ProductResponseDTO findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return productService.findById(tenantId(jwt), id);
    }

    @GetMapping
    public List<ProductResponseDTO> findAll(@AuthenticationPrincipal Jwt jwt) {
        return productService.findAll(tenantId(jwt));
    }

    @PutMapping("/{id}")
    public ProductResponseDTO update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ProductRequestDTO productRequestDto) {
        return productService.update(tenantId(jwt), id, productRequestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        productService.delete(tenantId(jwt), id);
    }

    private UUID tenantId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("tenant_id"));
    }

}
