package app.application.manage;

import java.util.List;

public interface WordManagementService {
    Object requestNewWord(String word, String reason, String severity);
    Object exceptionWord(String word, String reason, String severity);
    Object modifyWord(String word, String reason, String severity);

    boolean acceptWord(List<Long> requestId);
    Object getRequestStatus(Long requestId);
}
