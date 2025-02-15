package app.application.manage;

import app.dto.response.MessageResponse;

import java.util.List;
import java.util.UUID;

public interface WordManagementService {
    MessageResponse requestNewWord(UUID requestUserId, String word, String reason, String severity);

    MessageResponse exceptionWord(UUID requestUserId, String word, String reason, String severity);

    MessageResponse modifyWord(UUID requestUserId, String word, String reason, String severity);

    boolean acceptWord(List<Long> requestId);
}
