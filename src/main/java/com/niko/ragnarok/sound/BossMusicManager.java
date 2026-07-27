package com.niko.ragnarok.sound;

import com.niko.ragnarok.entity.Boss_Monster;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent; // 追加
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ボスBGMの再生・切り替え・フェードアウトを管理するクライアント専用マネージャー。
 *
 * 毎tick、近くにいるBoss_Monster（サブクラス含む）のうち、自分（ローカルプレイヤー）を
 * ターゲットしているものを探し、見つかればそのBGMを再生し、いなくなればフェードアウトする。
 */
@Mod.EventBusSubscriber(modid = "ragnarok", value = Dist.CLIENT)
public final class BossMusicManager {

    private static final double SEARCH_RADIUS = 64.0D;

    private static BossMusicSoundInstance currentInstance;
    private static SoundEvent currentSound;

    private BossMusicManager() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            hardStop();
            return;
        }

        SoundEvent targetMusic = findBossMusicForLocalPlayer(mc);

        if (targetMusic == null) {
            // 自分を狙っているボスがいなくなった → フェードアウト開始
            if (currentInstance != null && !currentInstance.isFadingOut()) {
                currentInstance.requestFadeOut();
            }
        } else if (currentSound != targetMusic) {
            // 新しいボスのBGMに切り替える（今のがあればフェードアウトさせる）
            if (currentInstance != null) {
                currentInstance.requestFadeOut();
            }
            currentInstance = new BossMusicSoundInstance(targetMusic);
            currentSound = targetMusic;
            mc.getSoundManager().play(currentInstance);

            // ★追加：ボスBGMの再生開始時に、現在流れているバニラのBGMを強制停止する
            mc.getMusicManager().stopPlaying();
        }

        // フェードアウトが完了したインスタンスの後片付け
        if (currentInstance != null && currentInstance.isFinished()) {
            currentInstance = null;
            currentSound = null;
        }
    }

    // ★追加：ボスBGM再生中に新しく別のBGM（バニラのBGMなど）が鳴ろうとしたらキャンセルする
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (currentInstance != null && !currentInstance.isFinished()) {
            // MUSICカテゴリのサウンドで、かつ現在再生中のボスBGMインスタンスではない場合
            if (event.getSound() != null && event.getSound().getSource() == SoundSource.MUSIC) {
                if (event.getSound() != currentInstance) {
                    event.setSound(null); // サウンドの再生をキャンセルして流さない
                }
            }
        }
    }

    @Nullable
    private static SoundEvent findBossMusicForLocalPlayer(Minecraft mc) {
        List<Boss_Monster> bosses = mc.level.getEntitiesOfClass(
                Boss_Monster.class,
                mc.player.getBoundingBox().inflate(SEARCH_RADIUS)
        );

        for (Boss_Monster boss : bosses) {
            SoundEvent music = boss.getBossMusic();
            if (music != null && boss.isTargeting(mc.player)) {
                return music;
            }
        }
        return null;
    }

    private static void hardStop() {
        if (currentInstance != null) {
            Minecraft.getInstance().getSoundManager().stop(currentInstance);
        }
        currentInstance = null;
        currentSound = null;
    }
}