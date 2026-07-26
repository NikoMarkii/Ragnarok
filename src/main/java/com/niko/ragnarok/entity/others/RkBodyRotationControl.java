package com.niko.ragnarok.entity.others;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

/**
 * カスタム BodyRotationControl - Ragnarok Mod 共通制御
 * 歩行・走行時は体を移動方向へ即座に同期し、静止時はバニラの挙動にそのまま委譲する
 */
public class RkBodyRotationControl extends BodyRotationControl {

    private final Mob mob;

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
        } else {
            // 2. 停止時：バニラの挙動（頭が一定角度まで自由に回り、
            //    そのあと体がゆっくり追いつく自然な首振り）にそのまま任せる
            super.clientTick();
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
}