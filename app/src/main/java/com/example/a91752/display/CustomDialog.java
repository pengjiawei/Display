package com.example.a91752.display;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by pengjiawei on 2018/1/25.
 */

public class CustomDialog extends AlertDialog {


    private Context context;
    private View customView;

    public CustomDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    public CustomDialog(@NonNull Context context, int themeResId,@NonNull int layoutId) {
        super(context, themeResId);
        this.context = context;
        LayoutInflater inflater= LayoutInflater.from(context);
        customView = inflater.inflate(layoutId, null);
        this.setView(customView);
    }

    @Override
    public View findViewById(int id) {
//        return super.findViewById(id);
        return customView.findViewById(id);
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        addContentView(customView,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//
//    }

    public View getCustomView() {
        return customView;
    }

}
