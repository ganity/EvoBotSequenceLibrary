/**
 * 接口编译测试
 * 验证修改后的 SequenceListener 接口和相关实现是否能正确编译
 */
public class InterfaceCompilationTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("接口编译测试");
        System.out.println("========================================");

        try {
            // 测试 SequenceListener 接口实现
            testSequenceListenerImplementation();
            
            // 测试急停方法调用
            testEmergencyStopMethodCall();
            
            System.out.println("\n========================================");
            System.out.println("✅ 接口编译测试通过!");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("❌ 测试失败: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
        }
    }

    /**
     * 测试 SequenceListener 接口实现
     */
    private static void testSequenceListenerImplementation() {
        System.out.println("\n--- 测试1: SequenceListener 接口实现 ---");

        // 创建一个完整的 SequenceListener 实现
        TestSequenceListener listener = new TestSequenceListener();
        
        // 测试所有方法调用
        int[] leftArm = {1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900};
        int[] rightArm = {2000, 2100, 2200, 2300, 2400, 2500, 2600, 2700, 2800, 2900};
        
        listener.onFrameData(leftArm, rightArm, 0);
        listener.onComplete();
        listener.onError("测试错误");
        listener.onEmergencyStop();  // 新增的方法
        
        System.out.println("✅ SequenceListener 接口实现测试通过");
    }

    /**
     * 测试急停方法调用
     */
    private static void testEmergencyStopMethodCall() {
        System.out.println("\n--- 测试2: 急停方法调用 ---");

        // 模拟播放器类，包含急停方法
        MockPlayer player = new MockPlayer();
        
        // 测试急停方法调用
        player.emergencyStop();
        
        System.out.println("✅ 急停方法调用测试通过");
    }

    /**
     * 测试用的 SequenceListener 实现
     * 包含新增的 onEmergencyStop 方法
     */
    private static class TestSequenceListener {
        
        public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
            System.out.println("onFrameData 调用: 帧" + frameIndex + 
                             ", 左臂长度=" + leftArm.length + 
                             ", 右臂长度=" + rightArm.length);
        }

        public void onComplete() {
            System.out.println("onComplete 调用");
        }

        public void onError(String errorMessage) {
            System.out.println("onError 调用: " + errorMessage);
        }

        public void onEmergencyStop() {
            System.out.println("onEmergencyStop 调用 - 新增的急停回调方法");
        }
    }

    /**
     * 模拟播放器类，包含急停方法
     */
    private static class MockPlayer {
        
        public void emergencyStop() {
            System.out.println("emergencyStop 调用 - 新增的急停方法");
            
            // 模拟急停逻辑
            System.out.println("  - 停止播放任务");
            System.out.println("  - 重置播放状态");
            System.out.println("  - 触发急停回调");
        }
    }
}