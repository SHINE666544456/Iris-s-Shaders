package dev.lightshader;

import dev.lightshader.mixin.GameRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** The "engine": keeps the right post effect bound to the game renderer and handles auto quality. */
public final class ShaderController {
    private static int retryCooldown = 0;
    private static int failures = 0;
    private static int autoTicks = 0;
    private static int lowStreak = 0;

    private ShaderController() {}

    /** Call after any config change so the controller re-applies immediately. */
    public static void markDirty() {
        retryCooldown = 0;
        failures = 0;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            autoTicks = 0;
            lowStreak = 0;
            return;
        }
        autoQuality(mc);
        apply(mc);
    }

    private static void apply(MinecraftClient mc) {
        // Leave vanilla spectator effects (creeper, spider, enderman) alone.
        if (mc.getCameraEntity() != mc.player) {
            return;
        }

        LightShaderConfig cfg = LightShaderConfig.get();
        GameRendererAccessor gr = (GameRendererAccessor) mc.gameRenderer;
        Identifier current = gr.lightshader$getPostProcessorId();
        Identifier wanted = cfg.enabled ? cfg.preset.effectId() : null;

        if (wanted == null) {
            if (current != null && LightShaderClient.MOD_ID.equals(current.getNamespace())) {
                gr.lightshader$setPostProcessorId(null);
                gr.lightshader$setPostProcessorEnabled(false);
            }
            return;
        }
        if (wanted.equals(current)) {
            return;
        }
        if (retryCooldown > 0) {
            retryCooldown--;
            return;
        }

        gr.lightshader$setPostProcessor(wanted);
        gr.lightshader$setPostProcessorEnabled(true);

        if (wanted.equals(gr.lightshader$getPostProcessorId())) {
            failures = 0;
        } else {
            failures++;
            retryCooldown = 40;
            LightShaderClient.LOGGER.warn("Post effect {} did not bind (attempt {})", wanted, failures);
            if (failures >= 3) {
                cfg.enabled = false;
                LightShaderConfig.save();
                notify(mc, Text.translatable("lightshader.msg.failed"));
            }
        }
    }

    private static void autoQuality(MinecraftClient mc) {
        LightShaderConfig cfg = LightShaderConfig.get();
        if (!cfg.enabled || !cfg.autoQuality || mc.currentScreen != null) {
            return;
        }
        if (++autoTicks < 20) {
            return;
        }
        autoTicks = 0;

        if (mc.getCurrentFps() < cfg.minFps) {
            lowStreak++;
        } else if (lowStreak > 0) {
            lowStreak = 0;
        }
        if (lowStreak < 8) {
            return;
        }

        lowStreak = -4; // grace period after a change
        Preset cheaper = cfg.preset.cheaper();
        if (cheaper != null) {
            cfg.preset = cheaper;
            notify(mc, Text.translatable("lightshader.msg.downgrade", cheaper.label()));
        } else {
            cfg.enabled = false;
            notify(mc, Text.translatable("lightshader.msg.autooff"));
        }
        LightShaderConfig.save();
        markDirty();
    }

    public static void cyclePreset(MinecraftClient mc) {
        LightShaderConfig cfg = LightShaderConfig.get();
        cfg.preset = cfg.preset.next();
        cfg.enabled = true;
        LightShaderConfig.save();
        markDirty();
        notify(mc, Text.translatable("lightshader.msg.preset", cfg.preset.label()));
    }

    public static void toggle(MinecraftClient mc) {
        LightShaderConfig cfg = LightShaderConfig.get();
        cfg.enabled = !cfg.enabled;
        LightShaderConfig.save();
        markDirty();
        notify(mc, Text.translatable(cfg.enabled ? "lightshader.msg.on" : "lightshader.msg.off"));
    }

    private static void notify(MinecraftClient mc, Text message) {
        if (mc.player != null) {
            mc.player.sendMessage(message, true);
        }
    }
}
