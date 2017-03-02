package com.adnaga;

import org.apache.cordova.*;
import android.app.Activity;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;
import dalvik.system.DexFile;
import android.util.Log;

public class Adnaga extends CordovaPlugin {
    private static final String LOG_TAG = "Adnaga";
    private static CallbackContext _adnagaCallbackContext;

    private static Map<String, IPlugin> _pluginMap = new HashMap<String, IPlugin>();

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("showAds")) {
            return showAds(callbackContext, data);
        } else if (action.equals("init")) {
            return initAdnaga(callbackContext, data);
        } else if (action.equals("loadAds")) {
            return loadAds(callbackContext, data);
        }
        return false;
    }

    private String[] getClassesOfPackage(String packageName) {
        ArrayList<String> classes = new ArrayList<String>();
        try {
            String packageCodePath = getActivity().getPackageCodePath();
            DexFile df = new DexFile(packageCodePath);
            for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
                String className = iter.nextElement();
                if (className.contains(packageName) && !className.contains("$")) {
                    classes.add(className);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes.toArray(new String[classes.size()]);
    }

    private boolean initAdnaga(CallbackContext callbackContext, JSONArray data) {
        _adnagaCallbackContext = callbackContext;
        // find all the IPlugin in the package, and register it into the Map
        for (String className : getClassesOfPackage("com.adnaga")) {
            if (className.startsWith("com.adnaga.Adnaga") && className.length() > 17) {
                Log.i(LOG_TAG, "Found class for Plugin: " + className);
                try {
                    Class<?> clazz = Class.forName(className);
                    IPlugin plugin = (IPlugin)clazz.newInstance();
                    String networkName = plugin.getNetworkName();
                    _pluginMap.put(networkName, plugin);
                    Log.i(LOG_TAG, String.format("Registered network plugin: %s.", networkName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // initSettings: netwk1:pid1|network2:pid2|...
        String initSettings = data.optString(0);
        Log.i(LOG_TAG, String.format("initSettings: %s", initSettings));
        for (String tuple : initSettings.split("\\|")) {
            if (tuple == null && tuple.isEmpty()) {
                continue;
            }
            String[] tokens = tuple.split(":");
            if (tokens.length == 2) {
                IPlugin plugin = _pluginMap.get(tokens[0]);
                plugin.init(tokens[1], this);
            } else {
                Log.e(LOG_TAG, String.format("Invalid tuple %s", tuple));
            }
        }

        PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject());
        result.setKeepCallback(true);
        _adnagaCallbackContext.sendPluginResult(result);
        return true;
    }

    private boolean loadAds(CallbackContext callbackContext, JSONArray data) {
        final String networkName = data.optString(0);
        final String pid = data.optString(1);

        if (_pluginMap.containsKey(networkName)) {
            IPlugin plugin = _pluginMap.get(networkName);
            plugin.loadAds(pid);
            return true;
        } else {
            Log.e(LOG_TAG, String.format("Network %s not registered", networkName));
            return false;
        }
    }

    private boolean showAds(CallbackContext callbackContext, JSONArray data) {
        final String networkName = data.optString(0);

        if (_pluginMap.containsKey(networkName)) {
            IPlugin plugin = _pluginMap.get(networkName);
            plugin.showAds(callbackContext);
            PluginResult result = new PluginResult(PluginResult.Status.OK, networkName);
            callbackContext.sendPluginResult(result);
            return true;
        } else {
            Log.w(LOG_TAG, String.format("Network %s not registered", networkName));
            return false;
        }
    }

    // =========== END of public facing methods ================

    public Activity getActivity() {
        return cordova.getActivity();
    }

    public void sendAdsEventToJs(String networkName, String eventName, String eventDetail) {
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
