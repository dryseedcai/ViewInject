package com.dryseed.viewinject;

public interface ViewInject<T> {
    void inject(T t, Object source);
}
