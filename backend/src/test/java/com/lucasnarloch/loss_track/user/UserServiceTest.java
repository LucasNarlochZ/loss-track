package com.lucasnarloch.loss_track.user;

import com.lucasnarloch.loss_track.domain.store.Store;
import com.lucasnarloch.loss_track.domain.store.StoreRepository;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantPlan;
import com.lucasnarloch.loss_track.domain.tenant.TenantRepository;
import com.lucasnarloch.loss_track.domain.tenant.TenantStatus;
import com.lucasnarloch.loss_track.domain.tenant.exceptions.TenantNotFound;
import com.lucasnarloch.loss_track.domain.user.User;
import com.lucasnarloch.loss_track.domain.user.UserRepository;
import com.lucasnarloch.loss_track.domain.user.UserService;
import com.lucasnarloch.loss_track.domain.user.UserStatus;
import com.lucasnarloch.loss_track.domain.user.UserRole;
import com.lucasnarloch.loss_track.domain.user.dtos.UserRequestDTO;
import com.lucasnarloch.loss_track.domain.user.dtos.UserResponseDTO;
import com.lucasnarloch.loss_track.domain.user.exceptions.UserNotFound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @Test
    void saveShouldCreateUserAndReturnResponseDto() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        UserRequestDTO request = new UserRequestDTO(
                "Lucas Narloch",
                "lucas@example.com",
                "secret",
                UserStatus.ACTIVE,
                UserRole.ADMIN
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO response = userService.save(tenantId, request);

        assertEquals("Lucas Narloch", response.name());
        assertEquals("lucas@example.com", response.email());
        assertEquals(UserStatus.ACTIVE, response.status());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(tenant, savedUser.getTenant());
        assertEquals("Lucas Narloch", savedUser.getName());
        assertEquals("lucas@example.com", savedUser.getEmail());
        assertEquals("hashed-secret", savedUser.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
    }

    @Test
    void saveShouldThrowTenantNotFoundWhenTenantDoesNotExist() {
        UUID tenantId = UUID.randomUUID();
        UserRequestDTO request = new UserRequestDTO(
                "Lucas Narloch",
                "lucas@example.com",
                "secret",
                UserStatus.ACTIVE,
                UserRole.ADMIN
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(TenantNotFound.class, () -> userService.save(tenantId, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByIdShouldReturnResponseDtoWhenUserExists() {
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        User user = new User(tenant, "Lucas Narloch", "lucas@example.com", "secret", UserStatus.ACTIVE);

        UUID tenantId = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(user));

        UserResponseDTO response = userService.findById(tenantId, id);

        assertEquals("Lucas Narloch", response.name());
        assertEquals("lucas@example.com", response.email());
        assertEquals(UserStatus.ACTIVE, response.status());
    }

    @Test
    void findByIdShouldThrowUserNotFoundWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();

        UUID tenantId = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> userService.findById(tenantId, id));
    }

    @Test
    void findAllShouldReturnResponseDtoList() {
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        User firstUser = new User(tenant, "Lucas Narloch", "lucas@example.com", "secret", UserStatus.ACTIVE);
        User secondUser = new User(tenant, "Maria Silva", "maria@example.com", "password", UserStatus.INACTIVE);

        UUID tenantId = UUID.randomUUID();
        when(userRepository.findAllByTenantId(tenantId)).thenReturn(List.of(firstUser, secondUser));

        List<UserResponseDTO> response = userService.findAll(tenantId);

        assertEquals(2, response.size());
        assertEquals("Lucas Narloch", response.get(0).name());
        assertEquals("Maria Silva", response.get(1).name());
    }

    @Test
    void updateShouldUpdateUserAndReturnResponseDtoWhenUserExists() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        User user = new User(tenant, "Lucas Narloch", "lucas@example.com", "secret", UserStatus.ACTIVE);
        UserRequestDTO request = new UserRequestDTO(
                "Lucas Atualizado",
                "lucas.atualizado@example.com",
                "new-secret",
                UserStatus.INACTIVE,
                UserRole.ADMIN
        );

        when(userRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-secret")).thenReturn("new-secret-hash");
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO response = userService.update(tenantId, id, request);

        assertEquals("Lucas Atualizado", response.name());
        assertEquals("lucas.atualizado@example.com", response.email());
        assertEquals(UserStatus.INACTIVE, response.status());
        assertEquals("Lucas Atualizado", user.getName());
        assertEquals("lucas.atualizado@example.com", user.getEmail());
        assertEquals("new-secret-hash", user.getPasswordHash());

        verify(userRepository).save(user);
    }

    @Test
    void updateShouldThrowUserNotFoundWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        UserRequestDTO request = new UserRequestDTO(
                "Lucas Atualizado",
                "lucas.atualizado@example.com",
                "new-secret",
                UserStatus.INACTIVE,
                UserRole.ADMIN
        );

        UUID tenantId = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> userService.update(tenantId, id, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteShouldDeleteUserWhenUserExists() {
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa LTDA", "12345678000199", TenantPlan.PRO, TenantStatus.ACTIVE);
        User user = new User(tenant, "Lucas Narloch", "lucas@example.com", "secret", UserStatus.ACTIVE);

        UUID tenantId = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(user));

        userService.delete(tenantId, id);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteShouldThrowUserNotFoundWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();

        UUID tenantId = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> userService.delete(tenantId, id));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void assignsUserToStore() {
        Tenant tenant = mock(Tenant.class);

        User user = new User(
                tenant,
                "Lucas",
                "lucas@example.com",
                "hashed-password",
                UserStatus.ACTIVE,
                UserRole.EMPLOYEE
        );
        Store store = mock(Store.class);
        when(userRepository.findByIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(user));
        when(storeRepository.findByIdAndTenantId(storeId, tenantId))
                .thenReturn(Optional.of(store));

        userService.assignToStore(tenantId, userId, storeId);

        assertSame(store, user.getStore());

    }
}
