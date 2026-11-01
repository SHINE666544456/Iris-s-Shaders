package dev.lightshader;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Each preset maps to assets/lightshader/post_effect/<key>.json. */
public enum Preset {
    LITE("lite"),
    BALANCED("balanced"),
    CINEMATIC("cinematic"),
    VIVID("vivid");

    public final String key;

    Preset(String key) {
        this.key = key;
    }

    public Identifier effectId() {
        return Identifier.of(LightShaderClient.MOD_ID, key);
    }

    public Text label() {
        return Text.translatable("lightshader.preset." + key);
    }

    public Preset next() {
        Preset[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** One step cheaper. Returns null when already at the cheapest preset. */
    public Preset cheaper() {
        return switch (this) {
            case VIVID, CINEMATIC -> BALANCED;
            case BALANCED -> LITE;
            case LITE -> null;
        };
    }
}
