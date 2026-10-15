package com.lucasnarloch.loss_track.product;

import com.lucasnarloch.loss_track.domain.product.Product;
import com.lucasnarloch.loss_track.domain.product.ProductRepository;
import com.lucasnarloch.loss_track.domain.product.ProductService;
import com.lucasnarloch.loss_track.domain.product.dtos.ProductRequestDTO;
import com.lucasnarloch.loss_track.domain.product.dtos.ProductResponseDTO;
import com.lucasnarloch.loss_track.domain.product.exceptions.ProductNotFound;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantPlan;
import com.lucasnarloch.loss_track.domain.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void saveShouldCreateProductAndReturnResponseDto() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa", "12345678000190", TenantPlan.BASIC);
        ProductRequestDTO request = new ProductRequestDTO("Notebook", "NOTE-001", "123456", "Eletronicos", "UN", BigDecimal.TEN);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO response = productService.save(tenantId, request);

        assertEquals("Notebook", response.name());
        assertEquals("NOTE-001", response.sku());
        assertEquals("123456", response.barcode());
        assertEquals("Eletronicos", response.category());
        assertEquals("UN", response.unit());
        assertEquals(BigDecimal.TEN, response.costPrice());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertEquals(tenant, savedProduct.getTenant());
        assertEquals("Notebook", savedProduct.getName());
        assertEquals("NOTE-001", savedProduct.getSku());
        assertEquals("123456", savedProduct.getBarcode());
        assertEquals("Eletronicos", savedProduct.getCategory());
        assertEquals("UN", savedProduct.getUnit());
        assertEquals(BigDecimal.TEN, savedProduct.getCostPrice());
    }

    @Test
    void findByIdShouldReturnResponseDtoWhenProductExists() {
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa", "12345678000190", TenantPlan.BASIC);
        Product product = new Product(tenant, "Notebook", "NOTE-001", "123456", "Eletronicos", "UN", BigDecimal.TEN);

        UUID tenantId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.findById(tenantId, id);

        assertEquals("Notebook", response.name());
        assertEquals("NOTE-001", response.sku());
        assertEquals("123456", response.barcode());
        assertEquals("Eletronicos", response.category());
        assertEquals("UN", response.unit());
        assertEquals(BigDecimal.TEN, response.costPrice());
    }

    @Test
    void findByIdShouldThrowProductNotFoundWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();

        UUID tenantId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFound.class, () -> productService.findById(tenantId, id));
    }

    @Test
    void findAllShouldReturnResponseDtoList() {
        Tenant tenant = new Tenant("Empresa", "12345678000190", TenantPlan.BASIC);
        Product firstProduct = new Product(tenant, "Notebook", "NOTE-001", "123456", "Eletronicos", "UN", BigDecimal.TEN);
        Product secondProduct = new Product(tenant, "Cadeira", "CAD-001", "654321", "Moveis", "UN", BigDecimal.ONE);

        UUID tenantId = UUID.randomUUID();
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(firstProduct, secondProduct));

        List<ProductResponseDTO> response = productService.findAll(tenantId);

        assertEquals(2, response.size());
        assertEquals("Notebook", response.get(0).name());
        assertEquals("Cadeira", response.get(1).name());
    }

    @Test
    void updateShouldUpdateProductAndReturnResponseDtoWhenProductExists() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa", "12345678000190", TenantPlan.BASIC);
        Product product = new Product(tenant, "Notebook", "NOTE-001", "123456", "Eletronicos", "UN", BigDecimal.TEN);
        ProductRequestDTO request = new ProductRequestDTO("Monitor", "MON-001", "789012", "Informatica", "UN", BigDecimal.ONE);

        when(productRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponseDTO response = productService.update(tenantId, id, request);

        assertEquals("Monitor", response.name());
        assertEquals("MON-001", response.sku());
        assertEquals("789012", response.barcode());
        assertEquals("Informatica", response.category());
        assertEquals("UN", response.unit());
        assertEquals(BigDecimal.ONE, response.costPrice());
        assertEquals("Monitor", product.getName());
        assertEquals("MON-001", product.getSku());
        assertEquals("789012", product.getBarcode());
        assertEquals("Informatica", product.getCategory());
        assertEquals("UN", product.getUnit());
        assertEquals(BigDecimal.ONE, product.getCostPrice());

        verify(productRepository).save(product);
    }

    @Test
    void updateShouldThrowProductNotFoundWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ProductRequestDTO request = new ProductRequestDTO("Monitor", "MON-001", "789012", "Informatica", "UN", BigDecimal.ONE);

        when(productRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFound.class, () -> productService.update(tenantId, id, request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteShouldDeleteProductWhenProductExists() {
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa", "12345678000190", TenantPlan.BASIC);
        Product product = new Product(tenant, "Notebook", "NOTE-001", "123456", "Eletronicos", "UN", BigDecimal.TEN);

        UUID tenantId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(product));

        productService.delete(tenantId, id);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteShouldThrowProductNotFoundWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();

        UUID tenantId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFound.class, () -> productService.delete(tenantId, id));
        verify(productRepository, never()).delete(any(Product.class));
    }
}
