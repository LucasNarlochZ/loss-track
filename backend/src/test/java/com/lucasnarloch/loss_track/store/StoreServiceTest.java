package com.lucasnarloch.loss_track.store;

import com.lucasnarloch.loss_track.domain.store.Store;
import com.lucasnarloch.loss_track.domain.store.StoreRepository;
import com.lucasnarloch.loss_track.domain.store.StoreService;
import com.lucasnarloch.loss_track.domain.store.StoreStatus;
import com.lucasnarloch.loss_track.domain.store.dtos.StoreRequestDTO;
import com.lucasnarloch.loss_track.domain.store.dtos.StoreResponseDTO;
import com.lucasnarloch.loss_track.domain.store.exceptions.StoreNotFound;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantPlan;
import com.lucasnarloch.loss_track.domain.tenant.TenantRepository;
import com.lucasnarloch.loss_track.domain.tenant.TenantStatus;
import com.lucasnarloch.loss_track.domain.tenant.exceptions.TenantNotFound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private StoreService storeService;

    @Test
    void saveShouldCreateStoreAndReturnResponseDto() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        StoreRequestDTO request = new StoreRequestDTO(
                "Loja Centro",
                "CENTRO",
                "11222333000144",
                "Rua Principal, 100",
                "Curitiba",
                "PR"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponseDTO response = storeService.save(tenantId, request);

        assertEquals("Loja Centro", response.name());
        assertEquals("CENTRO", response.code());
        assertEquals("11222333000144", response.cnpj());
        assertEquals("Rua Principal, 100", response.address());
        assertEquals("Curitiba", response.city());
        assertEquals("PR", response.state());
        assertEquals(StoreStatus.ACTIVE, response.status());

        ArgumentCaptor<Store> storeCaptor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(storeCaptor.capture());

        Store savedStore = storeCaptor.getValue();
        assertEquals(tenant, savedStore.getTenant());
        assertEquals("Loja Centro", savedStore.getName());
        assertEquals("CENTRO", savedStore.getCode());
        assertEquals("11222333000144", savedStore.getCnpj());
        assertEquals("Rua Principal, 100", savedStore.getAddress());
        assertEquals("Curitiba", savedStore.getCity());
        assertEquals("PR", savedStore.getState());
    }

    @Test
    void saveShouldThrowTenantNotFoundWhenTenantDoesNotExist() {
        UUID tenantId = UUID.randomUUID();
        StoreRequestDTO request = new StoreRequestDTO(
                "Loja Centro",
                "CENTRO",
                "11222333000144",
                "Rua Principal, 100",
                "Curitiba",
                "PR"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(TenantNotFound.class, () -> storeService.save(tenantId, request));
        verify(storeRepository, never()).save(any(Store.class));
    }

    @Test
    void findByIdShouldReturnResponseDtoWhenStoreExists() {
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        Store store = new Store(tenant, "Loja Centro", "CENTRO", "11222333000144", "Rua Principal, 100", "Curitiba", "PR");

        UUID tenantId = UUID.randomUUID();
        when(storeRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(store));

        StoreResponseDTO response = storeService.findById(tenantId, id);

        assertEquals("Loja Centro", response.name());
        assertEquals("CENTRO", response.code());
        assertEquals("11222333000144", response.cnpj());
        assertEquals(StoreStatus.ACTIVE, response.status());
    }

    @Test
    void findByIdShouldThrowStoreNotFoundWhenStoreDoesNotExist() {
        UUID id = UUID.randomUUID();

        UUID tenantId = UUID.randomUUID();
        when(storeRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(StoreNotFound.class, () -> storeService.findById(tenantId, id));
    }

    @Test
    void findAllShouldReturnResponseDtoList() {
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        Store firstStore = new Store(tenant, "Loja Centro", "CENTRO", "11222333000144", "Rua Principal, 100", "Curitiba", "PR");
        Store secondStore = new Store(tenant, "Loja Bairro", "BAIRRO", "55666777000188", "Rua Secundaria, 200", "Curitiba", "PR");

        UUID tenantId = UUID.randomUUID();
        when(storeRepository.findAllByTenantId(tenantId)).thenReturn(List.of(firstStore, secondStore));

        List<StoreResponseDTO> response = storeService.findAll(tenantId);

        assertEquals(2, response.size());
        assertEquals("Loja Centro", response.get(0).name());
        assertEquals("Loja Bairro", response.get(1).name());
    }

    @Test
    void updateShouldUpdateStoreAndReturnResponseDtoWhenStoreExists() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        Store store = new Store(tenant, "Loja Centro", "CENTRO", "11222333000144", "Rua Principal, 100", "Curitiba", "PR");
        StoreRequestDTO request = new StoreRequestDTO(
                "Loja Atualizada",
                "ATUAL",
                "99888777000166",
                "Avenida Brasil, 300",
                "Sao Paulo",
                "SP"
        );

        when(storeRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(store));
        when(storeRepository.save(store)).thenReturn(store);

        StoreResponseDTO response = storeService.update(tenantId, id, request);

        assertEquals("Loja Atualizada", response.name());
        assertEquals("ATUAL", response.code());
        assertEquals("99888777000166", response.cnpj());
        assertEquals("Avenida Brasil, 300", response.address());
        assertEquals("Sao Paulo", response.city());
        assertEquals("SP", response.state());
        assertEquals("Loja Atualizada", store.getName());
        assertEquals("ATUAL", store.getCode());
        assertEquals("99888777000166", store.getCnpj());

        verify(storeRepository).save(store);
    }

    @Test
    void updateShouldThrowStoreNotFoundWhenStoreDoesNotExist() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        StoreRequestDTO request = new StoreRequestDTO(
                "Loja Atualizada",
                "ATUAL",
                "99888777000166",
                "Avenida Brasil, 300",
                "Sao Paulo",
                "SP"
        );

        when(storeRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(StoreNotFound.class, () -> storeService.update(tenantId, id, request));
        verify(storeRepository, never()).save(any(Store.class));
    }

    @Test
    void deleteShouldDeleteStoreWhenStoreExists() {
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        Store store = new Store(tenant, "Loja Centro", "CENTRO", "11222333000144", "Rua Principal, 100", "Curitiba", "PR");

        UUID tenantId = UUID.randomUUID();
        when(storeRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(store));

        storeService.delete(tenantId, id);

        verify(storeRepository).delete(store);
    }

    @Test
    void deleteShouldThrowStoreNotFoundWhenStoreDoesNotExist() {
        UUID id = UUID.randomUUID();

        UUID tenantId = UUID.randomUUID();
        when(storeRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(StoreNotFound.class, () -> storeService.delete(tenantId, id));
        verify(storeRepository, never()).delete(any(Store.class));
    }
}
