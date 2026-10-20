package com.lucasnarloch.loss_track.domain.user;

import com.lucasnarloch.loss_track.domain.store.Store;
import com.lucasnarloch.loss_track.domain.store.StoreRepository;
import com.lucasnarloch.loss_track.domain.store.exceptions.StoreNotFound;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantRepository;
import com.lucasnarloch.loss_track.domain.tenant.exceptions.TenantNotFound;
import com.lucasnarloch.loss_track.domain.user.dtos.UserRequestDTO;
import com.lucasnarloch.loss_track.domain.user.dtos.UserResponseDTO;
import com.lucasnarloch.loss_track.domain.user.exceptions.UserNotFound;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, StoreRepository storeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO save(UUID tenantId, UserRequestDTO userRequestDto) {
        Tenant tenant = findTenantById(tenantId);
        User user = new User(
                tenant,
                userRequestDto.name(),
                userRequestDto.email(),
                passwordEncoder.encode(userRequestDto.password()),
                userRequestDto.status(),
                userRequestDto.role()
        );
        return new UserResponseDTO(userRepository.save(user));
    }

    public UserResponseDTO findById(UUID tenantId, UUID id) {
        User user = findEntityById(tenantId, id);
        return new UserResponseDTO(user);
    }

    public List<UserResponseDTO> findAll(UUID tenantId) {
        List<User> users = userRepository.findAllByTenantId(tenantId);
        return users.stream().map(UserResponseDTO::new).toList();
    }

    public UserResponseDTO update(UUID tenantId, UUID id, UserRequestDTO userRequestDto) {
        User user = findEntityById(tenantId, id);
        user.update(
                userRequestDto.name(),
                userRequestDto.email(),
                passwordEncoder.encode(userRequestDto.password()),
                userRequestDto.status(),
                userRequestDto.role()
        );
        return new UserResponseDTO(userRepository.save(user));
    }

    public void delete(UUID tenantId, UUID id) {
        User user = findEntityById(tenantId, id);
        userRepository.delete(user);
    }

    public void assignToStore(UUID tenantId, UUID userId, UUID storeId) {
        User user = findEntityById(tenantId, userId);

        Store store = findStoreById(tenantId, storeId);

        user.assignToStore(store);
    }

    private User findEntityById(UUID tenantId, UUID id) {
        return userRepository.findByIdAndTenantId(id, tenantId).orElseThrow(UserNotFound::new);
    }

    private Tenant findTenantById(UUID id) {
        return tenantRepository.findById(id).orElseThrow(TenantNotFound::new);
    }

    private Store findStoreById(UUID tenantId, UUID id) {
        return storeRepository.findByIdAndTenantId(id, tenantId).orElseThrow(StoreNotFound::new);
    }
}
