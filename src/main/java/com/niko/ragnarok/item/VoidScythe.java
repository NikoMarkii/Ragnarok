package com.niko.ragnarok.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.niko.ragnarok.entity.Projectile.VoidSlashEntity;
import com.niko.ragnarok.entity.RagnarokEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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

public class VoidScythe extends TieredItem {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;
    private static final int SPECIAL_COOLDOWN = 120; // 6秒クールダウン

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
        // クモの巣だけは剣と同じように即座に切り裂ける
        if (state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            return 15.0F;
        }
        return 0.1F;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // ヒット時のパーティクル演出
        if (!target.level().isClientSide && target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    target.getX(), target.getY() + 1.0D, target.getZ(),
                    5, 0.3D, 0.5D, 0.3D, 0.02D);
        }
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return super.onEntitySwing(stack, entity);
    }

    // 特殊技（右クリック）：巨大斬撃を放つ
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        // クライアント側：アニメーション開始
        if (level.isClientSide) {
            triggerBetterCombat(player);
        } else {
            // サーバー側：即座に出さず、アニメーションに合わせて遅らせる
            // Better Combat の Scythe アニメーションなら 5～8 Tick (約0.25～0.4秒) 程度が目安だ
            startDelayedShoot(level, player, 6); // 6 Tick 遅らせる
            player.getCooldowns().addCooldown(this, SPECIAL_COOLDOWN);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void startDelayedShoot(Level level, Player player, int delay) {
        new Thread(() -> {
            try {
                // 1 Tick = 50ms なので、delay * 50 ミリ秒待機
                Thread.sleep(delay * 50);

                // サーバーのスレッドに戻して実行（安全のため）
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.getServer().execute(() -> {
                        if (player.isAlive()) {
                            shootSlash(serverLevel, player, true);
                        }
                    });
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void triggerBetterCombat(Player player) {
        if (player.level().isClientSide) {
            try {
                Class<?> apiClass = Class.forName("net.bettercombat.api.client.BetterCombatClient");
            } catch (Exception e) {
                player.swing(InteractionHand.MAIN_HAND);
            }
            net.minecraft.client.Minecraft.getInstance().options.keyAttack.setDown(true);

            new Thread(() -> {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                net.minecraft.client.Minecraft.getInstance().options.keyAttack.setDown(false);
            }).start();
        }
    }
    private void shootSlash(Level level, Player player, boolean isBig) {
        VoidSlashEntity slash = new VoidSlashEntity(RagnarokEntities.VOID_SLASH.get(), level);
        slash.setOwner(player);
        slash.setBig(true);

        Vec3 lookAngle = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();

        // 修正：高さを player.getY() + 0.2D まで下げる（ほぼ足元）
        // これで当たり判定（白い線）が地面スレスレを通るようになる
        Vec3 spawnPos = player.position().add(0, 1D, 0).add(horizontalLook.scale(1.2D));

        slash.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        slash.shootFromRotation(player, 0.0F, player.getYRot(), 0.0F, 1.2F, 0.0F);

        level.addFreshEntity(slash);

        // 音の演出
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    // 正しい道具（ツルハシや斧）として扱わせない
    @Override
    public boolean isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state) {
        return false;
    }

    // 剣と同様、ブロックを壊した時の耐久度減少を大きくする
    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        if (state.getDestroySpeed(level, pos) != 0.0F) {
            stack.hurtAndBreak(2, entity, (e) -> {
                e.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            });
        }
        return true;
    }

    /**
     * プレイヤーの視線上のエンティティを取得（将来的な拡張用に残す）
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