package ru.practicum.shareit.booking;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Spy;
import ru.practicum.shareit.utils.TestUtils;
import ru.practicum.shareit.booking.dto.ShortBookingDto;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
class BookingMapperTest {
    @Spy
    BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);

    @Test
    void bookingToShortBookingDto() {
        User user = TestUtils.makeUser(1L);
        Item item = TestUtils.makeItem(1L, true, user);
        Booking booking = new Booking(1L, LocalDateTime.now(), LocalDateTime.now(), item, user, BookingStatus.WAITING);

        ShortBookingDto dto = bookingMapper.toShortBookingDto(booking);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getBookerId()).isEqualTo(1L);
    }
}
