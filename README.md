Strangers(随机视频聊天):

零:

> 1.用mikhaellopez:circularimageview库放置圆图, 在xml设置好

2.用Glide库显示图片

3.用ActivityMainBinding来绑定xml(每创建一个activity自动生成一个binding )

4.使用WebRTC视频服务, 以服务器为中间人请求和响应, 后续通信点对点;
将WebRTC文件放到assets下

5.连接时用lottie弄一个等待页面

6.在flaticon网站找图片元素

1.  LoginActivity(GoogleLogin):

```{=html}
<!-- -->
```
1.  用得到的AuthCredential获得当前user保存在RealtimeDB 的profiles下

2.  将相机和音频权限放到集合, 通知申请

```{=html}
<!-- -->
```
2.  MainActivity:

```{=html}
<!-- -->
```
1.  用lottie库, 把动图json文件放到res下创建的raw下, 在xml设置好

```{=html}
<!-- -->
```
3.  ConnectingActivity:

> 1\. 用lottie库, 把动图json文件放到res下创建的raw下, 在xml设置好
>
> 2\. 在shape里用gradient设置背景渐变
>
> 3\. 检索数据库users中的用户中status为0的用户
>
> 4\. (创建房间活动)如果没有status为0的用户, 在users下以自己id创建用户
>
> 5\. (加入房间活动)如果有, 那就将自己的id设在users下userId下的incoming,
> 并设置并列的status为1

四. CallActivity:

1\. 用webView启动资源下的html; 将InterfaceJava加入到WebView中,
连接启动后它其中注释的JavaSript会自动执行(把isPeerConnected设为true,
可以判断一下检查是否正常)

2\. 点击关麦或关屏幕调用响应的javascript函数(一开始也要调用一个)

3\. 连接成功启动后, 建立一个线程延迟4秒执行(等房主创建connId,
代码是同时的), 成功后用connId传入javascript方法开启VideoCall

4\. destroy时把退出变量设为true把数据库的users中的房主文档的值设为null,
点击关闭直接finish

五. RewardActivity

1\. 用kaopiz:kprogresshud库显示加载动画

2\. 用play-services-ads库实现广告功能, 点击按钮打开广告视频,
获得一定金币奖励(每个用户固定500金币, 每连接一次花费5金币)
