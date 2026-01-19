import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 急停功能测试
 * 模拟测试急停功能是否正常工作
 */
public class EmergencyStopTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("急停功能测试");
        System.out.println("========================================");

        try {
            testEmergencyStopCallback();
            testEmergencyStopTiming();
            
            System.out.println("\n========================================");
            System.out.println("✅ 所有急停测试通过!");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("❌ 测试失败: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
        }
    }

    /**
     * 测试急停回调功能
     */
    private static void testEmergencyStopCallback() throws Exception {
        System.out.println("\n--- 测试1: 急停回调功能 ---");

        final boolean[] emergencyStopCalled = {false};
        final boolean[] frameDataStopped = {false};
        final CountDownLatch latch = new CountDownLatch(1);

        // 模拟 SequenceListener
        MockSequenceListener listener = new MockSequenceListener() {
            private int frameCount = 0;

            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                frameCount++;
                System.out.println("播放帧 " + frameIndex + " (总计: " + frameCount + ")");
                
                // 模拟播放几帧后检查是否还在接收数据
                if (frameCount > 10 && emergencyStopCalled[0]) {
                    frameDataStopped[0] = false; // 如果急停后还收到数据，说明没有正确停止
                    System.err.println("❌ 急停后仍在接收帧数据!");
                }
            }

            @Override
            public void onEmergencyStop() {
                System.out.println("🚨 收到急停回调!");
                emergencyStopCalled[0] = true;
                frameDataStopped[0] = true;
                latch.countDown();
            }
        };

        // 模拟播放器
        MockSequencePlayer player = new MockSequencePlayer();
        
        // 开始模拟播放
        System.out.println("开始模拟播放...");
        player.startMockPlayback(listener);

        // 等待几帧后触发急停
        Thread.sleep(500);
        System.out.println("触发急停...");
        player.emergencyStop();

        // 等待急停回调
        boolean callbackReceived = latch.await(2, TimeUnit.SECONDS);
        
        if (!callbackReceived) {
            throw new RuntimeException("急停回调未在预期时间内收到");
        }
        
        if (!emergencyStopCalled[0]) {
            throw new RuntimeException("急停回调未被调用");
        }
        
        if (!frameDataStopped[0]) {
            throw new RuntimeException("急停后仍在接收帧数据");
        }

        System.out.println("✅ 急停回调功能测试通过");
    }

    /**
     * 测试急停时机
     */
    private static void testEmergencyStopTiming() throws Exception {
        System.out.println("\n--- 测试2: 急停时机测试 ---");

        final long[] emergencyStopTime = {0};
        final long[] lastFrameTime = {0};
        final CountDownLatch latch = new CountDownLatch(1);

        MockSequenceListener listener = new MockSequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                lastFrameTime[0] = System.currentTimeMillis();
                if (frameIndex % 5 == 0) {
                    System.out.println("播放帧 " + frameIndex);
                }
            }

            @Override
            public void onEmergencyStop() {
                emergencyStopTime[0] = System.currentTimeMillis();
                System.out.println("🚨 急停时间: " + emergencyStopTime[0]);
                latch.countDown();
            }
        };

        MockSequencePlayer player = new MockSequencePlayer();
        
        System.out.println("开始播放...");
        player.startMockPlayback(listener);

        // 等待一段时间后触发急停
        Thread.sleep(300);
        long triggerTime = System.currentTimeMillis();
        System.out.println("触发急停时间: " + triggerTime);
        player.emergencyStop();

        // 等待急停回调
        boolean callbackReceived = latch.await(1, TimeUnit.SECONDS);
        
        if (!callbackReceived) {
            throw new RuntimeException("急停回调超时");
        }

        // 验证急停响应时间
        long responseTime = emergencyStopTime[0] - triggerTime;
        System.out.println("急停响应时间: " + responseTime + "ms");
        
        if (responseTime > 100) {
            throw new RuntimeException("急停响应时间过长: " + responseTime + "ms");
        }

        System.out.println("✅ 急停时机测试通过");
    }

    /**
     * 模拟 SequenceListener 接口
     */
    private static abstract class MockSequenceListener {
        public abstract void onFrameData(int[] leftArm, int[] rightArm, int frameIndex);
        public void onComplete() {}
        public void onError(String errorMessage) {}
        public abstract void onEmergencyStop();
    }

    /**
     * 模拟序列播放器
     */
    private static class MockSequencePlayer {
        private volatile boolean playing = false;
        private volatile boolean emergencyStopped = false;
        private MockSequenceListener listener;
        private Thread playbackThread;

        public void startMockPlayback(MockSequenceListener listener) {
            this.listener = listener;
            this.playing = true;
            this.emergencyStopped = false;

            playbackThread = new Thread(() -> {
                int frame = 0;
                while (playing && !emergencyStopped) {
                    try {
                        // 模拟帧数据
                        int[] leftArm = new int[]{1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900};
                        int[] rightArm = new int[]{2000, 2100, 2200, 2300, 2400, 2500, 2600, 2700, 2800, 2900};
                        
                        listener.onFrameData(leftArm, rightArm, frame++);
                        
                        // 模拟40Hz播放频率 (25ms间隔)
                        Thread.sleep(25);
                        
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            
            playbackThread.start();
        }

        public void emergencyStop() {
            System.out.println("执行急停操作...");
            
            // 立即停止播放
            this.playing = false;
            this.emergencyStopped = true;
            
            // 中断播放线程
            if (playbackThread != null) {
                playbackThread.interrupt();
            }
            
            // 立即回调急停
            if (listener != null) {
                listener.onEmergencyStop();
            }
        }
    }
}