package com.niko.ragnarok.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * NPCとの会話用テキストボックス画面。
 *
 * - 本体(textbox1.png, 128x80)を画面下部に表示
 * - 名前欄(textbox2.png, 64x20)を本体の左上に重ねて表示
 * - 選択肢がある場合、同じtextbox2.pngを本体の右側に縦に並べて表示
 * - 本文は1文字ずつタイプ表示。タイプ中にクリック/Enter/Spaceで全文即表示、
 * 　全文表示後にもう一度操作すると次の行へ進む（選択肢がある行は選択肢クリックのみで進行）
 */
public class NpcDialogueScreen extends Screen {

    private static final ResourceLocation TEXTBOX_MAIN =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/textbox1.png");
    private static final ResourceLocation TEXTBOX_SUB =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/textbox2.png");

    // 元テクスチャの実サイズ（テクスチャファイルの実寸に合わせてある）
    private static final int MAIN_TEX_W = 128;
    private static final int MAIN_TEX_H = 35;
    private static final int SUB_TEX_W = 64;
    private static final int SUB_TEX_H = 20;

    // ドット絵をくっきり見せるための整数倍拡大率
    private static final int SCALE = 2;

    private static final float SUB_SCALE = 1.5f;

    // バニラのホットバー(182x22)の高さ。テキストボックスをこのすぐ上に置くために使う
    private static final int HOTBAR_HEIGHT = 22;

    // タイプ表示の速度：この tick 数ごとに1文字進む（小さいほど速い）
    private static final int TICKS_PER_CHAR = 1;

    // タイピング音の音程バラつき用（levelに依存しない安全な乱数）
    private static final java.util.Random RANDOM = new java.util.Random();

    private final List<DialogueLine> lines;
    @Nullable
    private final SoundEvent typingSound; // NPCごとに割り当てる、1文字表示されるたびに鳴る音
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

    /** タイピング音を使わない場合の簡易コンストラクタ */
    public NpcDialogueScreen(List<DialogueLine> lines) {
        this(lines, null);
    }

    @Override
    protected void init() {
        super.init();
        resetTyping();
    }

    private void resetTyping() {
        this.visibleChars = 0;
        this.tickCounter = 0;
        this.lineFullyShown = false;
    }

    @Override
    public void tick() {
        super.tick();

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
        // 空白文字では鳴らさない（それっぽいタイプ音になる）
        int idx = visibleChars - 1;
        if (idx < 0 || idx >= current.text().length()) {
            return;
        }
        if (Character.isWhitespace(current.text().charAt(idx))) {
            return;
        }

        SoundEvent sound = typingSound;
        if (sound == null) {
            return;
        }

        // 音程を少しだけランダムにして単調になりすぎないようにする
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

        // 各パーツの描画サイズ計算（floatからのキャスト）
        int mainW = MAIN_TEX_W * SCALE;
        int mainH = MAIN_TEX_H * SCALE;

        int subW = (int) (SUB_TEX_W * SUB_SCALE);
        int subH = (int) (SUB_TEX_H * SUB_SCALE);

        int gap = 4; // 本体と選択肢の間の隙間
        boolean hasChoices = lineFullyShown && !current.choices().isEmpty();

        // ★選択肢がある場合は「本体 + 隙間 + 選択肢」の全体の幅で画面中央を計算する
        int totalWidth = hasChoices ? (mainW + gap + subW) : mainW;
        int groupStartX = (screenW - totalWidth) / 2;

        int mainX = groupStartX;
        int mainY = screenH - HOTBAR_HEIGHT - mainH - 5;

        // ── 本体描画 ──
        graphics.blit(TEXTBOX_MAIN, mainX, mainY, mainW, mainH, 0, 0, MAIN_TEX_W, MAIN_TEX_H, MAIN_TEX_W, MAIN_TEX_H);

        // ── 名前欄（本体の左上に重ねる）──
        int nameX = mainX + 4;
        int nameY = mainY - subH + 2;
        graphics.blit(TEXTBOX_SUB, nameX, nameY, subW, subH, 0, 0, SUB_TEX_W, SUB_TEX_H, SUB_TEX_W, SUB_TEX_H);

        graphics.drawCenteredString(this.font, current.speaker(),
                nameX + subW / 2, nameY + (subH - this.font.lineHeight) / 2, 0xFFFFFF);

        // ── 本文（タイプ表示、折り返しあり）──
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

        // ── 選択肢（本体の右側、全文表示後のみ）──
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
            // 続きの矢印印
            graphics.drawString(this.font, "\u25BC",
                    mainX + mainW - paddingX - 4, mainY + mainH - paddingY - 2, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        DialogueLine current = getCurrentLine();
        if (current == null) {
            return true;
        }

        // タイプ表示中のクリック → 全文即表示するだけ（進行はさせない）
        if (!lineFullyShown) {
            skipTyping();
            return true;
        }

        // 選択肢がある行は、選択肢をクリックした時だけ進行する
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
        // Enter または Space で「クリックと同じ」進行操作
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void skipTyping() {
        DialogueLine current = getCurrentLine();
        if (current != null) {
            visibleChars = current.text().length();
            lineFullyShown = true;
        }
    }

    /** 次の行へ進む。行が無くなれば画面を閉じる */
    public void advance() {
        lineIndex++;
        if (getCurrentLine() == null) {
            this.onClose();
        } else {
            resetTyping();
        }
    }

    /** 選択肢から任意の行番号へジャンプする（分岐会話用） */
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
        return false; // シングルプレイでもゲームを一時停止させない
    }

    // ── データ定義 ──

    public record DialogueLine(String speaker, String text, List<DialogueChoice> choices) {
        public DialogueLine(String speaker, String text) {
            this(speaker, text, List.of());
        }
    }

    public record DialogueChoice(String label, Consumer<NpcDialogueScreen> onSelect) {
    }
}