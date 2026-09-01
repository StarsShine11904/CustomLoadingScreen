package alexiil.mc.mod.load.render;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.load.CLSLog;
import alexiil.mc.mod.load.ClsManager;
import alexiil.mc.mod.load.CustomLoadingScreen;
import alexiil.mc.mod.load.progress.LongTermProgressTracker;
import alexiil.mc.mod.load.progress.SingleProgressBarTracker;
import alexiil.mc.mod.load.progress.SingleProgressBarTracker.LockUnlocker;

public class MainSplashRenderer {
    private static volatile boolean enableCustom = false;
    public static Identifier fontLoc;
    public static volatile boolean pause = false;

    private static long start = -1;
    private static long diff = 0;
    private static volatile boolean reachedConstruct = false;
    private static volatile boolean finishedLoading = false;

    public static long getTotalTime() {
        if (start == -1) {
            start = System.currentTimeMillis();
        }
        diff = System.currentTimeMillis() - start;
        return diff;
    }

    public static void onReachConstruct() {
        if (!reachedConstruct) {
            start = System.currentTimeMillis();
            try {
                enableCustom = ClsManager.load();
            } catch (Exception e) {
                CLSLog.warn("Failed to initialize ClsManager config", e);
            }
            reachedConstruct = true;
        }
    }

    public static void finish() {
        CustomLoadingScreen.finish();
        finishedLoading = true;
        LongTermProgressTracker.save(SingleProgressBarTracker.getProgressSections());
        if (enableCustom) {
            ClsManager.finish();
        }
    }

    public static void renderSplash() {
        if (finishedLoading) {
            return;
        }

        onReachConstruct();
        getTotalTime();

        // 設定 OpenGL 基礎狀態
        if (CustomLoadingScreen.darkMode) {
            GL11.glClearColor(0, 0, 0, 1);
        } else {
            GL11.glClearColor(1, 1, 1, 1);
        }
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        int w = Display.getWidth();
        int h = Display.getHeight();
        GL11.glViewport(0, 0, w, h);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(-w / 2.0, w / 2.0, h / 2.0, -h / 2.0, -1000, 1000);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        renderFrame();

        Display.update();
        if (CustomLoadingScreen.fpsLimit > 0) {
            Display.sync(CustomLoadingScreen.fpsLimit);
        }
    }

    private static void renderFrame() {
        if (enableCustom) {
            ClsManager.renderFrame();
        } else {
            // 基礎保底文字渲染（當自訂主題未載入時）
            String status;
            String subStatus;
            double progress;
            try (LockUnlocker u = SingleProgressBarTracker.lockUpdate()) {
                status = SingleProgressBarTracker.getStatusText();
                subStatus = SingleProgressBarTracker.getSubStatus();
                progress = SingleProgressBarTracker.getProgress() / SingleProgressBarTracker.MAX_PROGRESS_D;
            }

            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);
            // 進入自訂/預設畫面的繪製分支
            GL11.glPopMatrix();
        }
    }

    public static boolean isFinished() {
        return finishedLoading;
    }
}
