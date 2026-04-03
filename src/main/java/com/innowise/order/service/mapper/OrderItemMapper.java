package com.innowise.order.service.mapper;

import com.innowise.order.dao.model.OrderItemModel;
import com.innowise.order.service.dto.OrderItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderItemMapper {

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "quantity", source = "quantity")
    OrderItemDto modelToDto(OrderItemModel orderItemModel);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item.id", source = "itemId")
    OrderItemModel dtoToModel(OrderItemDto orderItemDto);

}
