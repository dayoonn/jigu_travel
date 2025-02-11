package com.project_merge.jigu_travel.api.board.comment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDto {
    private Long commentId;
    private Long boardId;
    private String nickname;
    private String comment;
    private LocalDateTime createdAt;

}
