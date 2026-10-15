package com.lucasnarloch.loss_track.domain.product;

import com.lucasnarloch.loss_track.domain.product.dtos.ProductRequestDTO;
import com.lucasnarloch.loss_track.domain.product.dtos.ProductResponseDTO;
import com.lucasnarloch.loss_track.domain.product.exceptions.ProductNotFound;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantRepository;
import com.lucasnarloch.loss_track.domain.tenant.exceptions.TenantNotFound;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;

    public ProductService(ProductRepository productRepository, TenantRepository tenantRepository) {
        this.productRepository = productRepository;
        this.tenantRepository = tenantRepository;
    }

    public ProductResponseDTO save(UUID tenantId, ProductRequestDTO productRequestDto) {
        Tenant tenant = findTenantById(tenantId);
        Product product = new Product(
                tenant,
                productRequestDto.name(),
                productRequestDto.sku(),
                productRequestDto.barcode(),
                productRequestDto.category(),
                productRequestDto.unit(),
                productRequestDto.costPrice()
        );
        return new ProductResponseDTO(productRepository.save(product));
    }

    public ProductResponseDTO findById(UUID tenantId, UUID id) {
        Product product = findEntityById(tenantId, id);
        return new ProductResponseDTO(product);
    }

    public List<ProductResponseDTO> findAll(UUID tenantId) {
        List<Product> products = productRepository.findAllByTenantId(tenantId);
        return products.stream().map(ProductResponseDTO::new).toList();
    }

    public ProductResponseDTO update(UUID tenantId, UUID id, ProductRequestDTO productRequestDto) {
        Product product = findEntityById(tenantId, id);
        product.update(
                productRequestDto.name(),
                productRequestDto.sku(),
                productRequestDto.barcode(),
                productRequestDto.category(),
                productRequestDto.unit(),
                productRequestDto.costPrice()
        );
        return new ProductResponseDTO(productRepository.save(product));
    }

    public void delete(UUID tenantId, UUID id) {
        Product product = findEntityById(tenantId, id);
        productRepository.delete(product);
    }

    private Product findEntityById(UUID tenantId, UUID id) {
        return productRepository.findByIdAndTenantId(id, tenantId).orElseThrow(ProductNotFound::new);
    }

    private Tenant findTenantById(UUID id) {
        return tenantRepository.findById(id).orElseThrow(TenantNotFound::new);
    }
}
