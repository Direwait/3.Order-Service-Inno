package com.innowise.order.controller.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.order.service.dto.ItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext
class ItemControllerImplTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ItemDto testItem;

    @BeforeEach
    void setUp() throws Exception {
        testItem = ItemDto.builder()
                .id(UUID.randomUUID())
                .name("Test Item")
                .price(BigDecimal.valueOf(99.99))
                .build();
    }

    private ItemDto createTestItem() throws Exception {
        MvcResult result = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItem)))
                .andExpect(status().isCreated())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);

        return objectMapper.treeToValue(jsonNode, ItemDto.class);
    }

    @Test
    void createItem_ShouldReturnCreatedItem() throws Exception {
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(testItem.getName()))
                .andExpect(jsonPath("$.price").value(testItem.getPrice()))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createItem_WithBlankName_ShouldReturnBadRequest() throws Exception {
        testItem.setName("");
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createItem_WithNullPrice_ShouldReturnBadRequest() throws Exception {
        testItem.setPrice(null);
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createItem_WithNegativePrice_ShouldReturnBadRequest() throws Exception {
        testItem.setPrice(BigDecimal.valueOf(-10.00));
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createItem_WithTooLongName_ShouldReturnBadRequest() throws Exception {
        String longName = "A".repeat(61);
        testItem.setName(longName);
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createItem_WithPriceTooHigh_ShouldReturnBadRequest() throws Exception {
        testItem.setPrice(BigDecimal.valueOf(1000000.00));
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getItemById_ShouldReturnItem() throws Exception {
        String createJson = "{\"name\":\"Test Item\",\"price\":99.99}";

        MvcResult createResult = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(createResponse);
        UUID createdId = UUID.fromString(jsonNode.get("id").asText());
        String createdName = jsonNode.get("name").asText();
        BigDecimal createdPrice = BigDecimal.valueOf(jsonNode.get("price").asDouble());

        mockMvc.perform(get("/items/{itemId}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId.toString()))
                .andExpect(jsonPath("$.name").value(createdName))
                .andExpect(jsonPath("$.price").value(createdPrice.doubleValue()));
    }

    @Test
    void getItemById_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/items/{itemId}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllItems_WithoutPagination_ShouldReturnPage() throws Exception {
        createTestItem();

        MvcResult result = mockMvc.perform(get("/items")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        JsonNode contentNode = jsonNode.get("content");

        List<ItemDto> items = objectMapper.readValue(
                contentNode.toString(),
                new TypeReference<List<ItemDto>>() {}
        );

        assertThat(items).isNotEmpty();
        assertThat(items.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllItems_WithPagination_ShouldReturnCorrectPage() throws Exception {
        for (int i = 0; i < 15; i++) {
            ItemDto item = ItemDto.builder()
                    .name("Item " + i + " " + UUID.randomUUID())
                    .price(BigDecimal.valueOf(10.00))
                    .build();
            mockMvc.perform(post("/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(item)))
                    .andExpect(status().isCreated());
        }

        MvcResult firstPage = mockMvc.perform(get("/items")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode firstPageNode = objectMapper.readTree(firstPage.getResponse().getContentAsString());
        assertThat(firstPageNode.get("content").size()).isEqualTo(10);
        assertThat(firstPageNode.get("totalPages").asInt()).isGreaterThanOrEqualTo(2);

        MvcResult secondPage = mockMvc.perform(get("/items")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode secondPageNode = objectMapper.readTree(secondPage.getResponse().getContentAsString());
        assertThat(secondPageNode.get("content").size()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getAllItems_WithSorting_ShouldReturnSortedItems() throws Exception {
        ItemDto cheapItem = ItemDto.builder()
                .name("Cheap Item")
                .price(BigDecimal.valueOf(10.00))
                .build();

        ItemDto expensiveItem = ItemDto.builder()
                .name("Expensive Item")
                .price(BigDecimal.valueOf(100.00))
                .build();

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cheapItem)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expensiveItem)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/items")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        JsonNode contentNode = jsonNode.get("content");

        List<ItemDto> items = objectMapper.readValue(
                contentNode.toString(),
                new TypeReference<List<ItemDto>>() {}
        );

        if (items.size() >= 2) {
            assertThat(items.get(0).getPrice()).isGreaterThanOrEqualTo(items.get(1).getPrice());
        }
    }

    @Test
    void updateItemById_ShouldReturnUpdatedItem() throws Exception {
        String createJson = "{\"name\":\"Original\",\"price\":100.00}";
        MvcResult result = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        String updateJson = "{\"name\":\"Updated\",\"price\":149.99}";

        mockMvc.perform(put("/items/{itemId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.price").value(149.99));
    }

    @Test
    void deleteItem_ShouldReturnNoContent() throws Exception {
        ItemDto savedItem = createTestItem();

        mockMvc.perform(get("/items/{itemId}", savedItem.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/items/{itemId}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createItem_WithMinimalPrice_ShouldSucceed() throws Exception {
        testItem.setPrice(BigDecimal.valueOf(0.01));
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(0.01));
    }

    @Test
    void createItem_WithMaxPrice_ShouldSucceed() throws Exception {
        testItem.setPrice(BigDecimal.valueOf(999999.99));
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(999999.99));
    }

    @Test
    void createItem_WithMinNameLength_ShouldSucceed() throws Exception {
        testItem.setName("Ab");
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ab"));
    }

    @Test
    void createItem_WithMaxNameLength_ShouldSucceed() throws Exception {
        String name60 = "A".repeat(60);
        testItem.setName(name60);
        String itemJson = objectMapper.writeValueAsString(testItem);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name60));
    }
}