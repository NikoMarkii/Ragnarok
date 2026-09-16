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

/**
 * ボス戦中に一瞬だけ表示する、名前欄無しのテキストボックス。
 *
 * NpcDialogueScreen（会話用）とは違い、こちらは Screen ではなく
 * IGuiOverlay として実装している。Screen はプレイヤーの操作を奪って
 * しまうため、戦闘中に「一言喋らせたいだけ」の用途には向かない。
 * オーバーレイなら表示中もプレイヤーは普通に動ける。
 *
 * 使い方：どのボスからでも BossDialogueOverlay.show(text, sound) を
 * 呼ぶだけでよい（ただし呼び出しはクライアント側のみで有効。
 * サーバー側のエンティティコードからは ClientBossDialoguePacket 経由で呼ぶこと）。
 */
public class BossDialogueOverlay implements IGuiOverlay {

    public static final BossDialogueOverlay INSTANCE = new BossDialogueOverlay();

    private static final ResourceLocation TEXTBOX_MAIN =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/textbox1.png");
    private static final int MAIN_TEX_W = 100;
    private static final int MAIN_TEX_H = 25;
    private static final int SCALE = 2;

    private static final int TICKS_PER_CHAR = 1;
    private static final int HOLD_TICKS_AFTER_FULL = 60; // 全文表示後、消え始めるまでの待機（3秒）
    private static final int FADE_OUT_TICKS = 10;         // フェードアウトにかけるtick数

    private static final Random RANDOM = new Random();

    private String text = "";
    @Nullable
    private SoundEvent typingSound;

    private int visibleChars = 0;
    private int tickCounter = 0;
    private boolean fullyShown = false;
    private int holdTimer = 0;
    private boolean active = false;

    private BossDialogueOverlay() {
    }

    /** ボスの台詞を表示する。表示中に呼ばれた場合は上書きする */
    public static void show(String text, @Nullable SoundEvent typingSound) {
        INSTANCE.text = text;
        INSTANCE.typingSound = typingSound;
        INSTANCE.visibleChars = 0;
        INSTANCE.tickCounter = 0;
        INSTANCE.fullyShown = false;
        INSTANCE.holdTimer = 0;
        INSTANCE.active = true;
    }

    public static void hide() {
        INSTANCE.active = false;
    }

    /** 毎ゲームtick呼び出す（BossDialogueClientEvents から呼ばれる） */
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

        int baseBottomMargin = 22; // ホットバーの高さ
        LocalPlayer player = Minecraft.getInstance().player;

        if (player != null && !player.isCreative() && !player.isSpectator()) {
            // 基本のHUD（経験値バー + ハート1段目 + 満腹度）で約 18px
            int hudHeight = 18;

            // 最大体力 + 金のリンゴ等の吸収ハート（Absorption）の合計HPを計算
            float totalHealth = player.getMaxHealth() + player.getAbsorptionAmount();
            // ハート1段 = 20 HP (10個)。2段目以降は1段につき 10px 高くなる
            int healthRows = (int) Math.ceil(totalHealth / 20.0F);
            if (healthRows > 1) {
                hudHeight += (healthRows - 1) * 12;
            }

            baseBottomMargin += hudHeight;
        }

        int mainX = (screenW - mainW) / 2;
        int mainY = screenH - baseBottomMargin - mainH - 5;

        // ── 本体（フェード対応のためシェーダーカラーで透明度をかける）──
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(TEXTBOX_MAIN, mainX, mainY, mainW, mainH, 0, 0, MAIN_TEX_W, MAIN_TEX_H, MAIN_TEX_W, MAIN_TEX_H);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // ── 本文（タイプ表示、折り返しあり、縦方向は中央寄せ）──
        Font font = Minecraft.getInstance().font;
        String shown = text.substring(0, Math.min(visibleChars, text.length()));
        int paddingX = 8 * SCALE;
        int paddingY = 4 * SCALE; // 上下の余白（見た目に応じて微調整してね）
        int wrapWidth = mainW - paddingX * 2;

        List<FormattedCharSequence> wrapped = font.split(Component.literal(shown), wrapWidth);
        int lineHeight = font.lineHeight + 1;

        // ★縦中央寄せを廃止し、ボックスの上端から paddingY 分だけ下げた位置を基準にする
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