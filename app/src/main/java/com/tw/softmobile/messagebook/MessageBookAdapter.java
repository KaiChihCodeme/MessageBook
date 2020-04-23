package com.tw.softmobile.messagebook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class MessageBookAdapter extends RecyclerView.Adapter<MessageBookAdapter.ViewHolder> {
    private String[] arr_message;
    private String[] arr_timeStamp;
    private Listener listener;
    private Activity activity;
    private int[] arr_messageId;

    // 加入介面，讓使用者按下卡片視區時能呼叫onClick
    public interface Listener {
        void onClick(int position);

        void onLongClick(int position);
    }

    //activities and fragment會使用此方法來註冊listener
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    //建立view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // 定義各個資料項目使用的視區
        //recyclerview需顯示cardview，所以宣告viewHolder裡有cardview。若要在recycler視區內顯示別的類型的資料須在此處定義
        private CardView cardView;

        public ViewHolder(CardView itemView) {
            super(itemView);
            cardView = itemView;
        }
    }

    //用adapter建構是把資料傳給兩個變數
//    public MessageBookAdapter(String[] message, String[] time) {
//        this.arr_message = message;
//        this.arr_timeStamp = time;
//    }

    public MessageBookAdapter(String[] message, String[] time, int[] messageId, Activity activity) {
        this.arr_message = message;
        this.arr_timeStamp = time;
        this.arr_messageId = messageId;
        this.activity = activity;
    }

    //視區建立新view holder時呼叫這邊(使用cardview版面)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // 建立項目使用的畫面配置元件，指定viewholder需使用哪個版面(card)
        // 用layoutinflater將版面轉為cardview
        CardView cv = (CardView) LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.cardview_message, viewGroup, false);
        // 建立與回傳包裝好的畫面配置元件
        return new ViewHolder(cv);
    }

    //當recycler視區想要使用或重複使用view holder來顯示新資料時會呼叫此方法(且將資料放進去view相對應位置裡面)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        CardView cardView = viewHolder.cardView;

        //設定textview，並將message設定上去
        TextView tv_message = (TextView) cardView.findViewById(R.id.tv_message);
        tv_message.setText(arr_message[i]);

        //設定textview，並將time設定上去
        TextView tv_timeStamp = (TextView) cardView.findViewById(R.id.tv_timeStamp);
        tv_timeStamp.setText(arr_timeStamp[i]);

        //讓cardview可以被按下並啟動別的(將listener加入cardview)
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //當cardview被按下時呼叫listener.onClick()
                if (listener != null) {
                    listener.onClick(i);
                }
            }
        });

        //監聽長按刪除
        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
                dialog.setTitle(R.string.dialog_title);
                dialog.setMessage(R.string.dialog_message);
                dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int j) {

                    }
                });

                dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int j) {
//                        Thread delete_thread = new Thread(((MessageBookActivity)activity).deleteThread);
//                        delete_thread.start();
                        if (activity instanceof MessageBookActivity) {
                            Log.d(((MessageBookActivity) activity).TAG, "click messageid: " + arr_messageId[i]);
                            ((MessageBookActivity) activity).deleteMessage(arr_messageId[i]);
                        }
                    }
                });
                dialog.show();

                if (listener != null) {
                    listener.onLongClick(i);
                }

                return true;
            }
        });
    }


    @Override
    public int getItemCount() {
        //names陣列長度等於recycler視區的資料數目
        return arr_message.length;
    }

}
