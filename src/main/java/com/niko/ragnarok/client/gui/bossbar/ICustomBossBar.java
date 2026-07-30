package com.niko.ragnarok.client.gui.bossbar;

import net.minecraft.resources.ResourceLocation;

public interface ICustomBossBar {
    // ベース画像（120x6）のテクスチャパス
    ResourceLocation getBossBarBaseTexture();

    // フレーム画像（オーバーレイ）のテクスチャパス
    ResourceLocation getBossBarOverlayTexture();

    // ボスの体力割合（0.0F ~ 1.0F）
    float getBossProgress();

    // ★ 追加：ボスバーを表示すべきかどうか（デフォルトはtrue）
    default boolean shouldShowBossBar() {
        return true;
    }

    // --- 描画用のサイズと位置調整 ---

    // フレーム画像の実際の幅と高さ
    int getFrameWidth();
    int getFrameHeight();

    // ゲージ本体（120x3）の左上を原点(0,0)としたときの、フレーム描画開始位置(X, Y)
    int getFrameOffsetX();
    int getFrameOffsetY();

    default float getBossBarScale() {
        return 1.5F;
    }
    default int getBossNameColor() {
        return 0xFFFFFF;
    }
}