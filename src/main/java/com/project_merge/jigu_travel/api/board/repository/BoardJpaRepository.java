package com.project_merge.jigu_travel.api.board.repository;


import com.project_merge.jigu_travel.api.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardJpaRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByBoardId(Long boardId);
}