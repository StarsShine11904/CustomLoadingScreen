package alexiil.mc.mod.load.render;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.resource.Resource;
import net.minecraft.client.resource.ResourceImpl;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.load.CLSLog;
import alexiil.mc.mod.load.json.ConfigManager;

public class FontRendererSeparate extends TextRenderer {

    private static final BufferedImage EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);

    private final Map<Identifier, BufferedImage> textureData = new HashMap<>();
    private final Map<Identifier, Integer> textureLocations = new HashMap<>();

    private boolean __cls__replaced__underline;
    private boolean __cls__replaced__strikethrough;

    public FontRendererSeparate(
        GameOptions settings, Identifier location, TextureManager textureManagerIn, boolean unicode
    ) {
        super(settings, location, textureManagerIn, unicode);

        loadTex(location);
        for (int i = 0; i < 256; i++) {
            if (i == 8) continue;
            if (0xd8 <= i && i <= 0xf8) continue;
            loadTex(new Identifier(String.format("textures/font/unicode_page_%02x.png", i)));
        }
    }

    private BufferedImage loadTex(Identifier location) {
        try (InputStream stream = ConfigManager.getInputStream(location)) {
            BufferedImage img = TextureUtil.method_5866(stream);
            if (img == null) {
                CLSLog.warn("Failed to read a texture from " + location + " - " + stream);
                return EMPTY_IMAGE;
            }
            textureData.put(location, img);
            return img;
        } catch (FileNotFoundException e) {
            CLSLog.warn("loadTex(" + location + ") : " + e);
            return EMPTY_IMAGE;
        } catch (IOException e) {
            CLSLog.warn("loadTex(" + location + ") : " + e);
            return EMPTY_IMAGE;
        }
    }

    @Override
    protected void bindTexture(Identifier location) {
        if (textureLocations == null) {
            return;
        }
        Integer value = textureLocations.get(location);
        if (value == null) {
            BufferedImage img = textureData.computeIfAbsent(location, this::loadTex);
            if (img == null || img == EMPTY_IMAGE) {
                CLSLog.warn("Non-cached texture: '" + location + "'");
                return;
            }
            int next = GL11.glGenTextures();
            TextureUtil.method_5858(next, img); // 或 TextureUtil.uploadTextureImage
            textureLocations.put(location, next);
            value = next;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, value.intValue());
    }

    @Override
    protected Resource getResource(Identifier location) throws IOException {
        if ("config".equals(location.getNamespace())) {
            InputStream stream = ConfigManager.getInputStream(location);
            InputStream metaStream = null;
            try {
                metaStream = ConfigManager.getInputStream(
                    new Identifier(location.getNamespace(), location.getPath() + ".mcmeta")
                );
            } catch (IOException ignored) {}

            MinecraftClient client = MinecraftClient.getInstance();
            return new ResourceImpl(
                "cls config", location, stream, metaStream,
                client != null ? client.getResourcePackRepository().field_5393 : null
            );
        }
        return super.getResource(location);
    }

    public void destroy() {
        for (Integer value : textureLocations.values()) {
            GL11.glDeleteTextures(value.intValue());
        }
        textureLocations.clear();
    }

    @Override
    protected void method_956(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    @Override
    protected void method_961(float width) {
        if (__cls__replaced__strikethrough || __cls__replaced__underline) {
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            bb.begin(7, VertexFormats.POSITION);
            if (__cls__replaced__strikethrough) {
                int halfHeight = fontHeight / 2;
                bb.vertex(x, y + halfHeight, 0).next();
                bb.vertex(x + width, y + halfHeight, 0).next();
                bb.vertex(x + width, y + halfHeight - 1, 0).next();
                bb.vertex(x, y + halfHeight - 1, 0).next();
            }
            if (__cls__replaced__underline) {
                bb.vertex(x - 1, y + fontHeight, 0).next();
                bb.vertex(x + width, y + fontHeight, 0).next();
                bb.vertex(x + width, y + fontHeight - 1, 0).next();
                bb.vertex(x - 1, y + fontHeight - 1, 0).next();
            }
            tess.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        x += width;
    }
}
