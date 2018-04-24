package com.dryseed.viewinject;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class ViewInjectProcessor extends AbstractProcessor {
    /**
     * 一个类对象，代表具体某个类的代理类生成的全部信息，本例中为ProxyInfo
     * 一个集合，存放上述类对象（到时候遍历生成代理类），本例中为Map<String, ProxyInfo>，key为类的全路径。
     * <p>
     * Element
     * - VariableElement    //一般代表成员变量
     * - ExecutableElement  //一般代表类中的方法
     * - TypeElement        //一般代表代表类
     * - PackageElement     //一般代表Package
     */
    private Messager messager;
    private Elements elementUtils;
    private Map<String, ProxyInfo> mProxyMap = new HashMap<String, ProxyInfo>();

    /**
     * Filer mFileUtils; 跟文件相关的辅助类，生成JavaSourceCode.
     * Elements mElementUtils;跟元素相关的辅助类，帮助我们去获取一些元素相关的信息。
     * Messager mMessager;跟日志相关的辅助类。
     *
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    /**
     * @return 返回支持的注解类型
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(Bind.class.getCanonicalName());
        return supportTypes;
    }

    /**
     * @return 返回支持的源码版本
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * process中的实现，相比较会比较复杂一点，一般你可以认为两个大步骤：
     * 1. 收集信息
     * 2. 生成代理类（本文把编译时生成的类叫代理类）
     *
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.NOTE, "process...");
        //因为process可能会多次调用，避免生成重复的代理类，避免生成类的类名已存在异常。
        mProxyMap.clear();

        //一、收集信息
        Set<? extends Element> elesWithBind = roundEnv.getElementsAnnotatedWith(Bind.class);
        for (Element element : elesWithBind) {
            //检查element类型
            checkAnnotationValid(element, Bind.class);

            //field type 成员变量信息VariableElement
            VariableElement variableElement = (VariableElement) element;
            //class type 拿到对应的类信息TypeElement
            TypeElement classElement = (TypeElement) variableElement.getEnclosingElement();
            //full class name 类的全路径
            String fqClassName = classElement.getQualifiedName().toString();

            ProxyInfo proxyInfo = mProxyMap.get(fqClassName);
            if (proxyInfo == null) {
                proxyInfo = new ProxyInfo(elementUtils, classElement);
                mProxyMap.put(fqClassName, proxyInfo); //有Bind注解声明的类
            }

            //把被Bind注解声明的variableElement加入到proxyInfo.injectVariables中
            Bind bindAnnotation = variableElement.getAnnotation(Bind.class);
            int id = bindAnnotation.value();
            proxyInfo.injectVariables.put(id, variableElement);
        }

        //二、生成代理类
        for (String key : mProxyMap.keySet()) { //有Bind注解声明的类
            ProxyInfo proxyInfo = mProxyMap.get(key);
            try {
                //通过mFileUtils.createSourceFile来创建文件对象
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                        proxyInfo.getProxyClassFullName(),
                        proxyInfo.getTypeElement());
                Writer writer = jfo.openWriter();
                //三、生成Java代码
                writer.write(proxyInfo.generateJavaCode());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                error(
                        proxyInfo.getTypeElement(),
                        "Unable to write injector for type %s: %s",
                        proxyInfo.getTypeElement(), e.getMessage()
                );
            }

        }
        return true;
    }

    private boolean checkAnnotationValid(Element annotatedElement, Class clazz) {
        if (annotatedElement.getKind() != ElementKind.FIELD) {
            error(annotatedElement, "%s must be declared on field.", clazz.getSimpleName());
            return false;
        }
        if (ClassValidator.isPrivate(annotatedElement)) {
            error(annotatedElement, "%s() must can not be private.", annotatedElement.getSimpleName());
            return false;
        }

        return true;
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
    }
}
