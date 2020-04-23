package com.tw.softmobile.messagebook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageBookActivity extends AppCompatActivity {

    private int m_iMessageBookId = 1; //先預設ID是1
    final String TAG = "MessageBookActivity";
//    private String m_sResResult = "";
    private String m_sInputMessage;
    private String m_sGetMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_book);

        final EditText et_message = findViewById(R.id.et_submit_message);
        Button btn_submit = findViewById(R.id.btn_submit);
        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_sInputMessage = et_message.getText().toString();
                if (!m_sInputMessage.isEmpty() && m_sInputMessage != "") {
                    Thread thread_submit = new Thread(submitThread);
                    thread_submit.start();
                    //傳送後就把et清空
                    et_message.setText("");
                } else {
                    makeToastInThread(R.string.null_message);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Thread thread_data = new Thread(dataThread);
        thread_data.start();
    }

    //網路查詢訊息資料
    private Runnable dataThread = new Runnable() {
        @Override
        public void run() {
            //將要傳的id放入map
            Map<String, Integer> map_messageId = new HashMap<>();
            map_messageId.put("messageBookId", m_iMessageBookId);
            //先去要資料庫的資料
            HttpURLConnection connection = null;
            try {
                URL url_req = new URL("http://10.0.2.2:8080/springDB/DataServlet"); //用虛擬機需用此ip才能連到電腦的server
                try {
                    connection = setConnection(url_req, "POST",
                            10000, 10000, true, true);

                    OutputStream os = connection.getOutputStream();
                    DataOutputStream writer = new DataOutputStream(os);
                    //製作JSON
                    String jsonString = getJsonString(map_messageId);
                    writer.writeBytes(jsonString);
                    Log.d(TAG, "request json: " + jsonString);
                    writer.flush();
                    writer.close();
                    os.close();

                    //Get Res
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String line;
                    StringBuilder sb = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        Log.d(TAG, "response query json: " + sb.toString());
                        JSONObject jsonObject = new JSONObject(sb.toString());
                        reader.close();
                        switch (jsonObject.getString("rc")) {
                            case "0000":
                                //將取得結果加入recycler陣列
                                resToArray(jsonObject.getString("body"));
                                break;
                            case "9000":
                                //錯誤代碼9000，後端抓不到request過去的messageBookId
                                makeToastInThread(R.string.query_fail);
                                break;
                            case "9001":
                                //後端query不到資料回來
                                makeToastInThread(R.string.query_fail);
                                break;
                            default:
                                makeToastInThread(R.string.query_fail);
                                break;
                        }

                        //後端response後前端回應畫面
//                        if (sb.toString().equals("9000")) {
//                            //錯誤代碼9000，後端抓不到request過去的messageBookId
//                            makeToastInThread(R.string.query_fail);
//                        } else if (sb.toString().equals("9001")){
//                            //後端query不到資料回來
//                            makeToastInThread(R.string.query_fail);
//                        } else {
//                            //將取得結果加入recycler陣列
//                            resToArray(sb);
//                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } finally {
                if (connection!=null) {
                    connection.disconnect();
                }
            }
        }
    };

    private void makeToastInThread(int string) {
        Looper.prepare();
        Toast.makeText(MessageBookActivity.this, string, Toast.LENGTH_SHORT).show();
        Looper.loop();
    }

    //設定後端連線
    private HttpURLConnection setConnection(URL url, String method, int connectTimeOut,
                               int readTimeOut, boolean doInput, boolean doOutput) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod(method);
            connection.setConnectTimeout(connectTimeOut); //連線時間
            connection.setReadTimeout(readTimeOut); //收發時間
            connection.setDoInput(doInput); //允許輸入
            connection.setDoOutput(doOutput); //允許輸出
        } catch (IOException e) {
            e.printStackTrace();
        }

        return connection;
    }

    //網路寫入訊息資料至資料庫
    private Runnable submitThread = new Runnable() {
        @Override
        public void run() {
            //先去要資料庫的資料
            HttpURLConnection connection = null;
            try {
                URL url_req = new URL("http://10.0.2.2:8080/springDB/SubmitServlet"); //用虛擬機需用此ip才能連到電腦的server
                try {
                    connection = setConnection(url_req, "POST",
                            10000, 30000, true, true);

//                    OutputStream os = connection.getOutputStream();
//                    DataOutputStream writer = new DataOutputStream(os);
                    //製作JSON，傳送訊息與留言板id，時間直接在後端取得
                    String jsonString = "{\"messageBody\":\""+ m_sInputMessage
                            + "\",\"messageBookId\":\"" + m_iMessageBookId + "\"}";
//                    Charset.forName("UTF-8").encode(jsonString);
//                    writer.writeBytes(jsonString);
//                    writer.flush();
//                    writer.close();

                    Log.d(TAG, "request input json: " + jsonString);

                    //用UTF-8編碼，才能順利上傳中文至資料庫
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
                    bw.write(jsonString);
                    bw.flush();
                    bw.close();
//                    os.close();

                    //Get Res(已上傳成功)
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;

                    //後端回傳處理
                    while ((line = reader.readLine()) != null) {
                        try {
                            JSONObject jsonObj = new JSONObject(line);
                            Log.d(TAG, "Submit thread response code: " + jsonObj.getString("rc"));
                            switch (jsonObj.getString("rc")) {
                                case "0000":
                                    //更新留言板UI
                                    Thread thread_updateUI = new Thread(dataThread);
                                    thread_updateUI.start();
                                    break;
                                case "9000":
                                    //後端接不到request
                                    makeToastInThread(R.string.submit_fail);
                                    break;
                                case "9001":
                                    //後端接不到request
                                    makeToastInThread(R.string.submit_fail);
                                    break;
                                case "9002":
                                    //後端接不到request
                                    makeToastInThread(R.string.submit_fail);
                                    break;
                                default:
                                    Log.e(TAG, "SubmitThread cannot fetch response code");
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
//                        if (line.equals("9000")) {
//                            //後端接不到request
//                            makeToastInThread(R.string.submit_fail);
//                        } else if (line.equals("9001")) {
//                            //後端資料庫操作錯誤(無法上傳)
//                            makeToastInThread(R.string.submit_fail);
//                        } else if (line.equals("9002")) {
//                            //後端上傳資料欄位有誤
//                            makeToastInThread(R.string.submit_fail);
//                        } else if (line.equals("0000")){
//                            //上傳資料成功，取得訊息內容給寄出通知信用
////                            m_sGetMessage = line;
//                            //更新留言板UI
//                            Thread thread_updateUI = new Thread(dataThread);
//                            thread_updateUI.start();
//                            //寄出通知信
////                            Thread thread_sendMail = new Thread(sendMailThread);
////                            thread_sendMail.start();
//                        }
                        reader.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } finally {
                if (connection!=null) {
                    connection.disconnect();
                }
            }
        }
    };

//    //寄出通知信
//    private Runnable sendMailThread = new Runnable() {
//        @Override
//        public void run() {
//            //先去要資料庫的資料
//            HttpURLConnection connection = null;
//            try {
//                URL url_req = new URL("http://10.0.2.2:8080/springDB/SendMail"); //用虛擬機需用此ip才能連到電腦的server
//                try {
//                    connection = setConnection(url_req, "POST",
//                            10000, 10000, true, true);
//
//                    //製作JSON，傳送訊息與留言板id，時間直接在後端取得
//                    String message = m_sGetMessage;
//
//                    Log.d(TAG, "request mail message input: " + message);
//
//                    //用UTF-8編碼
//                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
//                    bw.write(message);
//                    bw.flush();
//                    bw.close();
//
//                    //Get Res(已上傳成功)
//                    // 此部分暫時不須處理信件處理的回傳
//                    InputStream is = connection.getInputStream();
////                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
////                    String line = reader.readLine();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } catch (MalformedURLException e) {
//                e.printStackTrace();
//            } finally {
//                if (connection!=null) {
//                    connection.disconnect();
//                }
//            }
//        }
//    };

    //網路刪除訊息
    public void deleteMessage(final int messageId) {
        Runnable deleteThread = new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url_req = new URL("http://10.0.2.2:8080/springDB/SafeDelete"); //用虛擬機需用此ip才能連到電腦的server
                    try {
                        connection = setConnection(url_req, "POST",
                                10000, 10000, true, true);

                        OutputStream os = connection.getOutputStream();
                        DataOutputStream writer = new DataOutputStream(os);
                        //製作JSON，傳送訊息id
                        String jsonString = "{\"messageId\":\"" + messageId +"\"}";
                        writer.writeBytes(jsonString);
                        Log.d(TAG, "Delete request input json: " + jsonString);
                        writer.flush();
                        writer.close();
                        os.close();

                        //Get Res(已上傳成功)
                        InputStream is = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        String line;

                        while ((line = reader.readLine()) != null) {
                            Log.d(TAG, "Delete response json: " + line);
                            JSONObject jsonObject = new JSONObject(line);
                            reader.close();
                            switch (jsonObject.getString("rc")) {
                                case "0000":
                                    //更新留言板UI
                                    Thread thread_updateUI = new Thread(dataThread);
                                    thread_updateUI.start();
                                    break;
                                case "9000":
                                    //後端request處理失敗
                                    makeToastInThread(R.string.delete_fail);
                                    break;
                                case "9001":
                                    //後端資料庫處理失敗
                                    makeToastInThread(R.string.delete_fail);
                                    break;
                            }

//                            if (line.equals("0000")) {
//                                reader.close();
//                                //更新留言板UI
//                                Thread thread_updateUI = new Thread(dataThread);
//                                thread_updateUI.start();
//                            } else if (line.equals("9000")) {
//                                //後端request處理失敗
//                                makeToastInThread(R.string.delete_fail);
//                            } else if (line.equals("9001")) {
//                                //後端資料庫處理失敗
//                                makeToastInThread(R.string.delete_fail);
//                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        };

        Thread delete_thread = new Thread(deleteThread);
        delete_thread.start();
    }

    private String getJsonString(Map<String, Integer> params) {
        JSONObject jsonObj = new JSONObject();
        for(String key:params.keySet()) {
            try {
                jsonObj.put(key, params.get(key));
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return jsonObj.toString();
    }

    private void resToArray(String sb) {
        try {
            JSONArray resJsonArr = new JSONArray(/*m_sResResult*/sb.toString());
            //將回應結果Json清空，避免下次呼叫方法時重複
//            m_sResResult = "";
            String[] arr_message = new String[resJsonArr.length()];
            String[] arr_timeStamp = new String[resJsonArr.length()];
            int[] arr_messageId = new int[resJsonArr.length()];

            for (int i = 0; i < resJsonArr.length(); i++) {
                JSONObject resJsonOb = resJsonArr.getJSONObject(i);
                String message = resJsonOb.getString("messageBody");
                String timeStamp = resJsonOb.getString("timeStamp");
                int messageId = resJsonOb.getInt("messageId");
                //加入reclerview用陣列
                arr_message[i] = message;
                arr_timeStamp[i] = timeStamp;
                arr_messageId[i] = messageId;
            }

            //加入完畢，製作recyclerview
            generateMessageRecycler(arr_message, arr_timeStamp, arr_messageId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void generateMessageRecycler(final String[] arr_message, final String[] arr_time,
                                         final int[] arr_messageId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //將陣列配給adapter
                MessageBookAdapter adapter = new MessageBookAdapter(arr_message, arr_time,
                        arr_messageId, MessageBookActivity.this);
                RecyclerView recyclerView = findViewById(R.id.rv_recycler);
                recyclerView.setAdapter(adapter);
                //設定為直向排列模式
                LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                recyclerView.setLayoutManager(layoutManager);

            }
        });
    }
}
