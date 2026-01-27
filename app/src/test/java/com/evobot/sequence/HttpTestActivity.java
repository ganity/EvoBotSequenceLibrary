package com.evobot.sequence;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;

/**
 * HTTP测试Activity
 * 用于在Android应用中运行HTTP测试
 */
public class HttpTestActivity extends Activity {
    
    private static final String TAG = "HttpTestActivity";
    
    private TextView logTextView;
    private ScrollView scrollView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建UI
        createUI();
        
        Log.d(TAG, "HTTP测试Activity已启动");
        appendLog("HTTP测试Activity已启动");
    }
    
    /**
     * 创建简单的UI
     */
    private void createUI() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        // 标题
        TextView titleView = new TextView(this);
        titleView.setText("EvoBot HTTP测试");
        titleView.setTextSize(20);
        titleView.setPadding(0, 0, 0, 20);
        layout.addView(titleView);
        
        // 快速连接测试按钮
        Button quickTestButton = new Button(this);
        quickTestButton.setText("快速连接测试");
        quickTestButton.setOnClickListener(v -> runQuickConnectionTest());
        layout.addView(quickTestButton);
        
        // 映射测试按钮
        Button mappingTestButton = new Button(this);
        mappingTestButton.setText("映射建立测试");
        mappingTestButton.setOnClickListener(v -> runMappingTest());
        layout.addView(mappingTestButton);
        
        // 下载测试按钮
        Button downloadTestButton = new Button(this);
        downloadTestButton.setText("动作下载测试");
        downloadTestButton.setOnClickListener(v -> runDownloadTest());
        layout.addView(downloadTestButton);
        
        // 完整HTTP测试按钮
        Button fullTestButton = new Button(this);
        fullTestButton.setText("完整HTTP测试");
        fullTestButton.setOnClickListener(v -> runFullHttpTest());
        layout.addView(fullTestButton);
        
        // 清除日志按钮
        Button clearLogButton = new Button(this);
        clearLogButton.setText("清除日志");
        clearLogButton.setOnClickListener(v -> clearLog());
        layout.addView(clearLogButton);
        
        // 日志显示区域
        logTextView = new TextView(this);
        logTextView.setTextSize(12);
        logTextView.setPadding(10, 10, 10, 10);
        logTextView.setBackgroundColor(0xFF000000);
        logTextView.setTextColor(0xFF00FF00);
        
        scrollView = new ScrollView(this);
        scrollView.addView(logTextView);
        
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0);
        scrollParams.weight = 1;
        scrollView.setLayoutParams(scrollParams);
        
        layout.addView(scrollView);
        
        setContentView(layout);
    }
    
    /**
     * 运行快速连接测试
     */
    private void runQuickConnectionTest() {
        appendLog("\n=== 开始快速连接测试 ===");
        
        new Thread(() -> {
            try {
                boolean result = HttpTestRunner.quickConnectionTest(this);
                runOnUiThread(() -> {
                    appendLog("快速连接测试结果: " + (result ? "✅ 成功" : "❌ 失败"));
                    appendLog("=== 快速连接测试完成 ===\n");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendLog("快速连接测试异常: " + e.getMessage());
                    Log.e(TAG, "快速连接测试异常", e);
                });
            }
        }).start();
    }
    
    /**
     * 运行映射测试
     */
    private void runMappingTest() {
        appendLog("\n=== 开始映射建立测试 ===");
        
        new Thread(() -> {
            try {
                int mappingCount = HttpTestRunner.testMappingCreation(this);
                runOnUiThread(() -> {
                    if (mappingCount > 0) {
                        appendLog("映射建立测试结果: ✅ 成功");
                        appendLog("建立了 " + mappingCount + " 个映射关系");
                        
                        // 显示一些映射示例
                        showMappingExamples();
                    } else {
                        appendLog("映射建立测试结果: ❌ 失败");
                    }
                    appendLog("=== 映射建立测试完成 ===\n");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendLog("映射建立测试异常: " + e.getMessage());
                    Log.e(TAG, "映射建立测试异常", e);
                });
            }
        }).start();
    }
    
    /**
     * 显示映射示例
     */
    private void showMappingExamples() {
        // 显示一些映射转换示例
        String[] testNames = {"左臂挥手", "右臂挥手", "点头确认", "摇头拒绝"};
        
        appendLog("映射转换示例:");
        for (String chineseName : testNames) {
            String englishName = ActionNameUtils.chineseToEnglish(chineseName);
            if (!chineseName.equals(englishName)) {
                appendLog("  " + chineseName + " -> " + englishName);
            }
        }
    }
    
    /**
     * 运行下载测试
     */
    private void runDownloadTest() {
        appendLog("\n=== 开始动作下载测试 ===");
        
        new Thread(() -> {
            try {
                boolean result = HttpTestRunner.testSingleActionDownload(this);
                runOnUiThread(() -> {
                    appendLog("动作下载测试结果: " + (result ? "✅ 成功" : "❌ 失败"));
                    appendLog("=== 动作下载测试完成 ===\n");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendLog("动作下载测试异常: " + e.getMessage());
                    Log.e(TAG, "动作下载测试异常", e);
                });
            }
        }).start();
    }
    
    /**
     * 运行完整HTTP测试
     */
    private void runFullHttpTest() {
        appendLog("\n=== 开始完整HTTP测试 ===");
        appendLog("注意: 完整测试可能需要较长时间，请耐心等待...");
        
        new Thread(() -> {
            try {
                // 运行完整的HTTP测试
                RealHttpTest httpTest = new RealHttpTest(this);
                httpTest.runAllTests();
                
                runOnUiThread(() -> {
                    appendLog("=== 完整HTTP测试完成 ===\n");
                    appendLog("详细结果请查看Logcat日志");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendLog("完整HTTP测试异常: " + e.getMessage());
                    Log.e(TAG, "完整HTTP测试异常", e);
                });
            }
        }).start();
    }
    
    /**
     * 添加日志
     */
    private void appendLog(String message) {
        String currentText = logTextView.getText().toString();
        String newText = currentText + message + "\n";
        logTextView.setText(newText);
        
        // 滚动到底部
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        
        // 同时输出到Logcat
        Log.d(TAG, message);
    }
    
    /**
     * 清除日志
     */
    private void clearLog() {
        logTextView.setText("");
        appendLog("日志已清除");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "HTTP测试Activity已销毁");
    }
}