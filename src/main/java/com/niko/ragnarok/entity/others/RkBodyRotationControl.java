package com.niko.ragnarok.entity.others;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

/**
 * カスタム BodyRotationControl - Ragnarok Mod 共通制御
 * 歩行・走行時の即時体向き同期と、頭部一定以上旋回（15度）時の即時追従を行う
 */
public class RkBodyRotationControl extends BodyRotationControl {

    private final Mob mob;
    private int headStableTimer;
    private float lastHeadY;

    public RkBodyRotationControl(Mob mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public void clientTick() {
        if (this.isMoving()) {
            // 1. 歩行/走行時：体の向き（yBodyRot）を移動方向（yRot）へ即座に合わせる
            this.mob.yBodyRot = this.mob.getYRot();

            // 可動域（首の可動限界）を超えないよう頭の向きを補正
            this.rotateHeadIfNecessary();

            this.lastHeadY = this.mob.yHeadRot;
            this.headStableTimer = 0;
        } else {
            // 2. 停止時：前回の頭の向きから 15度以上 動いたか判定
            if (Math.abs(this.mob.yHeadRot - this.lastHeadY) > 15.0F) {
                // タイマーリセット
                this.headStableTimer = 0;
                this.lastHeadY = this.mob.yHeadRot;

                // 可動域の限界を超えない範囲で体を即座に向かせる
                this.rotateBodyIfNecessary();
            } else {
                // 15度未満のわずかな動きの場合はタイマーを進め、10tick経過で追従
                this.headStableTimer++;
                if (this.headStableTimer > 10) {
                    this.rotateBodyIfNecessary();
                }
            }
        }
    }

    /**
     * モブが移動中（歩行・走行）かどうか判定
     */
    private boolean isMoving() {
        double dx = this.mob.getX() - this.mob.xo;
        double dz = this.mob.getZ() - this.mob.zo;
        return (dx * dx + dz * dz) > 2.500000277905201E-7D;
    }

    /**
     * 体の向きに対して頭が可動域（getMaxHeadYRot）を超えていたら補正する
     */
    private void rotateHeadIfNecessary() {
        this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, (float) this.mob.getMaxHeadYRot());
    }

    /**
     * 頭の向きに対して体が可動域を超えていたら即座に補正する
     */
    private void rotateBodyIfNecessary() {
        this.mob.yBodyRot = Mth.rotateIfNecessary(this.mob.yBodyRot, this.mob.yHeadRot, (float) this.mob.getMaxHeadYRot());
    }
}