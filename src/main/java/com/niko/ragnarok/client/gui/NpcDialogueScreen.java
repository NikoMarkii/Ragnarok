package com.niko.ragnarok.client.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class NpcDialogueScreen extends Screen {

    private static final ResourceLocation TEXTBOX_MAIN =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/textbox1.png");
    private static final ResourceLocation TEXTBOX_SUB =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/textbox2.png");

    private static final int MAIN_TEX_W = 128;
    private static final int MAIN_TEX_H = 35;
    private static final int SUB_TEX_W = 64;
    private static final int SUB_TEX_H = 20;

    private static final int SCALE = 2;
    private static final float SUB_SCALE = 1.5f;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int TICKS_PER_CHAR = 1;

    private static final java.util.Random RANDOM = new java.util.Random();

    private final List<DialogueLine> lines;
    @Nullable
    private final SoundEvent typingSound;
    private int lineIndex = 0;

    private int visibleChars = 0;
    private int tickCounter = 0;
    private boolean lineFullyShown = false;
    private int hoveredChoice = -1;

    public NpcDialogueScreen(List<DialogueLine> lines, @Nullable SoundEvent typingSound) {
        super(Component.literal("Dialogue"));
        this.lines = lines;
        this.typingSound = typingSound;
    }

    public NpcDialogueScreen(List<DialogueLine> lines) {
        this(lines, null);
    }

    @Override
    protected void init() {
        super.init();
        resetTyping();
        // 会話開始時、押されっぱなしの移動キーなどをすべてリセットする
        KeyMapping.releaseAll();
    }

    private void resetTyping() {
        this.visibleChars = 0;
        this.tickCounter = 0;
        this.lineFullyShown = false;
    }

    @Override
    public void tick() {
        super.tick();

        // 会話中、毎フレーム移動入力等をリセットしてプレイヤーを停止させる
        KeyMapping.releaseAll();

        DialogueLine current = getCurrentLine();
        if (current == null || lineFullyShown) {
            return;
        }

        tickCounter++;
        if (tickCounter >= TICKS_PER_CHAR) {
            tickCounter = 0;
            visibleChars++;
            playTypingSound(current);
            if (visibleChars >= current.text().length()) {
                visibleChars = current.text().length();
                lineFullyShown = true;
            }
        }
    }

    private void playTypingSound(DialogueLine current) {
        int idx = visibleChars - 1;
        if (idx < 0 || idx >= current.text().length() || Character.isWhitespace(current.text().charAt(idx))) {
            return;
        }

        SoundEvent sound = typingSound;
        if (sound == null) {
            return;
        }

        float pitch = 0.95F + RANDOM.nextFloat() * 0.1F;
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(sound, pitch, 1.0F));
    }

    private DialogueLine getCurrentLine() {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return null;
        }
        return lines.get(lineIndex);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        DialogueLine current = getCurrentLine();
        if (current == null) {
            this.onClose();
            return;
        }

        int screenW = this.minecraft.getWindow().getGuiScaledWidth();
        int screenH = this.minecraft.getWindow().getGuiScaledHeight();

        int mainW = MAIN_TEX_W * SCALE;
        int mainH = MAIN_TEX_H * SCALE;
        int subW = (int) (SUB_TEX_W * SUB_SCALE);
        int subH = (int) (SUB_TEX_H * SUB_SCALE);

        int gap = 4;
        boolean hasChoices = lineFullyShown && !current.choices().isEmpty();

        int totalWidth = hasChoices ? (mainW + gap + subW) : mainW;
        int groupStartX = (screenW - totalWidth) / 2;

        int mainX = groupStartX;

        // ── 動的Y座標計算（吸収ハート・多重ハート対応）──
        int baseBottomMargin = HOTBAR_HEIGHT;
        LocalPlayer player = this.minecraft.player;
        if (player != null && !player.isCreative() && !player.isSpectator()) {
            int hudHeight = 18;
            float totalHealth = player.getMaxHealth() + player.getAbsorptionAmount();
            int healthRows = (int) Math.ceil(totalHealth / 20.0F);
            if (healthRows > 1) {
                hudHeight += (healthRows - 1) * 10;
            }
            baseBottomMargin += hudHeight;
        }
        int mainY = screenH - baseBottomMargin - mainH - 5;

        // ── 本体描画 ──
        graphics.blit(TEXTBOX_MAIN, mainX, mainY, mainW, mainH, 0, 0, MAIN_TEX_W, MAIN_TEX_H, MAIN_TEX_W, MAIN_TEX_H);

        // ── 名前欄 ──
        int nameX = mainX + 4;
        int nameY = mainY - subH + 2;
        graphics.blit(TEXTBOX_SUB, nameX, nameY, subW, subH, 0, 0, SUB_TEX_W, SUB_TEX_H, SUB_TEX_W, SUB_TEX_H);
        graphics.drawCenteredString(this.font, current.speaker(),
                nameX + subW / 2, nameY + (subH - this.font.lineHeight) / 2, 0xFFFFFF);

        // ── 本文 ──
        String shownText = current.text().substring(0, Math.min(visibleChars, current.text().length()));
        int paddingX = 8 * SCALE;
        int paddingY = 6 * SCALE;
        int textX = mainX + paddingX;
        int textY = mainY + paddingY;
        int wrapWidth = mainW - (paddingX * 2);

        List<FormattedCharSequence> wrapped = this.font.split(Component.literal(shownText), wrapWidth);
        int lineHeight = this.font.lineHeight + 1;
        for (int i = 0; i < wrapped.size(); i++) {
            graphics.drawString(this.font, wrapped.get(i), textX, textY + i * lineHeight, 0xFFFFFF);
        }

        // ── 選択肢 ──
        hoveredChoice = -1;
        if (hasChoices) {
            int choiceX = mainX + mainW + gap;
            List<DialogueChoice> choices = current.choices();
            for (int i = 0; i < choices.size(); i++) {
                int cy = mainY + i * (subH + 2);
                boolean hovered = mouseX >= choiceX && mouseX < choiceX + subW
                        && mouseY >= cy && mouseY < cy + subH;
                if (hovered) {
                    hoveredChoice = i;
                }

                graphics.blit(TEXTBOX_SUB, choiceX, cy, subW, subH, 0, 0, SUB_TEX_W, SUB_TEX_H, SUB_TEX_W, SUB_TEX_H);
                int color = hovered ? 0xFFFF55 : 0xFFFFFF;
                graphics.drawCenteredString(this.font, choices.get(i).label(),
                        choiceX + subW / 2, cy + (subH - this.font.lineHeight) / 2, color);
            }
        } else if (lineFullyShown) {
            graphics.drawString(this.font, "\u25BC",
                    mainX + mainW - paddingX - 4, mainY + mainH - paddingY - 2, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true; // 左クリック以外も無効化・消費する
        }

        DialogueLine current = getCurrentLine();
        if (current == null) {
            return true;
        }

        if (!lineFullyShown) {
            skipTyping();
            return true;
        }

        if (!current.choices().isEmpty()) {
            if (hoveredChoice >= 0) {
                DialogueChoice choice = current.choices().get(hoveredChoice);
                if (choice.onSelect() != null) {
                    choice.onSelect().accept(this);
                }
            }
            return true;
        }

        advance();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter または Space で進行
        if (keyCode == 257 || keyCode == 335 || keyCode == 32) {
            DialogueLine current = getCurrentLine();
            if (current != null) {
                if (!lineFullyShown) {
                    skipTyping();
                } else if (current.choices().isEmpty()) {
                    advance();
                }
            }
            return true;
        }

        // ESCキーで閉じる動作はバニラの標準挙動に任せるため super に流す
        if (keyCode == 256) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // それ以外の全てのキー入力（WASD、インベントリEキー、スワップFキー等）を完全に無効化する
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return true; // キー離しイベントも全て消費
    }

    private void skipTyping() {
        DialogueLine current = getCurrentLine();
        if (current != null) {
            visibleChars = current.text().length();
            lineFullyShown = true;
        }
    }

    public void advance() {
        lineIndex++;
        if (getCurrentLine() == null) {
            this.onClose();
        } else {
            resetTyping();
        }
    }

    public void jumpTo(int index) {
        lineIndex = index;
        if (getCurrentLine() == null) {
            this.onClose();
        } else {
            resetTyping();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public record DialogueLine(String speaker, String text, List<DialogueChoice> choices) {
        public DialogueLine(String speaker, String text) {
            this(speaker, text, List.of());
        }
    }

    public record DialogueChoice(String label, Consumer<NpcDialogueScreen> onSelect) {
    }
}