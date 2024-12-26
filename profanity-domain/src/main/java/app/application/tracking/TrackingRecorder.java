package app.application.tracking;

import app.application.event.FilterEvent;

public interface TrackingRecorder {

    void recordTracking(FilterEvent event);

}
