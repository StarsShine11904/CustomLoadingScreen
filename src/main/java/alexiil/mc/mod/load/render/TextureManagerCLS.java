package alexiil.mc.mod.load.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureTickListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.load.CLSLog;
import alexiil.mc.mod.load.CustomLoadingScreen;

public class TextureManagerCLS extends TextureManager {

    /** Map of texture to last access tick. */
    private final Map<Identifier, Long> textures = new HashMap<>();
    private Long currentTime = System.currentTimeMillis();

    public TextureManagerCLS(ResourceManager resourceManager) {
        super(resourceManager);
    }

    private void onTextureAccess(Identifier resource) {
        textures.put(resource, currentTime);
    }

    @Override
    public void bindTexture(Identifier resource) {
        super.bindTexture(resource);
        onTextureAccess(resource);
    }

    @Override
    public AbstractTexture getTexture(Identifier resource) {
        AbstractTexture obj = super.getTexture(resource);
        if (obj != null) {
            onTextureAccess(resource);
        }
        return obj;
    }

    @Override
    public void destroyTexture(Identifier textureLocation) {
        super.destroyTexture(textureLocation);
        textures.remove(textureLocation);
    }

    @Override
    public boolean registerTexture(Identifier textureLocation, AbstractTexture textureObj) {
        onTextureAccess(textureLocation);
        return super.registerTexture(textureLocation, textureObj);
    }

    public void deleteAll() {
        for (Identifier location : textures.keySet().toArray(new Identifier[0])) {
            destroyTexture(location);
        }
    }

    public void onFrame() {
        if (CustomLoadingScreen.textureClearInterval == 0) {
            return;
        }

        Long last = currentTime;
        long next = System.currentTimeMillis();

        if (last + 1000 > next) {
            return;
        }

        currentTime = next;

        long minTime = currentTime - (CustomLoadingScreen.textureClearInterval * 1000L);

        List<Identifier> toRemove = new ArrayList<>();

        for (Map.Entry<Identifier, Long> entry : textures.entrySet()) {
            if (entry.getValue() < minTime) {
                toRemove.add(entry.getKey());
            }
        }

        for (Identifier tex : toRemove) {
            if (CustomLoadingScreen.debugResourceLoading) {
                CLSLog.info("[debug] Automatically deleting texture " + tex);
            }
            destroyTexture(tex);
        }
    }
}
