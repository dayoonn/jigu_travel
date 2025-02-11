package com.project_merge.jigu_travel.api.board.service;

import com.nimbusds.jose.util.Resource;
import com.project_merge.jigu_travel.api.auth.model.CustomUserDetails;
import com.project_merge.jigu_travel.api.board.comment.dto.CommentResponseDto;
import com.project_merge.jigu_travel.api.board.comment.service.CommentService;
import com.project_merge.jigu_travel.api.board.comment.service.CommentServiceImpl;
import com.project_merge.jigu_travel.api.board.dto.reponseDto.AttachmentDto;
import com.project_merge.jigu_travel.api.board.dto.reponseDto.BoardResponseDto;
import com.project_merge.jigu_travel.api.board.dto.reponseDto.BoardUpdateRequestDto;
import com.project_merge.jigu_travel.api.board.dto.requestDto.BoardUpdateResponseDto;
import com.project_merge.jigu_travel.api.board.entity.Attachment;
import com.project_merge.jigu_travel.api.board.entity.Board;
import com.project_merge.jigu_travel.api.board.repository.AttachmentJpaRepository;
import com.project_merge.jigu_travel.api.board.repository.BoardJpaRepository;
import com.project_merge.jigu_travel.api.user.model.User;
import com.project_merge.jigu_travel.api.user.repository.UserRepository;
import com.project_merge.jigu_travel.api.board.dto.requestDto.BoardPostsRequestDto;
import com.project_merge.jigu_travel.api.user.service.UserService;
import com.project_merge.jigu_travel.global.common.CommonResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardServiceImpl implements BoardService {

    @Autowired
    private CommentService commentService;

    @Autowired
    private BoardJpaRepository boardJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttachmentJpaRepository attachmentJpaRepository;

    @Value("${file.upload-dir}") // ✅ application.properties 값 가져오기
    private String uploadDir;

    // 📌 게시글 목록 조회 (로그인 없이 가능)

    public Page<BoardResponseDto> getBoardList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("boardId").descending());
        Page<Board> boardPage = boardJpaRepository.findAll(pageRequest);

        return boardPage.map(board -> BoardResponseDto.builder()
                .boardId(board.getBoardId())
                .userId(board.getUser().getLoginId())
                .nickname(board.getUser().getNickname())
                .title(board.getTitle())
                .content(board.getContent())
                .inquiryType(board.getInquiryType())
                .likes(board.getLikes())
                .createdAt(board.getCreatedAt())
                .build());
    }
    // 파일 저장 메서드
    private List<Attachment> saveFiles(Board board, List<MultipartFile> files) {
        List<Attachment> attachmentList = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
//                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);

//                    if (!originalFilename.matches(".*\\.(jpg|png|jpeg|gif|pdf|txt|docx)$")) {
//                        throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
//                    }

                    List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "png", "jpeg", "gif", "pdf", "txt", "docx");

                    if (originalFilename != null && originalFilename.contains(".")) {
                        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다: " + fileExtension);
                        }
                    }

                    // ✅ 게시글 ID 별 폴더 생성
                    String boardFolderPath = uploadDir + File.separator + board.getBoardId();
                    File boardFolder = new File(boardFolderPath);
                    if (!boardFolder.exists()) {
                        boardFolder.mkdirs(); // ✅ 폴더가 없으면 생성
                    }

                    File saveFile = new File(boardFolderPath + File.separator + originalFilename);

                    try {
                        file.transferTo(saveFile);
                        System.out.println("✅ [DEBUG] 파일 저장 완료: " + saveFile.getAbsolutePath());
                    } catch (IOException e) {
                        throw new RuntimeException("파일 저장 중 오류 발생", e);
                    }

                    Attachment attachment = Attachment.builder()
                            .board(board)
                            .fileName(originalFilename)
                            .filePath(board.getBoardId() + "/" + originalFilename)
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .build();

                    attachmentList.add(attachment);
                }
            }
        }
        return attachmentList;
    }


    // 게시글 작성
    @Override
    public CommonResponseDto createBoard(CustomUserDetails userDetails, String title, String content, String inquiryType, List<MultipartFile> files) {
        User user = userRepository.findByLoginIdAndDeletedFalse(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board newBoard = Board.builder()
                .user(user)
                .title(title)
                .content(content)
                .inquiryType(inquiryType)
                .likes(0L)
                .build();

        boardJpaRepository.save(newBoard);

        List<Attachment> attachments = saveFiles(newBoard, files);
        newBoard.setAttachments(attachments);
        boardJpaRepository.save(newBoard);

        return CommonResponseDto.builder()
                .message("SUCCESS")
                .build();
    }

    // 게시글 수정
    @Override
    public BoardUpdateResponseDto modifyBoard(CustomUserDetails userDetails, Long boardId, String title, String content, String inquiryType, List<MultipartFile> files, List<String> removedFileNames) {
        User user = userRepository.findByLoginIdAndDeletedFalse(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = boardJpaRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!user.getUserId().equals(board.getUser().getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("게시글을 수정할 권한이 없습니다.");
        }

        // ✅ 기존 첨부파일 유지 (기존 파일을 삭제하지 않음)
//        List<Attachment> existingAttachments = board.getAttachments();

        // ✅ 기존 파일 삭제 (사용자가 제거한 파일이 있을 때)
        if (removedFileNames != null && !removedFileNames.isEmpty()) {
            for (String fileName : removedFileNames) {
                String filePath = uploadDir + File.separator + boardId + File.separator + fileName;
                File fileToDelete = new File(filePath);
                if (fileToDelete.exists()) {
                    fileToDelete.delete();
                    System.out.println("🗑 삭제된 파일: " + filePath);
                }
            }
            attachmentJpaRepository.deleteByBoardAndFileNameIn(board, removedFileNames);
        }

        // ✅ 새로운 파일 저장
//        List<Attachment> newAttachments = saveFiles(board, files);
////        existingAttachments.addAll(newAttachments);
////        board.setAttachments(newAttachments);
//        List<Attachment> updatedAttachments = new ArrayList<>(board.getAttachments()); // 기존 파일 유지
//        updatedAttachments.addAll(newAttachments); // 새 파일 추가
//        board.setAttachments(updatedAttachments);

        // ✅ 기존 첨부파일 + 새 첨부파일을 저장
//        board.setAttachments(existingAttachments);
        // ✅ 기존 첨부파일 리스트 가져오기
        List<Attachment> existingAttachments = board.getAttachments();

        // ✅ 새로운 파일 저장
        List<Attachment> newAttachments = saveFiles(board, files);

        // ✅ 기존 첨부파일 리스트에 새로운 파일 추가 (setAttachments 사용 ❌)
        existingAttachments.addAll(newAttachments);

        board.setTitle(title);
        board.setContent(content);
        board.setInquiryType(inquiryType);
        boardJpaRepository.save(board);

        return BoardUpdateResponseDto.builder()
                .title(board.getTitle())
                .content(board.getContent())
                .inquiryType(board.getInquiryType())
                .build();

    }

    // 📌 게시글 삭제

    @Override
    public CommonResponseDto boardDeletion(CustomUserDetails userDetails, Long boardId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = boardJpaRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!user.getUserId().equals(board.getUser().getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("게시글을 삭제할 권한이 없습니다.");
        }

        boardJpaRepository.delete(board);

        return CommonResponseDto.builder()
                .message("SUCCESS")
                .build();
    }

    // 📌 게시글 상세조회
    @Override
    public BoardResponseDto getBoardDetail(Long boardId) {
        Board board = boardJpaRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // ✅ 첨부파일이 `null`이면 빈 리스트 반환
        List<AttachmentDto> attachments = board.getAttachments() != null ?
                board.getAttachments().stream()
                        .map(att -> new AttachmentDto(att.getFileId(), att.getFileName(), att.getFileSize()))
                        .collect(Collectors.toList())
                : new ArrayList<>();

        List<CommentResponseDto> comments = commentService.getCommentByBoardId(boardId);

        return BoardResponseDto.builder()
                .boardId(board.getBoardId())
                .userId(board.getUser().getLoginId())
                .nickname(board.getUser().getNickname())
                .title(board.getTitle())
                .content(board.getContent())
                .inquiryType(board.getInquiryType())
                .createdAt(board.getCreatedAt())
                .attachments(attachments)
                .comments(comments)
                .build();
    }
}
