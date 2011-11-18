package org.onehippo.forge.externalresource.api.scheduale.mediamosa;

/**
 * @version $Id$
 */
public interface MediaMosaJobListener {

    void whileInprogress(String assetId);

    void onFinished(String assetId);

    void whileWaiting(String assetId);

    void onFailed(String assetId);

    void onCancelled(String assetId);

}
