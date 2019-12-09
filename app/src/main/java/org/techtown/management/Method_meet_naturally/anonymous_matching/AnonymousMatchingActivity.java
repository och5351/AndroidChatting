package org.techtown.management.Method_meet_naturally.anonymous_matching;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.techtown.management.R;
import org.techtown.management.chatting.ChatActivity;

public class AnonymousMatchingActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();
    SQLiteDatabase sampleDB = null;
    String user; //사용자
    int flag; //채팅 갯수 top
    String roomName ="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anonymous_matching);

        dbMaker(); //Flag DB 없을 시 생성


        //툴바 생성 -----------------------------------------------------------
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("두근두근 편지함");
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"));
        setSupportActionBar(toolbar);
        //--------------------------------------------------------------------

        final TextView receiveText = (TextView)findViewById(R.id.receiveText);
        final ImageView mailBox = (ImageView) findViewById(R.id.mailBox);


        //어디서 로그인 했는지 확인 하기------------------------------------------------------------------------------------
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();

        SharedPreferences autoLogInDB = getSharedPreferences("autoLogInDB",MODE_PRIVATE);
        user = autoLogInDB.getString("Id","");


        //--------------------------------------------------------------------------------------------------------------
        databaseReference.child("userinfo").child(user).child("matching").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if("0".equals((String)dataSnapshot.getValue())){
                    flag =0;
                }else{
                    flag=1;
                    roomName = (String)dataSnapshot.getValue();
                }
                //대화기록 확인
                if(flag== 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //버전의 상황에 따라 이미지뷰 배경 설정방법
                        mailBox.setImageDrawable(getResources().getDrawable(R.drawable.noletter, getApplicationContext().getTheme()));
                        receiveText.setText("지금은 대화 중이지 않아요.. \n우리 용기 내서 먼저 보내볼까요? *^^*");
                    } else {
                        mailBox.setImageDrawable(getResources().getDrawable(R.drawable.noletter));
                        receiveText.setText("지금은 대화 중이지 않아요.. \n우리 용기 내서 먼저 보내볼까요? *^^*");
                    }
                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //버전의 상황에 따라 이미지뷰 배경 설정방법
                        mailBox.setImageDrawable(getResources().getDrawable(R.drawable.letter, getApplicationContext().getTheme()));
                        receiveText.setText("대화가 있어요!\n한 마디, 한 마디를 신중히 보내요.");
                    } else {
                        mailBox.setImageDrawable(getResources().getDrawable(R.drawable.letter));
                        receiveText.setText("대화가 있어요!\n한 마디, 한 마디를 신중히 보내요.");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        mailBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flag == 0) {
                    show();
                }else{
                    Intent intent = new Intent(getApplicationContext(), AnonymousChatActivity.class);
                    intent.putExtra("user",user);
                    intent.putExtra("roomName",roomName); /*송신*/
                    startActivity(intent);
                }
            }
        });

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //액션바 메뉴
        getMenuInflater().inflate(R.menu.actionbar_action , menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //액션바 내부 아이템 이벤트
        int id = item.getItemId();
        if (id == R.id.sendMessage) {
            if(flag == 0)
                show();
            else
                Toast.makeText(getApplicationContext(),"아직은 대화 중이에요.", Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void dbMaker(){ // 시퀄라이트 로컬 DB 저장
        try {
            sampleDB = this.openOrCreateDatabase("talk" +"_"+ user, MODE_PRIVATE, null);
            //테이블이 존재하지 않으면 새로 생성합니다.
            sampleDB.execSQL("CREATE TABLE IF NOT EXISTS " + "matchingTalk"
                    + " (name VARCHAR(20), contents TEXT, position INTEGER);");
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

    protected boolean isInclude() {//새 대화인지
        boolean jud = false;
        try {
            SQLiteDatabase ReadDB = this.openOrCreateDatabase("talk"+"_"+user, MODE_PRIVATE, null);
            //SELECT문을 사용하여 테이블에 있는 데이터를 가져옵니다..
            int count = 0;
            Cursor c = ReadDB.rawQuery("SELECT count(*) FROM matchingTalkRoomName;", null);
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String Name = c.getString(c.getColumnIndex("count(*)"));
                        if(Integer.parseInt(Name) > 0){
                            count = 1;
                        }
                        else
                            count = 0;
                    } while (c.moveToNext());
                }
            }

            if(count == 0){
                jud = true;
            }else{
                Cursor d = ReadDB.rawQuery("SELECT * FROM matchingTalkRoomName;", null);
                if (d != null) {
                    if (d.moveToFirst()) {
                        do {
                            String Name = d.getString(d.getColumnIndex("name"));
                            if(Name.equals(roomName)){
                                jud = false;
                            }else
                                jud = true;
                        } while (c.moveToNext());
                    }
                }
            }
            ReadDB.close();
        } catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("",  se.getMessage());
        }
        return jud;
    }

    void show() //다이얼로그 함수
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("안내");
        builder.setMessage("메시지를 보내시겠어요? \n첫 인상이 중요한 듯 첫 마디의 설렘도 중요해요~");
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
                        Intent intent = new Intent(getApplicationContext(), AnonymousChatActivity.class);
                        intent.putExtra("user",user); /*송신*/
                        startActivity(intent);
                    }
                });

        builder.show();
    }

}
