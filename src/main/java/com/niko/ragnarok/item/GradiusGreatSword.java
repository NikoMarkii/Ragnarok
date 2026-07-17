package com.niko.ragnarok.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class GradiusGreatSword extends SwordItem {

    private static final float PILLAR_DAMAGE = 15.0F;
    private static final double[] OFFSETS = { -0.35, 0.0, 0.35 };

    // 3秒は 60ティック (20tick/秒 × 3秒)
    private static final int CHARGE_TICKS = 60;
    private static final int COOLDOWN_TICKS = 150;

    public GradiusGreatSword(Properties properties) {
        super(Tiers.DIAMOND, 9, -3.1F, properties);
    }

    // --- ここから追加・変更箇所 ---

    /**
     * アイテムを構え続けられる最大時間を設定する。
     * 弓と同じく非常に長い時間を設定し、実質無制限にチャージできるようにする。
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    /**
     * チャージ中のプレイヤーのモーションを指定する。
     * BOW（弓を引く）のほか、SPEAR（トライデント）や BLOCK（盾のガード）なども選べるよ。
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    /**
     * 右クリックを押した瞬間の処理
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand) {

        // オフハンド無効
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        // クールダウン中は構えられないようにする
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }

        // プレイヤーにアイテムの「使用（チャージ）」を開始させる
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    /**
     * 右クリックを離した瞬間の処理
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        // プレイヤー以外が使った場合は無視
        if (!(livingEntity instanceof Player player)) return;

        // どれだけ長く構えていたかを計算する
        // getUseDuration(最大時間) - timeCharged(残りの時間) = 構えていた時間(ティック)
        int duration = this.getUseDuration(stack) - timeCharged;

        // チャージ時間が規定値（60ティック＝3秒）に達しているか判定
        if (duration >= CHARGE_TICKS) {
            if (!level.isClientSide()) {
                spawnFirePillarLines(level, player);

                // 前回の修正：Level#playSound を使い、第一引数に null を指定して自分にも聞こえるようにする
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0F, 0.7F);
            }

            // 技を放った後にクールダウンを付与
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        } else {
            // チャージが足りずに不発に終わった場合の処理（必要であれば）
            // 例：小さな煙を出したり、不発音を鳴らしたりする
        }
    }
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) return;

        int duration = this.getUseDuration(stack) - remainingUseDuration;

        // チャージ完了の瞬間（60tick目）に音を鳴らす
        if (duration == CHARGE_TICKS) {
            level.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS,
                    1.0F, 1.5F
            );
        }
    }

    // --- ここから下は元のコード（音の修正版） ---

    private void spawnFirePillarLines(Level level, Player player) {
        if (!(level instanceof ServerLevel sl)) return;

        Vec3 look = player.getLookAngle();
        double angle = Math.atan2(look.z, look.x);

        List<DelayedPillar> pillars = new ArrayList<>();

        for (double offset : OFFSETS) {
            double a = angle + offset;
            double dx = Math.cos(a);
            double dz = Math.sin(a);

            int delay = 0;
            for (double dist = 0; dist <= 20.0; dist += 4.0) {
                pillars.add(new DelayedPillar(
                        delay,
                        player.getX() + dx * dist,
                        player.getY(),
                        player.getZ() + dz * dist
                ));
                delay += 4;
            }
        }

        for (DelayedPillar pillar : pillars) {
            sl.getServer().tell(new net.minecraft.server.TickTask(
                    sl.getServer().getTickCount() + pillar.delay(),
                    () -> spawnSingleFirePillar(sl, pillar.x(), pillar.y(), pillar.z(), player)
            ));
        }
    }

    private void spawnSingleFirePillar(
            ServerLevel sl,
            double x, double baseY, double z,
            Player owner) {

        if (!owner.isAlive()) return;

        sl.sendParticles(ParticleTypes.EXPLOSION,
                x, baseY + 0.5, z, 1, 0, 0, 0, 0);

        double height = 7 + sl.getRandom().nextDouble() * 3;

        for (double y = 0; y < height; y += 0.2) {
            double radius = 0.8 + (y / height) * 0.4;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    x, baseY + y, z, 12, radius, 0.05, radius, 0);
            sl.sendParticles(ParticleTypes.SOUL,
                    x, baseY + y, z, 6, radius * 0.7, 0.05, radius * 0.7, 0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    x, baseY + y, z, 3, 0.2, 0.05, 0.2, 0);
        }

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                x, baseY + height, z, 40, 1.2, 0.3, 1.2, 0.05);

        AABB hitBox = new AABB(x - 1.0, baseY, z - 1.0,
                x + 1.0, baseY + height, z + 1.0);

        sl.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != owner && e.isAlive()
        ).forEach(e -> {
            e.invulnerableTime = 0;
            e.hurt(owner.damageSources().playerAttack(owner), PILLAR_DAMAGE);
            Vec3 kb = e.position().subtract(owner.position())
                    .normalize().scale(2.0);
            e.setDeltaMovement(kb.x, 1.0, kb.z);
            e.hurtMarked = true;
        });

        // 前回の修正：ServerLevel の playSound を使用する
        sl.playSound(null, x, baseY, z,
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.5F, 0.7F);
    }

    private record DelayedPillar(int delay, double x, double y, double z) {}
}