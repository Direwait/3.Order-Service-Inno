package com.innowise.order.controller.impl;

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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
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

        OrderDto updateDto = OrderDto.builder()
                .userId(testUserId)
                .items(List.of(OrderItemDto.builder()
                        .itemId(testItemId)
                        .quantity(5)
                        .build()))
                .build();

        webTestClient.put()
                .uri("/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
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

    private UUID createTestOrder(UUID userId, UUID itemId, int quantity) {
        stubUserSuccess(userId);

        OrderDto orderDto = OrderDto.builder()
                .userId(userId)
                .items(List.of(OrderItemDto.builder()
                        .itemId(itemId)
                        .quantity(quantity)
                        .build()))
                .build();

        String response = webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        try {
            return UUID.fromString(objectMapper.readTree(response).get("orderDto").get("id").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void stubUserSuccess(UUID userId) {
        WireMock.stubFor(get("/users/" + userId)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                    "id": "%s",
                                    "name": "John",
                                    "surname": "Doe",
                                    "active": true
                                }
                                """, userId))));
    }
    @Test
    void createOrder_WithMultipleItems_ShouldCalculateTotalPriceCorrectly() {
        stubUserSuccess(testUserId);

        ItemModel item2 = ItemModel.builder()
                .name("Second Item")
                .price(BigDecimal.valueOf(50.00))
                .build();
        UUID item2Id = itemRepository.save(item2).getId();

        OrderDto orderDto = OrderDto.builder()
                .userId(testUserId)
                .items(List.of(
                        OrderItemDto.builder().itemId(testItemId).quantity(2).build(),
                        OrderItemDto.builder().itemId(item2Id).quantity(3).build()
                ))
                .build();

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.orderDto.totalPrice").isEqualTo(349.98);
    }


    @Test
    void updateOrder_WithEmptyItems_ShouldSetTotalPriceToZero() {
        stubUserSuccess(testUserId);
        UUID orderId = createTestOrder(testUserId, testItemId, 1);

        OrderDto updateDto = OrderDto.builder()
                .userId(testUserId)
                .items(List.of())
                .build();

        webTestClient.put()
                .uri("/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderDto.totalPrice").isEqualTo(0);
    }
}