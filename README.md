# 腾讯云智能语音Android SDK接口

这是一个腾讯云语音sdk集成的完整demo，用户只需要填写自己的appid、projectid、secretId、secretKey即可进行实时语音识别。

这个demo中主要包含了：

### 一、sdk集成方法
#### 1. 在build.gradle下添加远程依赖
build.gradle文件中添加
```
compile 'com.tencent.aai:aai:2.1.2:@aar'
compile 'com.squareup.okhttp3:okhttp:3.6.0'
compile 'org.slf4j:slf4j-android:1.6.1-RC1'
```
即可将智能语音服务集成到自己的app中。

#### 2. 下载so文件和jar包后，进行本地构建
这种方式下，用户首先需要先下载对应的so文件和jar包（均在sdk-source目录下），然后将okhttp3、okio和slf4j三个库也集成到app中。

### 二、sdk的简单使用

sdk的介绍请参见[文档](https://www.qcloud.com/document/product/441/6892)。
