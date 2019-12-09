package org.techtown.management.Method_meet_naturally.anonymous_matching;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.techtown.management.R;
import org.techtown.management.chatting.ChatDataItem;
import org.techtown.management.chatting.ChatFindWho;

import java.util.ArrayList;

public class AnonymousChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private ArrayList<ChatDataItem> myDataList = null;

    AnonymousChatAdapter(ArrayList<ChatDataItem> dataList)
    {
        myDataList = dataList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view;
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(viewType == ChatFindWho.ViewType.CENTER_CONTENT)
        {
            view = inflater.inflate(R.layout.activity_chat_center_content, parent, false);
            return new AnonymousChatAdapter.CenterViewHolder(view);
        }
        else if(viewType == ChatFindWho.ViewType.LEFT_CONTENT)
        {
            view = inflater.inflate(R.layout.activity_chat_left_content, parent, false);
            return new AnonymousChatAdapter.LeftViewHolder(view);
        }
        else
        {
            view = inflater.inflate(R.layout.activity_chat_right_content, parent, false);
            return new AnonymousChatAdapter.RightViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position)
    {
        if(viewHolder instanceof AnonymousChatAdapter.CenterViewHolder)
        {
            ((AnonymousChatAdapter.CenterViewHolder) viewHolder).content.setText(myDataList.get(position).getContent());
        }
        else if(viewHolder instanceof AnonymousChatAdapter.LeftViewHolder)
        {
            ((AnonymousChatAdapter.LeftViewHolder) viewHolder).name.setText(myDataList.get(position).getName());
            ((AnonymousChatAdapter.LeftViewHolder) viewHolder).content.setText(myDataList.get(position).getContent());
        }
        else
        {
            ((AnonymousChatAdapter.RightViewHolder) viewHolder).name.setText(myDataList.get(position).getName());
            ((AnonymousChatAdapter.RightViewHolder) viewHolder).content.setText(myDataList.get(position).getContent());
        }
    }

    @Override
    public int getItemCount()
    {
        return myDataList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return myDataList.get(position).getViewType();
    }

    public class CenterViewHolder extends RecyclerView.ViewHolder{
        TextView content;

        CenterViewHolder(View itemView)
        {
            super(itemView);

            content = itemView.findViewById(R.id.content);
        }
    }

    public class LeftViewHolder extends RecyclerView.ViewHolder{
        TextView content;
        TextView name;
        ImageView image;

        LeftViewHolder(View itemView)
        {
            super(itemView);

            content = itemView.findViewById(R.id.content);
            name = itemView.findViewById(R.id.name);
            image = itemView.findViewById(R.id.imageView);
        }
    }

    public class RightViewHolder extends RecyclerView.ViewHolder{
        TextView content;
        TextView name;
        ImageView image;

        RightViewHolder(View itemView)
        {
            super(itemView);

            content = itemView.findViewById(R.id.content);
            name = itemView.findViewById(R.id.name);
            image = itemView.findViewById(R.id.imageView);
        }
    }
}
