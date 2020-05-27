package com.example.cryptochat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static String myName = "";

    RecyclerView chatWindow;
    private MessageController controller;
    private Server server;

    TextView onlineCount;
    private static int totalUsers = 0;

    /**
     * Увеличивает счетчик кол-ва пользователей на 1, устанавливает текст в счетчике интерфейса
     */
    private void incUserCount() {
        totalUsers++;
        onlineCount.setText(Integer.toString(totalUsers));
    }

    /**
     * Уменьшает счетчик кол-ва пользователей на 1, устанавливает текст в счетчике интерфейса
     */
    private void decUserCount() {
        totalUsers--;
        onlineCount.setText(Integer.toString(totalUsers));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your name");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                myName = input.getText().toString();
                server.sendName(myName);
            }
        });
        builder.show();

        onlineCount = findViewById(R.id.onlineCount);// Текстовый элемент с кол-вом пользвоателей онлайн

        chatWindow = findViewById(R.id.chatWindow);

        controller = new MessageController();

        controller
                .setIncomingLayout(R.layout.incoming_message)
                .setOutgoingLayout(R.layout.outgoing_message)
                .setMessageTextId(R.id.messageText)
                .setMessageTimeId(R.id.messageTime)
                .setUserNameId(R.id.userName)
                .appendTo(chatWindow, this);

        final EditText chatInput = findViewById(R.id.chatInput);
        Button sendMessage = findViewById(R.id.sendMessage);

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = chatInput.getText().toString();
                controller.addMessage(
                        new MessageController.Message(text, myName, true)
                );
                chatInput.setText("");
                server.sendMessage(text);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        server = new Server(new Consumer<Pair<String, String>>() {
            @Override
            public void accept(final Pair<String, String> pair) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        controller.addMessage(
                                new MessageController.Message(pair.second, pair.first, false)
                        );
                    }
                });
            }
        }, new Consumer<Pair<String, String>>() {
            @Override
            public void accept(final Pair<String, String> pair) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text = String.format("From %s: %s", pair.first, pair.second);
                        Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, new Consumer<Protocol.User>() { // Consumer для получени инфы о подключенном пользователе
            @Override
            public void accept(final Protocol.User user) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        incUserCount();
                        String text = String.format("%s подключился к чату", user.getName());// Формируем строку-сообщение для тоста
                        Toast toast = Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();// Выводим тост на экран
                    }
                });
            }
        }, new Consumer<Protocol.User>() { // Consumer для получени инфы об отключенном пользователе
            @Override
            public void accept(final Protocol.User user) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        decUserCount();
                    }
                });
            }
        });
        server.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        server.disconnect();
    }
}
