package org.onehippo.forge.externalresource.api.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id$
 */
public enum SynchronizationState {
    SYNCHRONIZED("synchronized"),
    UNSYNCHRONIZED("unsynchronized"),
    BROKEN("broken"),
    UNKNOWN("unkown"),
    //unused
    INPROGRESS("inprogress"),
    WAITING("waiting");


    public static final Map<String, SynchronizationState> STATE_TYPE_MAP = new HashMap<String, SynchronizationState>();

    static {
        STATE_TYPE_MAP.put(SYNCHRONIZED.getState(), SYNCHRONIZED);
        STATE_TYPE_MAP.put(UNSYNCHRONIZED.getState(), UNSYNCHRONIZED);
        STATE_TYPE_MAP.put(BROKEN.getState(), BROKEN);
        STATE_TYPE_MAP.put(UNKNOWN.getState(), UNKNOWN);
        STATE_TYPE_MAP.put(INPROGRESS.getState(), INPROGRESS);
        STATE_TYPE_MAP.put(WAITING.getState(), WAITING);
    }

    private final String state;

    SynchronizationState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public static SynchronizationState getType(final String state) {
        SynchronizationState type = STATE_TYPE_MAP.get(state);
        if (type != null) {
            return type;
        }
        return UNKNOWN;
    }
}
