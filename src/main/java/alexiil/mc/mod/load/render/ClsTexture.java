package alexiil.mc.mod.load.render;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.KHRDebug;

import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class ClsTexture extends ResourceTexture {

    private BufferedImage image;
    private boolean blur;
    private boolean clamp;

    public ClsTexture(Identifier location) {
        super(location);
    }

    public Identifier location() {
        return location;
    }

    @Override
    public int getGlId() {
        if (glId == -1) {
            glId = GL11.glGenTextures();
        }
        return glId;
    }

    @Override
    public void clearGlId() {
        if (glId != -1) {
            GL11.glDeleteTextures(glId);
            glId = -1;
        }
    }

    public void loadImage(ResourceManager resourceManager) throws IOException {
        try (InputStream is = TextureLoader.openResourceStream(location())) {
            if (is == null) {
                throw new FileNotFoundException(location().toString());
            }

            image = TextureUtil.method_5866(is); // 或 TextureUtil.readBufferedImage(is)
            blur = false;
            clamp = false;
        }
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        clearGlId();

        if (image == null) {
            loadImage(resourceManager);
        }

        int id = getGlId();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, blur ? GL11.GL_LINEAR : GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, blur ? GL11.GL_LINEAR : GL11.GL_NEAREST);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, clamp ? GL11.GL_CLAMP : GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, clamp ? GL11.GL_CLAMP : GL11.GL_REPEAT);

        int width = image.getWidth();
        int height = image.getHeight();
        IntBuffer buffer = BufferUtils.createIntBuffer(width * height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer.put(image.getRGB(x, y));
            }
        }

        buffer.flip();

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
            buffer
        );

        if (GLContext.getCapabilities().GL_KHR_debug) {
            KHRDebug.glObjectLabel(GL11.GL_TEXTURE, id, "CLS_custom_tex_'" + location() + "'");
        }
    }
}
