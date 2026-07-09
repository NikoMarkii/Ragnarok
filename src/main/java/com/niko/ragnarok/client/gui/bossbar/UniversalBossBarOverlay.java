package com.niko.ragnarok.client.gui.bossbar;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID, value = Dist.CLIENT)
public class UniversalBossBarOverlay {

    private static final int GAUGE_WIDTH = 120;
    private static final int GAUGE_HEIGHT = 3;
    private static final int BASE_TEXTURE_WIDTH = 120;
    private static final int BASE_TEXTURE_HEIGHT = 6;

    // ゲージを滑らかに減らす速度係数
    private static final float LERP_SPEED = 0.15F;

    // 各ボスの表示上の体力を保持するマップ
    private static final Map<Integer, Float> DISPLAYED_PROGRESS = new HashMap<>();

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // カスタムボスバーを持つエンティティを検索
        List<LivingEntity> bosses = mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(64.0D),
                entity -> entity instanceof ICustomBossBar);

        // ボスがいない場合はマップをクリアして終了
        if (bosses.isEmpty()) {
            DISPLAYED_PROGRESS.clear();
            return;
        }

        // ★ `event.setCanceled(true)` は綺麗さっぱり削除したよ！これで他のMODのボスバーを巻き込むことはないさ。

        // 存在しなくなったボスのデータをクリーンアップ
        DISPLAYED_PROGRESS.keySet().removeIf(id -> bosses.stream().noneMatch(b -> b.getId() == id));

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // ★ 現在画面に表示されている通常のボスバー（バニラや他MODのもの）の数を取得する
        int vanillaBossCount = getVanillaBossBarCount(mc.gui.getBossOverlay());

// ★ 通常のボスバーの数に応じて、カスタムボスバーの開始Y座標を動的に計算する
        int yOffset = 20 + (vanillaBossCount * 19);
        if (yOffset < 22) {
            yOffset = 22;
        }

        // ーーー ★ ここから追加：描画したボスの数を数えるカウンター ーーー
        int drawnCount = 0;

        // もし「バニラのボスも含めて画面全体で絶対に3体まで」にしたいなら、
        // 上の行を int drawnCount = vanillaBossCount; に変えるといいよ。
        // ーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーー

        for (LivingEntity entity : bosses) {
            // ★ 3体描画し終わっていたら、これ以上は処理せずにループを抜け出す
            if (drawnCount >= 3) {
                break;
            }

            ICustomBossBar boss = (ICustomBossBar) entity;
            int entityId = entity.getId();

            // 滑らかな進行度（Lerp）の計算
            float actualProgress = boss.getBossProgress();
            float currentDisplayed = DISPLAYED_PROGRESS.computeIfAbsent(entityId, id -> actualProgress);

            currentDisplayed = Mth.lerp(LERP_SPEED, currentDisplayed, actualProgress);
            if (Math.abs(currentDisplayed - actualProgress) < 0.001F) {
                currentDisplayed = actualProgress;
            }
            DISPLAYED_PROGRESS.put(entityId, currentDisplayed);

            float scale = boss.getBossBarScale();
            float scaledGaugeWidth = GAUGE_WIDTH * scale;

            // 画面中央に配置するためのX座標
            int screenX = (int) ((screenWidth - scaledGaugeWidth) / 2);
            int screenY = yOffset;

            int fillWidth = Mth.ceil(GAUGE_WIDTH * currentDisplayed);

            // ＝＝＝＝＝ 【歪みレイヤー①】 ボスバー全体の空間変形（Push） ＝＝＝＝＝
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(screenX, screenY, 0.0F);
            guiGraphics.pose().scale(scale, scale, 1.0F);

            // 1. ベース画像の下地（背景）
            guiGraphics.blit(boss.getBossBarBaseTexture(),
                    0, 0, 0, 0, GAUGE_WIDTH, GAUGE_HEIGHT, BASE_TEXTURE_WIDTH, BASE_TEXTURE_HEIGHT);

            // 2. ベース画像のゲージ本体（前景）
            if (fillWidth > 0) {
                guiGraphics.blit(boss.getBossBarBaseTexture(),
                        0, 0, 0, 3, fillWidth, GAUGE_HEIGHT, BASE_TEXTURE_WIDTH, BASE_TEXTURE_HEIGHT);
            }

            // 3. フレーム（オーバーレイ）
            int frameX = boss.getFrameOffsetX();
            int frameY = boss.getFrameOffsetY();
            int frameW = boss.getFrameWidth();
            int frameH = boss.getFrameHeight();

            guiGraphics.blit(boss.getBossBarOverlayTexture(),
                    frameX, frameY, 0, 0, frameW, frameH, frameW, frameH);

            // 4. ボスの名前の描画準備
            Component bossName = entity.getDisplayName();
            int textWidth = mc.font.width(bossName);

            // 名前の個別拡大率
            float nameScale = 0.7F;

            float scaledTextWidth = textWidth * nameScale;
            float textX = (GAUGE_WIDTH - scaledTextWidth) / 2.0F;
            int textY = frameY - (int)(mc.font.lineHeight * nameScale) + 9;

            // ＝＝＝＝＝ 【歪みレイヤー②】 名前専用の空間変形（Push） ＝＝＝＝＝
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(textX, textY, 0.0F);
            guiGraphics.pose().scale(nameScale, nameScale, 1.0F);

            // 名前を(0, 0)基準で描画
            guiGraphics.drawString(mc.font, bossName, 0, 0, boss.getBossNameColor(), true);

            // ＝＝＝＝＝ 【歪みレイヤー②】 名前専用の空間変形を解除（Pop） ＝＝＝＝＝
            guiGraphics.pose().popPose();

            // ＝＝＝＝＝ 【歪みレイヤー①】 ボスバー全体の空間変形を解除（Pop） ＝＝＝＝＝
            guiGraphics.pose().popPose();

            // 次のボスのためにY座標をずらす
            yOffset += (int) (frameH * scale) - 1;

            // ★ 1体描き終わったので、カウンターを1つ増やす
            drawnCount++;
        }
    }
    private static int getVanillaBossBarCount(net.minecraft.client.gui.components.BossHealthOverlay overlay) {
        if (overlay == null) return 0;
        try {
            // Mojangマッピングにおける一般的なフィールド名 "events" の取得を試みる
            Field field = overlay.getClass().getDeclaredField("events");
            field.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) field.get(overlay);
            return map != null ? map.size() : 0;
        } catch (Exception e) {
            try {
                // 万が一環境やバージョンによってフィールド名が異なる場合、クラス内の唯一の Map 型フィールドを自動抽出するフォールバックさ
                for (Field f : overlay.getClass().getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Map<?, ?> map = (Map<?, ?>) f.get(overlay);
                        return map != null ? map.size() : 0;
                    }
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }
}