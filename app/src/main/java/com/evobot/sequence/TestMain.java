package com.evobot.sequence;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * 测试入口Activity
 * 在Android应用中运行此Activity来测试序列播放器
 */
public class TestMain extends Activity {

    private static final String TAG = "TestMain";

    private TextView textView;
    private SequencePlayerTest tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建简单的UI显示测试结果
        textView = new TextView(this);
        textView.setText("EvoBot序列播放器测试\n\n正在启动测试...");
        textView.setTextSize(14);
        textView.setPadding(32, 32, 32, 32);
        setContentView(textView);

        // 延迟启动测试，确保UI完全加载
        textView.postDelayed(new Runnable() {
            @Override
            public void run() {
                runTests();
            }
        }, 1000);
    }

    /**
     * 运行所有测试
     */
    private void runTests() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "开始运行EvoBot序列播放器测试");
        Log.d(TAG, "========================================");

        updateUI("正在运行测试...\n\n请查看Logcat日志获取详细信息\n\n");

        tester = new SequencePlayerTest();
        tester.runAllTests(this);

        // 显示测试结果
        String result = tester.isTestPassed() ? "✅ 通过" : "❌ 失败";
        String log = tester.getTestLog();

        updateUI("测试完成!\n\n结果: " + result + "\n\n" + log);

        Log.d(TAG, "========================================");
        Log.d(TAG, "测试运行完成");
        Log.d(TAG, "========================================");
    }

    /**
     * 更新UI显示
     */
    private void updateUI(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tester != null) {
            tester.release();
        }
    }
}
