package org.techtown.management.chatting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ChatListActivity extends AppCompatActivity {

    private ArrayList<ChatDataItem> dataList = new ArrayList<>();
    private ChatListAdapter chatAdapter;
    String user;
    SQLiteDatabase sampleDB = null;

    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);

        final LinearLayoutManager manager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(manager); // LayoutManager 등록

        chatAdapter = new ChatListAdapter(dataList);
        recyclerView.setAdapter(chatAdapter);  // Adapter 등록

        Button allTalk = (Button)findViewById(R.id.allTalk);
        Button myTalk = (Button)findViewById(R.id.myTalk);

        SharedPreferences autoLogInDB = getSharedPreferences("autoLogInDB",MODE_PRIVATE);
        user = autoLogInDB.getString("userName","");

        //방이름
        FirebaseDatabase.getInstance().getReference().child("chat").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    dataList.add(new ChatDataItem(null, snapshot.getKey(), ChatFindWho.ViewType.LEFT_CONTENT));
                }

                chatAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                ChatDataItem dict = dataList.get(position);
                isInclude(dict.getName());

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        allTalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatAdapter.clear();
                FirebaseDatabase.getInstance().getReference().child("chat").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            dataList.add(new ChatDataItem(null, snapshot.getKey(), ChatFindWho.ViewType.LEFT_CONTENT));
                        }

                        chatAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
        myTalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatAdapter.clear();
                showList();
            }
        });

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                manager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

    }
    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private ChatListActivity.ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ChatListActivity.ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }

    private void dbMaker(String roomName){
        try {
            sampleDB = this.openOrCreateDatabase("talk"+"_"+user, MODE_PRIVATE, null);
            //테이블이 존재하지 않으면 새로 생성합니다.
            sampleDB.execSQL("CREATE TABLE IF NOT EXISTS " + roomName
                    + " (name VARCHAR(20), contents TEXT);");

            sampleDB.close();
        } catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("", se.getMessage());
        }
    }

    protected void isInclude(String roomName) {
        try {
            SQLiteDatabase ReadDB = this.openOrCreateDatabase("talk"+"_"+user, MODE_PRIVATE, null);
            //SELECT문을 사용하여 테이블에 있는 데이터를 가져옵니다..
            Cursor c = ReadDB.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table';", null);
            int flag = 0;
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String Name = c.getString(c.getColumnIndex("name"));
                        if(Name.equals(roomName)){
                            Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
                            intent.putExtra("roomName",roomName); /*송신*/
                            startActivity(intent);
                            flag = 1;
                            break;
                        }
                    } while (c.moveToNext());
                }
            }
            ReadDB.close();
            if(flag == 0) {
                show(roomName);
            }
        } catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("",  se.getMessage());
        }
    }

    void show(final String roomName)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("안내");
        builder.setMessage("새로운 방에 입장하시겠습니까?");
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dbMaker(roomName);
                        ChatDataItem chat = new ChatDataItem(user + "님이 입장하셨습니다.", null,ChatFindWho.ViewType.CENTER_CONTENT);
                        databaseReference.child("chat").child(roomName).child("txt").push().setValue(chat);
                        databaseReference.child("chat").child(roomName).child("user").push().setValue(user); // 데이터 푸쉬
                        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
                        intent.putExtra("roomName",roomName); /*송신*/
                        startActivity(intent);
                    }
                });
        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }


    protected void showList(){
        try {
            SQLiteDatabase ReadDB = this.openOrCreateDatabase("talk"+"_"+user, MODE_PRIVATE, null);
            //SELECT문을 사용하여 테이블에 있는 데이터를 가져옵니다..
            Cursor c = ReadDB.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table';", null);
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String Name = c.getString(c.getColumnIndex("name"));
                        if(!Name.contains("_userInfo")&&!Name.equals("android_metadata")) {
                            dataList.add(new ChatDataItem(null, Name, ChatFindWho.ViewType.LEFT_CONTENT));
                        }

                    } while (c.moveToNext());
                    chatAdapter.notifyDataSetChanged();
                }
            }
            ReadDB.close();

        } catch (SQLiteException se) {
            Toast.makeText(getApplicationContext(),  se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("",  se.getMessage());
        }
    }
}


