package com.lucasnarloch.loss_track.domain.store;

import com.lucasnarloch.loss_track.domain.store.dtos.StoreRequestDTO;
import com.lucasnarloch.loss_track.domain.store.dtos.StoreResponseDTO;
import com.lucasnarloch.loss_track.domain.store.exceptions.StoreNotFound;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantRepository;
import com.lucasnarloch.loss_track.domain.tenant.exceptions.TenantNotFound;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StoreService {
    private final StoreRepository storeRepository;
    private final TenantRepository tenantRepository;

    public StoreService(StoreRepository storeRepository, TenantRepository tenantRepository) {
        this.storeRepository = storeRepository;
        this.tenantRepository = tenantRepository;
    }

    public StoreResponseDTO save(UUID tenantId, StoreRequestDTO storeRequestDto) {
        Tenant tenant = findTenantById(tenantId);
        Store store = new Store(
                tenant,
                storeRequestDto.name(),
                storeRequestDto.code(),
                storeRequestDto.cnpj(),
                storeRequestDto.address(),
                storeRequestDto.city(),
                storeRequestDto.state()
        );
        return new StoreResponseDTO(storeRepository.save(store));
    }

    public StoreResponseDTO findById(UUID tenantId, UUID id) {
        Store store = findEntityById(tenantId, id);
        return new StoreResponseDTO(store);
    }

    public List<StoreResponseDTO> findAll(UUID tenantId) {
        List<Store> stores = storeRepository.findAllByTenantId(tenantId);
        return stores.stream().map(StoreResponseDTO::new).toList();
    }

    public StoreResponseDTO update(UUID tenantId, UUID id, StoreRequestDTO storeRequestDto) {
        Store store = findEntityById(tenantId, id);
        store.update(
                storeRequestDto.name(),
                storeRequestDto.code(),
                storeRequestDto.cnpj(),
                storeRequestDto.address(),
                storeRequestDto.city(),
                storeRequestDto.state()
        );
        return new StoreResponseDTO(storeRepository.save(store));
    }

    public void delete(UUID tenantId, UUID id) {
        Store store = findEntityById(tenantId, id);
        storeRepository.delete(store);
    }

    private Store findEntityById(UUID tenantId, UUID id) {
        return storeRepository.findByIdAndTenantId(id, tenantId).orElseThrow(StoreNotFound::new);
    }

    private Tenant findTenantById(UUID id) {
        return tenantRepository.findById(id).orElseThrow(TenantNotFound::new);
    }
}
