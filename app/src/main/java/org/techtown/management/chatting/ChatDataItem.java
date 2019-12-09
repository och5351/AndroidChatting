package org.techtown.management.chatting;

public class ChatDataItem {

    private String content;
    private String name;



    private int viewType;

    public ChatDataItem(){}
    public ChatDataItem(String content, String name ,int viewType) {
        this.content = content;
        this.viewType = viewType;
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public String getName() {
        return name;
    }

    public int getViewType() {
        return viewType;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }
}


