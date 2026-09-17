package com.niko.ragnarok.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class BossDialogueOverlay implements IGuiOverlay {

    public static final BossDialogueOverlay INSTANCE = new BossDialogueOverlay();

    private static final ResourceLocation TEXTBOX_MAIN =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/textbox1.png");

    // ★ NPC画面と同じ枠用テクスチャを指定
    private static final ResourceLocation TEXTBOX_ICON =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/icon.png");

    // デフォルトのアイコン（フォールバック用）
    private static final ResourceLocation DEFAULT_ICON =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/icon.png");

    private static final int MAIN_TEX_W = 100;
    private static final int MAIN_TEX_H = 25;

    private static final int ICON_TEX_W = 25;
    private static final int ICON_TEX_H = 25;

    private static final int SCALE = 2;
    private static final int GAP = 4;

    private static final int TICKS_PER_CHAR = 1;
    private static final int HOLD_TICKS_AFTER_FULL = 60;
    private static final int FADE_OUT_TICKS = 10;

    private static final Random RANDOM = new Random();

    private String text = "";
    @Nullable
    private SoundEvent typingSound;
    private ResourceLocation currentIcon = DEFAULT_ICON;

    private int visibleChars = 0;
    private int tickCounter = 0;
    private boolean fullyShown = false;
    private int holdTimer = 0;
    private boolean active = false;

    private BossDialogueOverlay() {
    }

    public static void show(String text, @Nullable SoundEvent typingSound) {
        show(text, typingSound, DEFAULT_ICON);
    }

    public static void show(String text, @Nullable SoundEvent typingSound, ResourceLocation icon) {
        INSTANCE.text = text;
        INSTANCE.typingSound = typingSound;
        INSTANCE.currentIcon = (icon != null) ? icon : DEFAULT_ICON;
        INSTANCE.visibleChars = 0;
        INSTANCE.tickCounter = 0;
        INSTANCE.fullyShown = false;
        INSTANCE.holdTimer = 0;
        INSTANCE.active = true;
    }

    public static void hide() {
        INSTANCE.active = false;
    }

    public void tick() {
        if (!active) {
            return;
        }

        if (!fullyShown) {
            tickCounter++;
            if (tickCounter >= TICKS_PER_CHAR) {
                tickCounter = 0;
                visibleChars++;
                playTypingSound();
                if (visibleChars >= text.length()) {
                    visibleChars = text.length();
                    fullyShown = true;
                }
            }
        } else {
            holdTimer++;
            if (holdTimer >= HOLD_TICKS_AFTER_FULL + FADE_OUT_TICKS) {
                active = false;
            }
        }
    }

    private void playTypingSound() {
        if (typingSound == null) {
            return;
        }
        int idx = visibleChars - 1;
        if (idx < 0 || idx >= text.length() || Character.isWhitespace(text.charAt(idx))) {
            return;
        }

        float pitch = 0.95F + RANDOM.nextFloat() * 0.1F;
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(typingSound, pitch, 1.0F));
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenW, int screenH) {
        if (!active) {
            return;
        }

        float alpha = 1.0F;
        if (fullyShown && holdTimer > HOLD_TICKS_AFTER_FULL) {
            int fadeElapsed = holdTimer - HOLD_TICKS_AFTER_FULL;
            alpha = 1.0F - Math.min(1.0F, fadeElapsed / (float) FADE_OUT_TICKS);
        }
        if (alpha <= 0.0F) {
            return;
        }

        int mainW = MAIN_TEX_W * SCALE;
        int mainH = MAIN_TEX_H * SCALE;
        int iconW = ICON_TEX_W * SCALE;
        int iconH = ICON_TEX_H * SCALE;

        int baseBottomMargin = 22;
        LocalPlayer player = Minecraft.getInstance().player;

        if (player != null && !player.isCreative() && !player.isSpectator()) {
            int hudHeight = 18;
            float totalHealth = player.getMaxHealth() + player.getAbsorptionAmount();
            int healthRows = (int) Math.ceil(totalHealth / 20.0F);
            if (healthRows > 1) {
                hudHeight += (healthRows - 1) * 12;
            }
            baseBottomMargin += hudHeight;
        }

        int totalWidth = iconW + GAP + mainW;
        int startX = (screenW - totalWidth) / 2;

        int iconX = startX;
        int mainX = iconX + iconW + GAP;
        int mainY = screenH - baseBottomMargin - mainH - 5;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        // ★ 1. アイコン背景枠の描画（NpcDialogueScreenと同じ規格）
        graphics.blit(TEXTBOX_ICON, iconX, mainY, iconW, iconH, 0, 0, ICON_TEX_W, ICON_TEX_H, ICON_TEX_W, ICON_TEX_H);

        // ★ 2. 顔グラフィックの描画（枠線の内側に収まるようにオフセット＆サイズ計算）
        if (currentIcon != null) {
            int faceOffset = 2 * SCALE; // 枠線の内側に納めるための余白
            int faceSize = (ICON_TEX_W * SCALE) - (faceOffset * 2);
            graphics.blit(currentIcon, iconX + faceOffset, mainY + faceOffset, faceSize, faceSize, 0, 0, 32, 32, 32, 32);
        }

        // メインボックスの描画
        graphics.blit(TEXTBOX_MAIN, mainX, mainY, mainW, mainH, 0, 0, MAIN_TEX_W, MAIN_TEX_H, MAIN_TEX_W, MAIN_TEX_H);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 本文
        Font font = Minecraft.getInstance().font;
        String shown = text.substring(0, Math.min(visibleChars, text.length()));
        int paddingX = 8 * SCALE;
        int paddingY = 4 * SCALE;
        int wrapWidth = mainW - paddingX * 2;

        List<FormattedCharSequence> wrapped = font.split(Component.literal(shown), wrapWidth);
        int lineHeight = font.lineHeight + 1;

        int textX = mainX + paddingX;
        int textY = mainY + paddingY;

        int textAlpha = Math.round(alpha * 255.0F);
        int argb = (textAlpha << 24) | 0xFFFFFF;

        for (int i = 0; i < wrapped.size(); i++) {
            graphics.drawString(font, wrapped.get(i), textX, textY + i * lineHeight, argb);
        }
        RenderSystem.disableBlend();
    }
}