package app.application.manage;

import app.core.data.manage.response.ResultMessage;

public interface SyncHandler {

    ResultMessage doSync(String password);

}
