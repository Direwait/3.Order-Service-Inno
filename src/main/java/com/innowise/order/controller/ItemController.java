package com.innowise.order.controller;

import com.innowise.order.service.dto.ItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

public interface ItemController {
    /**
     * Creates a new item.
     *
     * @param itemDto the item data (name and price)
     * @return created item with HTTP 201 status
     */
    ResponseEntity<ItemDto> createItem(ItemDto itemDto);

    /**
     * Retrieves an item by its ID.
     *
     * @param itemId the item UUID
     * @return the item with HTTP 200 status
     */
    ResponseEntity<ItemDto> getItemById(UUID itemId);

    /**
     * Retrieves all items with pagination support.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return a page of items with HTTP 200 status
     */
    ResponseEntity<Page<ItemDto>> getAllItems(Pageable pageable);

    /**
     * Updates an existing item by its ID.
     * Only name and price can be updated.
     *
     * @param itemId the item UUID
     * @param itemDto the updated item data
     * @return the updated item with HTTP 200 status
     */
    ResponseEntity<ItemDto> updateItemById(UUID itemId, ItemDto itemDto);

    /**
     * Deletes an item by its ID.
     * This operation is irreversible.
     *
     * @param itemId the item UUID
     * @return HTTP 204 status (No Content)
     */
    ResponseEntity<Void> deleteItem(UUID itemId);
}
