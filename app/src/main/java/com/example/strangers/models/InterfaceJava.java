package com.example.strangers.models;

import android.webkit.JavascriptInterface;

import com.example.strangers.activities.CallActivity;

public class InterfaceJava {

    CallActivity callActivity;

    public InterfaceJava(CallActivity callActivity){
        this.callActivity = callActivity;
    }

    @JavascriptInterface //此注释至关重要，因为它向在 WebView 中运行的 JavaScript 公开以下方法。如果没有此注释，由于安全限制，JavaScript 无法调用该方法
    public void onPeerConnected(){
        callActivity.onPeerConnected();
    } //泥马这个鬼函数真的会自己执行!!!!

}
