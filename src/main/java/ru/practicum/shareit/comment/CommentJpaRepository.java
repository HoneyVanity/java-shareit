package ru.practicum.shareit.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentJpaRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByItemId(long itemId);

    @Query("select c " +
            "from Comment c " +
            "where c.item.owner.id = :userId "
    )
    List<Comment> findAllByItem_Owner_Id(@Param("userId") long userId);
}