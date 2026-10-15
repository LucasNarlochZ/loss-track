package com.lucasnarloch.loss_track.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthorizationIT extends AbstractIntegrationIT {
    @ParameterizedTest
    @ValueSource(strings = {"/products", "/stores", "/users", "/tenants/me"})
    void rejectsUnauthenticatedRequests(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"OWNER,200", "ADMIN,403", "EMPLOYEE,403"})
    void onlyOwnerCanUpdateTenant(String role, int expectedStatus) throws Exception {
        var owner = register(CNPJ);
        jdbc.sql("UPDATE users SET role = :role WHERE id = :id")
                .param("role", role).param("id", owner.getId()).update();
        String token = accessToken(CNPJ);
        mockMvc.perform(put("/tenants/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Updated tenant", "cnpj", CNPJ,
                                "plan", "BASIC", "status", "ACTIVE"))))
                .andExpect(status().is(expectedStatus));
        assertThat(jdbc.sql("SELECT name FROM tenants WHERE id = :id")
                .param("id", owner.getTenant().getId()).query(String.class).single())
                .isEqualTo(expectedStatus == 200 ? "Updated tenant" : "Integration tenant");
    }

    @ParameterizedTest
    @CsvSource({"OWNER,201", "ADMIN,201", "EMPLOYEE,403"})
    void onlyOwnersAndAdminsCanManageUsers(String role, int expectedStatus) throws Exception {
        var owner = register(CNPJ);
        jdbc.sql("UPDATE users SET role = :role WHERE id = :id")
                .param("role", role).param("id", owner.getId()).update();
        String token = accessToken(CNPJ);
        mockMvc.perform(post("/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Employee", "email", "employee@example.com",
                                "password", PASSWORD, "status", "ACTIVE", "role", "EMPLOYEE"))))
                .andExpect(status().is(expectedStatus));
        mockMvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().is(expectedStatus == 201 ? 200 : 403));
        assertThat(users.count()).isEqualTo(expectedStatus == 201 ? 2 : 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"products", "stores", "users"})
    void isolatesResourceReadsListsUpdatesAndDeletesBetweenTenants(String resource) throws Exception {
        register(CNPJ);
        var otherOwner = register(OTHER_CNPJ);
        String firstToken = accessToken(CNPJ);
        String secondToken = accessToken(OTHER_CNPJ);
        var payload = payload(resource);
        var created = mockMvc.perform(post("/" + resource).header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isCreated()).andReturn();
        String id = body(created).path("id").asString();
        String path = "/" + resource + "/" + id;

        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/" + resource).header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(resource.equals("users") ? 1 : 0));
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));

        var spoofed = new java.util.HashMap<>(payload);
        spoofed.put("tenantId", otherOwner.getTenant().getId().toString());
        if (resource.equals("users")) {
            spoofed.put("email", "spoofed@example.com");
        } else if (resource.equals("products")) {
            spoofed.put("sku", "SPOOFED");
        } else {
            spoofed.put("code", "SPOOFED");
            spoofed.put("cnpj", "11111111000111");
        }
        mockMvc.perform(post("/" + resource).header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(spoofed)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(body(created).path("tenantId").asString()));
    }

    static Map<String, Object> payload(String resource) {
        return switch (resource) {
            case "products" -> Map.of("name", "Product", "sku", "SKU-1", "barcode", "123456789",
                    "category", "Food", "unit", "UN", "costPrice", 12.50);
            case "stores" -> Map.of("name", "Store", "code", "STORE-1", "cnpj", "22222222000122",
                    "address", "Main Street", "city", "Curitiba", "state", "PR");
            case "users" -> Map.of("name", "Employee", "email", "employee@example.com", "password", PASSWORD,
                    "status", "ACTIVE", "role", "EMPLOYEE");
            default -> throw new IllegalArgumentException(resource);
        };
    }
}
