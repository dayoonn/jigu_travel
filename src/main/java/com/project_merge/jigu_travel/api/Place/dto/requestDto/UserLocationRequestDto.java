package com.project_merge.jigu_travel.api.Place.dto.requestDto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UserLocationRequestDto {
    private Double latitude;
    private Double longitude;
}
