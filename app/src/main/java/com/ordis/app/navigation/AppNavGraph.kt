<?xml version="1.0" encoding="utf-8"?>
<manifest package="com.ordis.app" xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Ровно по одному разрешению -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:label="Ordis"
        android:supportsRtl="true"
        android:theme="@style/Theme.Ordis">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
