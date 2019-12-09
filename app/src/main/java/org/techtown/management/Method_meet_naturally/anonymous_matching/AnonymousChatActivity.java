package org.techtown.management.Method_meet_naturally.anonymous_matching;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.techtown.management.R;
import org.techtown.management.chatting.ChatDataItem;
import org.techtown.management.chatting.ChatFindWho;
import org.techtown.management.chatting.SoftKeyboardDetectorView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class AnonymousChatActivity  extends AppCompatActivity {

    private ArrayList<ChatDataItem> dataList = new ArrayList<>();
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();
    SQLiteDatabase sampleDB = null;
    String user; //사용자
    int flag; // 채팅이 있는지 없는지
    String target = "";
    long numChildren;
    String roomName = "";
    String text;
    AnonymousChatAdapter anonymousChatAdapter;
    LinearLayoutManager manager
            = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anonymous_chat);

        //툴바 생성 -----------------------------------------------------------
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("채팅");
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"));
        setSupportActionBar(toolbar);
        //--------------------------------------------------------------------

        //layout 구성
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);

        recyclerView.setLayoutManager(manager); // LayoutManager 등록

        anonymousChatAdapter = new AnonymousChatAdapter(dataList);
        recyclerView.setAdapter(anonymousChatAdapter);

        //에딧텍스트, 버튼 java구현
        final EditText editText = (EditText)findViewById(R.id.editText);
        Button submitB = (Button)findViewById(R.id.submitB);

        //소프트 키보드
        final SoftKeyboardDetectorView softKeyboardDecector = new SoftKeyboardDetectorView(this);
        addContentView(softKeyboardDecector, new FrameLayout.LayoutParams(-1, -1));
        softKeyboardDecector.setOnShownKeyboard(new SoftKeyboardDetectorView.OnShownKeyboardListener() {
            @Override
            public void onShowSoftKeyboard() {
                //키보드 등장할 때 채팅창 마지막 입력 내용을 바로 보이도록 처리
                manager.scrollToPosition(dataList.size() - 1);
            }
        });

        Intent intent = getIntent(); /*데이터 수신*/
        Bundle b = intent.getExtras();
        Iterator<String> iter = b.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            if(key.equals("user")){
                user = intent.getExtras().getString("user");
            }else if(key.equals("roomName")){
                roomName = intent.getExtras().getString("roomName");
                Log.v("채팅룸 이름",roomName);
            }
        }


        databaseReference.child("userinfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //이용자 수 조회
                numChildren = dataSnapshot.getChildrenCount();

                //flag = 0;

                //유저 메일함 조사
                String temp = (String)dataSnapshot.child(user).child("matching").getValue();
                if (temp.equals("0")) {
                    flag = 0;
                } else {
                    flag = 1;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        //if(!roomName.equals("")){
        //채팅로그 감시
        databaseReference.child("matchingChat").child(roomName).child("txt").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ChatDataItem chat = dataSnapshot.getValue(ChatDataItem.class);
                //Log.v("알림", "추가가 감지됨");
                if (chat.getContent() != null) {
                    if (chat.getName() != null) {
                        if (user.equals(chat.getName())) {
                            text = chat.getContent();
                            initializeData(1);
                        } else {
                            target = chat.getName();
                            text = chat.getContent();
                            initializeData(2);
                        }
                    } else {
                        text = chat.getContent();
                        initializeData(0);
                    }

                    anonymousChatAdapter.notifyDataSetChanged();
                    anonymousChatAdapter.notifyItemInserted(dataList.size() - 1);
                    manager.scrollToPosition(dataList.size() - 1);
                }
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
        //}


        submitB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText().toString().equals(""))
                    return;
                //첫 메시지 일 경우
                if(flag == 0) {
                    //랜덤 값
                    Random rnd = new Random();
                    Log.v("유저 수 ", ""+(int)numChildren);
                    int p = rnd.nextInt((int)numChildren);
                    String contents = editText.getText().toString();
                    randomFind(p, contents);
                    editText.setText(""); //입력창 초기화

                }else { //대화 중 일 경우
                    ChatDataItem chat = new ChatDataItem(editText.getText().toString(), user, ChatFindWho.ViewType.RIGHT_CONTENT); //ChatDTO를 이용하여 데이터를 묶는다.
                    //SQLite Local 저장
                    dbMaker();
                    insertToTalkTable(user, editText.getText().toString(),ChatFindWho.ViewType.RIGHT_CONTENT);//톡 내용 insert
                    databaseReference.child("matchingChat").child(roomName).child("txt").push().setValue(chat); // 데이터 푸쉬
                    editText.setText(""); //입력창 초기화
                    //chatReader();
                }
            }
        });





    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //액션바 메뉴
        getMenuInflater().inflate(R.menu.actionbar_action_ac , menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //액션바 내부 아이템 이벤트
        int id = item.getItemId();
        if (id == R.id.outChat) {
            show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void show() //다이얼로그 함수
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("나가기");
        builder.setMessage("채팅방을 나가시겠어요? \n(대화내용은 복구 되지 않습니다.)");
        builder.setPositiveButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setNegativeButton(
                "예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        databaseReference.child("matchingChat").child(roomName).child("user").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                                    if(user.equals((String)snapshot.getValue())){
                                        databaseReference.child("matchingChat").child(roomName).child("user").child(snapshot.getKey()).setValue(null);
                                        ChatDataItem chat = new ChatDataItem(user+"님이 나갔습니다.", null, ChatFindWho.ViewType.RIGHT_CONTENT);
                                        databaseReference.child("matchingChat").child(roomName).child("txt").push().setValue(chat);
                                        databaseReference.child("userinfo").child(user).child("matching").setValue("0");
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                        initTable();
                        finish();
                    }
                });

        builder.show();
    }

    //무작위 상대 찾기
    private void randomFind(final int p, final String contents){
        //final String[] who = {"없음"};
        databaseReference.child("userinfo").addListenerForSingleValueEvent(new ValueEventListener() {
            int count =0;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Log.v("카운터",count+"");
                    Log.v("랜덤 숫자", p+"");
                    if(count == p && !user.equals(snapshot.getKey())){ //무작위가 자신이 아닌지
                        Log.v("랜덤 숫자의 유저 :",snapshot.getKey());
                        //Log.v("상대 메일 상태",(String)dataSnapshot.child(snapshot.getKey()).child("mail").getValue());
                        if("0".equals((String)dataSnapshot.child(snapshot.getKey()).child("matching").getValue())) {//상대방의 mail함이 비었는지
                            target = snapshot.getKey();
                            Log.v("상대방",target);
                            roomName = user+target+count;//방이름 생성
                            databaseReference.child("userinfo").child(user).child("matching").setValue(roomName);
                            databaseReference.child("userinfo").child(target).child("matching").setValue(roomName); //상대방 mail함 도착알림
                            databaseReference.child("matchingChat").child(roomName).child("user").push().setValue(target);//방개설 후 user 등록
                            databaseReference.child("matchingChat").child(roomName).child("user").push().setValue(user);
                            dbMaker();
                            initTable();
                            ChatDataItem chat = new ChatDataItem(contents, user, ChatFindWho.ViewType.RIGHT_CONTENT);
                            insertToRoomTable();//방이름 insert
                            insertToTalkTable(user, contents,ChatFindWho.ViewType.RIGHT_CONTENT);//톡 내용 insert
                            databaseReference.child("matchingChat").child(roomName).child("txt").push().setValue(chat); // 데이터 푸쉬
                            chatReader();
                            flag=1;
                            break;
                        }
                        else {//아닐시 자신으로 리턴
                            target = user;
                            judgeTarget(contents);
                        }
                    }else if(count == p && user.equals(snapshot.getKey())){ //무작위가 자신이라면
                        target = user;
                        judgeTarget(contents);
                    }
                    count++;
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private  void judgeTarget(String contens){
        if(!target.equals("")){
            if (target.equals(user)) {
                Random rnd = new Random();
                //int k = rnd.nextInt(); // -2147483648 < k < 2147483647  ②
                Log.v("유저 수 ", ""+(int)numChildren);
                int p = rnd.nextInt((int)numChildren); // 0 <= p < 500 ③
                randomFind(p, contens);
            }else{
                Log.v("계속 돌아가면 무한 반복",""+2);
                Log.v("상대 아이디 :",target);

            }
        }
    }

    private void chatReader(){
        //채팅로그 감시
        databaseReference.child("matchingChat").child(roomName).child("txt").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ChatDataItem chat = dataSnapshot.getValue(ChatDataItem.class);
                Log.v("알림","추가가 감지됨");
                if(chat.getName() != null) {
                    if (user.equals(chat.getName())) {
                        text = chat.getContent();
                        initializeData(1);
                    } else {
                        target = chat.getName();
                        text = chat.getContent();
                        initializeData(2);
                    }
                }else{
                    text = chat.getContent();
                    initializeData(0);
                }

                anonymousChatAdapter.notifyDataSetChanged();
                anonymousChatAdapter.notifyItemInserted(dataList.size() - 1);
                manager.scrollToPosition(dataList.size() - 1);
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void dbMaker(){ // 시퀄라이트 로컬 DB 저장
        try {
            sampleDB = this.openOrCreateDatabase("talk" +"_"+ user, MODE_PRIVATE, null);
            //테이블이 존재하지 않으면 새로 생성합니다.
            sampleDB.execSQL("CREATE TABLE IF NOT EXISTS " + "matchingTalk"
                    + " (name VARCHAR(20), contents TEXT, position INTEGER);");
            sampleDB.execSQL("CREATE TABLE IF NOT EXISTS " + "matchingTalkRoomName"
                    + " (name VARCHAR(30));");
            //테이블이 존재하는 경우 기존 데이터를 지우기 위해서 사용합니다.
            //sampleDB.execSQL("DELETE FROM " + roomName  );
            //새로운 데이터를 테이블에 집어넣습니다..

            //sampleDB.execSQL("INSERT INTO " + roomName
            //  + " (name, contents)  Values ('" + chat.getName() + "', '" + chat.getContent()+"');");

            sampleDB.close();
        } catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("", se.getMessage());
        }
    }

    private void initTable(){ // matchingTalk 테이블 초기화
        try {
            sampleDB = this.openOrCreateDatabase("talk" +"_"+ user, MODE_PRIVATE, null);
            sampleDB.execSQL("DELETE FROM matchingTalk;");
            sampleDB.execSQL("DELETE FROM matchingTalkRoomName;");
            sampleDB.close();
        }catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("", se.getMessage());
        }
    }

    private void insertToTalkTable(String name, String content, int position){
        try {
            sampleDB = this.openOrCreateDatabase("talk" +"_"+ user, MODE_PRIVATE, null);
            sampleDB.execSQL("INSERT INTO matchingTalk"
                    + " (name, contents, position)  Values ('" + name + "', '" + content+ "', " + position + ");");
            sampleDB.close();
        }catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("", se.getMessage());
        }
    }

    private void insertToRoomTable(){
        try {
            sampleDB = this.openOrCreateDatabase("talk" +"_"+ user, MODE_PRIVATE, null);
            sampleDB.execSQL("INSERT INTO matchingTalkRoomName"
                    + " (name)  Values ('" + roomName + "');");
            sampleDB.close();
        }catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("", se.getMessage());
        }
    }

    public void initializeData(int choice) {
        if(choice == 0){
            dataList.add(new ChatDataItem(text, null,  ChatFindWho.ViewType.CENTER_CONTENT));
            insertToTalkTable("",target + "님이 나가셨습니다.",ChatFindWho.ViewType.CENTER_CONTENT);
            //dataList.add(new ChatDataItem(text, otherUser,  ChatFindWho.ViewType.LEFT_CONTENT));
        }else if(choice == 1) {
            dataList.add(new ChatDataItem(text, user,  ChatFindWho.ViewType.RIGHT_CONTENT));
            insertToTalkTable(target,text,ChatFindWho.ViewType.RIGHT_CONTENT);
        }else if(choice == 2){
            dataList.add(new ChatDataItem(text, target,  ChatFindWho.ViewType.LEFT_CONTENT));
            insertToTalkTable(target,text,ChatFindWho.ViewType.LEFT_CONTENT);
        }
    }

}
