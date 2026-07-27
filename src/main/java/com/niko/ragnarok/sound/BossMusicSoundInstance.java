package com.niko.ragnarok.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * ボスBGM専用のサウンドインスタンス。
 * ループ再生しつつ、フェードアウトのみを行う。再生時は最初から最大音量。
 * プレイヤーに張り付く形（位置減衰なし）で鳴らす。
 */
public class BossMusicSoundInstance extends AbstractTickableSoundInstance {

    private static final float FADE_STEP = 0.02F; // 1tickあたりの音量変化量（大きいほど速くフェードする）

    private boolean fadingOut = false;
    private boolean finished = false;

    public BossMusicSoundInstance(SoundEvent sound) {
        super(sound, SoundSource.MUSIC, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F; // ★変更：0からではなく、最初から最大音量(1.0F)で鳴らす
        this.attenuation = Attenuation.NONE; // 距離による減衰なし（BGMとして扱う）
    }

    /** フェードアウトを開始する。フェードアウトが終わるとisFinished()がtrueになる */
    public void requestFadeOut() {
        this.fadingOut = true;
    }

    public boolean isFadingOut() {
        return this.fadingOut;
    }

    /** フェードアウトが完了し、実質的に停止状態になったか */
    public boolean isFinished() {
        return this.finished;
    }

    @Override
    public void tick() {
        // プレイヤーに追従させる（位置ズレによる違和感を防ぐ）
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.x = mc.player.getX();
            this.y = mc.player.getY();
            this.z = mc.player.getZ();
        }

        // ★変更：フェードインの処理を削除し、フェードアウトの処理だけ残す
        if (this.fadingOut) {
            this.volume -= FADE_STEP;
            if (this.volume <= 0.0F) {
                this.volume = 0.0F;
                this.finished = true;
            }
        }
    }

    @Override
    public boolean canPlaySound() {
        return !this.finished;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}