
package com.niko.ragnarok.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;


public class VoidReaperEffect extends MobEffect {

    // ベースダメージ
    private static final float BASE_DAMAGE = 2.0F;
    // アンプリファイア1レベルあたりの追加ダメージ
    private static final float DAMAGE_PER_LEVEL = 1.0F;

    public VoidReaperEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity target, int amplifier) {
        if (target.level().isClientSide) {
            return;
        }

        // ダメージ計算
        float damage = BASE_DAMAGE + (amplifier * DAMAGE_PER_LEVEL);

        // 修正：魔法ダメージ、あるいは汎用ダメージに変更して無敵時間を貫通しやすくする
        // 奈落ダメージにこだわりたい場合は fellOutOfWorld() のままでも良いが、
        // 以下の「無敵時間リセット」を徹底する必要がある
        DamageSource voidDamage = target.damageSources().magic();

        // 核心的な修正：ダメージを与える「直前」に無敵時間を0にする
        target.invulnerableTime = 0;

        // ダメージを与える。もし false が返ってきたら何らかの耐性で弾かれている
        boolean success = target.hurt(voidDamage, damage);

        if (success) {
            // ダメージが通った直後、次の Tick でもダメージが通るように無敵時間を強制的にリセットし続ける
            target.invulnerableTime = 0;
        }

        // ビジュアルエフェクト（以下、ニコ氏の素晴らしいコードを継続）
        if (target.level() instanceof ServerLevel serverLevel) {
            spawnEffectParticles(serverLevel, target);

            // ダメージ音（小さめ）
            if (target.getRandom().nextFloat() < 0.3F) {
                target.level().playSound(null,
                        target.getX(), target.getY(), target.getZ(),
                        SoundEvents.SOUL_ESCAPE,
                        target.getSoundSource(),
                        0.3F,
                        1.5F + target.getRandom().nextFloat() * 0.5F
                );
            }
        }
    }

    /**
     * エフェクトパーティクルを生成
     */
    private void spawnEffectParticles(ServerLevel level, LivingEntity target) {
        Vec3 targetPos = target.position();
        double height = target.getBbHeight();

        // 魂のパーティクル（上昇）
        for (int i = 0; i < 3; i++) {
            double offsetX = (target.getRandom().nextDouble() - 0.5D) * 0.8D;
            double offsetZ = (target.getRandom().nextDouble() - 0.5D) * 0.8D;
            double offsetY = target.getRandom().nextDouble() * height;

            level.sendParticles(
                    ParticleTypes.SOUL,
                    targetPos.x + offsetX,
                    targetPos.y + offsetY,
                    targetPos.z + offsetZ,
                    1,
                    0, 0.1D, 0,
                    0.02D
            );
        }

        // ウィッチパーティクル（周囲）
        for (int i = 0; i < 2; i++) {
            double angle = target.getRandom().nextDouble() * Math.PI * 2;
            double radius = 0.5D;

            double x = targetPos.x + Math.cos(angle) * radius;
            double z = targetPos.z + Math.sin(angle) * radius;
            double y = targetPos.y + target.getRandom().nextDouble() * height;

            level.sendParticles(
                    ParticleTypes.WITCH,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.0D
            );
        }

        // たまに強いエフェクト
        if (target.getRandom().nextFloat() < 0.15F) {
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    targetPos.x,
                    targetPos.y + height * 0.5D,
                    targetPos.z,
                    5,
                    0.3D, 0.3D, 0.3D,
                    0.01D
            );
        }
    }

    // 修正：1.20.1等ではこのメソッド名が正解だ
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean isBeneficial() {
        return false;
    }
}
