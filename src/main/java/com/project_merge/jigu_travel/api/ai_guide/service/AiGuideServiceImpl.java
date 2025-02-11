package com.project_merge.jigu_travel.api.ai_guide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_merge.jigu_travel.api.Place.dto.requestDto.UserLocationRequestDto;
import com.project_merge.jigu_travel.api.Place.service.LocationService;
import com.project_merge.jigu_travel.api.ai_classification.entity.UserInterest;
import com.project_merge.jigu_travel.api.ai_classification.repository.UserInterestRepository;
import com.project_merge.jigu_travel.api.ai_guide.dto.AudioResponse;
import com.project_merge.jigu_travel.api.ai_guide.dto.TextResponse;
import com.project_merge.jigu_travel.api.ai_guide.dto.UserInputRequest;
import com.project_merge.jigu_travel.api.ai_guide.entity.ConversationHistory;
import com.project_merge.jigu_travel.api.ai_guide.fast.FastApiClient;
import com.project_merge.jigu_travel.api.ai_guide.repository.ConversationHistoryRepository;
import com.project_merge.jigu_travel.api.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiGuideServiceImpl implements AiGuideService {

    private final ConversationHistoryRepository conversationHistoryRepository;
    private final UserService userService;
    private final FastApiClient fastApiClient;
    private final UserInterestRepository interestRepository;
    private final LocationService locationService;


    // 텍스트 질문 처리
    @Override
    public TextResponse handleTextQuestion(String userQuestion, HttpSession session) {
        try {
            // 세션에서 대화 기록 가져오기
            UserInputRequest.ConversationHistory conversationHistory = getConversationHistoryFromSession(session);

            // UserInput 생성
            UserInputRequest userInput = createUserInput(userQuestion, conversationHistory);

            // FastAPI에 요청 보내기
            TextResponse response = fastApiClient.getAiGuideText(userInput);

            // 새로운 대화 항목 추가
            UserInputRequest.ConversationHistory.ConversationHistoryItem newItem = createConversationHistoryItem(response);

            // 세션과 DB에 대화 기록 저장
            saveConversationHistoryToSession(session, conversationHistory);
            saveConversationHistoryToDb(newItem, userInput, userService.getCurrentUserUUID());

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("API 요청 실패");
        }
    }

    // 음성 질문 처리
    @Override
    public AudioResponse handleAudioAndQuestion(MultipartFile audioFile, HttpSession session) {
        try {
            // 세션에서 대화 기록 가져오기
            UserInputRequest.ConversationHistory conversationHistory = getConversationHistoryFromSession(session);

            // 로그 출력
            System.out.println("FastAPI 요청 시작...");
            System.out.println("audioFile name: " + audioFile.getOriginalFilename());

            // UserInput 생성
            UserInputRequest userInput = createUserInput(" ", conversationHistory);

            // FastAPI 서버로 음성 파일과 질문을 전송
            AudioResponse response = fastApiClient.getAiGuideAudio(audioFile, convertUserInputToJson(userInput));

            // 새로운 대화 항목 추가
            UserInputRequest.ConversationHistory.ConversationHistoryItem newItem = createConversationHistoryItem(response);

            // 세션과 DB에 대화 기록 저장
            saveConversationHistoryToSession(session, conversationHistory);
            saveConversationHistoryToDb(newItem, userInput, userService.getCurrentUserUUID());

            return response; // FastAPI 응답 그대로 반환
        } catch (Exception e) {
            System.out.println("Error during API call: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("API 요청 실패");
        }
    }

    //대화 기록 처리 함수
    @Override
    public List<ConversationHistory> handleChatHistory( int offset, int limit) {
//        System.out.println("서비스 확인");
        //getConversationHistoryFromSession(session);
        UUID userId = userService.getCurrentUserUUID();
        Pageable pageable = PageRequest.of(offset / limit, limit); // offset을 limit으로 나눠서 페이지 번호로 설정
        Page<ConversationHistory> pageResult = conversationHistoryRepository.findByUserIdOrderByConversationDatetimeDesc(userId, pageable);
        // 콘솔에 페이지 결과 출력
//        System.out.println("Page Result: " + pageResult.getContent());
        return pageResult.getContent();  // 페이지에서 내용만 가져옴
    }

    // 대화 기록 저장 함수
    private void saveConversationHistoryToSession(HttpSession session, UserInputRequest.ConversationHistory conversationHistory) {
        session.setAttribute("conversation_history", conversationHistory);
    }

    // 대화 기록 DB에 저장
    private void saveConversationHistoryToDb(UserInputRequest.ConversationHistory.ConversationHistoryItem item, UserInputRequest userInput, UUID userId) {
        ConversationHistory history = new ConversationHistory();
        history.setUserId(userId);
        history.setConversationQuestion(item.getUser_question());
        history.setConversationAnswer(item.getAssistant_response());
        history.setConversationLatitude(userInput.getLatitude());
        history.setConversationLongitude(userInput.getLongitude());
        history.setConversationDatetime(LocalDateTime.now());

        conversationHistoryRepository.save(history);
        System.out.println("DB 저장 완료");
    }


    // 대화 기록 세션에서 가져오기
    private UserInputRequest.ConversationHistory getConversationHistoryFromSession(HttpSession session) {
        UserInputRequest.ConversationHistory conversationHistory = (UserInputRequest.ConversationHistory) session.getAttribute("conversation_history");
        if (conversationHistory == null) {
            conversationHistory = new UserInputRequest.ConversationHistory();
            conversationHistory.setHistory(new ArrayList<>());
        }
        return conversationHistory;
    }

    // UserInput 생성
    private UserInputRequest createUserInput(String userQuestion, UserInputRequest.ConversationHistory conversationHistory) {
        UserInputRequest userInput = new UserInputRequest();
        userInput.setUser_question(userQuestion);
//        userInput.setUser_category(Arrays.asList("맛집", "힐링"));
        Optional<UserInterest> userInterest = interestRepository.findByUserId(userService.getCurrentUserUUID());
        if(userInterest.isPresent()) {
            List<String> category = Arrays.asList(userInterest.get().getInterest(),userInterest.get().getInterest2());
            userInput.setUser_category(category);
        }else{
            System.out.println("‼️사용자 선호 카테고리 없음 : 기본 값으로 설정‼");
            userInput.setUser_category(Arrays.asList("맛집", "힐링")); //테스트용 기본값
        }

        UserLocationRequestDto lastUserLocation = locationService.getLastUserLocation();
        if(lastUserLocation != null) {
            userInput.setLatitude(lastUserLocation.getLatitude());
            userInput.setLongitude(lastUserLocation.getLongitude());
        }else{
            System.out.println("‼️최근 사용자 위치 받아오기 실패 : 기본 위치로 설정‼");
            userInput.setLatitude(37.508373); // 기본 위치
            userInput.setLongitude(127.103565); // 기본 위치
        }

        userInput.setConversation_history(conversationHistory);
        return userInput;
    }

    // UserInput 객체를 JSON으로 변환
    private String convertUserInputToJson(UserInputRequest userInput) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(userInput);
    }

    // 대화 항목 생성
    private UserInputRequest.ConversationHistory.ConversationHistoryItem createConversationHistoryItem(Object response) {
        UserInputRequest.ConversationHistory.ConversationHistoryItem newItem = new UserInputRequest.ConversationHistory.ConversationHistoryItem();
        if (response instanceof TextResponse) {
            newItem.setUser_question(((TextResponse) response).getConversation_history().getHistory().get(0).getUser_question());
            newItem.setAssistant_response(((TextResponse) response).getConversation_history().getHistory().get(0).getAssistant_response());
        } else if (response instanceof AudioResponse) {
            newItem.setUser_question(((AudioResponse) response).getConversation_history().getHistory().get(0).getUser_question());
            newItem.setAssistant_response(((AudioResponse) response).getConversation_history().getHistory().get(0).getAssistant_response());
        }
        return newItem;
    }
}
