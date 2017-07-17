package edu.buffalo.cse.cse486586.groupmessenger2;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Random;


import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    /*
     static final String REMOTE_PORT0 = "11108";
     static final String REMOTE_PORT1 = "11112";
     static final String REMOTE_PORT2 = "11116";
     static final String REMOTE_PORT3 = "11120";
     static final String REMOTE_PORT4 = "11124";
     */
    int port_array[]= {11108,11112,11116,11120,11124};
    static final int SERVER_PORT = 10000;
    static int msg_id=0;
    static int seq_no=0;
    static int null_port=0;
    static  String myport;
    static int order=0;
    Comparator<String> comparator = new StringLengthComparator();
    PriorityQueue<String> q = new PriorityQueue<String>(1000,comparator);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myport = String.valueOf((Integer.parseInt(portStr) * 2));

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        //  setContentView(R.layout.content_layout_id);

        final EditText editText= (EditText)findViewById(R.id.editText1);

        final Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG,"on click");
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            Socket clientSocket = null;
            while(true) {
                try {
                    clientSocket = serverSocket.accept();
                    BufferedReader in = null;
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    String inputLine="";
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    inputLine=in.readLine();
                    //   if(inputLine==null)break;
                    // Log.e("Input exception_1",inputLine);
                    int msgid=0,seq=0,port,deliverystatus=0;
                    String message;

                    int j=0,k=1,i=0;

                    for(i=0;i<inputLine.length();i++){
                        if(inputLine.charAt(i)=='#'){
                            String temp=inputLine.substring(j,i);
                            if(k==1){msgid=Integer.parseInt(temp);k++;j=i+1;}
                            else if(k==2){seq=Integer.parseInt(temp);k++;j=i+1;}
                            else if(k==3){port=Integer.parseInt(temp);k++;j=i+1;}
                            else if(k==4){
                                deliverystatus=Integer.parseInt(temp);
                                break;}
                        }
                    }


                    if(k==1){
                        Log.i("k_1",inputLine);
                        null_port=Integer.parseInt(inputLine);
                        Log.i("null_port",inputLine);
                        ArrayList<String> temp2 =new ArrayList<String>();
                        Log.i("null_port",inputLine);

                        while(q.size()>0){
                            String s=q.peek();
                            j=0;k=1;
                            int msgid_1=0,port_1=0,seq_1=0,deliverystatus_1=0;

                            for(i=0;i<s.length();i++){
                                if(s.charAt(i)=='#'){
                                    String temp=s.substring(j,i);
                                    if(k==1){msgid_1=Integer.parseInt(temp);k++;j=i+1;}
                                    else if(k==2){seq_1=Integer.parseInt(temp);k++;j=i+1;}
                                    else if(k==3){port_1=Integer.parseInt(temp);k++;j=i+1;}
                                    else if(k==4){
                                        deliverystatus_1=Integer.parseInt(temp);
                                        break;}
                                }
                            }
                            if(deliverystatus_1==0 && port_1==null_port)q.remove();
                            else {
                                q.remove();
                                temp2.add(s);
                            }

                        }
                        i=0;
                        for(i=0;i<temp2.size();i++) {
                            String s = temp2.get(i);
                            q.add(s);
                        }
                        out.println("ok");
                        clientSocket.close();
                    }
                    //replying with sequence number
                    else if(deliverystatus==0) {
                        Log.i("inputLine",inputLine);
                        message=inputLine.substring(i+1);
                        seq_no++;
                        String seqq = Integer.toString(seq_no);
                        seqq+="#";
                        seqq+=myport;
                        out.println(seqq);
                        q.add(inputLine);
                       // if(in.readLine()=="ok") clientSocket.close();
                    }
                    else {
                        Log.i("inputLine_1",inputLine);
                        ArrayList<String> q_str = new ArrayList<String>();
                        if(seq>seq_no)seq_no=seq;
                        //searching in a queue
                        while(q.size()>0){

                            String s=q.peek();

                            j=0;k=1;i=0;

                            boolean b=false;
                            for(i=0;i<s.length();i++){
                                if(s.charAt(i)=='#'){
                                    String temp=s.substring(j,i);
                                    if(k==1){
                                        int msgid_1=Integer.parseInt(temp);
                                        k++;
                                        j=i+1;
                                        if(msgid_1==msgid){b=true;} //message found
                                    }
                                    else if(k==2){seq=Integer.parseInt(temp);k++;j=i+1;}
                                    else if(k==3){port=Integer.parseInt(temp);k++;j=i+1;}
                                    else if(k==4){
                                        deliverystatus=Integer.parseInt(temp);
                                    }
                                }
                            }
                            if(b!=true) {
                                q_str.add(s);
                                q.remove();
                            }
                            else{
                                q.remove();
                                q_str.add(inputLine);
                                for(i=0;i<q_str.size();i++){
                                    q.add(q_str.get(i));
                                }
                                break;
                            }

                        }

                        //iterating through the queue
                        while(q.size()>0) {
                            //  Log.e("Input exception_3","deliverystatus");
                            String s=q.peek();
                            String ins="";
                            int msgid_1=0;
                            int port_1=0;
                            j=0;k=1;i=0;
                            for(i=0;i<s.length();i++){
                                if(s.charAt(i)=='#'){
                                    String temp=s.substring(j,i);
                                    if(k==1){
                                        msgid_1=Integer.parseInt(temp);
                                        k++;
                                        j=i+1;
                                    }
                                    else if(k==2){seq=Integer.parseInt(temp);k++;j=i+1;}
                                    else if(k==3){port=Integer.parseInt(temp);k++;j=i+1;}
                                    else if(k==4){deliverystatus=Integer.parseInt(temp);break;}
                                }
                            }
                            String message_1="";
                            message_1+=s.substring(i+1);
                            if(deliverystatus==1) {
                                //  Log.e("Input exception_3",message_1);
                                q.remove();
                                // publishProgress(message);
                                Uri uri;
                                uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                                ContentResolver cc;
                                cc = getContentResolver();
                                ContentValues cv = new ContentValues();
                                String str = Integer.toString(order);
                                order++;
                                cv.put("key", str);
                                cv.put("value", message_1);
                                cc.insert(uri, cv);
                            }
                            else break;
                        }
                        out.println("ok");
                        clientSocket.close();
                    }
                    // out.println("ok");
                    // if(in.readLine()=="ok") clientSocket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //  return null;
        }



        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");

            return;
        }
    }



    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            int max_seqno=Integer.MIN_VALUE;
            Random rand = new Random();
            msg_id =rand.nextInt(10000000);
            String msgToSend;
            String final_port_no="";
            try {
                int arr[] = {11108, 11112, 11116, 11120, 11124};
                msgToSend = Integer.toString(msg_id); //msgid
                msgToSend += "#";
                // if(i==0) msg_id++;
                msgToSend += Integer.toString(seq_no);  // seq number
                msgToSend += "#";
                msgToSend += myport; //sending  my port number
                msgToSend += "#";
                msgToSend += "0"; //delivery status
                msgToSend += "#";
                msgToSend += msgs[0]; //message

                for (int i = 0; i <= 4; i++) {

                    if (null_port != 0 && null_port == arr[i]) continue;

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            arr[i]);

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(msgToSend);
                    out.flush();
                    Log.e(TAG, msgToSend);

                    try {
                        String  seqq = in.readLine();

                        int temp = 0; //store the sequence number from the server
                        int j = 0;
                        for (j = 0; j < seqq.length(); j++) {
                            if (seqq.charAt(j) == '#') {
                                temp = Integer.parseInt(seqq.substring(0, j));
                                break;
                            }
                        }
                        //store the port
                        String p = seqq.substring(j + 1);

                        int temp_1 = 0;
                        temp_1 = Integer.parseInt(p);

                        if (temp > max_seqno) {
                            max_seqno = temp;
                            final_port_no = p;
                        } else if (temp == max_seqno) {
                            int rt = Integer.parseInt(final_port_no);
                            if (rt != 0 && rt > temp_1) final_port_no = p;
                        }
                     //   out.println("ok");   //change this if it doesn't work
                    } catch (NullPointerException n) {

                        null_port = arr[i];
                        Log.i("firstcatch", "firstcatch");
                        for (int k = 0; k <= 4; k++) {
                            if (null_port == arr[k]) continue;
                            Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    arr[k]);
                            BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
                            PrintWriter out1 = new PrintWriter(socket1.getOutputStream(), true);
                            String temp = "";
                            temp += Integer.toString(null_port);
                            temp += "\n";
                            out1.println(temp);
                            if(in1.readLine()=="ok")socket1.close();
                        }
                    }
                }

                msgToSend="";

                msgToSend+=Integer.toString(msg_id); //msgid
                msgToSend+="#";
                msgToSend+=Integer.toString(max_seqno);  // seq number
                msgToSend+="#";
                msgToSend+=final_port_no; //sending   port number of the suggested sequence
                msgToSend+="#";
                msgToSend+="1"; //delivery status
                msgToSend+="#";
                msgToSend+= msgs[0]; //message

                for(int i=0;i<=4;i++){
                    if(null_port!=0 && null_port==arr[i])continue;
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                arr[i]);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out.println(msgToSend);
                        out.flush();
                        Log.e(TAG, msgToSend);
                        String str = in.readLine();
                        if (str == "ok") socket.close();
                    }catch(NullPointerException n){
                        null_port = arr[i];
                        Log.i("secondcatch", "secondcatch");
                        for (int k = 0; k <= 4; k++) {
                            if (null_port == arr[k]) continue;
                            Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    arr[k]);
                            BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
                            PrintWriter out1 = new PrintWriter(socket1.getOutputStream(), true);
                            String temp = "";
                            temp += Integer.toString(null_port);
                            temp += "\n";
                            out1.println(temp);
                            if(in1.readLine()=="ok")socket1.close();
                        }
                    }
                }

            }catch(UnknownHostException e){
                Log.e(TAG, "ClientTask UnknownHostException");
            }catch(IOException e){
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }

    }





    public class StringLengthComparator implements Comparator<String>
    {
        @Override
        public int compare(String x, String y)
        {

            int msgid1=0,seq1=0,port1=0,deliverystatus1=0;
            int msgid2=0,seq2=0,port2=0,deliverystatus2=0;
            // String message;

            int j=0,k=1,i=0;

            for(i=0;i<x.length();i++){
                if(x.charAt(i)=='#'){
                    String temp=x.substring(j,i);
                    if(k==1){msgid1=Integer.parseInt(temp);k++;j=i+1;}
                    else if(k==2){seq1=Integer.parseInt(temp);k++;j=i+1;}
                    else if(k==3){port1=Integer.parseInt(temp);k++;j=i+1;}
                    else if(k==4){deliverystatus1=Integer.parseInt(temp);break;}
                }
            }
            j=0;k=1;
            for(i=0;i<y.length();i++){
                if(y.charAt(i)=='#'){
                    String temp=y.substring(j,i);
                    if(k==1){msgid2=Integer.parseInt(temp);k++;j=i+1;}
                    else if(k==2){seq2=Integer.parseInt(temp);k++;j=i+1;}
                    else if(k==3){port2=Integer.parseInt(temp);k++;j=i+1;}
                    else if(k==4){deliverystatus2=Integer.parseInt(temp);break;}
                }
            }


            if(seq1==seq2){
                if(deliverystatus1<deliverystatus2)return -1;
                else if(deliverystatus2<deliverystatus1)return 1;

                if(deliverystatus1==1 && deliverystatus2==1) {
                    if (port1 < port2) return -1;
                    else return 1;
                }
            }

            if(seq1<seq2){
                return -1;
            }
            return 1;

        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}