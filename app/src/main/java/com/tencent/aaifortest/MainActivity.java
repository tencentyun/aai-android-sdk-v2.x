package com.tencent.aaifortest;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tencent.aai.AAIClient;
import com.tencent.aai.audio.data.AudioRecordDataSource;
import com.tencent.aai.auth.AbsCredentialProvider;
import com.tencent.aai.auth.LocalCredentialProvider;
import com.tencent.aai.config.ClientConfiguration;
import com.tencent.aai.exception.ClientException;
import com.tencent.aai.exception.ServerException;
import com.tencent.aai.listener.AudioRecognizeResultListener;
import com.tencent.aai.listener.AudioRecognizeStateListener;
import com.tencent.aai.listener.AudioRecognizeTimeoutListener;
import com.tencent.aai.log.AAILogger;
import com.tencent.aai.model.AudioRecognizeRequest;
import com.tencent.aai.model.AudioRecognizeResult;
import com.tencent.aai.model.type.AudioRecognizeConfiguration;
import com.tencent.aai.model.type.AudioRecognizeTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button start;
    Button stop;
    Button cancel;
    Button test;

    TextView recognizeState;
    TextView volume;

    EditText recognizeResult;

    int currentRequestId = 0;

    Handler handler;

    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    AAIClient aaiClient;

    AbsCredentialProvider credentialProvider;

    private final String PERFORMANCE_TAG = "PerformanceTag";


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

    LinkedHashMap<String, String> resMap = new LinkedHashMap<>();
    private String buildMessage(Map<String, String> msg) {

        StringBuffer stringBuffer = new StringBuffer();
        Iterator<Map.Entry<String, String>> iter = msg.entrySet().iterator();
        while (iter.hasNext()) {
            String value = iter.next().getValue();
            stringBuffer.append(value+"\r\n");
        }
        return stringBuffer.toString();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra(CommonConst.config);

        // 初始化相应的控件
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        cancel = (Button)findViewById(R.id.cancel);
        test = (Button) findViewById(R.id.test);
        recognizeState = (TextView) findViewById(R.id.recognize_state);
        volume = (TextView) findViewById(R.id.volume);
        recognizeResult = (EditText) findViewById(R.id.recognize_result);
        handler = new Handler(getMainLooper());

        // 检查sdk运行的必要条件权限
        checkPermissions();

        // 用户配置：需要在控制台申请相关的账号;
        final int appid = bundle.getInt(CommonConst.appid);
        final int projectId = bundle.getInt(CommonConst.projectId);
        final String secretId = bundle.getString(CommonConst.secretId);
        final String secretKey = bundle.getString(CommonConst.secretKey);

        AAILogger.info(logger, "config : appid={}, projectId={}, secretId={}, secretKey={}", appid, projectId, secretId, secretKey);

        // 签名鉴权类，sdk中给出了一个本地的鉴权类，但由于需要用户提供secretKey，这可能会导致一些安全上的问题，
        // 因此，请用户自行实现CredentialProvider接口
        credentialProvider = new LocalCredentialProvider(secretKey);

        // 用户配置
        ClientConfiguration.setServerProtocolHttps(false); // 是否启用https，默认启用
        ClientConfiguration.setMaxAudioRecognizeConcurrentNumber(2); // 语音识别的请求的最大并发数
        ClientConfiguration.setMaxRecognizeSliceConcurrentNumber(10); // 单个请求的分片最大并发数

        // 识别结果回调监听器
        final AudioRecognizeResultListener audioRecognizeResultlistener = new AudioRecognizeResultListener() {

            boolean dontHaveResult = true;

            /**
             * 返回分片的识别结果
             * @param request 相应的请求
             * @param result 识别结果
             * @param seq 该分片所在语音流的序号 (0, 1, 2...)
             */
            @Override
            public void onSliceSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {

                if (dontHaveResult && !TextUtils.isEmpty(result.getText())) {
                    dontHaveResult = false;
                    Date date=new Date();
                    DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                    String time=format.format(date);
                    String message = String.format("voice flow order = %d, receive first response in %s, result is = %s", seq, time, result.getText());
                    Log.i(PERFORMANCE_TAG, message);
                }

                AAILogger.info(logger, "on slice success..");
                AAILogger.info(logger, "on slice success..");
                AAILogger.info(logger, "seq = {}, voiceid = {}, result = {}", seq, result.getVoiceId(), result.getText());
                resMap.put(String.valueOf(seq), result.getText());
                final String msg = buildMessage(resMap);
                AAILogger.info(logger, "msg="+msg);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeResult.setText(msg);
                    }
                });

            }

            /**
             * 返回语音流的识别结果
             * @param request 相应的请求
             * @param result 识别结果
             * @param seq 该语音流的序号 (1, 2, 3...)
             */
            @Override
            public void onSegmentSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
                dontHaveResult = true;
                AAILogger.info(logger, "on segment success");
                AAILogger.info(logger, "seq = {}, voiceid = {}, result = {}", seq, result.getVoiceId(), result.getText());
                resMap.put(String.valueOf(seq), result.getText());
                final String msg = buildMessage(resMap);
                AAILogger.info(logger, "msg="+msg);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeResult.setText(msg);
                    }
                });
            }

            /**
             * 识别结束回调，返回所有的识别结果
             * @param request 相应的请求
             * @param result 识别结果
             */
            @Override
            public void onSuccess(AudioRecognizeRequest request, String result) {
                AAILogger.info(logger, "onSuccess..");
                AAILogger.info(logger, "result = {}", result);
            }

            /**
             * 识别失败
             * @param request 相应的请求
             * @param clientException 客户端异常
             * @param serverException 服务端异常
             */
            @Override
            public void onFailure(AudioRecognizeRequest request, final ClientException clientException, final ServerException serverException) {
                if (clientException!=null) {
                    AAILogger.info(logger, "onFailure..:"+clientException.toString());
                }
                if (serverException!=null) {
                    AAILogger.info(logger, "onFailure..:"+serverException.toString());
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (clientException!=null) {
                            recognizeState.setText("识别状态：失败,  "+clientException.toString());
                        } else if (serverException!=null) {
                            recognizeState.setText("识别状态：失败,  "+serverException.toString());
                        }
                    }
                });
            }
        };


        /**
         * 识别状态监听器
         */
        final AudioRecognizeStateListener audioRecognizeStateListener = new AudioRecognizeStateListener() {

            /**
             * 开始录音
             * @param request
             */
            @Override
            public void onStartRecord(AudioRecognizeRequest request) {
                currentRequestId = request.getRequestId();
                AAILogger.info(logger, "onStartRecord..");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.start_record));
                        recognizeResult.setText("");
                    }
                });
            }

            /**
             * 结束录音
             * @param request
             */
            @Override
            public void onStopRecord(AudioRecognizeRequest request) {
                AAILogger.info(logger, "onStopRecord..");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.end_record));
                        // start.setEnabled(true);
                    }
                });
            }

            /**
             * 第seq个语音流开始识别
             * @param request
             * @param seq
             */
            @Override
            public void onVoiceFlowStartRecognize(AudioRecognizeRequest request, int seq) {




                AAILogger.info(logger, "onVoiceFlowStartRecognize.. seq = {}", seq);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.start_recognize));
                    }
                });
            }

            /**
             * 第seq个语音流结束识别
             * @param request
             * @param seq
             */
            @Override
            public void onVoiceFlowFinishRecognize(AudioRecognizeRequest request, int seq) {

                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                String time=format.format(date);
                String message = String.format("voice flow order = %d, recognize finish in %s", seq, time);
                Log.i(PERFORMANCE_TAG, message);

                AAILogger.info(logger, "onVoiceFlowFinishRecognize.. seq = {}", seq);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.end_recognize));
                    }
                });
            }

            /**
             * 第seq个语音流开始
             * @param request
             * @param seq
             */
            @Override
            public void onVoiceFlowStart(AudioRecognizeRequest request, int seq) {

                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                String time=format.format(date);
                String message = String.format("voice flow order = %d, start in %s", seq, time);
                Log.i(PERFORMANCE_TAG, message);

                AAILogger.info(logger, "onVoiceFlowStart.. seq = {}", seq);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.start_voice));
                    }
                });
            }

            /**
             * 第seq个语音流结束
             * @param request
             * @param seq
             */
            @Override
            public void onVoiceFlowFinish(AudioRecognizeRequest request, int seq) {

                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                String time=format.format(date);
                String message = String.format("voice flow order = %d, stop in %s", seq, time);
                Log.i(PERFORMANCE_TAG, message);

                AAILogger.info(logger, "onVoiceFlowFinish.. seq = {}", seq);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.end_voice));
                    }
                });
            }

            /**
             * 语音音量回调
             * @param request
             * @param volume
             */
            @Override
            public void onVoiceVolume(AudioRecognizeRequest request, final int volume) {
                AAILogger.info(logger, "onVoiceVolume..");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.volume.setText(getString(R.string.volume)+volume);
                    }
                });
            }

        };

        /**
         * 识别超时监听器
         */
        final AudioRecognizeTimeoutListener audioRecognizeTimeoutListener = new AudioRecognizeTimeoutListener() {

            /**
             * 检测第一个语音流超时
             * @param request
             */
            @Override
            public void onFirstVoiceFlowTimeout(AudioRecognizeRequest request) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.start_voice_timeout));
                    }
                });
            }

            /**
             * 检测下一个语音流超时
             * @param request
             */
            @Override
            public void onNextVoiceFlowTimeout(AudioRecognizeRequest request) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.end_voice_timeout));
                    }
                });
            }
        };


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AAILogger.info(logger, "the start button has clicked..");
                resMap.clear();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //   start.setEnabled(false);
                    }
                });
                AudioRecognizeRequest.Builder builder = new AudioRecognizeRequest.Builder();
                //File file = new File(Environment.getExternalStorageDirectory()+"/tencent_aai____/audio", "1.pcm");

                // 初始化识别请求
                final AudioRecognizeRequest audioRecognizeRequest = builder
                        .pcmAudioDataSource(new AudioRecordDataSource()) // 设置数据源
                        //.templateName(templateName) // 设置模板
                        .template(new AudioRecognizeTemplate(1,0,0)) // 设置自定义模板
                        .build();

                // 自定义识别配置
                final AudioRecognizeConfiguration audioRecognizeConfiguration = new AudioRecognizeConfiguration.Builder()
                        .enableAudioStartTimeout(false) // 是否使能起点超时停止录音
                        .enableAudioEndTimeout(false) // 是否使能终点超时停止录音
                        .enableSilentDetect(true) // 是否使能静音检测，true表示不检查静音部分
                        .minAudioFlowSilenceTime(1000) // 语音流识别时的间隔时间
                        .maxAudioFlowSilenceTime(10000) // 语音终点超时时间
                        .maxAudioStartSilenceTime(10000) // 语音起点超时时间
                        .minVolumeCallbackTime(80) // 音量回调时间
                        .sensitive(2)
                        .build();

                if (aaiClient==null) {
                    try {
                        aaiClient = new AAIClient(MainActivity.this, appid, projectId, secretId, credentialProvider);
                    } catch (ClientException e) {
                        e.printStackTrace();
                        AAILogger.info(logger, e.toString());
                    }
                }
                //currentRequestId = audioRecognizeRequest.getRequestId();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        aaiClient.startAudioRecognize(audioRecognizeRequest, audioRecognizeResultlistener,
                                audioRecognizeStateListener, audioRecognizeTimeoutListener,
                                audioRecognizeConfiguration);

                    }
                }).start();

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AAILogger.info(logger, "stop button is clicked..");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean taskExist = false;
                        if (aaiClient!=null) {
                            taskExist = aaiClient.stopAudioRecognize(currentRequestId);
                        }
                        if (!taskExist) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    recognizeState.setText(getString(R.string.cant_stop));
                                }
                            });
                        }
                    }
                }).start();

            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AAILogger.info(logger, "cancel button is clicked..");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean taskExist = false;
                        if (aaiClient!=null) {
                            taskExist = aaiClient.cancelAudioRecognize(currentRequestId);
                        }
                        if (!taskExist) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    recognizeState.setText(getString(R.string.cant_cancel));
                                }
                            });
                        }
                    }
                }).start();

            }
        });

        View view = getLayoutInflater().inflate(R.layout.dialog_config, null);
        ((TextView)view.findViewById(R.id.config_appid)).setText(getString(R.string.appid)+appid);
        ((TextView)view.findViewById(R.id.config_project_id)).setText(getString(R.string.project_id)+projectId);
        ((TextView)view.findViewById(R.id.config_secret_id)).setText(getString(R.string.secret_id)+secretId);
        ((TextView)view.findViewById(R.id.config_secret_key)).setText(getString(R.string.secret_key)+secretKey);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.confirm_config));
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aaiClient != null) {
            aaiClient.release();
        }
    }


}
