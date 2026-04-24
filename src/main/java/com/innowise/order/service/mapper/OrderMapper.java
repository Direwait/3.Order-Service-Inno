package com.innowise.order.service.mapper;

import com.innowise.order.dao.model.OrderModel;
import com.innowise.order.service.dto.OrderDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class, ItemMapper.class})
public interface OrderMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "items", source = "items")
    OrderDto modelToDto(OrderModel orderModel);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    OrderModel dtoToModel(OrderDto orderDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateFromDto(OrderDto orderDto, @MappingTarget OrderModel orderModel);

    default void setOrderItemsRelation(OrderModel orderModel) {
        if (orderModel.getItems() != null) {
            orderModel.getItems().forEach(item -> item.setOrder(orderModel));
        }
    }
}
