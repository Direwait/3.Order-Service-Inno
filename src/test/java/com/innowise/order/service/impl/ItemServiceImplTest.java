package com.innowise.order.service.impl;

import com.innowise.order.dao.model.ItemModel;
import com.innowise.order.dao.repository.ItemRepository;
import com.innowise.order.service.dto.ItemDto;
import com.innowise.order.service.mapper.ItemMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    private UUID testItemId;
    private ItemModel testItemModel;
    private ItemDto testItemDto;
    private ItemDto createItemDto;
    private ItemDto updateItemDto;

    @BeforeEach
    void setUp() {
        testItemId = UUID.randomUUID();

        testItemModel = ItemModel.builder()
                .id(testItemId)
                .name("Test Item")
                .price(BigDecimal.valueOf(99.99))
                .build();

        testItemDto = ItemDto.builder()
                .id(testItemId)
                .name("Test Item")
                .price(BigDecimal.valueOf(99.99))
                .build();

        createItemDto = ItemDto.builder()
                .name("New Item")
                .price(BigDecimal.valueOf(49.99))
                .build();

        updateItemDto = ItemDto.builder()
                .name("Updated Item")
                .price(BigDecimal.valueOf(149.99))
                .build();
    }


    @Test
    void createItem_ShouldCreateItemSuccessfully() {
        ItemModel newItemModel = ItemModel.builder()
                .name("New Item")
                .price(BigDecimal.valueOf(49.99))
                .build();

        ItemModel savedItemModel = ItemModel.builder()
                .id(UUID.randomUUID())
                .name("New Item")
                .price(BigDecimal.valueOf(49.99))
                .build();

        ItemDto savedItemDto = ItemDto.builder()
                .id(savedItemModel.getId())
                .name("New Item")
                .price(BigDecimal.valueOf(49.99))
                .build();

        when(itemMapper.dtoToModel(createItemDto)).thenReturn(newItemModel);
        when(itemRepository.save(newItemModel)).thenReturn(savedItemModel);
        when(itemMapper.modelToDto(savedItemModel)).thenReturn(savedItemDto);

        ItemDto result = itemService.createItem(createItemDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Item");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
        assertThat(result.getId()).isNotNull();

        verify(itemMapper).dtoToModel(createItemDto);
        verify(itemRepository).save(newItemModel);
        verify(itemMapper).modelToDto(savedItemModel);
    }

    @Test
    void createItem_ShouldHandleNullFields() {
        ItemDto incompleteDto = ItemDto.builder()
                .name("Only Name")
                .build();

        ItemModel incompleteModel = ItemModel.builder()
                .name("Only Name")
                .build();

        when(itemMapper.dtoToModel(incompleteDto)).thenReturn(incompleteModel);
        when(itemRepository.save(incompleteModel)).thenReturn(incompleteModel);
        when(itemMapper.modelToDto(incompleteModel)).thenReturn(incompleteDto);

        ItemDto result = itemService.createItem(incompleteDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Only Name");
        assertThat(result.getPrice()).isNull();

        verify(itemRepository).save(incompleteModel);
    }

    @Test
    void getItemById_ShouldReturnItemWhenExists() {
        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(testItemModel));
        when(itemMapper.modelToDto(testItemModel)).thenReturn(testItemDto);

        ItemDto result = itemService.getItemById(testItemId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getName()).isEqualTo("Test Item");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(99.99));

        verify(itemRepository).findById(testItemId);
        verify(itemMapper).modelToDto(testItemModel);
    }

    @Test
    void getItemById_ShouldThrowExceptionWhenItemNotFound() {
        when(itemRepository.findById(testItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItemById(testItemId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Item not found with id: " + testItemId);

        verify(itemRepository).findById(testItemId);
        verify(itemMapper, never()).modelToDto(any());
    }

    @Test
    void getAllItems_ShouldReturnPageOfItems() {
        Pageable pageable = Pageable.ofSize(10);
        List<ItemModel> items = List.of(testItemModel);
        Page<ItemModel> itemPage = new PageImpl<>(items, pageable, items.size());

        when(itemRepository.findAll(pageable)).thenReturn(itemPage);
        when(itemMapper.modelToDto(testItemModel)).thenReturn(testItemDto);

        Page<ItemDto> result = itemService.getAllItems(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testItemId);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Item");

        verify(itemRepository).findAll(pageable);
        verify(itemMapper).modelToDto(testItemModel);
    }

    @Test
    void getAllItems_ShouldReturnEmptyPageWhenNoItems() {
        Pageable pageable = Pageable.ofSize(10);
        Page<ItemModel> emptyPage = Page.empty(pageable);

        when(itemRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<ItemDto> result = itemService.getAllItems(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(itemRepository).findAll(pageable);
        verify(itemMapper, never()).modelToDto(any());
    }

    @Test
    void getAllItems_ShouldRespectPagination() {
        Pageable pageable = Pageable.ofSize(5);
        List<ItemModel> items = List.of(testItemModel);
        Page<ItemModel> itemPage = new PageImpl<>(items, pageable, 20);

        when(itemRepository.findAll(pageable)).thenReturn(itemPage);
        when(itemMapper.modelToDto(testItemModel)).thenReturn(testItemDto);

        Page<ItemDto> result = itemService.getAllItems(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(20);
        assertThat(result.getTotalPages()).isEqualTo(4);

        verify(itemRepository).findAll(pageable);
    }

    @Test
    void updateItem_ShouldUpdateItemSuccessfully() {
        ItemModel updatedModel = ItemModel.builder()
                .id(testItemId)
                .name("Updated Item")
                .price(BigDecimal.valueOf(149.99))
                .build();

        ItemDto updatedDto = ItemDto.builder()
                .id(testItemId)
                .name("Updated Item")
                .price(BigDecimal.valueOf(149.99))
                .build();

        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(testItemModel));
        doNothing().when(itemMapper).updateFromDto(updateItemDto, testItemModel);
        when(itemRepository.save(testItemModel)).thenReturn(updatedModel);
        when(itemMapper.modelToDto(updatedModel)).thenReturn(updatedDto);

        ItemDto result = itemService.updateItem(testItemId, updateItemDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Item");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(149.99));

        verify(itemRepository).findById(testItemId);
        verify(itemMapper).updateFromDto(updateItemDto, testItemModel);
        verify(itemRepository).save(testItemModel);
        verify(itemMapper).modelToDto(updatedModel);
    }

    @Test
    void updateItem_ShouldThrowExceptionWhenItemNotFound() {
        when(itemRepository.findById(testItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.updateItem(testItemId, updateItemDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Item not found with id: " + testItemId);

        verify(itemRepository).findById(testItemId);
        verify(itemMapper, never()).updateFromDto(any(), any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_ShouldUpdateOnlyProvidedFields() {
        ItemDto partialUpdateDto = ItemDto.builder()
                .price(BigDecimal.valueOf(199.99))
                .build();

        ItemModel updatedModel = ItemModel.builder()
                .id(testItemId)
                .name("Test Item")
                .price(BigDecimal.valueOf(199.99))
                .build();

        ItemDto expectedDto = ItemDto.builder()
                .id(testItemId)
                .name("Test Item")
                .price(BigDecimal.valueOf(199.99))
                .build();

        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(testItemModel));
        doNothing().when(itemMapper).updateFromDto(partialUpdateDto, testItemModel);
        when(itemRepository.save(testItemModel)).thenReturn(updatedModel);
        when(itemMapper.modelToDto(updatedModel)).thenReturn(expectedDto);

        ItemDto result = itemService.updateItem(testItemId, partialUpdateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Item"); // unchanged
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(199.99));

        verify(itemMapper).updateFromDto(partialUpdateDto, testItemModel);
    }

    @Test
    void deleteItemById_ShouldDeleteItemWhenExists() {
        when(itemRepository.existsById(testItemId)).thenReturn(true);
        doNothing().when(itemRepository).deleteById(testItemId);

        itemService.deleteItemById(testItemId);

        verify(itemRepository).existsById(testItemId);
        verify(itemRepository).deleteById(testItemId);
    }

    @Test
    void deleteItemById_ShouldThrowExceptionWhenItemNotFound() {
        when(itemRepository.existsById(testItemId)).thenReturn(false);

        assertThatThrownBy(() -> itemService.deleteItemById(testItemId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Item not found with id: " + testItemId);

        verify(itemRepository).existsById(testItemId);
        verify(itemRepository, never()).deleteById(any());
    }

    @Test
    void deleteItemById_ShouldHandleDeleteWithOrders() {
        when(itemRepository.existsById(testItemId)).thenReturn(true);
        doNothing().when(itemRepository).deleteById(testItemId);

        itemService.deleteItemById(testItemId);

        verify(itemRepository).existsById(testItemId);
        verify(itemRepository).deleteById(testItemId);
    }

    @Test
    void createItem_ShouldTrimName() {
        ItemDto dtoWithSpaces = ItemDto.builder()
                .name("  Item With Spaces  ")
                .price(BigDecimal.valueOf(10.00))
                .build();

        ItemModel modelWithSpaces = ItemModel.builder()
                .name("  Item With Spaces  ")
                .price(BigDecimal.valueOf(10.00))
                .build();

        when(itemMapper.dtoToModel(dtoWithSpaces)).thenReturn(modelWithSpaces);
        when(itemRepository.save(modelWithSpaces)).thenReturn(modelWithSpaces);
        when(itemMapper.modelToDto(modelWithSpaces)).thenReturn(dtoWithSpaces);

        ItemDto result = itemService.createItem(dtoWithSpaces);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("  Item With Spaces  ");
    }

    @Test
    void updateItem_ShouldHandlePriceUpdateToNull() {
        ItemDto updateWithNullPrice = ItemDto.builder()
                .name("Updated Name")
                .price(null)
                .build();

        ItemModel updatedModel = ItemModel.builder()
                .id(testItemId)
                .name("Updated Name")
                .price(null)
                .build();

        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(testItemModel));
        doNothing().when(itemMapper).updateFromDto(updateWithNullPrice, testItemModel);
        when(itemRepository.save(testItemModel)).thenReturn(updatedModel);
        when(itemMapper.modelToDto(updatedModel)).thenReturn(updateWithNullPrice);

        ItemDto result = itemService.updateItem(testItemId, updateWithNullPrice);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getPrice()).isNull();
    }

    @Test
    void getItemById_ShouldReturnItemWithAllFields() {
        ItemModel fullItemModel = ItemModel.builder()
                .id(testItemId)
                .name("Complete Item")
                .price(BigDecimal.valueOf(999.99))
                .build();

        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(fullItemModel));
        when(itemMapper.modelToDto(fullItemModel)).thenReturn(ItemDto.builder()
                .id(testItemId)
                .name("Complete Item")
                .price(BigDecimal.valueOf(999.99))
                .build());

        ItemDto result = itemService.getItemById(testItemId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getName()).isEqualTo("Complete Item");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.99));
    }
}