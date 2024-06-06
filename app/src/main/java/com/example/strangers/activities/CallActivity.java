package com.example.strangers.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.strangers.R;
import com.example.strangers.databinding.ActivityCallBinding;
import com.example.strangers.models.InterfaceJava;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class CallActivity extends AppCompatActivity {

    ActivityCallBinding binding;
    String uniqueId = "";
    FirebaseAuth auth;
    String username = "";
    String friendsUsername = "";

    boolean isPeerConnected = false;

    DatabaseReference firebaseRef;

    boolean isAudio = true;
    boolean isVideo = true;
    String createdBy;

    boolean pageExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        firebaseRef = FirebaseDatabase.getInstance().getReference().child("users");

        username = getIntent().getStringExtra("username"); //username是自己
        String incoming = getIntent().getStringExtra("incoming"); //房间主人的话是自己, 客人也是自己
        createdBy = getIntent().getStringExtra("createdBy");

        friendsUsername = "";

        if(incoming.equalsIgnoreCase(friendsUsername)) //忽略大小写
            friendsUsername = incoming;

        setupWebView();

        binding.micBtn.setOnClickListener(new View.OnClickListener() { //静音或开启语音
            @Override
            public void onClick(View v) {
                isAudio = !isAudio;
                callJavaScriptFunction("javascript:toggleAudio(\""+isAudio+"\")");
                if(isAudio){
                    binding.micBtn.setImageResource(R.drawable.btn_unmute_normal);
                }else {
                    binding.micBtn.setImageResource(R.drawable.btn_mute_normal);
                }
            }
        });

        binding.videoBtn.setOnClickListener(new View.OnClickListener() { //静音或开启语音
            @Override
            public void onClick(View v) {
                isVideo = !isVideo;
                callJavaScriptFunction("javascript:toggleVideo(\""+isVideo+"\")");
                if(isVideo){
                    binding.videoBtn.setImageResource(R.drawable.btn_video_normal);
                }else {
                    binding.videoBtn.setImageResource(R.drawable.btn_video_muted);
                }
            }
        });


    }

    void setupWebView(){
        binding.webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    request.grant(request.getResources()); //该行授予Web内容要求的资源
                }
            }
        });

        binding.webView.getSettings().setJavaScriptEnabled(true); //此启用WebView的JavaScript
        binding.webView.getSettings().setMediaPlaybackRequiresUserGesture(false); //这允许在没有用户交互的情况下在WebView中自动播放媒体
        binding.webView.addJavascriptInterface(new InterfaceJava(this),"Android"); //这通过WebView将Java对象暴露于JavaScript(别名设为Android)

        loadVideoCall();
    }

    public void loadVideoCall(){ //此方法将本地HTML文件加载到WebView中，并设置WebViewClient
        String filePath = "file:android_asset/call.html";
        binding.webView.loadUrl(filePath);

        binding.webView.setWebViewClient(new WebViewClient(){ //这为WebView设置了WebViewClient。 WebViewClient处理各种事件，例如页面加载
            @Override
            public void onPageFinished(WebView view, String url) { //只在main frame执行, 通知服务器
                super.onPageFinished(view, url); //当页面加载完成时，此方法被调用
                 initializePeer();
            }
        });
    }

    void initializePeer(){ //此方法初始化了对等连接
        uniqueId = getUniqueId(); //创建一个独立id

        callJavaScriptFunction("javascript:init(\"" + uniqueId + "\")"); //这是一个以唯一ID作为参数的JavaScript函数

        if(createdBy.equalsIgnoreCase(username)){ //如果当前为房主
            firebaseRef.child(username).child("connId").setValue(uniqueId); //加一个connId设为新的UUID
            firebaseRef.child(username).child("isAvailable").setValue(true);

            binding.controls.setVisibility(View.VISIBLE); //controls包含了3个按钮
        }else {
            new Handler().postDelayed(new Runnable() { //创建线程2秒后执行
                @Override
                public void run() {
                    friendsUsername = createdBy; //friendsUsername为房主id
                    FirebaseDatabase.getInstance().getReference()
                            .child("users")
                            .child(friendsUsername)
                            .child("connId") //如果当前不是房主就找房主connId(因为同时连线延迟2秒让房主先创建connId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.getValue() != null){ //如果有connId
                                        sendCallRequest();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            },10000);
        }

    }

    public void onPeerConnected(){
        isPeerConnected = true;
    }

    void sendCallRequest() {
        if (!isPeerConnected){
            Toast.makeText(this, "You are not connected. Please check your internet.", Toast.LENGTH_SHORT).show();
            return;
        }

        listenConnId();
    }

    void listenConnId(){
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue() == null){
                    return;
                }

                binding.controls.setVisibility(View.VISIBLE); //只要有connId
                String connId = snapshot.getValue(String.class);
                callJavaScriptFunction("javascript:startCall(\""+connId+"\")"); //用connId开启VideoCall
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    void callJavaScriptFunction(String function){ //此方法调用JavaScript函数。
        binding.webView.post(new Runnable() { //这是将来在某个时候执行的可运行的。
            @Override //这是`runnable`接口的运行方法。当实现接口`runnable`的对象用于创建线程时，启动线程会导致对象的“运行”方法在该分别执行线程中被调用
            public void run() {
                binding.webView.evaluateJavascript(function, null); //这评估JavaScript函数。
            } //此行在WebView中调用JavaScript函数。 `eartiuteJavaScript`方法评估JavaScript函数，并返回所评估的最后一个语句的结果。在这种情况下，由于第二个参数为“ null”，因此不使用结
        }); //此方法允许您在WebView的上下文中调用Java的JavaScript函数。它确保在正确的线程上调用JavaScript函数。当您需要与Android应用中的Web内容互动时，这一点特别有用
    }

    String getUniqueId(){ //获取独有id
        return UUID.randomUUID().toString();
    }
}