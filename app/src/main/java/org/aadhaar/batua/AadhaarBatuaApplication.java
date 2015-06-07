package org.aadhaar.batua;

import android.app.Application;

public class AadhaarBatuaApplication extends Application {

    private static AadhaarBatuaApplication INSTANCE;
    private User mUser;

    @Override
    public void onCreate() {
        super.onCreate();

        INSTANCE = this;
    }

    public static AadhaarBatuaApplication getInstance() {
        return INSTANCE;
    }

    public void setUser(final User user) {
        mUser = user;
    }

    public User getUser() {
        return mUser;
    }

}
