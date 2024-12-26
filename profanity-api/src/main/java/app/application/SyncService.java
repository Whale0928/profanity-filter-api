package app.application;

import app.application.manage.SyncHandler;
import app.core.data.manage.response.ResultMessage;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SyncService {

    private final SyncHandler syncHandler;

    public SyncService(SyncHandler syncHandler) {
        this.syncHandler = syncHandler;
    }

    public ResultMessage doSync(String password) {
        Objects.requireNonNull(password, "비밀번호를 제공해야 합니다.");
        return syncHandler.doSync(password);
    }
}
