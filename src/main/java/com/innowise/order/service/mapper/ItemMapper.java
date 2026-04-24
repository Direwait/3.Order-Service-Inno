package com.innowise.order.service.mapper;

import com.innowise.order.dao.model.ItemModel;
import com.innowise.order.service.dto.ItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "id", source = "id")
    ItemDto modelToDto(ItemModel itemModel);

    @Mapping(target = "id", source = "id")
    ItemModel dtoToModel(ItemDto orderItemDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromDto(ItemDto cardDto, @MappingTarget ItemModel itemModel);
}
