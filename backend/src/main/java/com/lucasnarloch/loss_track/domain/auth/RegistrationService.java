package com.lucasnarloch.loss_track.domain.auth;

import com.lucasnarloch.loss_track.domain.auth.dtos.RegisterRequestDTO;
import com.lucasnarloch.loss_track.domain.auth.dtos.RegisterResponseDTO;
import com.lucasnarloch.loss_track.domain.auth.exceptions.CnpjAlreadyRegistered;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantPlan;
import com.lucasnarloch.loss_track.domain.tenant.TenantRepository;
import com.lucasnarloch.loss_track.domain.tenant.dtos.TenantResponseDTO;
import com.lucasnarloch.loss_track.domain.user.User;
import com.lucasnarloch.loss_track.domain.user.UserRepository;
import com.lucasnarloch.loss_track.domain.user.UserRole;
import com.lucasnarloch.loss_track.domain.user.UserStatus;
import com.lucasnarloch.loss_track.domain.user.dtos.UserResponseDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(TenantRepository tenantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (tenantRepository.existsByCnpj(request.cnpj())) {
            throw new CnpjAlreadyRegistered();
        }
        Tenant tenant = tenantRepository.save(new Tenant(request.tenantName(), request.cnpj(), TenantPlan.BASIC));
        User owner = userRepository.save(new User(
                tenant,
                request.ownerName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                UserStatus.ACTIVE,
                UserRole.OWNER
        ));
        return new RegisterResponseDTO(new TenantResponseDTO(tenant), new UserResponseDTO(owner));
    }
}
