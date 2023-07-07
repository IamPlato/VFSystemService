# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

###### 基本设置
#指定代码的压缩级别
-optimizationpasses 5

# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共的库的类
-dontskipnonpubliclibraryclasses

# 指定不去忽略非公共的库的类的成员
-dontskipnonpubliclibraryclassmembers

# 有了verbose这句话，混淆后就会生成映射文件
# 包含有类名->混淆后类名的映射关系
# 然后使用printmapping指定映射文件的名称
#-verbose
#-printmapping proguardMapping.txt

# 指定混淆时采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不改变
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# 保护代码中的Annotation不被混淆，这在JSON实体映射时非常重要，比如fastJson
-keepattributes *Annotation*

# 避免混淆泛型，这在JSON实体映射时非常重要，比如fastJson
-keepattributes Signature

# 抛出异常时保留代码行号，在异常分析中可以方便定位
-keepattributes SourceFile,LineNumberTable

# 内部接口不被混淆
-keepattributes InnerClasses
-dontoptimize

# 用于告诉ProGuard，不要跳过对非公开类的处理。默认情况下是跳过的，因为程序中不会引用它们，有些情况下人们编写的代码与类库中的类在同一个包下，并且对包中内容加以引用，此时需要加入此条声明。
-dontskipnonpubliclibraryclasses

# 这个是给Microsoft Windows用户的，因为ProGuard假定使用的操作系统是能区分两个只是大小写不同的文件名，但是Microsoft Windows不是这样的操作系统，所以必须为ProGuard指定-dontusemixedcaseclassnames选项
-dontusemixedcaseclassnames


###### 保留不能被混淆
# 保留所有的本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

#保持继承自系统类的class不被混淆
# 保留了继承自Activity、Application这些类的子类,因为这些子类，都有可能被外部调用
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

# 针对android-support-v4.jar的解决方案
-keep interface android.support.v4.app.** { *; }
-keep class android.support.v4.** { *; }
-keep public class * extends android.support.v4.**
-keep public class * extends android.app.Fragment

# 针对android-support-v7.jar的解决方案
-keep interface android.support.v7.app.** { *; }
-keep class android.support.v7.** { *; }
-keep public class * extends android.support.v7.**

# 枚举类不能被混淆
-keepclassmembers enum * {*;}

# 接口类不能被混淆
-keepclassmembers interface * {*;}

# 保留自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View {*;}

# 对于R（资源）下的所有类及其方法，都不能被混淆
-keep class **.R* {*;}
-keep class **.R$* {*;}

# 保留Parcelable序列化的类不被混淆
-keep class * implements android.os.Parcelable {*;}

# 保留Serializable序列化的类不被混淆
-keepclassmembers class * implements java.io.Serializable {*;}

# 对于带有回调函数onXXEvent的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
}

# 对于带有回调函数OnXXListenerXX的，不能被混淆
-keepclassmembers class * {
    void *(**On*Listener);
}

-keepclassmembers class * {
    interface *(**Listener);
}

-keepclassmembers class * {
    interface *(**);
}

-keepclassmembers class * {
    void *(**On*);
}
-keepclassmembers class * {
    void *(**on*);
}
-keepclassmembers class * {
    boolean *(**On*);
}
-keepclassmembers class * {
    boolean *(**on*);
}

#关闭日志输出
#-assumenosideeffects class android.util.Log {
#	public static boolean isLoggable(java.lang.String, int);
#	public static int v(...);
#	public static int i(...);
#	public static int w(...);
#	public static int d(...);
#	public static int e(...);
#}

#保留Bind标注
-keep class * {
    @butterknife.Bind <fields>;
}

# 保留内嵌类不被混淆
#-keep class com.example.xxx.MainActivity$* { *; }

#-------------EventBus-----------------------
#-keepattributes *Annotation* 这句上边写过了
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
#-------------EventBus-----------------------

#保留引用的jar包类
-keep class android.serialport.SerialPort {*;}
-keep class com.socsi.** { *; }
-keep class com.verifone.** { *; }
-keep class com.ivsign.android.IDCReader.** { *; }
-keep class com.verifone.zxing.** {*;}
-keep class com.ivsign.android.IDCReader.** { *; }
-keep class socsi.middleware.** { *; }
-keep class socsi.serialport.** { *; }
-keep class cn.verifone.atoolsjar.** { *; }
-keep class com.verifone.smartpos.emvmiddleware.** {*;}
-keep class socsi.emvl1.** {*;}
-keep class org.bouncycastle.** {*;}
-keep class com.synodata.** {*;}
-keep class org.opencv.** {*;}
-keep class org.apache.commons.lang3.** {*;}
-keep class com.vfi.smartpos.deviceservice.aidl.**{*;}
-keep class cn.com.keshengxuanyi.mobilereader.** {*;}
-keep class com.synjones.bluetooth.** {*;}
-keep class com.verifyliceselib.** {*;}
-keep class net.sourceforge.zbar.** {*;}
-keep class com.verifone.decoder.** {*;}
-keep class org.bouncycastle.** {*;}
-keep class com.sygg.gson.** {*;}
-keep class com.synodata.** {*;}
-keep class org.sybouncycastle.** {*;}
-keep class org.syopencv.core.** {*;}
-keep class com.alibaba.fastjson.** { *; }
-keep class com.vfc.** { *; }
#--------------org.dom4j.io------------
-keep class org.dom4j.** {*;}
-keep class org.xml.** {*;}
-dontwarn org.dom4j.**
-keep class javax.xml.parsers.** {*;}
#--------------org.dom4j.io------------
-ignorewarnings
-dontwarn com.socsi.**
-dontwarn socsi.middleware.**
-dontwarn org.bouncycastle.**
-dontwarn cn.com.keshengxuanyi.**
-dontwarn com.alibaba.fastjson.**
