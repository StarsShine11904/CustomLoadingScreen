package alexiil.mc.mod.load.baked.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.load.render.MinecraftDisplayerRenderer;
import alexiil.mc.mod.load.render.TextureLoader;
import alexiil.mc.mod.load.render.TextureLoader.PreScannedImageData;

import buildcraft.lib.expression.node.value.NodeVariableDouble;

public class BakedImageRender extends BakedRenderPositioned {

    private static final int TESS_INT_COUNT = 64;

    private final Tessellator tess = new Tessellator(TESS_INT_COUNT);

    protected final Identifier res;
    private final BakedArea pos, tex;

    PreScannedImageData preScanned = null;

    public BakedImageRender(
        NodeVariableDouble varWidth, NodeVariableDouble varHeight, String res, BakedArea pos, BakedArea tex
    ) {
        super(varWidth, varHeight);
        this.res = new Identifier(res);
        this.pos = pos;
        this.tex = tex;
    }

    @Override
    public void preLoad(MinecraftDisplayerRenderer renderer) {
        super.preLoad(renderer);

        preScanned = TextureLoader.preScan(res);
    }

    @Override
    public void evaluateVariables(MinecraftDisplayerRenderer renderer) {
        pos.evaluate();
        tex.evaluate();
        varWidth.value = pos._w;
        varHeight.value = pos._h;
    }

    @Override
    public void render(MinecraftDisplayerRenderer renderer) {
        bindTexture(renderer);
        BufferBuilder vb = tess.getBuffer();
        vb.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
        vb.vertex(pos._x, pos._y + pos._h, 0).texture(tex._x, tex._y + tex._h).next();
        vb.vertex(pos._x + pos._w, pos._y + pos._h, 0).texture(tex._x + tex._w, tex._y + tex._h).next();
        vb.vertex(pos._x + pos._w, pos._y, 0).texture(tex._x + tex._w, tex._y).next();
        vb.vertex(pos._x, pos._y, 0).texture(tex._x, tex._y).next();
        tess.draw();
    }

    public void bindTexture(MinecraftDisplayerRenderer renderer) {
        if (preScanned != null) {
            preScanned.bind(renderer.textureManager);
        } else {
            TextureLoader.bindTexture(renderer.textureManager, res);
        }
    }

    @Override
    public String getLocation() {
        return res.toString();
    }
}
