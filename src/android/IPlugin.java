package com.adnaga;

import org.apache.cordova.*;

public interface IPlugin {
    public void init(String pid, Adnaga adnaga);
    public void loadAds(String pid);
    public void showAds(final CallbackContext callbackContext);
    public String getNetworkName();
}
