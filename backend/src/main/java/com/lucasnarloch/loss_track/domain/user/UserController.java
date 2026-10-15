package com.lucasnarloch.loss_track.domain.user;

import com.lucasnarloch.loss_track.domain.user.dtos.UserRequestDTO;
import com.lucasnarloch.loss_track.domain.user.dtos.UserResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserRequestDTO userRequestDto) {
        return userService.save(tenantId(jwt), userRequestDto);
    }

    @GetMapping("/{id}")
    public UserResponseDTO findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return userService.findById(tenantId(jwt), id);
    }

    @GetMapping
    public List<UserResponseDTO> findAll(@AuthenticationPrincipal Jwt jwt) {
        return userService.findAll(tenantId(jwt));
    }

    @PutMapping("/{id}")
    public UserResponseDTO update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UserRequestDTO userRequestDto) {
        return userService.update(tenantId(jwt), id, userRequestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        userService.delete(tenantId(jwt), id);
    }

    private UUID tenantId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("tenant_id"));
    }
}
