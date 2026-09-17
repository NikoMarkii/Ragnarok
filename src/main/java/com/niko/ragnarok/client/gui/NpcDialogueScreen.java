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
    private static final ResourceLocation TEXTBOX_ICON =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/icon.png");

    private static final int MAIN_TEX_W = 128;
    private static final int MAIN_TEX_H = 35;
    private static final int SUB_TEX_W = 64;
    private static final int SUB_TEX_H = 20;
    private static final int ICON_TEX_W = 35;
    private static final int ICON_TEX_H = 35;

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
        int iconW = ICON_TEX_W * SCALE;
        int iconH = ICON_TEX_H * SCALE;

        int gap = 4;
        boolean hasChoices = lineFullyShown && !current.choices().isEmpty();

        int totalWidth = iconW + gap + (hasChoices ? (mainW + gap + subW) : mainW);
        int groupStartX = (screenW - totalWidth) / 2;

        int iconX = groupStartX;
        int mainX = iconX + iconW + gap;

        // ── 動的Y座標計算 ──
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

        // ── アイコン背景枠の描画 ──
        graphics.blit(TEXTBOX_ICON, iconX, mainY, iconW, iconH, 0, 0, ICON_TEX_W, ICON_TEX_H, ICON_TEX_W, ICON_TEX_H);

        // ★ 顔グラフィックの描画（指定がある場合のみ、枠の内側に描画）
        if (current.faceIcon() != null) {
            int faceOffset = 2 * SCALE; // 枠線の内側に納めるための余白
            int faceSize = (ICON_TEX_W * SCALE) - (faceOffset * 2);
            graphics.blit(current.faceIcon(), iconX + faceOffset, mainY + faceOffset, faceSize, faceSize, 0, 0, 32, 32, 32, 32);
        }

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
        if (button != 0) return true;
        DialogueLine current = getCurrentLine();
        if (current == null) return true;

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
        if (keyCode == 256) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return true;
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

    // ★ DialogueLine に faceIcon (顔グラのResourceLocation) を追加
    public record DialogueLine(
            String speaker,
            String text,
            @Nullable ResourceLocation faceIcon,
            List<DialogueChoice> choices
    ) {
        // 顔グラ指定 + 選択肢なし
        public DialogueLine(String speaker, String text, ResourceLocation faceIcon) {
            this(speaker, text, faceIcon, List.of());
        }

        // 顔グラなし + 選択肢なし（既存コード互換用）
        public DialogueLine(String speaker, String text) {
            this(speaker, text, null, List.of());
        }

        // 顔グラなし + 選択肢あり（既存コード互換用）
        public DialogueLine(String speaker, String text, List<DialogueChoice> choices) {
            this(speaker, text, null, choices);
        }
    }

    public record DialogueChoice(String label, Consumer<NpcDialogueScreen> onSelect) {
    }
}