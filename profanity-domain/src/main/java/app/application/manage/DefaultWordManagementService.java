package app.application.manage;

import app.domain.manage.WordManagementRepository;
import app.domain.manage.WordManagementRequest;
import app.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static app.dto.message.SuccessBusinessMessage.REQUEST_SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultWordManagementService implements WordManagementService {
    private final WordManagementRepository wordManagementRepository;

    @Override
    public MessageResponse requestNewWord(UUID requestUserId, String word, String reason, String severity) {
        wordManagementRepository.save(WordManagementRequest.builder()
                .requestUserId(requestUserId)
                .word(word)
                .reason(reason)
                .severity(severity)
                .requestType("NEW")
                .build());
        return MessageResponse.of(REQUEST_SUCCESS);
    }

    @Override
    public MessageResponse exceptionWord(UUID requestUserId, String word, String reason, String severity) {
        wordManagementRepository.save(WordManagementRequest.builder()
                .requestUserId(requestUserId)
                .word(word)
                .reason(reason)
                .severity(severity)
                .requestType("EXCEPTION")
                .build());
        return MessageResponse.of(REQUEST_SUCCESS);
    }

    @Override
    public MessageResponse modifyWord(UUID requestUserId, String word, String reason, String severity) {
        wordManagementRepository.save(WordManagementRequest.builder()
                .requestUserId(requestUserId)
                .word(word)
                .reason(reason)
                .severity(severity)
                .requestType("MODIFY")
                .build());
        return MessageResponse.of(REQUEST_SUCCESS);
    }

    @Override
    public boolean acceptWord(List<Long> requestId) {
        return false;
    }
}
