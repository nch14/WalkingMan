package cn.chenhaonee.walkingman.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import cn.chenhaonee.walkingman.R;
import cn.chenhaonee.walkingman.me.MyInfo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LoginActivity extends AppCompatActivity {

    private EditText userNameView;
    private EditText passwordView;

    private Button loginButton;

    private TextView sendPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        showLogin();
    }

    private void showLogin() {
        userNameView = (EditText) findViewById(R.id.input_id);
        passwordView = (EditText) findViewById(R.id.input_password);

        loginButton = (Button) findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new DoLogin());

        sendPassword = (TextView) findViewById(R.id.link_signup);
    }

    private class DoLogin implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            boolean checkId = checkId();
            boolean checkPwd = checkPassword();
            if (checkId && checkPwd) {
                login();
            }
        }
    }

    private boolean checkId() {
        String s = userNameView.getText().toString();
        if (s == null) {
            userNameView.setError("账号不能为空");
            return false;
        }
        if (s.length() <= 4) {
            userNameView.setError("位数不正确");
            return false;
        }
        return true;
    }

    private boolean checkPassword() {
        String s = passwordView.getText().toString();
        if (s == null) {
            passwordView.setError("密码不能为空");
            return false;
        }
        return true;
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private void login() {
        loginButton.requestFocus();
        Executors.newFixedThreadPool(1).submit(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));

                /*OkHttpClient client = new OkHttpClient();
                JSONObject object = new JSONObject();
                try {
                    object.put("username", userNameView.getText().toString());
                    object.put("password", passwordView.getText().toString());
                    object.put("xValue", 1);
                    object.put("yValue", 1);
                    RequestBody params = RequestBody.create(JSON, object.toString());
                    Request request = new Request.Builder().url(MyInfo.url + "/user/login").post(params).build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()){
                        ResponseBody body = response.body();
                        if (body.string().equals("success")){
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }
        });
    }

    private String[] loadUser() {
        File filesDir = getFilesDir();
        File todoFile = new File(filesDir, "user.txt");
        ArrayList<String> items;
        try {
            items = new ArrayList<>(FileUtils.readLines(todoFile));
        } catch (IOException e) {
            items = new ArrayList<>();
        }
        if (items.size() != 0) {
            return new String[]{items.get(0), items.get(1)};
        }
        return null;
    }

    public void saveUser(String[] strings) {
        ArrayList items = new ArrayList();
        items.add(strings[0]);
        items.add(strings[1]);
        File filesDir = getFilesDir();
        File todoFile = new File(filesDir, "user.txt");
        try {
            FileUtils.writeLines(todoFile, items);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


