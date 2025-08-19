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
class ValidationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void rejects_negative_quantity_on_replace() {
        String base = "http://localhost:" + port;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(
            "storeId", "S1",
            "sku", "VAL-1",
            "quantity", -1
        ), headers);
        ResponseEntity<String> response = rest.postForEntity(base + "/api/commands/inventory/replace", entity, String.class);
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void rejects_stale_etag() {
        String base = "http://localhost:" + port;

        // first write (no If-Match)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(
            "storeId", "S2",
            "sku", "VAL-2",
            "quantity", 1
        ), headers);
        ResponseEntity<String> r1 = rest.postForEntity(base + "/api/commands/inventory/replace", entity, String.class);
        assertThat(r1.getStatusCode().is2xxSuccessful()).isTrue();
        r1.getHeaders().getETag();

        // second write with wrong etag
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("If-Match", "\"999\"");
        entity = new HttpEntity<>(Map.of(
            "storeId", "S2",
            "sku", "VAL-2",
            "delta", 1
        ), headers);
        ResponseEntity<String> r2 = rest.postForEntity(base + "/api/commands/inventory/adjust", entity, String.class);
        assertThat(r2.getStatusCode().value()).isEqualTo(412);
    }
}


