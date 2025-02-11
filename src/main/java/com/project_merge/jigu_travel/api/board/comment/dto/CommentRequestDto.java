package com.project_merge.jigu_travel.api.board.comment.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequestDto {
    private Long boardId;
    private String content;
}
