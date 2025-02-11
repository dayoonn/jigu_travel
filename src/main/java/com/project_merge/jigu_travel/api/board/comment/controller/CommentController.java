package com.project_merge.jigu_travel.api.board.comment.controller;

import com.project_merge.jigu_travel.api.auth.model.CustomUserDetails;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentRequestDto;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentResponseDto;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentUpdateRequestDto;
import com.project_merge.jigu_travel.api.board.comment.service.CommentService;
import com.project_merge.jigu_travel.api.board.comment.service.CommentServiceImpl;
import com.project_merge.jigu_travel.api.board.dto.requestDto.BoardUpdateResponseDto;
import com.project_merge.jigu_travel.global.common.BaseResponse;
import com.project_merge.jigu_travel.global.common.CommonResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/comments")
public class CommentController {


    private final CommentService commentService;

    // 댓글 조회
    @GetMapping("/{boardId}")
    public ResponseEntity<BaseResponse<List<CommentResponseDto>>> getComments(@PathVariable Long boardId) {
        List<CommentResponseDto> comments = commentService.getCommentByBoardId(boardId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<List<CommentResponseDto>>builder()
                        .code(HttpStatus.OK.value())
                        .data(comments)
                        .build());
    }

    // 댓글 작성
    @PostMapping
    public ResponseEntity<BaseResponse<CommentResponseDto>> postComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CommentRequestDto requestDto) {

//        return ResponseEntity.ok(commentService.createComment(request.getArticleId(), request.getBody()));

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    BaseResponse.<CommentResponseDto>builder()
                            .code(HttpStatus.UNAUTHORIZED.value())
                            .message("로그인이 필요합니다.")
                            .build()
            );
        }

        CommentResponseDto createdComment = commentService.addComment(userDetails, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.<CommentResponseDto>builder()
                .code(HttpStatus.OK.value())
                .data(createdComment)
                .build());
    }

    // 댓글 수정
    @PatchMapping("/{commentId}")
    public ResponseEntity<BaseResponse<CommentResponseDto>> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequestDto requestDto) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    BaseResponse.<CommentResponseDto>builder()
                            .code(HttpStatus.UNAUTHORIZED.value())
                            .message("로그인이 필요합니다.")
                            .build());
        }

        CommentResponseDto updatedComment = commentService.updateComment(userDetails, commentId, requestDto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<CommentResponseDto>builder()
                        .code(HttpStatus.OK.value())
                        .data(updatedComment)
                        .build());
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<BaseResponse<Void>> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    BaseResponse.<Void>builder()
                            .code(HttpStatus.UNAUTHORIZED.value())
                            .message("로그인이 필요합니다.")
                            .build());
        }

        commentService.deleteComment(userDetails, commentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(BaseResponse.<Void>builder()
                        .code(HttpStatus.NO_CONTENT.value())
                        .message("댓글이 삭제되었습니다.")
                        .build());
    }

}
