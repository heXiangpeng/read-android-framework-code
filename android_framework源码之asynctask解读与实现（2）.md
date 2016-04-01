##android framework源码之AsyncTask解读与实现（2）

>AsyncTask是一个用于UI线程，方便异步操作并且把结果返回给UI主线程的一个类，而不需要去操作**threads和handlers**


###一.AsyncTask的用法

定义一个下载操作类

```
 private class DownloadFilesTask extends AsyncTask<URL, Integer, Long> {
// 必须重载这一个方法 
     protected Long doInBackground(URL... urls) {
         int count = urls.length;
         long totalSize = 0;
         for (int i = 0; i < count; i++) {
             totalSize += Downloader.downloadFile(urls[i]);
             publishProgress((int) ((i / (float) count) * 100));
             // Escape early if cancel() is called
             if (isCancelled()) break;
         }
         return totalSize;
     }

// 
     protected void onProgressUpdate(Integer... progress) {
         setProgressPercent(progress[0]);
     }
// 这个方法也是通常比较重要的
     protected void onPostExecute(Long result) {
         showDialog("Downloaded " + result + " bytes");
     }
 }
 

```

创建这个类之后，我们可以非常简单的操作

```
 new DownloadFilesTask().execute(url1, url2, url3); //开启任务
```

谷歌为开发者集成和封装了很好的操作类，以至于我们很轻松的去使用它.

###二.开始源码

```
//定义了一个抽象类
public abstract class AsyncTask<Params, Progress, Result> {



}

```

变量

```
省略掉几个参数

 private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

//初始化一个队里为128的堆
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);
            
            
//执行线程队列，线程安全           
 private static class SerialExecutor implements Executor {
     final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }


```

类构造方法

```
public AsyncTask() {
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);

                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                //noinspection unchecked
                return postResult(doInBackground(mParams));
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occured while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }


//唤起一个线程
 private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }
    
    
//用了一个Handler的方法去实现消息传递
   private static class InternalHandler extends Handler {
        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult result = (AsyncTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }  
    
    
//执行结果的输出
private Result postResult(Result result) {
        @SuppressWarnings("unchecked")
        Message message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
                new AsyncTaskResult<Result>(this, result));
        message.sendToTarget();
        return result;
    }

    
```

执行线程方法

```
  public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

```

### 三.简单的原理实现



简单实现了一个单一线程的异步线程方法

```

public abstract class HYAsyncTask {

 //源码是通过参数去传递对象，而这里简便的使用一个变量
    String name = "";


    Handler inetrHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

           String name = (String)msg.obj;

            switch (msg.what){
                case 1:

//用handler去实现参数传递
                    onPostExecute(name);


                    break;

                default:
                    break;
            }
        }
    };


    public HYAsyncTask(){

        new Thread(new Runnable() {
            @Override
            public void run() {


                name = doInBackground(name);

                Message message = inetrHandler.obtainMessage(1,name);
                message.sendToTarget();


            }
        }).start();

    }




    protected abstract String doInBackground(String name);

    protected void onPostExecute(String result) {
    }



}

```

使用方法

```
 HYAsyncTask task = new HYAsyncTask() {
            @Override
            protected String doInBackground(String name) {

                return "化缘";
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                Log.e("name","123"+result);

            }
        };
```

不知道这样说是不是把AsyncTask的运行机制说明白了，如果有问题，大家可以评论。代码可以去**[github]()**

### 四.下一篇预告

下一篇的任务是学习ActivityManagerService的运行机制，(1)统一调度各应用程序的Activity
       （2）内存管理
       （3）进程管理 的功能
       
感觉一步步入坑了。       


