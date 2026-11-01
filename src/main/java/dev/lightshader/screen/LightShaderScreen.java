package dev.lightshader.screen;

import dev.lightshader.LightShaderConfig;
import dev.lightshader.ShaderController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class LightShaderScreen extends Screen {
    private static final int[] FPS_STEPS = {30, 45, 60, 90, 120};

    private final Screen parent;
    private ButtonWidget enabledBtn;
    private ButtonWidget presetBtn;
    private ButtonWidget autoBtn;
    private ButtonWidget fpsBtn;

    public LightShaderScreen(Screen parent) {
        super(Text.translatable("lightshader.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LightShaderConfig cfg = LightShaderConfig.get();
        int w = 200;
        int x = this.width / 2 - w / 2;
        int y = this.height / 4;
        int step = 24;

        enabledBtn = this.addDrawableChild(ButtonWidget.builder(enabledLabel(), b -> {
            cfg.enabled = !cfg.enabled;
            changed();
        }).dimensions(x, y, w, 20).build());

        presetBtn = this.addDrawableChild(ButtonWidget.builder(presetLabel(), b -> {
            cfg.preset = cfg.preset.next();
            changed();
        }).dimensions(x, y + step, w, 20).build());

        autoBtn = this.addDrawableChild(ButtonWidget.builder(autoLabel(), b -> {
            cfg.autoQuality = !cfg.autoQuality;
            changed();
        }).dimensions(x, y + step * 2, w, 20).build());

        fpsBtn = this.addDrawableChild(ButtonWidget.builder(fpsLabel(), b -> {
            int next = FPS_STEPS[0];
            for (int i = 0; i < FPS_STEPS.length; i++) {
                if (FPS_STEPS[i] == cfg.minFps) {
                    next = FPS_STEPS[(i + 1) % FPS_STEPS.length];
                    break;
                }
            }
            cfg.minFps = next;
            changed();
        }).dimensions(x, y + step * 3, w, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("lightshader.reload"), b -> {
            this.client.reloadResources();
            ShaderController.markDirty();
        }).dimensions(x, y + step * 4 + 8, w, 20).build());

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> this.close())
                .dimensions(x, y + step * 5 + 8, w, 20).build());
    }

    private void changed() {
        LightShaderConfig.save();
        ShaderController.markDirty();
        enabledBtn.setMessage(enabledLabel());
        presetBtn.setMessage(presetLabel());
        autoBtn.setMessage(autoLabel());
        fpsBtn.setMessage(fpsLabel());
    }

    private static Text onOff(boolean v) {
        return Text.translatable(v ? "lightshader.on" : "lightshader.off");
    }

    private static Text enabledLabel() {
        return Text.translatable("lightshader.shaders", onOff(LightShaderConfig.get().enabled));
    }

    private static Text presetLabel() {
        return Text.translatable("lightshader.preset", LightShaderConfig.get().preset.label());
    }

    private static Text autoLabel() {
        return Text.translatable("lightshader.auto", onOff(LightShaderConfig.get().autoQuality));
    }

    private static Text fpsLabel() {
        return Text.translatable("lightshader.minfps", LightShaderConfig.get().minFps);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
