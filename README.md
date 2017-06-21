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

### 二、Demo使用方法


#### 删除sdk-resource目录

直接使用android studio导入项目后即可使用，如果您选用在build.gradle下添加远程依赖来进行构建，请删除sdk-resource目录 ，该目录仅仅是保存了您本地构建需要的库文件，和demo工程没有直接的依赖关系。

#### 不可暴露自己的secretKey
登录页面时需要的配置信息，您可以直接在app上填写，也可以修改assets目录下的config.json文件的配置，然后点击读取配置即可。这仅仅用于测试，正式环境下不可暴露自己的secretKey。

### 三、sdk的简单使用

sdk的介绍请参见[文档](https://www.qcloud.com/document/product/441/6892)。
