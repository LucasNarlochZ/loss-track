package com.lucasnarloch.loss_track.integration;

import com.lucasnarloch.loss_track.domain.auth.dtos.RegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PersistenceIT extends AbstractIntegrationIT {
    @ParameterizedTest
    @ValueSource(strings = {"products", "stores", "users"})
    void persistsResourceCreationUpdatesAndDeletion(String resource) throws Exception {
        var owner = register(CNPJ);
        String token = accessToken(CNPJ);
        var payload = new HashMap<>(AuthorizationIT.payload(resource));
        var created = mockMvc.perform(post("/" + resource).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        UUID id = UUID.fromString(body(created).path("id").asString());
        assertThat(jdbc.sql("SELECT tenant_id FROM " + resource + " WHERE id = :id")
                .param("id", id).query(UUID.class).single()).isEqualTo(owner.getTenant().getId());
        if (resource.equals("products")) {
            assertThat(jdbc.sql("SELECT cost_price FROM products WHERE id = :id").param("id", id)
                    .query(java.math.BigDecimal.class).single()).isEqualByComparingTo("12.50");
        }

        payload.put("name", "Updated name");
        String path = "/" + resource + "/" + id;
        mockMvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated name"));
        assertThat(jdbc.sql("SELECT name FROM " + resource + " WHERE id = :id")
                .param("id", id).query(String.class).single()).isEqualTo("Updated name");
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated name"));
        mockMvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(jdbc.sql("SELECT count(*) FROM " + resource + " WHERE id = :id")
                .param("id", id).query(Long.class).single()).isZero();
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"products", "stores", "users"})
    void databaseEnforcesTenantScopedUniquenessAndForeignKeys(String resource) throws Exception {
        register(CNPJ);
        var other = register(OTHER_CNPJ);
        String token = accessToken(CNPJ);
        var payload = new HashMap<>(AuthorizationIT.payload(resource));
        var created = mockMvc.perform(post("/" + resource).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isCreated()).andReturn();
        UUID id = UUID.fromString(body(created).path("id").asString());
        String columns = switch (resource) {
            case "products" -> "tenant_id, name, sku, unit, cost_price";
            case "stores" -> "tenant_id, name, code, cnpj, address, city, state";
            case "users" -> "tenant_id, name, email, password_hash";
            default -> throw new IllegalArgumentException(resource);
        };
        String values = resource.equals("stores")
                ? "tenant_id, name, code, '44444444000144', address, city, state" : columns;
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO " + resource + " (id, " + columns + ") "
                        + "SELECT gen_random_uuid(), " + values + " FROM " + resource + " WHERE id = :id")
                .param("id", id).update()).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("UPDATE " + resource + " SET tenant_id = :tenant WHERE id = :id")
                .param("tenant", UUID.randomUUID()).param("id", id).update())
                .isInstanceOf(DataIntegrityViolationException.class);

        if (resource.equals("stores")) {
            payload.put("cnpj", "33333333000133");
        }
        mockMvc.perform(post("/" + resource).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(OTHER_CNPJ))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(other.getTenant().getId().toString()));
    }

    @Test
    void rollsBackTenantCreationWhenOwnerPersistenceFails() {
        assertThatThrownBy(() -> registration.register(new RegisterRequestDTO(
                "Rollback tenant", CNPJ, "x".repeat(256), EMAIL, PASSWORD)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.sql("SELECT count(*) FROM tenants").query(Long.class).single()).isZero();
        assertThat(users.count()).isZero();
    }

    @Test
    void sameEmailCanAuthenticateIndependentlyInDifferentTenants() throws Exception {
        var first = register(CNPJ);
        var second = register(OTHER_CNPJ);
        String firstToken = accessToken(CNPJ);
        String secondToken = accessToken(OTHER_CNPJ);
        assertThat(first.getId()).isNotEqualTo(second.getId());
        mockMvc.perform(get("/tenants/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(first.getTenant().getId().toString()));
        mockMvc.perform(get("/tenants/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(second.getTenant().getId().toString()));
    }
}
