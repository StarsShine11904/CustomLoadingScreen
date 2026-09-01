package alexiil.mc.mod.load.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alexiil.mc.mod.load.render.MainSplashRenderer;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    public abstract TextureManager getTextureManager();

    /**
     * 在客戶端初始化早期觸發自訂載入畫面的準備工作
     */
    @Inject(method = "init", at = @At("HEAD"))
    private void onInitHead(CallbackInfo ci) {
        MainSplashRenderer.onReachConstruct();
    }

    /**
     * 攔截原版的啟動畫面繪製方法，替換為自訂載入畫面渲染
     */
    @Inject(method = "drawSplashScreen", at = @At("HEAD"), cancellable = true)
    private void onDrawSplashScreen(TextureManager textureManager, CallbackInfo ci) {
        if (!MainSplashRenderer.isFinished()) {
            MainSplashRenderer.renderSplash();
            ci.cancel();
        }
    }

    /**
     * 遊戲載入完成進入標題畫面時，結束並釋放載入畫面資源
     */
    @Inject(method = "init", at = @At("RETURN"))
    private void onInitReturn(CallbackInfo ci) {
        MainSplashRenderer.finish();
    }
}
