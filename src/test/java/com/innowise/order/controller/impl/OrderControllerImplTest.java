package com.innowise.order.controller.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.order.dao.model.ItemModel;
import com.innowise.order.dao.repository.ItemRepository;
import com.innowise.order.service.dto.OrderDto;
import com.innowise.order.service.dto.OrderItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class OrderControllerImplTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ItemRepository itemRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID testUserId;
    private UUID testItemId;

    @BeforeEach
    void setUp() {
        WireMock.reset();
        testUserId = UUID.randomUUID();

        ItemModel item = ItemModel.builder()
                .name("Test Item")
                .price(BigDecimal.valueOf(99.99))
                .build();
        testItemId = itemRepository.save(item).getId();
    }

    @Test
    void getOrderById_Success() {
        stubUserSuccess(testUserId);
        UUID orderId = createTestOrder(testUserId, testItemId, 2);

        webTestClient.get()
                .uri("/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderDto.id").isEqualTo(orderId.toString())
                .jsonPath("$.orderDto.userId").isEqualTo(testUserId.toString());
    }

    @Test
    void getOrderById_NotFound() {
        webTestClient.get()
                .uri("/orders/{orderId}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getOrdersWithDateRangeAndStatuses() {
        stubUserSuccess(testUserId);
        createTestOrder(testUserId, testItemId, 1);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders")
                        .queryParam("status", "CREATED")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody();
    }

    @Test
    void getOrdersByUserId_Success() {
        stubUserSuccess(testUserId);
        createTestOrder(testUserId, testItemId, 1);
        createTestOrder(testUserId, testItemId, 2);

        webTestClient.get()
                .uri("/orders/users/{userId}", testUserId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(2);
    }

    @Test
    void updateOrder_Success() {
        stubUserSuccess(testUserId);
        UUID orderId = createTestOrder(testUserId, testItemId, 1);

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("userId", testUserId.toString());
        updateRequest.put("userEmail", "test@mail.com");
        updateRequest.put("items", List.of(
                Map.of("itemId", testItemId.toString(), "quantity", 5)
        ));

        webTestClient.put()
                .uri("/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderDto.totalPrice").isEqualTo(499.95);
    }

    @Test
    void softDelete_Success() {
        stubUserSuccess(testUserId);
        UUID orderId = createTestOrder(testUserId, testItemId, 1);

        webTestClient.patch()
                .uri("/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class).isEqualTo(true);
    }

    @Test
    void hardDelete_Success() {
        stubUserSuccess(testUserId);
        UUID orderId = createTestOrder(testUserId, testItemId, 1);

        webTestClient.delete()
                .uri("/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createOrder_WithMultipleItems_ShouldCalculateTotalPriceCorrectly() {
        stubUserSuccess(testUserId);

        ItemModel item2 = ItemModel.builder()
                .name("Second Item")
                .price(BigDecimal.valueOf(50.00))
                .build();
        UUID item2Id = itemRepository.save(item2).getId();

        Map<String, Object> request = new HashMap<>();
        request.put("userId", testUserId.toString());
        request.put("userEmail", "test@mail.com");
        request.put("items", List.of(
                Map.of("itemId", testItemId.toString(), "quantity", 2),
                Map.of("itemId", item2Id.toString(), "quantity", 3)
        ));

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.orderDto.totalPrice").isEqualTo(349.98);
    }

    @Test
    void createOrder_WithEmptyItems_ShouldReturnBadRequest() {
        stubUserSuccess(testUserId);

        OrderDto orderDto = OrderDto.builder()
                .userId(testUserId)
                .userEmail("test@mail.com")
                .items(List.of())
                .build();

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.items").exists();
    }

    @Test
    void updateOrder_WithEmptyItems_ShouldReturnBadRequest() {
        stubUserSuccess(testUserId);
        UUID orderId = createTestOrder(testUserId, testItemId, 1);

        OrderDto updateDto = OrderDto.builder()
                .userId(testUserId)
                .userEmail("test@mail.com")
                .items(List.of())
                .build();

        webTestClient.put()
                .uri("/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.items").exists();
    }

    @Test
    void createOrder_WithoutUserEmail_ShouldReturnBadRequest() {
        stubUserSuccess(testUserId);

        OrderDto orderDto = OrderDto.builder()
                .userId(testUserId)
                .items(List.of(OrderItemDto.builder()
                        .itemId(testItemId)
                        .quantity(1)
                        .build()))
                .build();

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.userEmail").exists();
    }

    @Test
    void createOrder_WithNonExistentItem_ShouldReturnBadRequest() {
        stubUserSuccess(testUserId);

        OrderDto orderDto = OrderDto.builder()
                .userId(testUserId)
                .userEmail("test@mail.com")
                .items(List.of(OrderItemDto.builder()
                        .itemId(UUID.randomUUID())
                        .quantity(1)
                        .build()))
                .build();

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private UUID createTestOrder(UUID userId, UUID itemId, int quantity) {
        stubUserSuccess(userId);

        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId.toString());
        request.put("userEmail", "test@mail.com");
        request.put("items", List.of(
                Map.of(
                        "itemId", itemId.toString(),
                        "quantity", quantity
                )
        ));

        String response = webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        try {
            return UUID.fromString(objectMapper.readTree(response).get("orderDto").get("id").asText());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse order ID from response: " + response, e);
        }
    }

    private void stubUserSuccess(UUID userId) {
        WireMock.stubFor(get(urlEqualTo("/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                    "id": "%s",
                                    "name": "John",
                                    "surname": "Doe",
                                    "email": "test@mail.com",
                                    "active": true
                                }
                                """, userId))));
        WireMock.stubFor(get(urlPathEqualTo("/users/by-email"))
                .withQueryParam("email", equalTo("test@mail.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                            {
                                "id": "%s",
                                "name": "John",
                                "surname": "Doe",
                                "email": "test@mail.com",
                                "active": true
                            }
                            """, userId))));
        WireMock.stubFor(get(urlPathEqualTo("/users/batch"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                            [{
                                "id": "%s",
                                "name": "John",
                                "surname": "Doe",
                                "email": "test@mail.com",
                                "active": true
                            }]
                            """, userId))));
    }
}