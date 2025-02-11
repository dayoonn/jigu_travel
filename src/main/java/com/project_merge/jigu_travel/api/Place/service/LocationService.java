package com.project_merge.jigu_travel.api.Place.service;

import com.project_merge.jigu_travel.api.Place.dto.requestDto.UserLocationRequestDto;

public interface LocationService {
    void saveUserLocation(UserLocationRequestDto locationRequestDto);
    void saveLocationToDB();
    UserLocationRequestDto getLastUserLocation();
}
