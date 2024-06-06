package com.example.strangers.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.strangers.R;
import com.example.strangers.databinding.ActivityConnectingBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class ConnectingActivity extends AppCompatActivity {

    ActivityConnectingBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    boolean isOkay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectingBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        String profile = getIntent().getStringExtra("profile");
        Glide.with(this)
                .load(profile)
                .into(binding.profile);

        String username = auth.getUid(); //Auth的uid作为username

        database.getReference().child("users")//users集合中, 有子元素为status的文件
                .orderByChild("status")
                .equalTo(0).limitToFirst(1) //查询数据库的“users”子节点，查找“status”等于 0 的条目，将结果限制为第一个匹配项
                .addListenerForSingleValueEvent(new ValueEventListener() { //只触发一次
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) { // snapshot是触发位置的数据信息
                        if(snapshot.getChildrenCount() > 0){ //查看是否有子节点(实际上查看是否有至少一个用户)
                            isOkay = true;
                            // Room Available
                            for(DataSnapshot childSnap : snapshot.getChildren()){ //遍历userId文件的子元素, snapshot代表检索出来的集合快照, 本来是用户集合, 现在是只有第一个用户的集合
                                database.getReference()
                                        .child("users")
                                        .child(childSnap.getKey())
                                        .child("incoming") //将自己加进对方的房间, 自己不创建东西
                                        .setValue(username);
                                database.getReference()
                                        .child("users")
                                        .child(childSnap.getKey())
                                        .child("status") //设置对方的房间状态为1
                                        .setValue(1);
                                Intent intent = new Intent(ConnectingActivity.this, CallActivity.class);
                                String incoming = childSnap.child("incoming").getValue(String.class);
                                String createdBy = childSnap.child("createdBy").getValue(String.class);
                                boolean isAvailable = childSnap.child("isAvailable").getValue(Boolean.class);
                                intent.putExtra("username", username);
                                intent.putExtra("incoming", incoming);
                                intent.putExtra("createdBy", createdBy);
                                intent.putExtra("isAvailable", isAvailable);
                                startActivity(intent);
                                finish();
                            }
                        }else {
                            // Not Available

                            HashMap<String, Object> room = new HashMap<>();
                            room.put("incoming", username);
                            room.put("createdBy", username);
                            room.put("isAvailable", true);
                            room.put("status", 0);

                            database.getReference()
                                    .child("users")
                                    .child(username) //直接把哈希表塞进去, 后面会变成一条条条目?
                                    .setValue(room).addOnSuccessListener(new OnSuccessListener<Void>() { //在users目录的用户id文件上创建room条目
                                        @Override
                                        public void onSuccess(Void unused) {
                                            database.getReference() //又监听本用户数据, 一旦status变为1启动聊天
                                                    .child("users")
                                                    .child(username).addValueEventListener(new ValueEventListener() { //每次更改都触发, 侦听数据后续更改
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            if(snapshot.child("status").exists()){
                                                                if(snapshot.child("status").getValue(Integer.class) == 1){ //调用侦听, 如果status为1

                                                                    if (isOkay)
                                                                        return;

                                                                    isOkay = true;
                                                                    Intent intent = new Intent(ConnectingActivity.this, CallActivity.class);
                                                                    String incoming = snapshot.child("incoming").getValue(String.class);
                                                                    String createdBy = snapshot.child("createdBy").getValue(String.class);
                                                                    boolean isAvailable = snapshot.child("isAvailable").getValue(Boolean.class);
                                                                    intent.putExtra("username", username);
                                                                    intent.putExtra("incoming", incoming);
                                                                    intent.putExtra("createdBy", createdBy);
                                                                    intent.putExtra("isAvailable", isAvailable);
                                                                    startActivity(intent);
                                                                    finish();
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}