package com.laweact.e2e.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

public final class ApiAssertions {

    private ApiAssertions() {
    }

    public static void assertSuccess(ResponseEntity<JsonNode> response, HttpStatus status) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("success").asBoolean()).isTrue();
        assertThat(response.getBody().path("data").isNull()).isFalse();
        assertThat(response.getBody().path("data").isMissingNode()).isFalse();
    }

    public static void assertErrorCode(
            ResponseEntity<JsonNode> response,
            HttpStatus status,
            String expectedCode
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("success").asBoolean()).isFalse();

        JsonNode errors = response.getBody().path("errors");
        assertThat(errors.isArray()).isTrue();
        assertThat(errors.size()).isGreaterThan(0);

        boolean found = false;
        for (JsonNode error : errors) {
            if (expectedCode.equals(error.path("code").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found)
                .as("esperava error.code=%s em %s", expectedCode, errors)
                .isTrue();
    }

    public static void assertErrorDetailContains(
            ResponseEntity<JsonNode> response,
            HttpStatus status,
            String detailFragment
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("success").asBoolean()).isFalse();

        JsonNode errors = response.getBody().path("errors");
        boolean found = false;
        for (JsonNode error : errors) {
            String detail = error.path("detail").asText("");
            if (detail.contains(detailFragment)) {
                found = true;
                break;
            }
        }
        assertThat(found)
                .as("esperava detail contendo '%s' em %s", detailFragment, errors)
                .isTrue();
    }
}
