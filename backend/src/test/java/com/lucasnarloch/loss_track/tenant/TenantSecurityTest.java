package com.lucasnarloch.loss_track.tenant;

import com.lucasnarloch.loss_track.config.SecurityConfig;
import com.lucasnarloch.loss_track.domain.tenant.TenantController;
import com.lucasnarloch.loss_track.domain.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantController.class)
@Import(SecurityConfig.class)
class TenantSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean(name = "accessJwtDecoder")
    private JwtDecoder jwtDecoder;

    @Test
    void employeeShouldNotUpdateTenantSettings() throws Exception {
        mockMvc.perform(put("/tenants/me")
                        .with(jwt()
                                .jwt(token -> token.claim("tenant_id", UUID.randomUUID().toString()))
                                .authorities(() -> "ROLE_EMPLOYEE"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Company",
                                  "cnpj": "12345678000190",
                                  "plan": "BASIC",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
