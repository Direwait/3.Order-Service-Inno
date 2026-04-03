package com.innowise.order.controller.impl;

import com.innowise.order.controller.ItemController;
import com.innowise.order.service.dto.ItemDto;
import com.innowise.order.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/items")
public class ItemControllerImpl implements ItemController {
    private final ItemService itemService;

    @PostMapping()
    @Override
    public ResponseEntity<ItemDto> createItem(@Valid @RequestBody ItemDto itemDto) {
        var item = itemService.createItem(itemDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/{itemId}")
    @Override
    public ResponseEntity<ItemDto> getItemById(@PathVariable UUID itemId) {
        var itemById = itemService.getItemById(itemId);
        return ResponseEntity.ok(itemById);
    }

    @GetMapping()
    @Override
    public ResponseEntity<Page<ItemDto>> getAllItems(@PageableDefault(size = 10) Pageable pageable) {
        var allItems = itemService.getAllItems(pageable);
        return ResponseEntity.ok(allItems);
    }

    @PutMapping("/{itemId}")
    @Override
    public ResponseEntity<ItemDto> updateItemById(
            @PathVariable UUID itemId,
            @Valid @RequestBody ItemDto itemDto
    ) {
        var updatedItem = itemService.updateItem(itemId, itemDto);
        return ResponseEntity.ok(updatedItem);
    }

    @DeleteMapping("/{itemId}")
    @Override
    public ResponseEntity<Void> deleteItem(@PathVariable UUID itemId) {
        itemService.deleteItemById(itemId);
        return ResponseEntity.noContent().build();
    }
}
