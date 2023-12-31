package ru.practicum.shareit.request;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.request.dto.CreateRequestDto;
import ru.practicum.shareit.request.dto.RequestDto;
import ru.practicum.shareit.request.service.RequestService;
import ru.practicum.shareit.core.pagination.PaginationMapper;

import javax.validation.Valid;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/requests")
public class RequestController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final RequestService requestService;

    @PostMapping
    public RequestDto createRequest(
            @RequestHeader(name = USER_ID_HEADER) long userId,
            @Valid @RequestBody CreateRequestDto dto
    ) {
        return requestService.createRequest(dto, userId);
    }

    @GetMapping
    public List<RequestDto> getOwnRequests(@RequestHeader(name = USER_ID_HEADER) long userId) {
        return requestService.getOwnRequests(userId);
    }

    @GetMapping("/all")
    public List<RequestDto> getOtherRequests(
            @RequestHeader(name = USER_ID_HEADER) long userId,
            @PositiveOrZero @RequestParam(required = false) Integer from,
            @PositiveOrZero @RequestParam(required = false) Integer size
    ) {
        return requestService.getOtherRequests(userId, PaginationMapper.toPageable(from, size));
    }

    @GetMapping("/{requestId}")
    public RequestDto getById(@PathVariable long requestId, @RequestHeader(name = USER_ID_HEADER) long userId) {
        return requestService.getById(requestId, userId);
    }
}
