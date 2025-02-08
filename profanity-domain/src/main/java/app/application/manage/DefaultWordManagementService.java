package app.application.manage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultWordManagementService implements WordManagementService {
    @Override
    public Object requestNewWord(String word, String reason, String severity) {
        return null;
    }

    @Override
    public Object exceptionWord(String word, String reason, String severity) {
        return null;
    }

    @Override
    public Object modifyWord(String word, String reason, String severity) {
        return null;
    }

    @Override
    public boolean acceptWord(List<Long> requestId) {
        return false;
    }

    @Override
    public Object getRequestStatus(Long requestId) {
        return null;
    }
}
