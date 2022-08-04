
cordova 封装的腾讯地图定位sdk
##Android下使用
1、配置AppKey
<application>
    ...
    <meta-data android:name="TencentMapSDK" android:value="您申请的Key" />
</application>
2、在 build.gradle 文件的 dependencies 中配置

compile fileTree(include: [’*.jar’], dir: ‘libs’)
