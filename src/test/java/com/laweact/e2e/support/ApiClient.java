package com.laweact.e2e.support;

import java.util.List;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Equivalente ao RequestMaker do exemplo Node: centraliza HTTP nos testes E2E.
 */
public class ApiClient {

    private final TestRestTemplate restTemplate;
    private String bearerToken;

    public ApiClient(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void logout() {
        this.bearerToken = null;
    }

    public void authenticate(String token) {
        this.bearerToken = token;
    }

    public ResponseEntity<JsonNode> post(String path, Object body) {
        return restTemplate.exchange(path, HttpMethod.POST, jsonEntity(body), JsonNode.class);
    }

    public ResponseEntity<JsonNode> patch(String path, Object body) {
        return restTemplate.exchange(path, HttpMethod.PATCH, jsonEntity(body), JsonNode.class);
    }

    public ResponseEntity<JsonNode> get(String path) {
        return restTemplate.exchange(path, HttpMethod.GET, jsonEntity(null), JsonNode.class);
    }

    public ResponseEntity<JsonNode> delete(String path) {
        return restTemplate.exchange(path, HttpMethod.DELETE, jsonEntity(null), JsonNode.class);
    }

    public ResponseEntity<JsonNode> delete(String path, Object body) {
        return restTemplate.exchange(path, HttpMethod.DELETE, jsonEntity(body), JsonNode.class);
    }

    private HttpEntity<?> jsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }
        return body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
    }
}
