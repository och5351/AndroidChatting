package org.techtown.management.chatting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.techtown.management.R;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    private ArrayList<ChatDataItem> dataList = new ArrayList<>();
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();
    private ChatAdapter chatAdapter;
    private InputMethodManager inputMMg;
    SQLiteDatabase sampleDB = null;

    String user;
    String otherUser;
    String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
        final LinearLayoutManager manager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);

        recyclerView.setLayoutManager(manager); // LayoutManager 등록

        chatAdapter = new ChatAdapter(dataList);
        recyclerView.setAdapter(chatAdapter);

        final EditText editText = (EditText)findViewById(R.id.editText);
        Button submitB = (Button)findViewById(R.id.submitB);

        Intent intent = getIntent(); /*데이터 수신*/

        final String roomName = intent.getExtras().getString("roomName"); //방이름

        final SoftKeyboardDetectorView softKeyboardDecector = new SoftKeyboardDetectorView(this);

        addContentView(softKeyboardDecector, new FrameLayout.LayoutParams(-1, -1));
        softKeyboardDecector.setOnShownKeyboard(new SoftKeyboardDetectorView.OnShownKeyboardListener() {
            @Override
            public void onShowSoftKeyboard() {
                //키보드 등장할 때 채팅창 마지막 입력 내용을 바로 보이도록 처리
                manager.scrollToPosition(dataList.size() - 1);
            }
        });

        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                inputMMg = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMMg.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        });

        inputMMg = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        submitB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText().toString().equals(""))
                    return;

                ChatDataItem chat = new ChatDataItem(editText.getText().toString(), user,ChatFindWho.ViewType.RIGHT_CONTENT); //ChatDTO를 이용하여 데이터를 묶는다.
                //SQLite Local 저장
                dbMaker(roomName, chat);
                databaseReference.child("chat").child(roomName).child("txt").push().setValue(chat); // 데이터 푸쉬
                editText.setText(""); //입력창 초기화

            }
        });

        SharedPreferences autoLogInDB = getSharedPreferences("autoLogInDB",MODE_PRIVATE);
        user = autoLogInDB.getString("userName","");

        //firebase 채팅기록 확인
        databaseReference.child("chat").child(roomName).child("txt").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ChatDataItem chat = dataSnapshot.getValue(ChatDataItem.class);
                if(chat.getName() != null) {
                    if (user.equals(chat.getName())) {
                        text = chat.getContent();
                        initializeData(1);
                    } else {
                        otherUser = chat.getName();
                        text = chat.getContent();
                        initializeData(2);
                    }
                }else{
                    text = chat.getContent();
                    initializeData(0);
                }

                //Collections.reverse(dataList);
                chatAdapter.notifyDataSetChanged();
                chatAdapter.notifyItemInserted(dataList.size() - 1);
                manager.scrollToPosition(dataList.size() - 1);

            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



    }

    public void initializeData(int choice) {
        if(choice == 0){
            dataList.add(new ChatDataItem(text, null,  ChatFindWho.ViewType.CENTER_CONTENT));
            //dataList.add(new ChatDataItem(text, otherUser,  ChatFindWho.ViewType.LEFT_CONTENT));
        }else if(choice == 1) {
            dataList.add(new ChatDataItem(text, user,  ChatFindWho.ViewType.RIGHT_CONTENT));
        }else if(choice == 2){
            dataList.add(new ChatDataItem(text, otherUser,  ChatFindWho.ViewType.LEFT_CONTENT));
        }
    }

    private void dbMaker(String roomName, ChatDataItem chat){
        try {
            sampleDB = this.openOrCreateDatabase("talk"+"_"+user, MODE_PRIVATE, null);
            //테이블이 존재하지 않으면 새로 생성합니다.
            sampleDB.execSQL("CREATE TABLE IF NOT EXISTS " + roomName
                    + " (name VARCHAR(20), contents TEXT);");
            sampleDB.execSQL("CREATE TABLE IF NOT EXISTS " + roomName+"_userInfo"
                    + " (name VARCHAR(20));");
            //테이블이 존재하는 경우 기존 데이터를 지우기 위해서 사용합니다.
            //sampleDB.execSQL("DELETE FROM " + roomName  );
            //새로운 데이터를 테이블에 집어넣습니다..

            sampleDB.execSQL("INSERT INTO " + roomName
                    + " (name, contents)  Values ('" + chat.getName() + "', '" + chat.getContent()+"');");

            sampleDB.close();
        } catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("", se.getMessage());
        }
    }
}