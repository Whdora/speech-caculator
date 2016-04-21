package com.example.aya.calculator;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.alibaba.fastjson.JSONArray;

import java.util.List;

public  class MyListViewAdapter extends BaseAdapter {
    private Context context;
    private JSONArray listItems;

    public MyListViewAdapter(Context context, JSONArray listItem){
        this.listItems = listItem;
        this.context = context;
    }

    public int getCount(){
        return this.listItems.size();
    }

    public Object getItem(int position){
        return this.listItems.get(position);
    }

    public long getItemId(int position){
        return position;
    }

    public View getView(int position, View convertView,ViewGroup parent){
        if (convertView == null){
            convertView = new TextView(this.context);
            ((TextView)convertView).setTextSize(20);
            convertView.setPadding(30, 10, 10, 10);
        }
        ((TextView) convertView).setText((String)this.listItems.get(position));

        return convertView;
    }

    public void addItem(String newRecord){
        if (this.listItems.size() == 1 && this.listItems.get(0).equals("您还没有计算过哦")){
            this.listItems.clear();
        }
        this.listItems.add(newRecord);
        this.notifyDataSetChanged();
    }
}
