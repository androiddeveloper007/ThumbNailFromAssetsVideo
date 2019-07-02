package com.thumbnailfromasset;

import android.os.Handler;

import java.lang.ref.WeakReference;

public class UIHandler<T> extends Handler {

    protected WeakReference<T> ref;

    public UIHandler(T cls){
        ref = new WeakReference<T>(cls);
    }

    public T getRef(){
        return ref != null ? ref.get() : null;
    }
}
