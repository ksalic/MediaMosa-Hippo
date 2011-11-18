package org.onehippo.forge.externalresource.api.scheduale.mediamosa;

import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id$
 */
public enum MediaMosaJobState {
    WAITING("WAITING"),
    INPROGRESS("INPROGRESS"),
    FINISHED("FINISHED"),
    CANCELLED("CANCELLED"),
    FAILED("FAILED"),
    UNKNOWN("UNKOWN");

    public static final Map<String, MediaMosaJobState> STATE_TYPE_MAP = new HashMap<String, MediaMosaJobState>();

    static {
        STATE_TYPE_MAP.put(WAITING.getState(), WAITING);
        STATE_TYPE_MAP.put(INPROGRESS.getState(), INPROGRESS);
        STATE_TYPE_MAP.put(FINISHED.getState(), FINISHED);
        STATE_TYPE_MAP.put(CANCELLED.getState(), CANCELLED);
        STATE_TYPE_MAP.put(FAILED.getState(), FAILED);
    }

    private final String state;

    MediaMosaJobState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public static MediaMosaJobState getType(final String state) {
        MediaMosaJobState type = STATE_TYPE_MAP.get(state);
        if (type != null) {
            return type;
        }
        return UNKNOWN;
    }
}
