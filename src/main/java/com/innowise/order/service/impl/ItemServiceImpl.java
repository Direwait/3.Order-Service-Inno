package com.innowise.order.service.impl;

import com.innowise.order.dao.model.ItemModel;
import com.innowise.order.dao.repository.ItemRepository;
import com.innowise.order.service.ItemService;
import com.innowise.order.service.dto.ItemDto;
import com.innowise.order.service.mapper.ItemMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemDto createItem(ItemDto itemDto) {
        var item = itemMapper.dtoToModel(itemDto);
        var savedItem = itemRepository.save(item);
        return itemMapper.modelToDto(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItemById(UUID itemId) {
        ItemModel item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + itemId));
        return itemMapper.modelToDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemDto> getAllItems(Pageable pageable) {
        return itemRepository.findAll(pageable)
                .map(itemMapper::modelToDto);
    }

    @Override
    @Transactional
    public ItemDto updateItem(UUID itemId, ItemDto itemDto) {
        ItemModel item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + itemId));
        itemMapper.updateFromDto(itemDto, item);
        ItemModel updatedItem = itemRepository.save(item);
        return itemMapper.modelToDto(updatedItem);
    }

    @Override
    @Transactional
    public void deleteItemById(UUID itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new EntityNotFoundException("Item not found with id: " + itemId);
        }
        itemRepository.deleteById(itemId);
    }
}
