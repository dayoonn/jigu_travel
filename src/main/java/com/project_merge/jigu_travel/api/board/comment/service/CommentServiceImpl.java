package com.project_merge.jigu_travel.api.board.comment.service;

import com.project_merge.jigu_travel.api.auth.model.CustomUserDetails;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentRequestDto;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentResponseDto;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentUpdateRequestDto;
import com.project_merge.jigu_travel.api.board.comment.entity.Comment;
import com.project_merge.jigu_travel.api.board.comment.repository.CommentJpaRepository;
import com.project_merge.jigu_travel.api.board.entity.Board;
import com.project_merge.jigu_travel.api.board.repository.BoardJpaRepository;
import com.project_merge.jigu_travel.api.user.model.User;
import com.project_merge.jigu_travel.api.user.repository.UserRepository;
import com.project_merge.jigu_travel.global.common.CommonResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CommentServiceImpl implements CommentService{

    @Autowired
    private CommentJpaRepository commentJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardJpaRepository boardJpaRepository;

    // 댓글 조회
    @Override
    public List<CommentResponseDto> getCommentByBoardId(Long boardId) {
        Board board = boardJpaRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        return commentJpaRepository.findByBoardOrderByCreatedAtAsc(board).stream()
                .map(comment -> CommentResponseDto.builder()
                        .commentId(comment.getCommentId())
                        .boardId(boardId)
                        .comment(comment.getContent())
                        .nickname(comment.getUser().getNickname())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    // 댓글 작성
    @Override
    public CommentResponseDto addComment(CustomUserDetails userDetails, CommentRequestDto requestDto) {
        User user = userRepository.findByLoginIdAndDeletedFalse(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = boardJpaRepository.findById(requestDto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .board(board)
                .user(user)
                .content(requestDto.getContent())
                .build();

        commentJpaRepository.save(comment);

        return CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .boardId(board.getBoardId())
                .nickname(user.getNickname())
                .comment(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    // 댓글 수정
    @Override
    public CommentResponseDto updateComment(CustomUserDetails userDetails, Long commentId, CommentUpdateRequestDto requestDto) {
        Comment comment = commentJpaRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        comment.setContent(requestDto.getContent());
        commentJpaRepository.save(comment);

        return CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .comment(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    // 댓글 삭제
    @Override
    public void deleteComment(CustomUserDetails userDetails, Long commentId) {
        Comment comment = commentJpaRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        commentJpaRepository.delete(comment);
    }


}
