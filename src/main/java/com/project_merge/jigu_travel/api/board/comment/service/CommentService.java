package com.project_merge.jigu_travel.api.board.comment.service;

import com.project_merge.jigu_travel.api.auth.model.CustomUserDetails;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentRequestDto;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentResponseDto;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentUpdateRequestDto;
import com.project_merge.jigu_travel.global.common.CommonResponseDto;

import java.util.List;

public interface CommentService {
    List<CommentResponseDto> getCommentByBoardId(Long boardId);
    CommentResponseDto addComment(CustomUserDetails userDetails, CommentRequestDto requestDto);
    CommentResponseDto updateComment(CustomUserDetails userDetails, Long commentId, CommentUpdateRequestDto requestDto);
    void deleteComment(CustomUserDetails userDetails, Long commentId);

//    CommonResponseDto createComment(CustomUserDetails userDetails, Long boardId, String comment);
//    CommonResponseDto deleteComment(CustomUserDetails userDetails, Long commentId);



}
