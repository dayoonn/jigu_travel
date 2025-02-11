package com.project_merge.jigu_travel.api.Place.service;

import com.project_merge.jigu_travel.api.Place.dto.requestDto.UserLocationRequestDto;
import com.project_merge.jigu_travel.api.Place.entity.UserLocation;
import com.project_merge.jigu_travel.api.Place.repository.UserLocationRepository;
import com.project_merge.jigu_travel.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final Map<UUID, List<UserLocationRequestDto>> userLocation = new HashMap<>();
    private final UserLocationRepository userLocationRepository;
    private final UserService userService;

    @Override
    public void saveUserLocation(UserLocationRequestDto locationRequestDto) {
        UUID userId = userService.getCurrentUserUUID();
        userLocation.computeIfAbsent(userId, k -> new ArrayList<>()).add(locationRequestDto);
    }

    // 여행 종료 시, HashMap에 저장된 위치 데이터를 DB로 저장 후 삭제
    @Override
    public void saveLocationToDB() {
        UUID userId = userService.getCurrentUserUUID();
        List<UserLocationRequestDto> locations = userLocation.get(userId);

        // 위치 데이터가 없으면 저장하지 않음
        if (locations == null || locations.isEmpty()) {
            System.out.println("저장할 위치 데이터가 없습니다. (userId: " + userId + ")");
            return;
        }

        // 위치 데이터를 JSON으로 변환하여 DB에 저장
        UserLocation locationEntity = new UserLocation();
        locationEntity.setUserId(userId);
        locationEntity.setLocations(locations); // JSON 변환하여 저장
        userLocationRepository.save(locationEntity);

        // HashMap에서 데이터 삭제 (메모리 정리)
        userLocation.remove(userId);
        System.out.println("여행 종료 - 위치 데이터 저장 완료! (userId: " + userId + ")");
    }

    // 마지막 저장된 위치 반환
    public UserLocationRequestDto getLastUserLocation() {
        UUID userId = userService.getCurrentUserUUID();
        List<UserLocationRequestDto> locations = userLocation.get(userId);

        if (locations != null && !locations.isEmpty()) {
            return locations.get(locations.size() - 1);  // 가장 최근 위치 반환
        }

        return null;  // 저장된 위치가 없으면 null 반환
    }

}
