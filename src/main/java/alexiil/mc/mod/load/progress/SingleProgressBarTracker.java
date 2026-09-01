package alexiil.mc.mod.load.progress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import alexiil.mc.mod.load.CustomLoadingScreen;
import alexiil.mc.mod.load.Translation;
import alexiil.mc.mod.load.render.MainSplashRenderer;

public class SingleProgressBarTracker {
    public static final int MAX_PROGRESS = 1 << 20; // about 1,000,000
    public static final double MAX_PROGRESS_D = MAX_PROGRESS;

    private static String status = "Minecraft Initializing";
    private static String subStatus = "Legacy Fabric";
    private static int progress = 0;
    private static boolean isInReload = false;

    private static final boolean needsLock = CustomLoadingScreen.useFrame;
    public static final Lock updateLock = new ReentrantLock(true);
    private static final LockUnlocker lockUnlocker = () -> updateLock.unlock();
    private static final LockUnlocker no_opUnlocker = () -> {};

    private static final List<ProgressSectionInfo> progressSections = new ArrayList<>();
    private static ProgressSectionInfo currentInfo;

    public static LockUnlocker lockUpdate() {
        if (needsLock) {
            updateLock.lock();
            update();
            return lockUnlocker;
        }
        update();
        return no_opUnlocker;
    }

    @FunctionalInterface
    public interface LockUnlocker extends AutoCloseable {
        @Override
        void close();
    }

    private static void update() {
        // 基本平滑時間進度（供未收到具體事件時的保底更新）
        if (progress < MAX_PROGRESS) {
            long time = MainSplashRenderer.getTotalTime();
            // 在啟動階段根據時間平滑推進至約 90%
            int targetProgress = (int) Math.min(MAX_PROGRESS * 0.9, (time / 3000.0) * (MAX_PROGRESS * 0.9));
            if (targetProgress > progress) {
                progress = targetProgress;
            }
        }
    }

    public static void setStatus(String newStatus, String newSubStatus, double percentage) {
        status = newStatus != null ? newStatus : "";
        subStatus = newSubStatus != null ? newSubStatus : "";
        progress = (int) (Math.max(0.0, Math.min(1.0, percentage)) * MAX_PROGRESS);
        if (progress >= MAX_PROGRESS) {
            status = Translation.translate("customloadingscreen.finishing");
        }
    }

    public static void setInReload(boolean reloading) {
        isInReload = reloading;
    }

    public static List<ProgressSectionInfo> getProgressSections() {
        if (currentInfo != null) {
            currentInfo.time = MainSplashRenderer.getTotalTime() - currentInfo.time;
            progressSections.add(currentInfo);
            currentInfo = null;
        }
        return progressSections;
    }

    public static String getStatusText() {
        return status;
    }

    public static String getSubStatus() {
        return subStatus;
    }

    public static int getProgress() {
        return progress;
    }

    public static boolean isInReload() {
        return isInReload;
    }
}
