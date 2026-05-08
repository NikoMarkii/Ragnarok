
package com.niko.ragnarok.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.niko.ragnarok.effect.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * ヴォイドサイズ - 虚無を刈り取る大鎌
 *
 * 特殊能力「死神の刻印」:
 * - 右クリックで視線上の敵にVoid Reaperエフェクトを付与
 * - 1秒間（20tick）継続ダメージを与える
 * - クールダウン: 6秒（120tick）
 *
 * Better Combat互換:
 * - 攻撃範囲: 4.5ブロック
 * - 攻撃角度: 120度の薙ぎ払い
 * - 攻撃速度倍率: 0.8（重量感のある振り）
 */
public class VoidScythe extends TieredItem {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    // 特殊技の設定
    private static final double SPECIAL_RANGE = 8.0D; // 8ブロック先まで狙える
    private static final int SPECIAL_COOLDOWN = 120; // 6秒クールダウン
    private static final int EFFECT_DURATION = 20; // 1秒間（20tick）
    private static final int EFFECT_AMPLIFIER = 2; // エフェクトレベル3

    public VoidScythe(Properties properties) {
        super(Tiers.NETHERITE, properties);

        // 攻撃力: 基礎1.0 + 18.0 = 19.0ダメージ（強力な大鎌）
        // 攻撃速度: 4.0(基礎) - 3.2 = 0.8（重厚な振り）
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 18.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3.2D, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }
    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player) {
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        // クモの巣だけは剣と同じように即座に切り裂けるようにしておくのが「マイクラの武器」らしい振る舞いだね
        if (state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            return 15.0F;
        }
        return 0.1F;
    }

    // 正しい道具（ツルハシや斧）として扱わせないようにする
    @Override
    public boolean isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state) {
        return false;
    }

    // 剣と同様、ブロックを壊した時の耐久度減少を大きくする（オプション）
    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        if (state.getDestroySpeed(level, pos) != 0.0F) {
            stack.hurtAndBreak(2, entity, (e) -> {
                e.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            });
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // クールダウン中なら何もしない
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            // 視線上のターゲットを取得
            LivingEntity target = getEntityLookAt(player, SPECIAL_RANGE);

            if (target != null) {
                // 死神の刻印を発動
                activateReaperMark(level, player, target);

                // クールダウン開始
                player.getCooldowns().addCooldown(this, SPECIAL_COOLDOWN);

                return InteractionResultHolder.success(stack);
            } else {
                // ターゲットが見つからない場合のフィードバック
                player.displayClientMessage(
                        Component.translatable("message.ragnarok.void_scythe.no_target")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    /**
     * 死神の刻印を発動
     */
    private void activateReaperMark(Level level, Player player, LivingEntity target) {
        // サーバー側の処理
        if (level instanceof ServerLevel serverLevel) {
            // 虚無のエフェクトを付与
            target.addEffect(new MobEffectInstance(
                    ModMobEffects.VOID_REAPER_EFFECT.get(),
                    EFFECT_DURATION,
                    EFFECT_AMPLIFIER,
                    false,
                    true,
                    true
            ));

            // エフェクト音
            level.playSound(null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.8F
            );

            level.playSound(null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.WITHER_HURT,
                    SoundSource.PLAYERS,
                    0.7F,
                    0.5F
            );

            // プレイヤーへのフィードバック
            player.displayClientMessage(
                    Component.translatable("message.ragnarok.void_scythe.marked")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
            );

            // パーティクルエフェクト
            spawnReaperParticles(serverLevel, target);
        }
    }

    /**
     * 死神の刻印パーティクルを生成
     */
    private void spawnReaperParticles(ServerLevel level, LivingEntity target) {
        Vec3 targetPos = target.position();

        // 螺旋状のパーティクル
        for (int i = 0; i < 30; i++) {
            double angle = Math.toRadians(i * 12);
            double height = i * 0.1D;
            double radius = 1.5D - (i * 0.04D);

            double x = targetPos.x + Math.cos(angle) * radius;
            double y = targetPos.y + height;
            double z = targetPos.z + Math.sin(angle) * radius;

            level.sendParticles(
                    ParticleTypes.WITCH,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.0D
            );

            level.sendParticles(
                    ParticleTypes.SOUL,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.0D
            );
        }

        // 中心の爆発エフェクト
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                targetPos.x,
                targetPos.y + 1.0D,
                targetPos.z,
                15,
                0.5D, 0.5D, 0.5D,
                0.05D
        );
    }

    /**
     * プレイヤーの視線上のエンティティを取得
     */
    private LivingEntity getEntityLookAt(Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 reachVec = viewVec.scale(range);
        Vec3 targetPos = eyePos.add(reachVec);

        // 視線範囲の判定ボックス
        AABB searchBox = player.getBoundingBox()
                .expandTowards(reachVec)
                .inflate(1.5D);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eyePos,
                targetPos,
                searchBox,
                entity -> entity instanceof LivingEntity
                        && !entity.isSpectator()
                        && entity.isPickable()
                        && entity != player
        );

        if (hitResult != null && hitResult.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 通常攻撃時のエフェクト
        if (!target.level().isClientSide && target.level() instanceof ServerLevel serverLevel) {
            // 小規模なパーティクル
            Vec3 targetPos = target.position();
            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    targetPos.x,
                    targetPos.y + 1.0D,
                    targetPos.z,
                    5,
                    0.3D, 0.5D, 0.3D,
                    0.02D
            );
        }

        stack.hurtAndBreak(1, attacker, entity ->
                entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.ragnarok.void_scythe.desc1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ragnarok.void_scythe.desc2")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15; // エンチャント適性が高い
    }
}