package com.time.hexiangpeng.hyasynctask;


import android.os.Handler;
import android.os.Message;




/**
 * Created by hexiangpeng on 16/4/1.
 */
public abstract class HYAsyncTask {


    String name = "huayuan";


    Handler inetrHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

           String name = (String)msg.obj;

            switch (msg.what){
                case 1:


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
