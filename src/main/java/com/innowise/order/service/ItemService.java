package com.innowise.order.service;

import com.innowise.order.service.dto.ItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ItemService {
    ItemDto createItem(ItemDto itemDto);

    ItemDto getItemById(UUID itemId);

    Page<ItemDto> getAllItems(Pageable pageable);

    ItemDto updateItem(UUID itemId, ItemDto itemDto);

    void deleteItemById(UUID itemId);
}
