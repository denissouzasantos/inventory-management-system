package com.example.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryFlowTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void endToEndFlow() throws Exception {
        String base = "http://localhost:" + port;
        // Replace stock for store A
        postJson(base + "/api/commands/inventory/replace", Map.of("storeId", "A", "sku", "SKU1", "quantity", 10));
        // Adjust stock for store B
        postJson(base + "/api/commands/inventory/adjust", Map.of("storeId", "B", "sku", "SKU1", "delta", 5));

        Thread.sleep(200); // wait for async bus

        Map<?,?> global = rest.getForObject(base + "/api/query/inventory/global/SKU1", Map.class);
        assertThat(global.get("quantity")).isEqualTo(15);

        Map<?,?> storeA = rest.getForObject(base + "/api/query/inventory/store/A/SKU1", Map.class);
        assertThat(storeA.get("quantity")).isEqualTo(10);
    }

    private void postJson(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = rest.postForEntity(url, entity, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}

