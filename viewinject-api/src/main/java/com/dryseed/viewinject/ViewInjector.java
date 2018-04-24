package com.dryseed.viewinject;

import android.app.Activity;
import android.view.View;

/**
 * 有了代理类之后，我们一般还会提供API供用户去访问，例如本例的访问入口是
 * //Activity中
 * Ioc.inject(Activity);
 * //Fragment中，获取ViewHolder中
 * Ioc.inject(this, view);
 * <p>
 * <p>
 * API就干两件事：
 * ① 根据传入的host寻找我们生成的代理类：例如MainActivity->MainActity$$ViewInjector。
 * ② 强转为统一的接口，调用接口提供的方法。
 */
public class ViewInjector {
    private static final String SUFFIX = "$$ViewInject";

    public static void injectView(Activity activity) {
        ViewInject proxyActivity = findProxyActivity(activity);
        //3.调用接口提供的方法
        proxyActivity.inject(activity, activity);
    }

    public static void injectView(Object object, View view) {
        ViewInject proxyActivity = findProxyActivity(object);
        //3.调用接口提供的方法
        proxyActivity.inject(object, view);
    }

    private static ViewInject findProxyActivity(Object activity) {
        try {
            //1. 根据传入的host寻找我们生成的代理类：例如MainActivity->MainActity$$ViewInjector。
            Class clazz = activity.getClass();
            Class injectorClazz = Class.forName(clazz.getName() + SUFFIX);
            //2. 强转成ViewInject接口
            return (ViewInject) injectorClazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException(String.format("can not find %s , something when compiler.", activity.getClass().getSimpleName() + SUFFIX));
    }
}
