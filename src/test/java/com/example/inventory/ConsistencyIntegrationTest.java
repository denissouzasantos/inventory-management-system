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
class ConsistencyIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void projection_is_updated_synchronously_without_waits() {
        String base = "http://localhost:" + port;

        postJsonWithETag(base + "/api/commands/inventory/replace", Map.of("storeId", "S1", "sku", "SYNC1", "quantity", 5), null);
        // Ajuste para outra loja não precisa de If-Match (registro novo) – mantém consistência por partição storeId|sku
        postJsonWithETag(base + "/api/commands/inventory/adjust", Map.of("storeId", "S2", "sku", "SYNC1", "delta", 7), null);

        Map<?,?> g = rest.getForObject(base + "/api/query/inventory/global/SYNC1", Map.class);
        assertThat(g.get("quantity")).isEqualTo(12);
    }

    private String postJsonWithETag(String url, Map<String, Object> body, String ifMatch) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (ifMatch != null) headers.set("If-Match", ifMatch);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = rest.postForEntity(url, entity, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getHeaders().getETag();
    }
}


