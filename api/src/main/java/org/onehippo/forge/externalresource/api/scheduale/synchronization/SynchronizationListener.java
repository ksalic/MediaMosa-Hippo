package org.onehippo.forge.externalresource.api.scheduale.synchronization;

/**
 * @version $Id$
 */
public interface SynchronizationListener {

    void onFinished();

    void onFailed();

}
