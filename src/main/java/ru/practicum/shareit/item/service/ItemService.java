package ru.practicum.shareit.item.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingJpaRepository;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.comment.Comment;
import ru.practicum.shareit.comment.CommentJpaRepository;
import ru.practicum.shareit.comment.CommentMapper;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.comment.dto.CreateCommentDto;
import ru.practicum.shareit.core.exception.NotFoundException;
import ru.practicum.shareit.item.ItemJpaRepository;
import ru.practicum.shareit.item.dto.CreateItemDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.UpdateItemDto;
import ru.practicum.shareit.request.RequestJpaRepository;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.core.exception.FieldValidationException;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemService {

    ItemJpaRepository repo;
    BookingJpaRepository bookingRepo;
    UserService userService;
    ItemMapper mapper;
    BookingMapper bookingMapper;
    CommentMapper commentMapper;
    CommentJpaRepository commentRepo;

    RequestJpaRepository requestRepo;

    public List<ItemDto> getByUserId(Long userId, Pageable pageable) {

        Map<Long, List<Comment>> commentsByItem = commentRepo
                .findAllByItem_Owner_Id(userId)
                .stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId()));

        Map<Long, List<Booking>> bookingsByItem = bookingRepo
                .findAllByItemOwnerIdOrderByStartDesc(userId, pageable)
                .stream()
                .collect(Collectors.groupingBy(
                        booking -> booking.getItem().getId()));

        return repo.findAllByOwnerId(userId, pageable).stream()
                .peek(item -> {
                    List<Booking> bookings = bookingsByItem.getOrDefault(item.getId(), Collections.emptyList());
                    item.setNextBooking(bookingMapper.toShortBookingDto(getNextBooking(bookings)));
                    item.setLastBooking(bookingMapper.toShortBookingDto(getLastBooking(bookings)));
                })
                .peek(item -> item.setComments(commentsByItem.getOrDefault(item.getId(), Collections.emptyList())
                        .stream()
                        .map(commentMapper::toCommentDto)
                        .collect(Collectors.toList())))
                .map(mapper::toItemDto)
                .collect(Collectors.toList());
    }

    public List<ItemDto> searchByText(String text, Pageable pageable) {
        if (text.isBlank()) {
            return Collections.emptyList();
        }

        return repo.findAllByText(text, pageable)
                .stream()
                .map(mapper::toItemDto)
                .collect(Collectors.toList());
    }

    public ItemDto getById(long id, Long userId) {

        Item item = repo.findById(id).orElseThrow(() -> new NotFoundException("item", id));

        if (Objects.equals(item.getOwner().getId(), userId)) {
            List<Booking> bookings = bookingRepo.findAllByItemIdOrderByStartAsc(id);

            item.setNextBooking(bookingMapper.toShortBookingDto(getNextBooking(bookings)));
            item.setLastBooking(bookingMapper.toShortBookingDto(getLastBooking(bookings)));
        }

        List<CommentDto> comments = commentRepo.findAllByItemId(item.getId())
                .stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList());
        item.setComments(comments);

        return mapper.toItemDto(item);
    }

    public ItemDto create(Long userId, CreateItemDto dto) {

        User user = userService.getById(userId);
        Item newItem = mapper.toItem(dto);
        newItem.setOwner(user);

        if (dto.getRequestId() != null) {
            requestRepo.findById(dto.getRequestId()).ifPresentOrElse(newItem::setRequest, () -> {
                throw new NotFoundException("request", dto.getRequestId());
            });
        }
        return mapper.toItemDto(repo.save(newItem));
    }

    public ItemDto update(long id, Long userId, UpdateItemDto dto) {
        User user = userService.getById(userId);
        Item item = repo.findById(id).orElseThrow(() -> new NotFoundException("item", id));

        if (!user.equals(item.getOwner())) {
            throw new NotFoundException("item", id);
        }

        if (dto.getName() != null) {
            item.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription());
        }

        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }

        return mapper.toItemDto(repo.save(item));
    }

    public ItemDto delete(long id) {
        Item item = repo.findById(id).orElseThrow(() -> new NotFoundException("item", id));
        repo.deleteById(id);
        return mapper.toItemDto(repo.save(item));
    }

    private Booking getNextBooking(List<Booking> bookings) {
        List<Booking> filteredBookings = bookings.stream()
                .filter(booking -> booking.getStart().isAfter(LocalDateTime.now()) && !booking.getStatus().equals(BookingStatus.REJECTED))
                .sorted(Comparator.comparing(Booking::getStart))
                .collect(Collectors.toList());

        return filteredBookings.isEmpty() ? null : filteredBookings.get(0);
    }

    public List<Booking> getAllBookings(long id) {
        return bookingRepo.findAllByItemIdOrderByStartAsc(id);
    }

    private Booking getLastBooking(List<Booking> bookings) {
        List<Booking> filteredBookings = bookings.stream()
                .filter(booking -> booking.getEnd().isBefore(LocalDateTime.now()) ||
                        (booking.getStart().isBefore(LocalDateTime.now()) &&
                                booking.getEnd().isAfter(LocalDateTime.now())))
                .sorted(Comparator.comparing(Booking::getStart))
                .collect(Collectors.toList());

        return filteredBookings.isEmpty() ? null : filteredBookings.get(filteredBookings.size() - 1);
    }

    public CommentDto comment(long id, long userId, CreateCommentDto commentDto) {
        Item item = repo.findById(id).orElseThrow(() -> new NotFoundException("item", id));
        User user = userService.getById(userId);

        List<Booking> bookings = bookingRepo.findAllByBookerIdAndEndBeforeOrderByStartDesc(userId, LocalDateTime.now(), Pageable.unpaged());

        if (bookings.isEmpty()) {
            throw new FieldValidationException("userId", "User didn't book this item");
        }

        Comment comment = commentMapper.toComment(commentDto);
        comment.setItem(item);
        comment.setAuthor(user);
        comment.setCreated(LocalDateTime.now());

        return commentMapper.toCommentDto(commentRepo.save(comment));
    }
}
