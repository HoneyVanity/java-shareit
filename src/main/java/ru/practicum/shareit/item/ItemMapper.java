package ru.practicum.shareit.item;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.shareit.item.dto.CreateItemDto;
import ru.practicum.shareit.item.dto.ItemDto;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    Item toItem(CreateItemDto dto);

    @Mapping(target = "requestId", source = "request.id")
    ItemDto toItemDto(Item item);
}
