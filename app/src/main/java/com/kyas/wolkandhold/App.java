package com.kyas.wolkandhold;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.yandex.mapkit.MapKitFactory;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        String apiKey = null;
        try {
            Context context = getApplicationContext();
            ApplicationInfo appInfo = context
                    .getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

            Bundle metaData = appInfo.metaData;
            if (metaData != null) {
                apiKey = metaData.getString("com.yandex.maps.apikey");
                Log.d("MetaData", "API KEY: " + apiKey);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Error", "onCreate: ", e);
        }
        MapKitFactory.setApiKey(apiKey);
        MapKitFactory.initialize(this);
    }
}
