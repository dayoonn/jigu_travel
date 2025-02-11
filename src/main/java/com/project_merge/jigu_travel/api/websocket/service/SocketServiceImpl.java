package com.project_merge.jigu_travel.api.websocket.service;

import com.project_merge.jigu_travel.api.Place.service.PlaceServiceImpl;
import com.project_merge.jigu_travel.api.user.service.UserService;
import com.project_merge.jigu_travel.api.websocket.dto.requestDto.LocationRequestDto;
import com.project_merge.jigu_travel.api.websocket.dto.responseDto.PlaceResponseDto;
import com.project_merge.jigu_travel.api.websocket.dto.responseDto.SubscribeInitResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocketServiceImpl implements SocketService{

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private PlaceServiceImpl placeServiceImpl;

    private final UserService userService;

    @Override
    public void sendMessage(String accessToken, LocationRequestDto locationRequestDto) {
        String destination = getDestination(locationRequestDto.getServiceUUID());

        List<PlaceResponseDto> result = placeServiceImpl.findNearbyPlace(locationRequestDto.getLatitude(), locationRequestDto.getLongitude(), 1);
        messagingTemplate.convertAndSend(destination, result);
    }

    @Override
    public SubscribeInitResponseDto createUUID(String accessToken) {
        UUID userUUID = userService.getCurrentUserUUID(); // 사용자 고유 UUID 가져오기
        return SubscribeInitResponseDto.builder()
                .serviceUUID(userUUID)
                .build();
    }

    private String getDestination(UUID serviceUUID) {
        return "/sub/" + serviceUUID;
    }
}
