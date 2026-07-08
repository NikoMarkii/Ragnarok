package com.niko.ragnarok.client;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Gradius;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID, value = Dist.CLIENT)
public class GradiusBossBarOverlay {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/gui/gradius_boss_bar.png");

    private static final int FRAME_U = 0;
    private static final int FRAME_V = 25;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 35;
    private static final int VANILLA_BOSS_BAR_WIDTH = 182;
    private static final float BAR_SCALE = VANILLA_BOSS_BAR_WIDTH / (float) FRAME_WIDTH;
    private static final int FILL_U = 5;
    private static final int RED_FILL_V = 71;
    private static final int BLUE_FILL_V = 101;
    private static final int FILL_WIDTH = 118;
    private static final int FILL_HEIGHT = 8;
    private static final int FILL_X_OFFSET = 5;
    private static final int FILL_Y_OFFSET = 15;
    private static final int SKULL_U = 44;
    private static final int SKULL_V = 1;
    private static final int SKULL_SIZE = 16;
    private static final int HELMET_U = 68;
    private static final int HELMET_V = 0;
    private static final int HELMET_WIDTH = 14;
    private static final int HELMET_HEIGHT = 17;
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 128;

    // ── 補間用の現在表示値を管理するマップ ──
    private static final java.util.Map<Integer, Float> displayedHealth =
            new java.util.HashMap<>();

    // 補間速度（小さいほど滑らか・遅い、大きいほど速い）
    private static final float LERP_SPEED = 0.30F;

    // render()を複数体対応に変更
    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.level == null || minecraft.player == null) {
            return;
        }

        List<Gradius> gradiusList = findVisibleGradiusList(minecraft.player);
        if (gradiusList.isEmpty()) return;

        int otherBarCount = countOtherBossBars(minecraft, gradiusList);

        for (int i = 0; i < gradiusList.size(); i++) {
            int index = otherBarCount + i;

            // ボスバーが2本を超える場合は描画しない
            if (index >= 2) break;

            renderBar(event.getGuiGraphics(), minecraft, gradiusList.get(i), index);
        }
    }

    /**
     * BossHealthOverlayに登録されているBossEventのうち
     * グラディウス以外のものの数を返す
     */
    @SuppressWarnings("unchecked")
    private static int countOtherBossBars(Minecraft minecraft, List<Gradius> gradiusList) {
        try {
            // グラディウスの表示名セットを作成
            java.util.Set<String> gradiusNames = new java.util.HashSet<>();
            for (Gradius g : gradiusList) {
                gradiusNames.add(g.getDisplayName().getString());
            }

            // BossHealthOverlayのeventsフィールドをリフレクションで取得
            java.lang.reflect.Field[] fields =
                    net.minecraft.client.gui.components.BossHealthOverlay.class
                            .getDeclaredFields();

            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(minecraft.gui.getBossOverlay());
                if (value instanceof java.util.Map<?, ?> map) {
                    int count = 0;
                    for (Object entry : map.values()) {
                        if (entry instanceof net.minecraft.world.BossEvent bossEvent) {
                            // グラディウスの名前と一致しないものをカウント
                            if (!gradiusNames.contains(bossEvent.getName().getString())) {
                                count++;
                            }
                        }
                    }
                    return count;
                }
            }
        } catch (Exception e) {
            // リフレクション失敗時は0（グラディウスが最上部に表示される）
        }
        return 0;
    }

    // 単体 → リスト取得に変更
    private static List<Gradius> findVisibleGradiusList(Player player) {
        return player.level().getEntitiesOfClass(
                Gradius.class,
                player.getBoundingBox().inflate(128.0D),
                gradius -> (gradius.isAlive() || gradius.isActuallyDying()) // 死亡アニメ中も含める
                        && !gradius.isStandby()
                        && player.distanceToSqr(gradius) <= 16384.0D
        );
    }

    // indexパラメータを追加して縦オフセットを計算
    private static void renderBar(GuiGraphics guiGraphics, Minecraft minecraft,
                                  Gradius gradius, int index) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int scaledFrameWidth = Math.round(FRAME_WIDTH * BAR_SCALE);
        int scaledFrameHeight = Math.round(FRAME_HEIGHT * BAR_SCALE);
        int x = (screenWidth - scaledFrameWidth) / 2;

        int barSpacing = scaledFrameHeight - 7;
        int y = -15 + index * barSpacing;
        int frameY = y + Math.round(14 * BAR_SCALE);

        float actualProgress = Mth.clamp(
                gradius.getHealth() / gradius.getMaxHealth(), 0.0F, 1.0F);

        // ── 補間処理 ──
        int entityId = gradius.getId();
        float current = displayedHealth.getOrDefault(entityId, actualProgress);

        // 実際の体力に向けてLERP
        current = Mth.lerp(LERP_SPEED, current, actualProgress);

        // 十分近づいたらスナップ（ごく小さな差は無視）
        if (Math.abs(current - actualProgress) < 0.001F) {
            current = actualProgress;
        }

        // 死亡後は表示値を削除
        if (gradius.isRemoved()) {
            displayedHealth.remove(entityId);
        } else {
            displayedHealth.put(entityId, current);
        }

        boolean phase2 = gradius.isPhase2Color();
        int fillWidth = Mth.ceil(FILL_WIDTH * current); // actualProgressではなくcurrentを使用
        int fillV = phase2 ? BLUE_FILL_V : RED_FILL_V;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(BAR_SCALE, BAR_SCALE, 1.0F);

        guiGraphics.blit(TEXTURE, 0, 14,
                FRAME_U, FRAME_V, FRAME_WIDTH, FRAME_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (fillWidth > 0) {
            guiGraphics.blit(TEXTURE, FILL_X_OFFSET, 14 + FILL_Y_OFFSET,
                    FILL_U, fillV, fillWidth, FILL_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        if (phase2) {
            guiGraphics.blit(TEXTURE, (FRAME_WIDTH - SKULL_SIZE) / 2, 14,
                    SKULL_U, SKULL_V, SKULL_SIZE, SKULL_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        } else {
            guiGraphics.blit(TEXTURE, (FRAME_WIDTH - HELMET_WIDTH) / 2, 14,
                    HELMET_U, HELMET_V, HELMET_WIDTH, HELMET_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        guiGraphics.pose().popPose();

        Component name = gradius.getDisplayName();
        int textX = (screenWidth - minecraft.font.width(name)) / 2;
        guiGraphics.drawString(minecraft.font, name, textX,
                frameY + scaledFrameHeight - 17, 0xFFFFFFFF, true);
    }
}
