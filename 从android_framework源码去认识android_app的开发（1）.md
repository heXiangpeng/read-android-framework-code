# 从android framework源码去认识android app的开发（1）
###导语
> android的framework是直接用应用之下的一层，定义了，view,thread,等的java实现，他通过JNI的方式去与下层的硬件去通信，为了去了解android的世界，我们将从java源码的世界去认识一个app是如何使用sdk的代码构建起来的。


## 一.认识android的架构

Android其本质就是在标准的Linux系统上增加了Java虚拟机Dalvik，并在Dalvik虚拟机上搭建了一个JAVA的application framework，所有的应用程序都是基于JAVA的application framework之上。

android分为四个层，从高层到低层分别是**应用程序层、应用程序框架层、系统运行库层和linux核心层**。

## 二.搭建环境

* 搭建开发环境

  对国内的开发者来说最痛苦的是无法去访问[android](http://developer.android.com/intl/zh-cn/guide/index.html)开发网站。为了更好的认识世界，对程序员来说，会翻墙也是的一门技术，带你去领略墙外的世界，好了，不废话了,  **国内开发者访问([androiddevtools](http://www.androiddevtools.cn))** 上面已经有了所有你要的资源，同时可以下载到我们的主角**[framework](http://pan.baidu.com/s/1dD5Z1Hf)**
  
  但是这样的搭建只能去阅读源代码，我们无法去更进一步去**实现自己的rom,我们看到锤子的系统在早期的开放rom是自己从新实现了framework的代码，现在看起来他成功了，所以我们还要去搭建android系统的源码编译环境**。
  
* 搭建源码编译环境

  1.  http://www.cnblogs.com/bluestorm/p/4419135.html
  2.  https://source.android.com/source/downloading.html（这里详细的介绍了如何下载编译）
  
  
## 三.开始主题

我们知道我们在一开始写c程序的时候都有一个运行的入口，比如

```
#include <iostream>
#include <cmath>
#include <algorithm>
using namespace std;

//这里的main就是应用的入口
int main(int argc, const char * argv[]){
    
    
    
    return 0;
}


```

那问题来了

####android程序的入口在哪里呢？

   答：在android/app/ActivityThread.java
  
####但是上面的c语言程序一运行完就退出程序了，app会一直运行，他是怎样做到的呢？

在计算机网络原理中我们用**[socket](http://baike.baidu.com/link?url=tfXj8ddPzy664k_i05aIQvsl-Nt3Fg1aOX_Ae8bLZgb2h-SHvhrLKxQx6irzV2SfBnk4sKs8yp4EyJeQ6MGN-unfzDkrw8fBZwjk9hMV2pq)**实现一个服务器端，不断的接听客户端的访问，而且他的代码是这样实现的：


```
#include <winsock2.h>
#pragma comment(lib, "WS2_32.lib")
 
#include <stdio.h>
void main() 
{
    WORD wVersionRequested;//版本号
    WSADATA wsaData;
    int err;
 
    wVersionRequested = MAKEWORD(2, 2);//2.2版本的套接字
    //加载套接字库,如果失败返回
    err = WSAStartup(wVersionRequested, &wsaData);
    if (err != 0)
    {
        return;
    }
 
    //判断高低字节是不是2,如果不是2.2的版本则退出
    if (LOBYTE(wsaData.wVersion) != 2 || 
         
        HIBYTE(wsaData.wVersion) != 2)
         
    {
        return;
    }
     
         //创建流式套接字,基于TCP(SOCK_STREAM)
 
         SOCKET socSrv = socket(AF_INET, SOCK_STREAM, 0);
 
         //Socket地址结构体的创建
 
         SOCKADDR_IN addrSrv;
 
         addrSrv.sin_addr.S_un.S_addr = htonl(INADDR_ANY);//转换Unsigned long型为网络字节序格
         addrSrv.sin_family = AF_INET;//指定地址簇
         addrSrv.sin_port = htons(6000);
        //指定端口号,除sin_family参数外,其它参数都是网络字节序,因此需要转换
 
         //将套接字绑定到一个端口号和本地地址上
         bind(socSrv, (SOCKADDR*)&addrSrv, sizeof(SOCKADDR));//必须用sizeof，strlen不行
 
         listen(socSrv, 5);
         
         SOCKADDR_IN addrClient;//字义用来接收客户端Socket的结构体
 
         int len = sizeof(SOCKADDR);//初始化参数,这个参数必须进行初始化，sizeof
 
         //循环等待接受客户端发送请求
 
         while (1)
         {
                   //等待客户请求到来；当请求到来后，接受连接请求，
 
                   //返回一个新的对应于此次连接的套接字（accept）。
                   //此时程序在此发生阻塞
 
                   SOCKET sockConn = accept(socSrv, (SOCKADDR*)&addrClient, &len);
 
                   char sendBuf[100];
 
                   sprintf(sendBuf, "Welcome %s to JoyChou", 
 
                            inet_ntoa(addrClient.sin_addr));//格式化输出
 
                   //用返回的套接字和客户端进行通信
 
                   send(sockConn, sendBuf, strlen(sendBuf)+1, 0);//多发送一个字节
 
                   //接收数据
 
                   char recvBuf[100];
 
                   recv(sockConn, recvBuf, 100, 0);
 
                   printf("%s\n", recvBuf);
                   closesocket(sockConn);
 
         }
}
 
```

  
他采用了一个while死循环去监听客户端的请求。 


#### 在一遍啰嗦之后，我们的主角终于闪亮的登场了。

先上源代码

```
public final class ActivityThread {



public static void main(String[] args) {
        SamplingProfilerIntegration.start();

       
        CloseGuard.setEnabled(false);

        Environment.initForCurrentUser();

       
        EventLogger.setReporter(new EventLoggingReporter());

        Security.addProvider(new AndroidKeyStoreProvider());

        final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
        TrustedCertificateStore.setDefaultUserDirectory(configDir);

        Process.setArgV0("<pre-initialized>");

        Looper.prepareMainLooper();
        
        
        //从中可以看到为我们的app开辟了一个线程进入了looper之中

        ActivityThread thread = new ActivityThread();
        thread.attach(false);
        

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }

        AsyncTask.init();

        if (false) {
            Looper.myLooper().setMessageLogging(new
                    LogPrinter(Log.DEBUG, "ActivityThread"));
        }

        Looper.loop();

        throw new RuntimeException("Main thread loop unexpectedly exited");
    }

}

```

看到源码失望了，没有一个while循环啊，其实用了他方法实现

```
 //用一个looper的机制循环监听响应
 Looper.prepareMainLooper();
 
 Looper.loop();

```


进一步深入代码

```
 public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;

       
        Binder.clearCallingIdentity();
        final long ident = Binder.clearCallingIdentity();


// 我们在这里看到了一个循环监听消息

        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }

         
            Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }

            msg.target.dispatchMessage(msg);

            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }

            // Make sure that during the course of dispatching the
            // identity of the thread wasn't corrupted.
            final long newIdent = Binder.clearCallingIdentity();
            if (ident != newIdent) {
                Log.wtf(TAG, "Thread identity changed from 0x"
                        + Long.toHexString(ident) + " to 0x"
                        + Long.toHexString(newIdent) + " while dispatching to "
                        + msg.target.getClass().getName() + " "
                        + msg.callback + " what=" + msg.what);
            }

            msg.recycleUnchecked();
        }
    }


```
  
  
 

## 四.个人信息
  1. [github](https://github.com/heXiangpeng)
  2. [weibo](http://weibo.com/3220027262/profile?rightmod=1&wvr=6&mod=personinfo)
  3. 如果有任何需求可以评论或者私信我，我将一一答复
  
  
## 五. 下一篇的任务

从源码的角度去分析AsyncTask的原理和实现，并且实现一个模仿的AsyncTask，

希望一起成长，一起学习。
   
  




