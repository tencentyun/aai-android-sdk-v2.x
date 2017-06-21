package com.tencent.aaifortest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 用户输入一些在腾讯云官网上注册时得到的信息，如appid、projectId、secretId和secretKey。
 *
 * 测试时用户可以直接在loggin页面上填写这些信息，或者通过配置文件读取，正式业务下请不要暴露secretKey
 *
 */
public class LoginActivity extends AppCompatActivity {

    EditText appid;
    EditText projectId;
    EditText secretKey;
    EditText secretId;

    Button start;
    Button readConfig;

    String appidString;
    String projectIdString;
    String secretKeyString;
    String secretIdString;

    int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private void checkPermissions() {

        List<String> permissions = new LinkedList<>();

        addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        addPermission(permissions, Manifest.permission.RECORD_AUDIO);
        addPermission(permissions, Manifest.permission.INTERNET);
        addPermission(permissions, Manifest.permission.READ_PHONE_STATE);

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

    }

    private void addPermission(List<String> permissionList, String permission) {

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(permission);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        checkPermissions();
        appid = (EditText) findViewById(R.id.appid);
        projectId = (EditText) findViewById(R.id.project_id);
        secretId = (EditText) findViewById(R.id.secret_id);
        secretKey = (EditText) findViewById(R.id.secret_key);

        start = (Button) findViewById(R.id.start);
        readConfig = (Button) findViewById(R.id.read_config);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appidString = appid.getText().toString();
                projectIdString = projectId.getText().toString();
                secretKeyString = secretKey.getText().toString();
                secretIdString = secretId.getText().toString();

                if (TextUtils.isEmpty(appidString) || TextUtils.isEmpty(secretIdString) || TextUtils.isEmpty(secretKeyString)) {
                    Toast.makeText(LoginActivity.this, getString(R.string.input_config_first), Toast.LENGTH_SHORT).show();
                } else {

                    try {
                        int appidInt = Integer.parseInt(appidString);
                        int projectIdInt = TextUtils.isEmpty(projectIdString) ? 0 : Integer.parseInt(projectIdString);
                        Bundle bundle = new Bundle();
                        bundle.putInt(CommonConst.appid, appidInt);
                        bundle.putInt(CommonConst.projectId, projectIdInt);
                        bundle.putString(CommonConst.secretId, secretIdString);
                        bundle.putString(CommonConst.secretKey, secretKeyString);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra(CommonConst.config, bundle);
                        startActivity(intent);
                    } catch (NumberFormatException e) {
                        Toast.makeText(LoginActivity.this, getString(R.string.invalid_para), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        readConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readConfig();
            }
        });
    }

    private void readConfig() {

        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = getAssets().open("config.json");
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder config = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                config.append(line);
            }
            JSONObject jsonObject = new JSONObject(config.toString());
            appid.setText(jsonObject.getString(CommonConst.appid));
            projectId.setText(jsonObject.getString(CommonConst.projectId));
            secretId.setText(jsonObject.getString(CommonConst.secretId));
            secretKey.setText(jsonObject.getString(CommonConst.secretKey));
            Toast.makeText(this, getString(R.string.read_config_success), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.read_config_failed), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (JSONException e) {
            Toast.makeText(this, getString(R.string.read_config_failed), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            try {

               if (bufferedReader != null) {
                   bufferedReader.close();
               }

               if (inputStream != null) {
                   inputStream.close();
               }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
