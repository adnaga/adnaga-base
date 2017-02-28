package com.adnaga;

import org.apache.cordova.*;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.content.Context;
import android.telephony.TelephonyManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

public class Adnaga extends CordovaPlugin {
    private static final String LOG_TAG = "Adnaga";
    private CallbackContext _adnagaCallbackContext;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("showAds")) {
            // return showAds(callbackContext, data);
            return true;
        } else if (action.equals("init")) {
            // all ads event callback goes to this callback
            // _aduniteCallbackContext = callbackContext;
            // initAdunite(callbackContext, data.optBoolean(0), data.optString(1), data.optString(2));
            // PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject());
            // result.setKeepCallback(true);
            // _aduniteCallbackContext.sendPluginResult(result);
            return true;
        } else if (action.equals("loadAds")) {
            // return loadAds(callbackContext, data);
            return true;
        } else {
            return false;
        }
    }

    private void initAdnaga(CallbackContext callbackContext,
            final boolean enableApplovin, final String adcolonyAppAndZoneId,
            final String chartboostAppIdAndSignature) {
        // some sdk requires init before using
        // applovin
        // if (enableApplovin) {
        //     Log.w(LOG_TAG, "applovin ads is enabled.");
        //     _adDialog = AppLovinInterstitialAd.create(AppLovinSdk.getInstance(getActivity()), getActivity());
        //     MyAppLovinListener myAppLovinListener = new MyAppLovinListener();
        //     _adDialog.setAdDisplayListener(myAppLovinListener);
        //     _adDialog.setAdLoadListener(myAppLovinListener);
        //     _adDialog.setAdClickListener(myAppLovinListener);
        //     AppLovinSdk.initializeSdk(getActivity());
        //     // start a polling thread to check if ads is ready to show
        //     Thread checkAppLovinThread = new Thread(new Runnable() { public void run() {
        //         while (true) {
        //             if (_applovinReady == false) {
        //                 boolean result = _adDialog.isAdReadyToDisplay();
        //                 Log.d(LOG_TAG, "checking applovin ready state = " + result);
        //                 if (result) {
        //                     sendAdsEventToJs("applovin", "READY", "");
        //                     _applovinReady = true;
        //                 }
        //             }
        //             try {
        //                 Thread.sleep(3000);
        //             } catch (InterruptedException e) { e.printStackTrace(); }
        //         }
        //     }});
        //     checkAppLovinThread.start();
        // }
        // // adcolony
        // if ((adcolonyAppAndZoneId != null) && (!"".equals(adcolonyAppAndZoneId)) && (!"null".equals(adcolonyAppAndZoneId))) {
        //     Log.w(LOG_TAG, "adcolony ads is enabled. appId_zoneId=" + adcolonyAppAndZoneId);
        //     String[] tokens = adcolonyAppAndZoneId.split("_");
        //     AdColony.configure(getActivity(), "version:1.0,store:google", tokens[0] /* appid */, tokens[1] /* zoneid */);
        //     AdColony.addAdAvailabilityListener(new AdColonyListener());
        // }
        // // chartboost
        // if ((chartboostAppIdAndSignature != null) && (!"".equals(chartboostAppIdAndSignature)) && (!"null".equals(chartboostAppIdAndSignature))) {
        //     getActivity().runOnUiThread(new Runnable() {
        //         @Override
        //         public void run() {
        //             Log.w(LOG_TAG, "chartboost ads is enabled. appId_signatureId=" + chartboostAppIdAndSignature);
        //             String[] tokens = chartboostAppIdAndSignature.split("_");
        //             Chartboost.setAutoCacheAds(false);
        //             Chartboost.startWithAppId(getActivity(), tokens[0] /* appid */, tokens[1] /* signature */);
        //             Chartboost.setDelegate(new MyChartboostListener());
        //             Chartboost.onCreate(getActivity());
        //             Chartboost.onStart(getActivity());
        //         }
        //     });
        // }
    }

    private boolean loadAds(CallbackContext callbackContext, JSONArray data) {
        final String networkName = data.optString(0);
        final String pid = data.optString(1);

        // if ("admob".equals(networkName)) {
        //     loadAdmobAds(pid);
        // } else if ("applovin".equals(networkName)) {
        //     // no op
        // } else if ("adcolony".equals(networkName)) {
        //     // no op
        // } else if ("cb".equals(networkName)) {
        //     loadChartboostAds();
        // } else {
        //     Log.e(LOG_TAG, "adnetwork not supported: " + networkName);
        // }
        return true;
    }

    private boolean showAds(CallbackContext callbackContext, JSONArray data) {
        // final String networkName = data.optString(0);
        // if ("admob".equals(networkName)) {
        //     showAdmobAds(callbackContext);
        // } else if ("applovin".equals(networkName)) {
        //     showApplovinAds(callbackContext);
        // } else if ("adcolony".equals(networkName)) {
        //     showAdcolonyAds(callbackContext);
        // } else if ("cb".equals(networkName)) {
        //     showChartboostAds(callbackContext);
        // } else {
        //     Log.e(LOG_TAG, "adnetwork not supported: " + networkName);
        // }
        // PluginResult result = new PluginResult(PluginResult.Status.OK, networkName);
        // callbackContext.sendPluginResult(result);
        return true;
    }

    // =========== END of public facing methods ================

    private Activity getActivity() {
        return cordova.getActivity();
    }

    private void sendAdsEventToJs(String networkName, String eventName, String eventDetail) {
        Log.w(LOG_TAG, String.format("Emit AdsEvent: %s - %s - %s", networkName, eventName, eventDetail));
        PluginResult result = new PluginResult(PluginResult.Status.OK, buildAdsEvent(networkName, eventName, eventDetail));
        result.setKeepCallback(true);
        if (_adnagaCallbackContext != null) {
            _adnagaCallbackContext.sendPluginResult(result);
        } else {
            Log.e(LOG_TAG, String.format("_adnagaCallbackContext is null, cannot send result back, network=%s event=%s", networkName, eventName));
        }
    }

    private JSONObject buildAdsEvent(String networkName, String eventName, String eventDetail) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("network_name", networkName);
            obj.put("event_name", eventName);
            obj.put("event_detail", eventDetail);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return null;
        }
        return obj;
    }
}
