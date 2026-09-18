package com.niko.ragnarok.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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

    // 画面全体の出現・閉じるアニメーション
    private int animTicks = 0;
    private boolean isClosing = false;
    private static final int ANIM_DURATION = 6; // 約0.3秒

    // 選択肢の表示・シフトアニメーション
    private int choiceAnimTicks = 0;
    private static final int CHOICE_ANIM_DURATION = 5; // 約0.25秒

    // 選択肢決定後の保留アクション管理
    @Nullable
    private Consumer<NpcDialogueScreen> pendingChoiceAction = null;
    private boolean isChoiceTransitioning = false;

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
        this.animTicks = 0;
        this.isClosing = false;
        this.choiceAnimTicks = 0;
        this.pendingChoiceAction = null;
        this.isChoiceTransitioning = false;
    }

    private void resetTyping() {
        this.visibleChars = 0;
        this.tickCounter = 0;
        this.lineFullyShown = false;
        this.choiceAnimTicks = 0;
        this.pendingChoiceAction = null;
        this.isChoiceTransitioning = false;
    }

    public void startClosing() {
        if (!isClosing) {
            this.isClosing = true;
            this.animTicks = ANIM_DURATION;
        }
    }

    @Override
    public void tick() {
        super.tick();
        KeyMapping.releaseAll();

        // 1. 全体フェードイン/アウト処理
        if (!isClosing) {
            if (animTicks < ANIM_DURATION) {
                animTicks++;
            }
        } else {
            animTicks--;
            if (animTicks <= 0) {
                super.onClose();
                return;
            }
        }

        DialogueLine current = getCurrentLine();
        if (current == null || isClosing) {
            return;
        }

        // 2. タイピング進行（選択肢遷移中は停止）
        if (!lineFullyShown && !isChoiceTransitioning) {
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

        // 3. 選択肢アニメーションの進行タイマー
        boolean targetHasChoices = lineFullyShown && !current.choices().isEmpty() && !isChoiceTransitioning;
        if (targetHasChoices) {
            if (choiceAnimTicks < CHOICE_ANIM_DURATION) {
                choiceAnimTicks++;
            }
        } else {
            if (choiceAnimTicks > 0) {
                choiceAnimTicks--;
                // 消滅アニメーションが完了した瞬間に保留していた選択肢の処理を実行
                if (choiceAnimTicks == 0 && isChoiceTransitioning) {
                    isChoiceTransitioning = false;
                    if (pendingChoiceAction != null) {
                        Consumer<NpcDialogueScreen> action = pendingChoiceAction;
                        pendingChoiceAction = null;
                        action.accept(this);
                    }
                }
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
        if (current == null && !isClosing) {
            startClosing();
            return;
        }

        // 全体のイージング (Ease-Out Cubic)
        float progress = Math.min(1.0f, Math.max(0.0f, (animTicks + (isClosing ? -partialTick : partialTick)) / (float) ANIM_DURATION));
        float eased = 1.0f - (float) Math.pow(1.0 - progress, 3);
        float alpha = eased;
        int yOffset = (int) ((1.0f - eased) * 15.0f);

        // 選択肢用のイージング計算
        boolean targetHasChoices = lineFullyShown && current != null && !current.choices().isEmpty() && !isChoiceTransitioning;
        float choiceDelta = targetHasChoices ? partialTick : -partialTick;
        float choiceProgress = Math.min(1.0f, Math.max(0.0f, (choiceAnimTicks + choiceDelta) / (float) CHOICE_ANIM_DURATION));
        float choiceEased = 1.0f - (float) Math.pow(1.0 - choiceProgress, 3);

        int screenW = this.minecraft.getWindow().getGuiScaledWidth();
        int screenH = this.minecraft.getWindow().getGuiScaledHeight();

        int mainW = MAIN_TEX_W * SCALE;
        int mainH = MAIN_TEX_H * SCALE;
        int subW = (int) (SUB_TEX_W * SUB_SCALE);
        int subH = (int) (SUB_TEX_H * SUB_SCALE);
        int iconW = ICON_TEX_W * SCALE;
        int iconH = ICON_TEX_H * SCALE;
        int gap = 4;

        // 横幅の拡張補間（本体が横へ滑らかにずれる動き）
        float currentExtraWidth = (gap + subW) * choiceEased;
        float totalWidth = iconW + gap + mainW + currentExtraWidth;
        int groupStartX = (int) ((screenW - totalWidth) / 2.0f);

        int iconX = groupStartX;
        int mainX = iconX + iconW + gap;

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

        int mainY = screenH - baseBottomMargin - mainH - 5 + yOffset;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        // アイコン枠
        graphics.blit(TEXTBOX_ICON, iconX, mainY, iconW, iconH, 0, 0, ICON_TEX_W, ICON_TEX_H, ICON_TEX_W, ICON_TEX_H);

        // 顔グラフィック
        if (current != null && current.faceIcon() != null) {
            int faceOffset = 2 * SCALE;
            int faceSize = (ICON_TEX_W * SCALE) - (faceOffset * 2);
            graphics.blit(current.faceIcon(), iconX + faceOffset, mainY + faceOffset, faceSize, faceSize, 0, 0, 32, 32, 32, 32);
        }

        // メインボックス
        graphics.blit(TEXTBOX_MAIN, mainX, mainY, mainW, mainH, 0, 0, MAIN_TEX_W, MAIN_TEX_H, MAIN_TEX_W, MAIN_TEX_H);

        // 名前枠
        int nameX = mainX + 4;
        int nameY = mainY - subH + 2;
        graphics.blit(TEXTBOX_SUB, nameX, nameY, subW, subH, 0, 0, SUB_TEX_W, SUB_TEX_H, SUB_TEX_W, SUB_TEX_H);

        int textAlpha = Math.round(alpha * 255.0F);
        int whiteColor = (textAlpha << 24) | 0xFFFFFF;

        if (current != null) {
            graphics.drawCenteredString(this.font, current.speaker(),
                    nameX + subW / 2, nameY + (subH - this.font.lineHeight) / 2, whiteColor);

            String shownText = current.text().substring(0, Math.min(visibleChars, current.text().length()));
            int paddingX = 8 * SCALE;
            int paddingY = 6 * SCALE;
            int textX = mainX + paddingX;
            int textY = mainY + paddingY;
            int wrapWidth = mainW - (paddingX * 2);

            List<FormattedCharSequence> wrapped = this.font.split(Component.literal(shownText), wrapWidth);
            int lineHeight = this.font.lineHeight + 1;
            for (int i = 0; i < wrapped.size(); i++) {
                graphics.drawString(this.font, wrapped.get(i), textX, textY + i * lineHeight, whiteColor);
            }

            hoveredChoice = -1;

            // ★ 選択肢の描画（位置固定＋純粋なアルファフェードイン/アウト）
            if (choiceEased > 0.001f) {
                int choiceX = mainX + mainW + gap;
                float choiceAlpha = alpha * choiceEased;
                int choiceTextAlpha = Math.round(choiceAlpha * 255.0F);

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, choiceAlpha);

                List<DialogueChoice> choices = current.choices();
                for (int i = 0; i < choices.size(); i++) {
                    // 固定Y位置で描画
                    int cy = mainY + i * (subH + 2);

                    // 遷移中ではない＆アニメーション完了時のみホバーを許可
                    boolean hovered = !isChoiceTransitioning && (choiceProgress >= 0.8F)
                            && mouseX >= choiceX && mouseX < choiceX + subW
                            && mouseY >= cy && mouseY < cy + subH;
                    if (hovered) {
                        hoveredChoice = i;
                    }

                    graphics.blit(TEXTBOX_SUB, choiceX, cy, subW, subH, 0, 0, SUB_TEX_W, SUB_TEX_H, SUB_TEX_W, SUB_TEX_H);
                    int color = hovered ? ((choiceTextAlpha << 24) | 0xFFFF55) : ((choiceTextAlpha << 24) | 0xFFFFFF);
                    graphics.drawCenteredString(this.font, choices.get(i).label(),
                            choiceX + subW / 2, cy + (subH - this.font.lineHeight) / 2, color);
                }

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            }

            // ▼ マーク（選択肢がない時）
            if (lineFullyShown && current.choices().isEmpty()) {
                graphics.drawString(this.font, "\u25BC",
                        mainX + mainW - paddingX - 4, mainY + mainH - paddingY - 2, whiteColor);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || isClosing || isChoiceTransitioning) return true;
        DialogueLine current = getCurrentLine();
        if (current == null) return true;

        if (!lineFullyShown) {
            skipTyping();
            return true;
        }

        if (!current.choices().isEmpty()) {
            if (hoveredChoice >= 0) {
                playClickSound();
                DialogueChoice choice = current.choices().get(hoveredChoice);
                if (choice.onSelect() != null) {
                    this.pendingChoiceAction = choice.onSelect();
                    this.isChoiceTransitioning = true;
                }
            }
            return true;
        }

        advance();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isClosing || isChoiceTransitioning) return true;

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
            startClosing();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return true;
    }

    private void playClickSound() {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void skipTyping() {
        DialogueLine current = getCurrentLine();
        if (current != null) {
            visibleChars = current.text().length();
            lineFullyShown = true;
        }
    }

    public void advance() {
        DialogueLine current = getCurrentLine();
        if (current != null && current.nextIndex() != null) {
            int next = current.nextIndex();
            if (next < 0) {
                startClosing();
                return;
            } else {
                jumpTo(next);
                return;
            }
        }

        lineIndex++;
        if (getCurrentLine() == null) {
            startClosing();
        } else {
            resetTyping();
        }
    }

    public void jumpTo(int index) {
        lineIndex = index;
        if (getCurrentLine() == null) {
            startClosing();
        } else {
            resetTyping();
        }
    }

    @Override
    public void onClose() {
        if (!isClosing) {
            startClosing();
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public record DialogueLine(
            String speaker,
            String text,
            @Nullable ResourceLocation faceIcon,
            List<DialogueChoice> choices,
            @Nullable Integer nextIndex
    ) {
        public DialogueLine(String speaker, String text, ResourceLocation faceIcon, List<DialogueChoice> choices) {
            this(speaker, text, faceIcon, choices, null);
        }

        public DialogueLine(String speaker, String text, ResourceLocation faceIcon, Integer nextIndex) {
            this(speaker, text, faceIcon, List.of(), nextIndex);
        }

        public DialogueLine(String speaker, String text, ResourceLocation faceIcon) {
            this(speaker, text, faceIcon, List.of(), null);
        }

        public DialogueLine(String speaker, String text) {
            this(speaker, text, null, List.of(), null);
        }

        public DialogueLine(String speaker, String text, List<DialogueChoice> choices) {
            this(speaker, text, null, choices, null);
        }
    }

    public record DialogueChoice(String label, Consumer<NpcDialogueScreen> onSelect) {
    }
}