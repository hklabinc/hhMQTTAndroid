package com.hklab.hhmqttandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MainActivity extends AppCompatActivity implements MqttCallback {

    private static final String LOGTAG = "[HHCHOI]";
    TextView textView;
    EditText editTextAddr, editTextTopicSub, editTextTopicPub, editTextMessage;
    Button buttonSub, buttonPub;
    final Handler handler = new Handler();  // Handler

    private MqttClient client;
    private MqttConnectOptions option;
    private static final int    QOS    = 2;     // At most once (0), At least once (1), Exactly once (2)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddr = findViewById(R.id.editTextAddr);
        editTextTopicSub = findViewById(R.id.editTextTopicSub);
        editTextTopicPub = findViewById(R.id.editTextTopicPub);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSub = findViewById(R.id.buttonSub);
        buttonPub = findViewById(R.id.buttonPub);
        textView = findViewById(R.id.textView);

        // MQTT 연결
        buttonSub.setOnClickListener(v -> {
            String serverAddr = "tcp://" + editTextAddr.getText().toString();
            initialize(serverAddr);

            if(client != null) {
                String topic_sub = editTextTopicSub.getText().toString();
                subscribe(topic_sub);
            }
        });

        // 메시지 전송
        buttonPub.setOnClickListener(v -> {
            if (client != null) {   //연결이 되어있다면
                // Handler 사용 (UI 접근 위해)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String topic = editTextTopicPub.getText().toString();
                        String message = editTextMessage.getText().toString();
                        editTextMessage.setText("");

                        publish(topic, message);
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "연결 필요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* MQTT initialize */
    public void initialize(String serverAddr) {
        option = new MqttConnectOptions();
        option.setCleanSession(true);
        option.setKeepAliveInterval(30);
        try {
            client = new MqttClient(serverAddr, "Android", new MemoryPersistence());        // clientId 써줘야! 안써주면 에러 발생!
            client.setCallback(this);
            client.connect(option);

            Log.d(LOGTAG, "MQTT initialize success!");
            Toast.makeText(getApplicationContext(), "연결 성공", Toast.LENGTH_SHORT).show();
        } catch (MqttException e) {
            Log.d(LOGTAG, "MQTT initialize error!");
            Toast.makeText(getApplicationContext(), "연결 실패", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /* MQTT subscribe */
    public void subscribe(String topic_sub) {
        try {
            client.subscribe(topic_sub, QOS);
            Log.d(LOGTAG, "MQTT subscribe success!");
        } catch (MqttException e) {
            Log.d(LOGTAG, "MQTT subscribe error!");
            e.printStackTrace();
        }
    }

    /* MQTT publish */
    public void publish(String topic_pub, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());    // 전송할 메시지
            mqttMessage.setQos(QOS);
            client.publish(topic_pub, mqttMessage);     // Sent message via MQTT

            Log.d(LOGTAG, "MQTT tx: " + topic_pub + ", " + message.length() + " bytes, " + message);
            Toast.makeText(getApplicationContext(), "메시지 전송", Toast.LENGTH_SHORT).show();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /* MQTT close */
    public void close(){
        if(client != null){
            try {
                client.disconnect();
                client.close();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    // 메시지 도착시 자동으로 호출됨
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String rxMsg = message.toString();
        Log.d(LOGTAG, "MQTT rx: " + topic + ", " + rxMsg.length() + " bytes, " + rxMsg);

        // Handler 사용 (UI 접근 위해)
        handler.post(new Runnable() {
            @Override
            public void run() {
                textView.append(topic + " : " + message + "\n");
            }
        });
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(LOGTAG, "MQTT connection lost!");
        Toast.makeText(getApplicationContext(), "MQTT connection lost!", Toast.LENGTH_SHORT).show();
    }

    // 메시지 전송이 완료되면 자동으로 호출됨
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(LOGTAG, "MQTT delivery complete!");
        // Toast.makeText(getApplicationContext(), "MQTT delivery complete!", Toast.LENGTH_SHORT).show(); // 여기 Toast가 있으면 connectionLost 됨
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // MQTT 종료
        close();
    }
}