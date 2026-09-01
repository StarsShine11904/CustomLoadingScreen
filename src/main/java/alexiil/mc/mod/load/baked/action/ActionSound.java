package alexiil.mc.mod.load.baked.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.load.baked.BakedAction;

import buildcraft.lib.expression.api.IExpressionNode.INodeBoolean;
import buildcraft.lib.expression.api.IExpressionNode.INodeObject;

public class ActionSound extends BakedAction {
    public static final SoundManager sndHandler = MinecraftClient.getInstance().getSoundManager();
    public final INodeObject<String> sound;
    public final INodeBoolean repeat;
    private SoundInstance currentSound = null;

    public ActionSound(INodeBoolean conditionStart, INodeBoolean conditionEnd, INodeObject<String> sound, INodeBoolean repeat) {
        super(conditionStart, conditionEnd);
        this.sound = sound;
        this.repeat = repeat;
    }

    @Override
    protected void start() {
        Identifier soundLocation = new Identifier(sound.evaluate());
        // currentSound = PositionedSoundInstance.master(new SoundEvent(soundLocation), 1.0F);
        // sndHandler.play(currentSound);
        // TODO finish action sound!
    }

    @Override
    protected void tick() {
        if (currentSound != null && (!sndHandler.isPlaying(currentSound)) && repeat.evaluate() && !conditionEnd.evaluate()) {
            sndHandler.play(currentSound);
        }
    }

    @Override
    protected void end() {}
}
