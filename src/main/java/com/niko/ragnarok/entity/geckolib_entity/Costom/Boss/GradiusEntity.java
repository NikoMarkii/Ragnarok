package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.client.gui.bossbar.ICustomBossBar;
import com.niko.ragnarok.entity.Boss_Monster;
import com.niko.ragnarok.entity.Projectile.BlueFireballEntity;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostKnightEntity;
import com.niko.ragnarok.entity.others.RkBodyRotationControl;
import com.niko.ragnarok.entity.others.RkCombatUtil;
import com.niko.ragnarok.entity.others.RkSmoothMoveControl;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import com.niko.ragnarok.network.RagnarokNetwork;
import com.niko.ragnarok.network.ScreenShakePacket;
import com.niko.ragnarok.sound.RagnarokSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerBossEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

/**
 * グラディウス - Ragnarokモッドの最初のボス
 *
 * 攻撃パターン:
 * - attack1 : 薙ぎ払い (発生30tick, ターゲットをノックバック)
 * - attack2 : 叩きつけ (発生30tick, 扇状ショックウェーブ)
 * - attack3 : 踏みつけ (発生20tick, 周囲360度衝撃波)
 * - summon_attack : 召喚    (発生30tick, 円形にゾンビを召喚)
 * - charge     : 突進      (charge_start→charge_loop→charge_end)
 * - death      : 死亡アニメーション
 *
 * ────────────────────────────────────────────────────────────
 * ■ このクラス全体の設計方針（なぜこう作っているか）
 * ────────────────────────────────────────────────────────────
 *
 * 1) 「サーバー側の状態」と「見た目（アニメーション）」を分離している
 *    Minecraftは、ワールドの実際のロジック(当たり判定・ダメージ・移動)を計算する
 *    「サーバー側」と、それを画面に表示するだけの「クライアント側」に分かれている。
 *    しかし通常、モブのAI(Goal)はサーバー側でしか動かない。
 *    そのため、クライアント側のアニメーション再生ロジックがGoalの内部変数を
 *    直接参照することはできない（クライアント側では絶対に更新されないため）。
 *    これを解決するために、「今どの攻撃をしていて、経過tickは何か」を表す値を
 *    ATTACK_STATE / CHARGE_PHASE / GUARD_PHASE のような
 *    SynchedEntityData（EntityDataAccessor）としてサーバー→クライアントへ
 *    自動同期し、アニメーションコントローラーはそちらだけを見て再生内容を決める。
 *    ＝「Goalの内部状態を直接見ない、同期された値だけを信頼する」が鉄則。
 *
 * 2) 攻撃・移動・クールダウンを1つのGoal（GradiusAttackGoal）にまとめている
 *    「移動用Goal」「攻撃用Goal」を分けるやり方も可能だが、優先度とフラグ(Flag.MOVE等)
 *    の取り合いで、思ったとおりに制御を渡し合わせるのが難しくなりがち
 *    （実際、開発の過程で「別Goalに移動を任せたら、優先度の高い攻撃Goalが
 *    　常にFlagを握ったままで一切出番が来なかった」というバグが起きた）。
 *    そのため、1つのGoalのtick()の中で「クールダウン中か」「攻撃中か」
 *    「間合いの中か外か」を毎tick判定し、その場で移動 or 攻撃を選ぶ、
 *    という単純な状態機械（ステートマシン）にしている。
 *
 * 3) forceFinishAttack というフラグで「攻撃を出したら最後まで再生する」を保証している
 *    素直に実装すると、詠唱/攻撃モーションの途中でターゲットが死んだり
 *    見失ったりした瞬間に、Goalが canUse()==false と判断されて強制終了し、
 *    アニメーションが再生途中でカクッと止まる（不自然）。
 *    それを防ぐため、攻撃を開始した瞬間に forceFinishAttack=true にしておき、
 *    「たとえターゲットがいなくなっても、このフラグが立っている間はGoalを離さない」
 *    というルールを canUse()/tick() に入れている。
 *    ただし、これを入れっぱなしにすると「二度とターゲットが見つからない」場合に
 *    永久にその場で固まる新しいバグを生むので、
 *    noTargetFreezeTicks で「一定時間探しても見つからなければ諦める」という
 *    タイムアウトを必ずセットで入れている。
 *
 * 4) MoveControl / BodyRotationControl を自作のものに差し替えている
 *    バニラのMoveControlは、パスの再計算のたびに一瞬で（最大90度/tick）
 *    体の向きを変えてしまい、近距離ですれ違う時などに不自然にクルッと
 *    反転して見えることがある。RkSmoothMoveControl は、この回転速度に
 *    上限をかけて滑らかにしつつ、バニラのジャンプ・段差処理はそのまま活かす
 *    （＝内部ロジックを丸ごと再実装するのではなく、super.tick()に処理を
 *    委譲した上で「回転量だけ」後から制限する、というリスクの低いやり方）。
 * ────────────────────────────────────────────────────────────
 */
public class GradiusEntity extends Boss_Monster implements GeoEntity, ICustomBossBar {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ──────────────────────────────────────────
    // データシンク
    // ──────────────────────────────────────────
    // ↓ すべて「サーバーで計算した状態を、描画を担当するクライアントに配る箱」。
    //   Goal（AI）はサーバーでしか動かないので、アニメーションの分岐に使う値は
    //   必ずここを経由させる。逆に言うと、ここに乗っていない値は
    //   クライアント側のアニメーションコントローラーからは絶対に見えない。
    /**
     * 0=なし  1~3=通常攻撃  4=召喚  5=突進中  6=突進終了処理
     */
    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(GradiusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_DYING =
            SynchedEntityData.defineId(GradiusEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * charge フェーズ: 0=なし 1=start 2=loop 3=end
     */
    private static final EntityDataAccessor<Integer> CHARGE_PHASE =
            SynchedEntityData.defineId(GradiusEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> GUARD_PHASE =
            SynchedEntityData.defineId(
                    GradiusEntity.class,
                    EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> JUMP_SLAM_PHASE =
            SynchedEntityData.defineId(
                    GradiusEntity.class,
                    EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> PHASE2 =
            SynchedEntityData.defineId(GradiusEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_STANDBY =
            SynchedEntityData.defineId(GradiusEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_STANDBY_ENDING =
            SynchedEntityData.defineId(GradiusEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_PHASE2_COLOR =
            SynchedEntityData.defineId(GradiusEntity.class, EntityDataSerializers.BOOLEAN);

    // ──────────────────────────────────────────
    // 内部タイマー
    // ──────────────────────────────────────────
    private int customDeathTime = 0;
    private static final int DEATH_DURATION = 50; // 2.5秒

    private int previousAttackState = 0;

    private boolean wasMoving = false;
    private boolean playingWalkStop = false;

    private int guardTimer = 0;
    private boolean guarding = false;

    private boolean awakening = false;
    private int awakeningTimer = 0;

    private ServerPlayer lastAttacker;

    private final Queue<Vec3> trail = new ArrayDeque<>();

    public int getAwakeningTimer() {
        return awakeningTimer;
    }

    // 待機状態フラグ（ダンジョン配置時のみtrue）
    private boolean standby = false;
    private boolean standbyEnding = false;
    private int standbyEndTimer = 0;
    private static final int STANDBY_END_DURATION = 70;
    private static final double STANDBY_DETECT_RANGE = 7.0; // 感知距離（ブロック）

    private LivingEntity pendingTarget = null;

    public boolean isStandbyEnding() {
        return this.entityData.get(IS_STANDBY_ENDING);
    }

    private void setStandbyEnding(boolean value) {
        this.standbyEnding = value;
        this.entityData.set(IS_STANDBY_ENDING, value);
    }
    @Override
    protected boolean isInStandbyState() {
        return this.standby || this.standbyEnding;
    }

    // ──────────────────────────────────────────
    // ボスバー
    // ──────────────────────────────────────────
    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.NOTCHED_10);

    // ──────────────────────────────────────────
    // コンストラクタ
    // ──────────────────────────────────────────
    public GradiusEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.bossEvent.setVisible(false); // ボスバーは「初めて視認された時」に表示するため、生成直後は隠しておく
        // バニラのMoveControlは急な回頭をするため、独自のものに差し替える。
        // 第2引数(8.0F)は「1tickあたり最大8度まで回転してよい」という上限。
        // 小さいほど滑らかに曲がるが、追跡の反応が遅く感じられるので、
        // モブの体格・移動速度に合わせて調整するパラメータ。
        this.moveControl = new RkSmoothMoveControl(this, 8.0F);
    }

    private void sendScreenShake(float intensity, int duration) {
        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        // 範囲内のプレイヤーにパケット送信
        for (net.minecraft.server.level.ServerPlayer player :
                sl.getPlayers(p -> p.distanceToSqr(this) < 64 * 64)) {
            RagnarokNetwork.CHANNEL.sendTo(
                    new ScreenShakePacket(intensity, duration),
                    player.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    // ──────────────────────────────────────────
    // 属性
    // ──────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D); // 突進中に押されない
    }

    // ──────────────────────────────────────────
    // ゴール登録
    // ──────────────────────────────────────────
    @Override
    protected void registerGoals() {
        // 数字は優先度（小さいほど優先）。同じFlag(MOVE/LOOK等)を要求するGoal同士は
        // 優先度が高い方だけが動き、低い方は自動的に待機状態になる。
        this.goalSelector.addGoal(0, new FloatGoal(this)); // 溺れ防止（水中で浮く）。常に最優先。
        this.goalSelector.addGoal(1, new GradiusAttackGoal(this, 1.1D)); // 移動・攻撃をまとめて担当（詳細はクラス内コメント参照）
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D)); // ターゲットがいない時の徘徊
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F)); // 非戦闘時に近くのプレイヤーを見る
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this)); // 殴られたら殴った相手を最優先でターゲットにする
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                // クリエイティブ/スペクテイターのプレイヤーは殴っても無敵で意味が無く、
                // 見た目上「攻撃してこないのにずっと追いかけてくる」という不自然な挙動になるため除外する
                livingEntity -> !(livingEntity instanceof Player player
                        && (player.isCreative() || player.isSpectator()))
        ));

    }

    // ──────────────────────────────────────────
    // データシンク初期化
    // ──────────────────────────────────────────
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_STATE, 0);
        this.entityData.define(IS_DYING, false);
        this.entityData.define(CHARGE_PHASE, 0);
        this.entityData.define(PHASE2, false);
        this.entityData.define(JUMP_SLAM_PHASE, 0);
        this.entityData.define(GUARD_PHASE, 0);
        this.entityData.define(IS_STANDBY, false);
        this.entityData.define(IS_STANDBY_ENDING, false);
        this.entityData.define(IS_PHASE2_COLOR, false);

    }

    // ──────────────────────────────────────────
    // ゲッター / セッター
    // ──────────────────────────────────────────
    public int getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    public void setAttackState(int s) {
        this.entityData.set(ATTACK_STATE, s);
    }

    public boolean isActuallyDying() {
        return this.entityData.get(IS_DYING);
    }

    private void setDying(boolean b) {
        this.entityData.set(IS_DYING, b);
    }

    public int getChargePhase() {
        return this.entityData.get(CHARGE_PHASE);
    }

    public void setChargePhase(int p) {
        this.entityData.set(CHARGE_PHASE, p);
    }

    public int getJumpSlamPhase() {
        return this.entityData.get(JUMP_SLAM_PHASE);
    }

    public void setJumpSlamPhase(int phase) {
        this.entityData.set(JUMP_SLAM_PHASE, phase);
    }

    public int getGuardPhase() {
        return entityData.get(GUARD_PHASE);
    }

    public void setGuardPhase(int phase) {
        entityData.set(GUARD_PHASE, phase);
    }

    public boolean isPhase2() {
        return this.entityData.get(PHASE2);
    }

    public void setPhase2(boolean value) {
        this.entityData.set(PHASE2, value);
    }

    public boolean isPhase2Color() {
        return this.entityData.get(IS_PHASE2_COLOR);
    }

    private void setPhase2Color(boolean value) {
        this.entityData.set(IS_PHASE2_COLOR, value);
    }

    // ──────────────────────────────────────────
    // 右手に大剣を持たせる
    // ──────────────────────────────────────────
    @Override
    protected void populateDefaultEquipmentSlots(
            net.minecraft.util.RandomSource random,
            net.minecraft.world.DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Ragnarok_mainItems.GRADIUS_GREAT_SWORD.get()));
        this.setDropChance(
                EquipmentSlot.MAINHAND,
                0.0F
        );
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effectInstance) {
        // 有益なエフェクト（カテゴリがBENEFICIAL）のみ受け付ける
        return effectInstance.getEffect().getCategory()
                == net.minecraft.world.effect.MobEffectCategory.BENEFICIAL;
    }

    @Override
    protected net.minecraft.world.entity.ai.control.BodyRotationControl createBodyControl() {
        return new RkBodyRotationControl(this);
    }

    // ──────────────────────────────────────────
    // 死亡処理（カスタム：アニメーションが終わってから消滅）
    // ──────────────────────────────────────────
    // ★重要な設計判断：あえて super.die(source) を一切呼んでいない。
    //   バニラのLivingEntity#die()は、呼んだ瞬間に
    //   ・ロットテーブルからのドロップ
    //   ・死亡アニメーション状態への遷移
    //   ・経験値のドロップ
    //   などを全部その場でまとめて行ってしまう。
    //   しかしこのボスは「死亡モーションを最後まで再生してから消える」演出にしたいので、
    //   即座に色々処理されると都合が悪い。
    //   そこで super.die() は一切呼ばず、代わりに isDying フラグだけを立てて、
    //   実際のドロップ・消滅処理はすべて aiStep() 側で
    //   customDeathTime がアニメーションの長さに達した時に自分で行う
    //   （tickDeath()もバニラの処理を止めるために空実装で上書きしている）。
    //   ＝ 「バニラの自動処理に相乗りしてから後始末する」のではなく
    //   　「最初から自動処理を一切発生させない」というアプローチ。
    @Override
    public void die(DamageSource source) {

        if (!this.level().isClientSide
                && !this.isActuallyDying()) {

            // ── 待機状態を強制解除 ──
            this.setStandby(false);
            this.setStandbyEnding(false);

            // ★トドメを刺したプレイヤーを取得して lastAttacker に保存
            if (source.getEntity() instanceof ServerPlayer sp) {
                this.lastAttacker = sp;
            }

            if (this.lastAttacker != null) {
                this.lastAttacker.displayClientMessage(
                        Component.translatable(
                                "message.ragnarok.gradius.death",this.lastAttacker.getName().getString()
                        ).withStyle(ChatFormatting.GOLD),
                        true
                );

                // ★ここで進捗をプレイヤーに直接付与するメソッドを呼び出す！
                this.grantDefeatAdvancement(this.lastAttacker);
            }

            this.setDying(true);

            this.customDeathTime = 0;

            this.setAttackState(0);
            this.setChargePhase(0);
        }
    }
    private void grantDefeatAdvancement(ServerPlayer player) {
        if (player.getServer() == null) return;

        ResourceLocation advancementId = ResourceLocation.fromNamespaceAndPath("ragnarok", "defeat_gradius");
        net.minecraft.advancements.Advancement advancement = player.getServer().getAdvancements().getAdvancement(advancementId);

        if (advancement != null) {
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            }
        }
    }

    public BossEvent.BossBarColor getBossBarColor() {
        return this.bossEvent.getColor();
    }

    @Override
    protected void tickDeath() { /* バニラ無効 */ }
    // ↑ バニラのtickDeath()は「死亡してから一定tickでdiscard(消滅)する」処理そのもの。
    //   die()でsuper.die()を呼んでいない以上、本来tickDeath()が呼ばれることはあまりないが、
    //   念のためバニラの消滅処理が紛れ込まないよう、丸ごと空にして無効化している。
    //   消滅タイミングは前述の通り aiStep() 側で自前管理する。

    @Override
    public boolean isDeadOrDying() {
        return this.isActuallyDying() || super.isDeadOrDying();
    }
    // ↑ 「体力が0になった直後〜死亡モーションが終わるまで」の間も
    //   isDeadOrDying()==true にしておかないと、他の判定（例えばターゲットに
    //   選ばれ続けてしまう等）と噛み合わなくなるための調整。

    @Override
    public boolean shouldDropExperience() {
        return !this.isBaby();
    }

    public boolean isStandby() {
        return this.entityData.get(IS_STANDBY);
    }

    public void setStandby(boolean value) {
        this.entityData.set(IS_STANDBY, value);
        this.standby = value;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Standby", this.isStandby());
        tag.putBoolean("StandbyEnding", this.standbyEnding);
        tag.putInt("StandbyEndTimer", this.standbyEndTimer);
        tag.putBoolean("Phase2", this.isPhase2());
        tag.putBoolean("Awakening", this.awakening);
        tag.putInt("AwakeningTimer", this.awakeningTimer);
        tag.putBoolean("IsDying", this.isActuallyDying());
        tag.putInt("CustomDeathTime", this.customDeathTime);
        tag.putBoolean("Phase2Color", this.isPhase2Color());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Standby")) {
            boolean sb = tag.getBoolean("Standby");
            this.standby = sb;
            this.entityData.set(IS_STANDBY, sb);
        }
        if (tag.contains("StandbyEnding")) {
            setStandbyEnding(tag.getBoolean("StandbyEnding"));
        }
        this.standbyEndTimer = tag.getInt("StandbyEndTimer");

        if (tag.contains("Phase2")) {
            boolean p2 = tag.getBoolean("Phase2");
            this.entityData.set(PHASE2, p2);
            // NBT読み込み時点でもbossEventに色を反映
            // （startSeenByPlayerより前に呼ばれる場合の保険）
            if (p2 && !this.level().isClientSide()) {
                this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
            }
        }
        if (tag.contains("Phase2Color")) {
            setPhase2Color(tag.getBoolean("Phase2Color"));
        }
        if (tag.contains("Awakening")) {
            this.awakening = tag.getBoolean("Awakening");
            this.awakeningTimer = tag.getInt("AwakeningTimer");
        }
        // ── 追加：死亡状態を復元 ──
        if (tag.contains("IsDying")) {
            boolean dying = tag.getBoolean("IsDying");
            this.entityData.set(IS_DYING, dying);
            if (dying) {
                this.customDeathTime = tag.getInt("CustomDeathTime");
                // 死亡中はボスバーを0に
                this.bossEvent.setProgress(0.0F);
            }
        }
    }

    public static GradiusEntity createForDungeon(EntityType<? extends Monster> type, Level level) {
        GradiusEntity gradiusEntity = new GradiusEntity(type, level);
        gradiusEntity.setStandby(true);
        return gradiusEntity;
    }

    // ──────────────────────────────────────────
    // aiStep（毎tick）
    // ──────────────────────────────────────────
    @Override
    public void aiStep() {
        super.aiStep();

        // ── 待機状態処理 ──
        if ((standby || standbyEnding) && !this.isActuallyDying()) {
            tickStandby();
            return; // 待機中はAI・攻撃・ボスバー更新をスキップ
        }

        trail.add(this.position());
        if (trail.size() > 6) trail.poll();

        if (!isPhase2()
                && !awakening
                && !isActuallyDying()
                && this.getHealth() > 0
                && this.getHealth() <= this.getMaxHealth() * 0.5F) {

            awakening = true;
            awakeningTimer = 0;

            this.setAttackState(99); // awake専用
            this.getNavigation().stop();
            if (!this.level().isClientSide
                    && lastAttacker != null
                    && lastAttacker.isAlive()) {

                lastAttacker.displayClientMessage(
                        Component.translatable("message.ragnarok.gradius.awakening")
                                .withStyle(ChatFormatting.DARK_RED),
                        true
                );
            }
        }
        if (awakening) {

            awakeningTimer++;

            this.setDeltaMovement(Vec3.ZERO);

            if (awakeningTimer == 25) {
                doAwakeningBurst();
                this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
                setPhase2Color(true); // ← 追加（クライアントに25tick目で同期）
            }

            if (awakeningTimer >= 45) {

                awakening = false;
                setPhase2(true);

                this.setAttackState(0);
            }
            if (this.level() instanceof ServerLevel sl
                    && this.tickCount % 2 == 0) {

                sl.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(),
                        this.getY() + 1.5,
                        this.getZ(),
                        8,
                        1.0,
                        1.0,
                        1.0,
                        0.05
                );
            }

            return;
        }
        // ──── 死亡カウント ────
        // super.die()を呼んでいないので、ここが実質的な「死亡シーケンス本体」になる。
        // customDeathTimeを毎tick進め、DEATH_DURATION(=2.5秒)経ったら
        // ドロップ・経験値・演出・discard(消滅)をまとめて実行する。
        if (this.isActuallyDying()) {
            this.customDeathTime++;
            // hurtTime/hurtDurationはバニラの「被ダメ時の赤い明滅」を制御するフィールド。
            // 死んでいる間は毎tick最大値に上書きし続けることで、
            // 時間経過で自然に消える処理を上書きし、死亡演出中ずっと赤いままにしている。
            this.hurtTime = this.hurtDuration;

            if (!this.level().isClientSide && this.customDeathTime % 5 == 0) {
                spawnDeathParticles();
            }

            // ── 死亡中もボスバーのゲージを0のまま表示し続ける ──
            if (!this.level().isClientSide) {
                this.bossEvent.setProgress(0.0F);
                // 色はそのまま保持（phase2ならBLUE）
            }

            if (this.customDeathTime >= DEATH_DURATION && !this.level().isClientSide) {
                if (this.level() instanceof ServerLevel sl) {

                    // ── 正しい死亡原因を渡す ──
                    // 通常のダメージソース(source)ではなく、
                    // 直近でトドメを刺したプレイヤー基準のDamageSourceを
                    // 作り直している。ロットテーブルの条件（"killer_entity"等）や
                    // 実績の判定を正しく機能させるための工夫。
                    DamageSource deathSource = this.lastAttacker != null
                            ? this.damageSources().playerAttack(this.lastAttacker)
                            : this.damageSources().generic();

                    this.dropFromLootTable(deathSource, true);

                    this.level().addFreshEntity(
                            new net.minecraft.world.entity.ExperienceOrb(
                                    sl,
                                    this.getX(), this.getY(), this.getZ(),
                                    this.xpReward));

                    sl.sendParticles(
                            ParticleTypes.SOUL,
                            this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                            25,
                            this.getBbWidth() * 0.5D, this.getBbHeight() * 0.5D, this.getBbWidth() * 0.5D,
                            0.05D
                    );
                    this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.8F);
                }

                // discard(即消滅)ではなく、死因を伴うremove(KILLED)を使うことで、
                // 他のMod/実績システムが「討伐された」と正しく認識できるようにしている。
                this.remove(RemovalReason.KILLED);
            }
            return;
        }

        // ──── ボスバー進捗 ────
        if (!this.level().isClientSide) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    private void tickStandby() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        if (standbyEnding) {
            standbyEndTimer++;

            if (standbyEndTimer == 50 && !this.level().isClientSide()) {
                spawnStandbyEndBurst();
                this.playSound(SoundEvents.WITHER_SPAWN, 2.0F, 0.8F);
            }

            // 70tick目：戦闘開始
            if (standbyEndTimer >= STANDBY_END_DURATION) {
                setStandbyEnding(false); // ← 変更
                this.standbyEndTimer = 0;
                if (pendingTarget != null) {
                    this.setTarget(pendingTarget);
                    pendingTarget = null;
                }
                if (!this.level().isClientSide()) {
                    this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
                }
            }
            return;
        }

        // standby中：プレイヤーが近づいたか監視
        if (!this.level().isClientSide()) {
            Player nearest = this.level().getNearestPlayer(
                    this, STANDBY_DETECT_RANGE);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                this.wakeUpFromStandby(nearest);
            }
        }
    }

    private void spawnStandbyEndBurst() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        // 円状に青い炎を爆発させる（半径ごとに遅延）
        for (int radius = 1; radius <= 8; radius++) {
            final double r = radius;
            sl.getServer().tell(new net.minecraft.server.TickTask(
                    sl.getServer().getTickCount() + radius,
                    () -> {
                        for (int i = 0; i < 24; i++) {
                            double angle = Math.PI * 2 * i / 24;
                            sl.sendParticles(
                                    ParticleTypes.SOUL_FIRE_FLAME,
                                    this.getX() + Math.cos(angle) * r,
                                    this.getY() + 0.5,
                                    this.getZ() + Math.sin(angle) * r,
                                    1, 0, 0.3, 0, 0.05
                            );
                        }
                        // 内側から外側へ炎が広がる演出
                        sl.sendParticles(
                                ParticleTypes.SOUL,
                                this.getX(),
                                this.getY() + 1.0,
                                this.getZ(),
                                10, r * 0.5, 0.5, r * 0.5, 0.05
                        );
                    }
            ));
        }

        // 大爆発パーティクル（即時）
        sl.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 1.0, this.getZ(),
                80, 2.0, 1.0, 2.0, 0.15
        );
        sl.sendParticles(
                ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 1.0, this.getZ(),
                5, 1.0, 0.5, 1.0, 0.1
        );
    }

    private void wakeUpFromStandby(LivingEntity target) {
        this.standby = false;
        this.entityData.set(IS_STANDBY, false);
        setStandbyEnding(true); // ← 変更
        this.standbyEndTimer = 0;
        this.pendingTarget = target;
        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }


    // ──────────────────────────────────────────
    // ボスバー：プレイヤー追加・削除
    // ──────────────────────────────────────────
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
        if (this.isPhase2() || this.isPhase2Color()) {
            this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
        }
    }

    @Override
    public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public SoundEvent getBossMusic() {
        return RagnarokSoundEvents.GRADIUS_MUSIC.get();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {

        if (awakening) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            this.lastAttacker = player;
        }
        if (this.standby && attacker instanceof LivingEntity livingAttacker) {
            if (!(livingAttacker instanceof Player p && p.isCreative())) {
                this.wakeUpFromStandby(livingAttacker);
            }
        }

        // ガード中
        if (getGuardPhase() == 2) {
            if (attacker instanceof LivingEntity living) {
                living.knockback(1.5F,
                        this.getX() - living.getX(),
                        this.getZ() - living.getZ());
            }
            this.playSound(SoundEvents.ANVIL_PLACE, 1.2F, 0.8F);
            return false;
        }

        // ガード発動
        if (!this.level().isClientSide
                && attacker != null     // ← 【追加】ここがポイントだ
                && getGuardPhase() == 0
                && !isBusy()
                && random.nextFloat() < 0.25F) {
            startGuard();
            return false;
        }

        // ── Boss_Monster経由でダメージキャップ・軽減タイマーを適用 ──
        return super.hurt(source, amount);  // ← これはそのままでOK
    }

    private void startGuard() {

        this.guardTimer = 0;
        this.guarding = true;

        // 攻撃中断
        setAttackState(0);

        // 突進中断
        setChargePhase(0);

        // ジャンプ攻撃中断
        setJumpSlamPhase(0);

        setGuardPhase(1);

        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
    }

    private boolean isBusy() {
        return getAttackState() > 0
                || getChargePhase() > 0
                || getJumpSlamPhase() > 0
                || awakening;
    }

    private void breakShield(LivingEntity e, int cooldown) {
        if (!(e instanceof Player player)) return;

        // ── 盾でブロック中のときのみスタン付与 ──
        if (!player.isBlocking()) return;

        player.getCooldowns().addCooldown(Items.SHIELD, cooldown);
        player.disableShield(true);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isImmobile() {
        // 通常の不動条件（死亡時など）か、あるいは「待機状態」であるなら、完全に体を固定する
        return super.isImmobile() || this.isStandby();
    }

    // ──────────────────────────────────────────
    // サウンド
    // ──────────────────────────────────────────
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.RAVAGER_STEP, 1.0F, 1.0F);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public ResourceLocation getDefaultLootTable() {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "entities/gradius");
    }


    // ──────────────────────────────────────────
    // アニメーション登録（GeckoLib）
    // ──────────────────────────────────────────
    // ここは完全にクライアント側（描画側）のロジック。
    // 参照しているのは getAttackState()/getChargePhase() など「同期された値」だけで、
    // GradiusAttackGoalの内部変数(attackTimer等)を直接見ることは絶対にない
    // （クラス冒頭のコメントで説明した「1) サーバー側の状態と見た目を分離している」の実践部分）。
    // AnimationControllerは複数登録でき、それぞれが「レイヤー」のように重なる。
    // ここでは「base_controller（歩行/待機など下地の動き）」と、
    // 別途「攻撃専用のコントローラー」を分け、attackState等に応じて
    // どちらを優先表示するかをPlayStateの返し方で制御している。
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // ── 移動 / 待機レイヤー ──
        controllers.add(new AnimationController<>(this,
                "base_controller",
                5, // トランジション（アニメ同士の補間）にかけるtick数。大きいほど滑らかに繋がるが反応が遅れる
                state -> {

                    if (this.isActuallyDying())
                        return PlayState.STOP;

                    // ── 待機中・待機終了中はbase_controllerを止める ──
                    if (this.isStandby() || this.isStandbyEnding()) {
                        return PlayState.STOP;
                    }

                    if (this.getAttackState() > 0)
                        return state.setAndContinue(
                                RawAnimation.begin().thenLoop("idle"));

                    if (this.getChargePhase() > 0)
                        return state.setAndContinue(
                                RawAnimation.begin().thenLoop("idle"));

                    boolean moving = state.isMoving();

                    if (moving) {
                        wasMoving = true;
                        playingWalkStop = false;
                        return state.setAndContinue(
                                RawAnimation.begin().thenLoop("walk"));
                    }

                    if (wasMoving && !playingWalkStop) {
                        wasMoving = false;
                        playingWalkStop = true;
                        state.getController().forceAnimationReset();
                        return state.setAndContinue(
                                RawAnimation.begin().thenPlay("walk_stop"));
                    }

                    if (playingWalkStop) {
                        if (state.getController().hasAnimationFinished()) {
                            playingWalkStop = false;
                        }
                        return state.setAndContinue(
                                RawAnimation.begin().thenPlay("walk_stop"));
                    }

                    return state.setAndContinue(
                            RawAnimation.begin().thenLoop("idle"));
                }));

        // ── アクション（攻撃・突進・死亡）レイヤー ──
        controllers.add(new AnimationController<>(this, "action_controller", 3, state -> {
            // ── 待機状態 ──
            if (this.isStandby()) {
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("standby"));
            }
            if (this.isStandbyEnding()) { // standbyEnding → isStandbyEnding()
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("standby_end"));
            }
            // 死亡
            if (this.isActuallyDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("death"));
            }
            if (this.awakening) {
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("awake"));
            }
            int guard = this.getGuardPhase();

            if (guard > 0) {

                if (guard == 1) {
                    return state.setAndContinue(
                            RawAnimation.begin()
                                    .thenPlay("guard_start"));
                }

                if (guard == 2) {
                    return state.setAndContinue(
                            RawAnimation.begin()
                                    .thenLoop("guard_loop"));
                }

                if (guard == 3) {
                    return state.setAndContinue(
                            RawAnimation.begin()
                                    .thenPlay("guard_end"));
                }
            }

            // 突進フェーズ
            int cp = this.getChargePhase();
            if (cp > 0) {
                if (cp == 1) return state.setAndContinue(RawAnimation.begin().thenPlay("charge_start"));
                if (cp == 2) return state.setAndContinue(RawAnimation.begin().thenLoop("charge_loop"));
                if (cp == 3) return state.setAndContinue(RawAnimation.begin().thenPlay("charge_end"));
            }
            int jump = this.getJumpSlamPhase();
            if (jump > 0) {
                if (jump == 1)
                    return state.setAndContinue(
                            RawAnimation.begin()
                                    .thenPlay("jump_slam_start"));
                if (jump == 2)
                    return state.setAndContinue(
                            RawAnimation.begin()
                                    .thenLoop("jump_slam_loop"));
                if (jump == 3)
                    return state.setAndContinue(
                            RawAnimation.begin()
                                    .thenPlay("jump_slam_end"));
            }

            // 通常攻撃 / 召喚
            int atk = this.getAttackState();
            if (atk > 0) {
                if (this.previousAttackState != atk) {
                    state.getController().forceAnimationReset();
                    this.previousAttackState = atk;
                }
                String animName = switch (atk) {
                    case 1 -> "attack1";
                    case 2 -> "attack2";
                    case 3 -> "attack3";
                    case 4 -> "summon_attack";
                    case 5 -> "attack1_2";
                    case 6 -> "attack5";
                    case 7 -> "attack6";
                    default -> "idle";
                };
                return state.setAndContinue(RawAnimation.begin().thenPlay(animName));
            }

            if (this.previousAttackState != 0) {
                this.previousAttackState = 0;
                state.getController().forceAnimationReset();
            }

            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ──────────────────────────────────────────
    // パーティクルユーティリティ
    // ──────────────────────────────────────────
    private void spawnDeathParticles() {
        if (this.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 12; i++) {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 3,
                        this.getY() + this.random.nextDouble() * 3,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 3,
                        1, 0, 0.05, 0, 0.01);
            }
        }
    }

    private void doAwakeningBurst() {

        if (!(this.level() instanceof ServerLevel sl))
            return;

        for (int i = 0; i < 300; i++) {

            double dx = (random.nextDouble() - 0.5) * 6;
            double dy = random.nextDouble() * 3;
            double dz = (random.nextDouble() - 0.5) * 6;

            sl.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(),
                    this.getY() + 1.5,
                    this.getZ(),
                    1,
                    dx,
                    dy,
                    dz,
                    0.1
            );
        }

        sl.sendParticles(
                ParticleTypes.SOUL,
                this.getX(),
                this.getY() + 1.5,
                this.getZ(),
                200,
                2,
                2,
                2,
                0.2
        );

        this.playSound(
                SoundEvents.WARDEN_SONIC_BOOM,
                2.0F,
                0.8F
        );
    }

    @Override
    public ResourceLocation getBossBarBaseTexture() {
        // 第二形態ならbase2、それ以外(第一形態など)ならbase1を返す
        return this.isPhase2Color() ?
                ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/gui/gradius_bossbar/gradius_boss_bar_base2.png") :
                ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/gui/gradius_bossbar/gradius_boss_bar_base1.png");
    }

    @Override
    public ResourceLocation getBossBarOverlayTexture() {
        return this.isPhase2Color() ?
                ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/gui/gradius_bossbar/gradius_boss_bar_overlay2.png") :
                ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/gui/gradius_bossbar/gradius_boss_bar_overlay1.png");
    }

    @Override
    public float getBossProgress() {
        return this.getHealth() / this.getMaxHealth();
    }

    @Override
    public int getFrameWidth() {
        // 実際のフレーム画像(overlay1/2.png)の横幅を返す
        return 128; // 仮の値
    }

    @Override
    public int getFrameHeight() {
        // 実際のフレーム画像の縦幅を返す
        return 30; // 仮の値
    }

    @Override
    public int getFrameOffsetX() {
        // ゲージ幅(120)に対してフレーム(128)を中央揃え：(120 - 128) / 2 = -4
        return (120 - getFrameWidth()) / 2;
    }

    @Override
    public int getFrameOffsetY() {
        return -12;
    }

    @Override
    public boolean shouldShowBossBar() {
        // 待機中（standby）または待機終了演出中（standbyEnding）はボスバーを表示しない
        return !this.isStandby() && !this.isStandbyEnding();
    }

    // ══════════════════════════════════════════
    //  AttackGoal（すべての攻撃・突進を管理）
    // ══════════════════════════════════════════
    static class GradiusAttackGoal extends Goal {

        private final GradiusEntity mob;
        private final double speed;
        private LivingEntity target;

        // ── タイマー ──
        private int attackTimer = 0;
        private int cooldown = 0;
        private int chargeTimer = 0;


        // ── 突進 ──
        private Vec3 chargeVec = Vec3.ZERO;
        private int chargeStartDuration = 22; // charge_startアニメの長さ(tick)
        private boolean chargingActive = false;
        private static final double CHARGE_SPEED = 1.5D;
        private static final double CHARGE_RANGE = 20.0D; // 突進を使う最大距離
        private int jumpSlamTimer = 0;
        private boolean slamDone = false;
        private boolean forceFinishAttack = false;
        private int noTargetFreezeTicks = 0;

        // forceFinishAttack中でも、ターゲット不在のままこのtick数を超えたら
        // 強制的にあきらめてGoalを手放す（でないと永久にその場で固まる）
        private static final int MAX_NO_TARGET_FREEZE_TICKS = 20;

        // ── 攻撃判定距離 ──
        private static final double MELEE_RANGE_SQ = 16.0D; // 4ブロック以内で近接
        private static final double ATTACK_START_SQ = 9.0D; // 3ブロック未満で攻撃開始

        // ── 攻撃発生タイミング(tick) ──
        private static final int ATK1_HIT = 25;
        private static final int ATK1_END = 45;
        private static final int ATK2_HIT = 25;
        private static final int ATK2_END = 45;
        private static final int ATK3_HIT = 15;
        private static final int ATK3_END = 30;
        private static final int ATK4_HIT = 25;
        private static final int ATK4_END = 40;
        private static final int SUMMON_HIT = 20;
        private static final int SUMMON_END = 40;

        // 攻撃後クールダウン(tick)
        private static final int COOLDOWN_NORMAL = 30;
        private static final int COOLDOWN_CHARGE = 40;

        private boolean chargeAttackDone = false;

        private boolean comboActive = false;
        private int comboStep = 0;

        private float chargeEndYaw;
        private Vec3 jumpTarget;
        private boolean firePillarSpawned = false;
        private static final int FIRE_TOTAL = 40;
        private static final int FIRE_HIT_TICK = 25;

        private int guardTimer = 0;
        private boolean guarding = false;

        private final List<ScheduledFirePillar>
                scheduledPillars = new ArrayList<>();

        private final List<ScheduledBlockWave> scheduledBlockWaves = new ArrayList<>();

        private final List<FallingBlockEntity> activeWaveBlocks = new ArrayList<>();

        private final List<BlueFireballEntity> preparedFireballs =
                new ArrayList<>();

        private boolean fireballsSpawned = false;
        private boolean fireballsShot = false;

        private int summonCooldown = 0;

        public GradiusAttackGoal(GradiusEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mob.isActuallyDying()) return false;
            // ── 待機中・待機終了演出中はゴールを起動しない ──
            if (this.mob.isStandby() || this.mob.isStandbyEnding()) return false;

            LivingEntity t = this.mob.getTarget();

            // forceFinishAttackが立っている＝「攻撃/詠唱の途中」を意味するので、
            // ターゲットの有無に関係なく無条件でtrueを返す。
            // これを先にチェックしないと、攻撃モーションの途中でターゲットを見失った瞬間に
            // canUse()がfalseになり、Goalそのものが強制終了して
            // アニメーションが再生途中でぶつ切りになってしまう。
            if (this.forceFinishAttack) return true;

            return t != null && t.isAlive();
        }
        // 注：canContinueToUse()はあえてオーバーライドしていない。
        //   デフォルト実装は「毎tick canUse()をもう一度呼ぶ」だけなので、
        //   上のforceFinishAttackの分岐がそのままGoal継続の判定にも使い回される。

        @Override
        public void start() {
            this.target = this.mob.getTarget();
            this.attackTimer = 0;
            this.cooldown = 0;
        }

        @Override
        public void stop() {
            // Goalが（何らかの理由で）中断された時のリセット処理。
            // 「攻撃の演出用に使っている一時変数」を一つでも戻し忘れると、
            // 次に攻撃を始めた時に前回の値が残ったままバグる可能性があるため、
            // ここで使っているフラグ・タイマー類は基本的に全部リストアップして初期化する。
            this.mob.setAttackState(0);
            this.mob.setChargePhase(0);
            this.mob.setJumpSlamPhase(0);  // ← 追加
            this.chargingActive = false;
            this.chargeTimer = 0;          // ← 追加
            this.jumpSlamTimer = 0;        // ← 追加
            this.attackTimer = 0;          // ← 追加
            this.slamDone = false;         // ← 追加
            this.chargeAttackDone = false; // ← 追加
            this.forceFinishAttack = false;// ← 追加
            this.noTargetFreezeTicks = 0;
            this.comboActive = false;      // ← 追加
            this.comboStep = 0;            // ← 追加
            scheduledBlockWaves.clear();   // ← 追加
            activeWaveBlocks.clear();
            this.mob.getNavigation().stop();
            this.mob.setDeltaMovement(Vec3.ZERO); // ← 突進中断時に速度もリセット
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        // ── メインtick ──
        @Override
        public void tick() {
            LivingEntity t = this.mob.getTarget();
            // このボスは「毎tick必ず呼ばれる処理」をここでまとめて先に消化する。
            // スケジュール済みの火柱・崩落ブロックの進行や、それらによる
            // 継続ダメージ判定は、攻撃中かどうかに関わらず常に更新し続ける必要がある
            // （例えば突進などで別の攻撃に切り替わっても、既に発生済みの火柱は消えない）。
            tickScheduledPillars();
            tickScheduledBlockWaves();
            tickWaveBlockDamage();

            if (summonCooldown > 0) summonCooldown--;


            // 常にターゲットの方を向く（体・頭とも固定）
            // ここでMoveControlに向きを任せず、毎tick明示的にyRot/yBodyRot/yHeadRotを
            // 上書きしているのは「攻撃中・突進中は移動方向とは無関係に、
            // 必ずターゲット（または突進方向）へ正面を向かせたい」ため。
            // MoveControl任せだと「移動していない時は向きが更新されない」ので、
            // 停止して攻撃するこのボスには向かない。
            if (target != null) {
                if (this.mob.getChargePhase() > 0) {
                    // 突進フェーズ中だけは例外：ターゲットではなく、
                    // 発動時に決めた突進方向(chargeVec)を向き続ける。
                    // （突進中にターゲットが横に避けても、律儀にそちらへ首を振らせない）
                    float chargeYaw = (float) (Math.toDegrees(Math.atan2(this.chargeVec.z, this.chargeVec.x)) - 90F);
                    this.mob.setYRot(chargeYaw);
                    this.mob.yBodyRot = chargeYaw;
                    this.mob.yHeadRot = chargeYaw;
                } else {
                    // それ以外はターゲットの方を向く
                    this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

                    Vec3 lookVec = target.position().subtract(this.mob.position());
                    float yRot = (float) (Math.toDegrees(Math.atan2(lookVec.z, lookVec.x)) - 90F);

                    this.mob.setYRot(yRot);
                    this.mob.yBodyRot = yRot;
                    this.mob.yHeadRot = yRot;
                }
            }

            if (t == null || !t.isAlive()) {
                // ── ターゲット死亡時：突進・ジャンプ切りは即中断してリセット ──
                // 突進やジャンプ切りは「ターゲットに向かって高速で移動し続ける」演出なので、
                // ターゲットが消えた状態でforceFinishAttackのルールに従って
                // 最後まで再生させてしまうと、誰もいない方向へ突進し続ける
                // 見た目上おかしい動きになる。そのためこの2つだけは
                // forceFinishAttackを無視して即座に中断・リセットする特別扱いにしている。
                if (this.mob.getChargePhase() > 0
                        || this.mob.getJumpSlamPhase() > 0) {
                    this.mob.setChargePhase(0);
                    this.mob.setJumpSlamPhase(0);
                    this.chargingActive = false;
                    this.chargeTimer = 0;
                    this.jumpSlamTimer = 0;
                    this.slamDone = false;
                    this.chargeAttackDone = false;
                    this.mob.setDeltaMovement(Vec3.ZERO);
                    this.forceFinishAttack = false;
                    this.mob.setAttackState(0);
                    this.mob.getNavigation().stop();
                    return;
                }

                if (!this.forceFinishAttack) {
                    // 攻撃中でも突進中でもない、単なる「ターゲットがいない」状態。
                    // 普通に攻撃状態を解除して待機に戻る。
                    this.mob.setAttackState(0);
                    this.mob.setChargePhase(0);
                    this.mob.getNavigation().stop();
                    return;
                }

                // ここに来るのは「通常攻撃/詠唱の最中に、ターゲットだけがいなくなった」場合。
                // アニメーションを不自然に中断させたくないので、attackStateは維持したまま
                // その場で待機する（＝アニメーションだけは進行し続ける想定）。
                // ただし、ターゲットが戻ってこないまま長時間経過したら、Goalが
                // 永久に居座って動けなくなるのを防ぐため強制的にあきらめる。
                this.mob.getNavigation().stop();
                noTargetFreezeTicks++;
                if (noTargetFreezeTicks > MAX_NO_TARGET_FREEZE_TICKS) {
                    this.forceFinishAttack = false;
                    this.mob.setAttackState(0);
                    noTargetFreezeTicks = 0;
                }
                return;
            }
            // ここまで来た＝ターゲットが有効。念のためタイムアウトカウンターをリセットしておく。
            noTargetFreezeTicks = 0;
            if (mob.awakening) {
                return;
            }
            if (this.mob.awakening) {
                this.mob.getNavigation().stop();
                return;
            }
            if (mob.getGuardPhase() > 0) {

                attackTimer = 0;
                chargeTimer = 0;
                jumpSlamTimer = 0;

                comboActive = false;
                comboStep = 0;

                cooldown = 20;

                tickGuard();
                return;
            }
            this.target = t;

            this.mob.getLookControl().setLookAt(t, 30F, 30F);

            // クールダウン中は接近のみ
            // 「攻撃を出し切ったら、しばらく無防備になる代わりに、その間もじりじり
            // 距離を詰める」という設計。完全に足を止めると単調になりやすいので、
            // クールダウン＝待機時間ではなく「次の攻撃の準備をしながら近づく時間」にしている。
            if (this.cooldown > 0) {
                this.cooldown--;
                this.mob.getNavigation().moveTo(t, this.speed);
                return;
            }

            // ── 突進フェーズ処理 ──
            // 突進・ジャンプ切りは他の通常攻撃(attack1〜4)とは別の
            // 独立したフェーズ変数(CHARGE_PHASE/JUMP_SLAM_PHASE)で管理している。
            // 理由：これらは複数tickにまたがる「予備動作→加速→着地」のような
            // 複数ステップを持つ攻撃で、ATTACK_STATE(単純な1〜4の値)だけでは
            // 表現しきれないため、専用のフェーズ管理を用意している。
            int cp = this.mob.getChargePhase();
            if (cp > 0) {
                tickCharge(t, cp);
                return;
            }
            int jumpPhase = this.mob.getJumpSlamPhase();

            if (jumpPhase > 0) {
                tickJumpSlam(t, jumpPhase);
                return;
            }

            // ── 攻撃中 ──
            if (this.mob.getAttackState() > 0) {
                this.attackTimer++;
                this.mob.getNavigation().stop();
                RkCombatUtil.faceTarget(mob, t); // 攻撃中はターゲットへ体・頭を向ける
                executeAttack(t);
                return;
            }

            // ── 行動選択 ──
            // ここが「次に何をするか」の分岐点。距離に応じて使える選択肢が変わる。
            double distSq = this.mob.distanceToSqr(t);
            double chargeRangeSq = CHARGE_RANGE * CHARGE_RANGE; // 400
            if (distSq > chargeRangeSq) {
                // 遠すぎて突進の射程(20ブロック)にも入っていない距離。
                // 4分の1の確率でジャンプ切りを織り交ぜつつ、基本は普通に接近する。
                // （毎回同じ動きだと単調になるため、確率で行動にブレを持たせている）
                if (this.mob.random.nextInt(4) == 0) {
                    startJumpSlam(t);
                } else {
                    this.mob.getNavigation().moveTo(t, this.speed);
                }
                return;
            }
            // 突進判定：一定以上離れていて確率で突進
            if (distSq > ATTACK_START_SQ * ATTACK_START_SQ
                    && distSq <= chargeRangeSq) {
                if (this.mob.random.nextInt(8) == 0) {
                    // 半々で突進かジャンプ切り
                    if (this.mob.random.nextBoolean()) {
                        startCharge(t);
                    } else {
                        startJumpSlam(t);
                    }
                    return;
                }
                this.mob.getNavigation().moveTo(t, this.speed);
                return;
            }

            // 近接攻撃射程内に入ったら攻撃
            if (distSq <= ATTACK_START_SQ * ATTACK_START_SQ) {
                this.mob.getNavigation().stop();
                startAttack();
            } else {
                this.mob.getNavigation().moveTo(t, this.speed);
            }
        }

        // ──────────────────────────────────────
        // 攻撃選択（ランダム）
        // ──────────────────────────────────────
        private void startAttack() {
            int roll = this.mob.random.nextInt(100);

            if (mob.isPhase2()) {
                if (roll < 15) {
                    mob.setAttackState(6);
                } else if (roll < 30) {
                    mob.setAttackState(7);
                } else if (roll < 50) {
                    mob.setAttackState(1);
                } else if (roll < 65) {
                    mob.setAttackState(5);
                } else if (roll < 80) {
                    mob.setAttackState(2);
                } else if (roll < 96) { // ★ 94 から 96 に変更（召喚確率を 6% から 4% へ引き下げ）
                    mob.setAttackState(3);
                } else {
                    // 召喚：クールタイム中は別の攻撃に差し替え
                    if (summonCooldown <= 0) {
                        mob.setAttackState(4);
                        summonCooldown = 2400; // ★ 1500tick(75秒) から 2400tick(120秒) へ延長
                    } else {
                        mob.setAttackState(3); // 踏みつけに差し替え
                    }
                }
            } else {
                if (roll < 25) {
                    mob.setAttackState(1);
                } else if (roll < 45) {
                    mob.setAttackState(5);
                } else if (roll < 70) {
                    mob.setAttackState(2);
                } else if (roll < 94) { // ★ 88 から 94 に変更（召喚確率を 12% から 6% へ引き下げ）
                    mob.setAttackState(3);
                } else {
                    // 召喚：クールタイム中は薙ぎ払いに差し替え
                    if (summonCooldown <= 0) {
                        mob.setAttackState(4);
                        summonCooldown = 1200; // ★ 600tick(30秒) から 1200tick(60秒) へ延長
                    } else {
                        mob.setAttackState(1);
                    }
                }
            }

            attackTimer = 0;
            forceFinishAttack = true;
        }

        // ──────────────────────────────────────
        // 攻撃実行（毎tick）
        // ──────────────────────────────────────
        // executeAttack()は「今のATTACK_STATEの値」を見て、対応する攻撃の
        // タイムライン処理を分岐実行する巨大switch。各caseの中身は基本的に
        // 「attackTimerが特定の値になったら判定/ダメージ/次への遷移を行う」の
        // 繰り返しなので、attack1（薙ぎ払い）だけ詳しくコメントし、
        // 他のcase（attack2〜4, summon等）は同じ考え方の応用として省略気味にしている。
        private void executeAttack(LivingEntity t) {
            switch (this.mob.getAttackState()) {

                // ── attack1：薙ぎ払い ──
                case 1 -> {
                    // 攻撃モーションの「踏み込み」部分(20〜24tick目)だけ、ゆっくり前進させる。
                    // 予備動作中に少しだけ距離を詰めることで、ギリギリ届かない位置に
                    // いたターゲットにも当たるようにする調整。
                    if (this.attackTimer >= 20
                            && this.attackTimer < 25) {

                        moveTowardTarget(t, 0.25D);
                    }
                    // ATK1_HIT(=25)tick目が「実際に判定が発生する瞬間」。
                    // ここで強めに前進させてから、実際のダメージ処理(doSwipe)を呼ぶ。
                    if (attackTimer == ATK1_HIT) {
                        moveTowardTarget(t, 1.0D);
                        doSwipe(t);
                    }

                    // ATK1_END(=45)tick目でアニメーションの再生自体は終わるが、
                    // ここでは単純にクールダウンへ移行せず、確率で「次の攻撃へ
                    // そのまま繋げるコンボ」に派生させている（comboActive/comboStep）。
                    // ＝1回の攻撃で必ず終わるのではなく、演出の途中で分岐する
                    // 「モーションキャンセル」的な仕組み。
                    if (attackTimer >= ATK1_END) {
                        if (comboActive && comboStep == 10) {

                            comboStep = 11;

                            if (mob.random.nextBoolean()) {
                                mob.setAttackState(2);
                            } else {
                                mob.setAttackState(3);
                            }

                            attackTimer = 0;
                            return;
                        }
                        if (mob.random.nextFloat() < 0.4F) { //40%

                            comboActive = true;
                            comboStep = 1;

                            mob.setAttackState(5); // attack1_2
                            attackTimer = 0;

                        } else {
                            finishAttack(COOLDOWN_NORMAL);
                        }
                    }
                }

                // ── attack2：叩きつけ ──
                case 2 -> {

                    if (attackTimer == ATK2_HIT) {
                        doSlam(t);
                        if (mob.isPhase2()) {
                            spawnFireRings();
                        }
                    }

                    if (attackTimer >= ATK2_END) {

                        comboActive = false;
                        comboStep = 0;

                        finishAttack(COOLDOWN_NORMAL);
                    }
                }

                // ── attack3：踏みつけ ──
                case 3 -> {

                    if (attackTimer == ATK3_HIT) {
                        doStomp();
                        // 第二形態：20tick後にランダム青火柱
                        if (mob.isPhase2()) {
                            scheduleRandomFirePillars(20);
                        }
                    }

                    if (attackTimer >= ATK3_END) {

                        comboActive = false;
                        comboStep = 0;

                        finishAttack(COOLDOWN_NORMAL);
                    }
                }

                // ── summon_attack：召喚 ──
                case 4 -> {
                    if (this.attackTimer == SUMMON_HIT) {
                        doSummon();
                    }
                    if (this.attackTimer >= SUMMON_END) finishAttack(COOLDOWN_NORMAL);
                }
                case 5 -> {
                    if (this.attackTimer >= 20
                            && this.attackTimer < 25) {

                        moveTowardTarget(t, 0.25D);
                    }
                    if (attackTimer == ATK4_HIT) {
                        moveTowardTarget(t, 1.0D);
                        doReverseSwipe();
                    }

                    if (attackTimer >= ATK4_END) {

                        if (comboActive && comboStep == 1) {

                            comboStep = 2;

                            if (mob.random.nextBoolean()) {
                                mob.setAttackState(2);
                            } else {
                                mob.setAttackState(3);
                            }

                            attackTimer = 0;

                        } else if (mob.random.nextFloat() < 0.4F) {

                            comboActive = true;
                            comboStep = 10; //逆始動

                            mob.setAttackState(1);
                            attackTimer = 0;

                        } else {
                            finishAttack(COOLDOWN_NORMAL);
                        }
                    }
                }
                case 6 -> {
                    if (mob.isPhase2()) {
                        doFirePillarAttack(t);
                        return;
                    }

                    // 既存の逆薙ぎは第一形態側に戻すなど整理推奨
                    doReverseSwipe();
                }
                case 7 -> {

                    if (!mob.isPhase2()) {
                        finishAttack(20);
                        return;
                    }

                    if (attackTimer == 25
                            && !fireballsSpawned) {

                        fireballsSpawned = true;

                        createFireballs();
                    }

                    if (attackTimer == 35
                            && !fireballsShot) {

                        fireballsShot = true;

                        launchFireballs(t);
                    }

                    if (attackTimer >= 45) {

                        for (BlueFireballEntity fireball : preparedFireballs) {

                            if (fireball.isAlive()) {
                                fireball.discard();
                            }
                        }

                        preparedFireballs.clear();

                        fireballsSpawned = false;
                        fireballsShot = false;

                        finishAttack(40);
                    }
                }
            }
        }

        private void createFireballs() {

            preparedFireballs.clear();

            for (int i = 0; i < 12; i++) {

                double angle =
                        (Math.PI * 2 * i) / 12;

                double radius = 4.0;

                double x =
                        mob.getX()
                                + Math.cos(angle) * radius;

                double z =
                        mob.getZ()
                                + Math.sin(angle) * radius;

                double y =
                        mob.getY()
                                + 2.5;

                BlueFireballEntity fireball =
                        new BlueFireballEntity(
                                mob.level(),
                                mob,
                                null
                        );

                fireball.setPos(x, y, z);

                // 空中待機
                fireball.setDeltaMovement(Vec3.ZERO);

                mob.level().addFreshEntity(fireball);

                preparedFireballs.add(fireball);
            }

            mob.playSound(
                    SoundEvents.BLAZE_SHOOT,
                    2.0F,
                    0.5F
            );
        }

        private void launchFireballs(
                LivingEntity target) {

            for (BlueFireballEntity fireball
                    : preparedFireballs) {

                if (!fireball.isAlive())
                    continue;

                Vec3 dir =
                        target.position()
                                .add(
                                        0,
                                        target.getBbHeight() * 0.5,
                                        0)
                                .subtract(
                                        fireball.position())
                                .normalize();

                fireball.setTarget(target);

                fireball.setDeltaMovement(
                        dir.scale(0.9)
                );
            }

            mob.playSound(
                    SoundEvents.BLAZE_SHOOT,
                    2.0F,
                    1.2F
            );
        }

        // ──────────────────────────────────────
        // attack1：薙ぎ払い（ターゲットをノックバック）
        // ──────────────────────────────────────
        private void doSwipe(LivingEntity primary) {
            // 前方扇形(±60度)の全エンティティをヒット
            Vec3 look = this.mob.getLookAngle();
            AABB box = this.mob.getBoundingBox()
                    .move(look.x * 3, 0, look.z * 3)
                    .inflate(4.0, 2.0, 4.0);

            for (LivingEntity e : getHittableEntities(box)) {
                e.invulnerableTime = 0;
                float dmg = (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
                e.hurt(this.mob.damageSources().mobAttack(this.mob), dmg);

                // ノックバック：グラディウスから離れる方向へ強く吹き飛ばす
                Vec3 kb = e.position().subtract(this.mob.position()).normalize().scale(2.5);
                e.setDeltaMovement(kb.x, 0.6, kb.z);
                e.hurtMarked = true;
            }

            this.mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.8F);
            spawnSlashParticles(look);
        }

        private void doReverseSwipe() {

            Vec3 look = this.mob.getLookAngle();

            AABB box = this.mob.getBoundingBox()
                    .move(look.x * 3, 0, look.z * 3)
                    .inflate(4.0, 2.0, 4.0);

            for (LivingEntity e : getHittableEntities(box)) {

                e.invulnerableTime = 0;

                float dmg =
                        (float) this.mob.getAttributeValue(
                                Attributes.ATTACK_DAMAGE);

                e.hurt(
                        this.mob.damageSources().mobAttack(this.mob),
                        dmg
                );

                Vec3 kb = e.position()
                        .subtract(this.mob.position())
                        .normalize()
                        .scale(2.0);

                e.setDeltaMovement(kb.x, 0.5, kb.z);
                e.hurtMarked = true;
            }

            this.mob.playSound(
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    1.5F,
                    0.6F
            );

            spawnReverseSlashParticles();
        }

        private void spawnReverseSlashParticles() {

            if (!(this.mob.level() instanceof ServerLevel sl))
                return;

            for (int i = 0; i < 20; i++) {

                double angle =
                        Math.toRadians(90 - i * 18);

                sl.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.mob.getX() + Math.cos(angle) * 3,
                        this.mob.getY() + 2.0,
                        this.mob.getZ() + Math.sin(angle) * 3,
                        1,
                        0,
                        0,
                        0,
                        0
                );
            }
        }

        private void spawnFireRings() {

            int delay = 0;

            for (double radius = 3;
                 radius <= 9;
                 radius++) {

                int pillars = (int) (radius * 2);

                for (int i = 0;
                     i < pillars;
                     i++) {

                    double angle =
                            Math.PI * 2
                                    * i / pillars;

                    double x =
                            mob.getX()
                                    + Math.cos(angle)
                                    * radius;

                    double z =
                            mob.getZ()
                                    + Math.sin(angle)
                                    * radius;

                    scheduledPillars.add(
                            new ScheduledFirePillar(
                                    delay,
                                    x,
                                    z
                            )
                    );
                }

                delay += 4;
            }
        }
        private void tickScheduledBlockWaves() {
            Iterator<ScheduledBlockWave> it = scheduledBlockWaves.iterator();
            while (it.hasNext()) {
                ScheduledBlockWave wave = it.next();
                wave.remainingTicks--;
                if (wave.remainingTicks <= 0) {
                    if (mob.level() instanceof ServerLevel sl) {
                        spawnBlockWaveRing(sl, wave.center, wave.radius,
                                wave.startAngle, wave.endAngle);
                    }
                    it.remove();
                }
            }
        }

        private void doFirePillarAttack(LivingEntity target) {
            this.mob.getNavigation().stop();

            // 剣を突き立てるタイミング演出
            if (attackTimer == 1) {
                this.mob.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 2.0F, 0.7F);
            }

            // 25tickで発動（ここが本体）
            if (attackTimer == FIRE_HIT_TICK && !firePillarSpawned) {
                firePillarSpawned = true;
                mob.sendScreenShake(2.5F, 25);

                spawnFirePillarLines(target);
            }

            // 終了処理
            if (attackTimer >= FIRE_TOTAL) {
                firePillarSpawned = false;
                finishAttack(40);
            }
        }

        private void spawnFirePillarLines(
                LivingEntity target) {

            Vec3 dir =
                    target.position()
                            .subtract(mob.position())
                            .normalize();

            double angle =
                    Math.atan2(dir.z, dir.x);

            double[] offsets = {
                    -0.35,
                    0.0,
                    0.35
            };

            for (double offset : offsets) {

                double a = angle + offset;

                double dx = Math.cos(a);
                double dz = Math.sin(a);

                int delay = 0;

                for (double dist = 0;
                     dist <= 20;
                     dist += 4) {

                    scheduledPillars.add(
                            new ScheduledFirePillar(
                                    delay,
                                    mob.getX() + dx * dist,
                                    mob.getZ() + dz * dist
                            )
                    );

                    delay += 4;
                }
            }
        }

        private void tickScheduledPillars() {

            Iterator<ScheduledFirePillar> it =
                    scheduledPillars.iterator();

            while (it.hasNext()) {

                ScheduledFirePillar pillar =
                        it.next();

                pillar.remainingTicks--;

                if (pillar.remainingTicks <= 0) {

                    spawnSingleFirePillar(
                            pillar.x,
                            pillar.z
                    );

                    it.remove();
                }
            }
        }

        private void spawnSingleFirePillar(
                double x,
                double z) {

            if (!(mob.level() instanceof ServerLevel sl))
                return;

            // 発生時爆発
            sl.sendParticles(
                    ParticleTypes.EXPLOSION,
                    x,
                    mob.getY() + 0.5,
                    z,
                    1,
                    0,
                    0,
                    0,
                    0
            );

            double height = 7 + mob.random.nextDouble() * 3;

            // メイン火柱
            for (double y = 0; y < height; y += 0.2) {

                double radius = 0.8 + (y / height) * 0.4;

                sl.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        x,
                        mob.getY() + y,
                        z,
                        12,
                        radius,
                        0.05,
                        radius,
                        0
                );

                sl.sendParticles(
                        ParticleTypes.SOUL,
                        x,
                        mob.getY() + y,
                        z,
                        6,
                        radius * 0.7,
                        0.05,
                        radius * 0.7,
                        0
                );

                sl.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        x,
                        mob.getY() + y,
                        z,
                        3,
                        0.2,
                        0.05,
                        0.2,
                        0
                );
            }

            // 頂上の爆発
            sl.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    x,
                    mob.getY() + height,
                    z,
                    40,
                    1.2,
                    0.3,
                    1.2,
                    0.05
            );

            // ─────────────
            // ダメージ判定
            // ─────────────
            AABB hitBox = new AABB(
                    x - 1.0,
                    mob.getY(),
                    z - 1.0,
                    x + 1.0,
                    mob.getY() + height,
                    z + 1.0
            );

            for (LivingEntity e : mob.level().getEntitiesOfClass(
                    LivingEntity.class,
                    hitBox,
                    ent -> ent != mob
                            && ent.isAlive()
                            && isHostileToGradius(ent)
            )) {

                e.invulnerableTime = 0;

                e.hurt(
                        mob.damageSources().mobAttack(mob),
                        (float) mob.getAttributeValue(
                                Attributes.ATTACK_DAMAGE) * 1.1F
                );

                Vec3 kb = e.position()
                        .subtract(mob.position())
                        .normalize()
                        .scale(2.0);

                e.setDeltaMovement(
                        kb.x,
                        1.0,
                        kb.z
                );

                e.hurtMarked = true;
            }

            mob.playSound(
                    SoundEvents.TRIDENT_THUNDER,
                    1.5F,
                    0.7F
            );
        }

        // ── 予約済み火柱 ──
        // 「今すぐ全部の火柱を発生させる」のではなく、位置(x,z)と
        // 「あと何tickで発生するか(remainingTicks)」だけを持ったデータとして
        // リストに溜めておき、tickScheduledPillars()で毎tickカウントダウンする方式。
        // こうすることで「範囲内にランダムに複数の火柱を、時間差でバラバラに
        // 発生させる」という演出を、Goalの状態を複雑にせず単純なリスト管理で実現している。
        // TickTask（Minecraftの汎用遅延実行）を使わず自前のリストにしているのは、
        // 突進などで攻撃自体が中断された時に scheduledBlockWaves.clear() のように
        // 一括でキャンセルしやすくするため。
        private static class ScheduledFirePillar {

            int remainingTicks;
            double x;
            double z;

            ScheduledFirePillar(
                    int remainingTicks,
                    double x,
                    double z) {

                this.remainingTicks = remainingTicks;
                this.x = x;
                this.z = z;
            }
        }

        private void scheduleRandomFirePillars(int delayTicks) {
            int count = 6 + mob.random.nextInt(5); // 6〜10本

            for (int i = 0; i < count; i++) {
                double angle = mob.random.nextDouble() * Math.PI * 2;
                double dist = 3.0 + mob.random.nextDouble() * 7.0; // 半径3〜10ブロック

                scheduledPillars.add(new ScheduledFirePillar(
                        delayTicks,
                        mob.getX() + Math.cos(angle) * dist,
                        mob.getZ() + Math.sin(angle) * dist
                ));
            }
        }

        private void moveTowardTarget(LivingEntity target, double speed) {

            Vec3 dir = target.position()
                    .subtract(this.mob.position());

            dir = new Vec3(dir.x, 0, dir.z);

            if (dir.lengthSqr() < 0.01)
                return;

            dir = dir.normalize();

            this.mob.setDeltaMovement(
                    dir.x * speed,
                    this.mob.getDeltaMovement().y,
                    dir.z * speed
            );
        }

        // ──────────────────────────────────────
        // attack2：叩きつけ（扇状ショックウェーブ）
        // ──────────────────────────────────────
        private void doSlam(LivingEntity target) {

            Vec3 look = target.position()
                    .subtract(this.mob.position())
                    .normalize();

            Vec3 impactPos = this.mob.position().add(
                    look.x * 5,
                    0,
                    look.z * 5
            );

            // 扇状（正面120度）判定
            AABB slamBox = this.mob.getBoundingBox()
                    .move(look.x * 4, 0, look.z * 4)
                    .inflate(5.0, 2.0, 5.0);

            for (LivingEntity e : getHittableEntities(slamBox)) {
                // 正面120度の範囲チェック
                Vec3 toEntity = e.position().subtract(this.mob.position()).normalize();
                double dot = look.dot(toEntity);
                if (dot < 0.5) continue; // cos(60°)=0.5

                e.invulnerableTime = 0;
                float dmg = (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3f;
                mob.breakShield(e, 100);
                e.hurt(this.mob.damageSources().mobAttack(this.mob), dmg);

                // ショックウェーブで前方に吹き飛ばす
                e.setDeltaMovement(look.x * 2.0, 0.8, look.z * 2.0);
                e.hurtMarked = true;
            }

            this.mob.playSound(SoundEvents.GENERIC_EXPLODE, 1.5F, 0.6F);
            spawnShockwaveParticles(impactPos, 120.0, 6.0);
            mob.sendScreenShake(1.5F, 15);

            if (this.mob.level() instanceof ServerLevel) {
                final BlockPos center = this.mob.blockPosition();

                if (mob.isPhase2()) {
                    // 第二形態：円状
                    for (int r = 3; r <= 8; r++) {
                        scheduledBlockWaves.add(
                                new ScheduledBlockWave(r * 2, center, r));
                    }
                } else {
                    // 第一形態：扇状（正面120度）
                    double angle = Math.atan2(look.z, look.x);
                    for (int r = 3; r <= 8; r++) {
                        scheduledBlockWaves.add(
                                new ScheduledBlockWave(r * 2, center, r,
                                        angle - Math.toRadians(60),
                                        angle + Math.toRadians(60)));
                    }
                }
            }
        }

        // ──────────────────────────────────────
        // attack3：踏みつけ（周囲360度衝撃波）
        // ──────────────────────────────────────
        private void doStomp() {
            // 周囲6ブロック全方向
            AABB stompBox = this.mob.getBoundingBox().inflate(3.0, 2.0, 3.0);

            for (LivingEntity e : getHittableEntities(stompBox)) {
                e.invulnerableTime = 0;
                float dmg = (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2f;
                mob.breakShield(e, 100);
                e.hurt(this.mob.damageSources().mobAttack(this.mob), dmg);

                // 外側へノックバック
                Vec3 kb = e.position().subtract(this.mob.position()).normalize().scale(3.0);
                e.setDeltaMovement(kb.x, 0.9, kb.z);
                e.hurtMarked = true;
            }

            // 地面揺れ演出
            this.mob.playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.4F);
            mob.sendScreenShake(0.9F, 10);

            // カメラシェイク代わりにブロック破壊パーティクル
            if (this.mob.level() instanceof ServerLevel sl) {
                final BlockPos center = this.mob.blockPosition();

                for (int r = 3; r <= 8; r++) {
                    scheduledBlockWaves.add(
                            new ScheduledBlockWave(r * 2, center, r)
                    );
                }

                // ── 周囲3ブロックにブロックパーティクルを出す ──
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        if (Math.sqrt(x * x + z * z) > 3.0) continue;

                        BlockPos checkPos = center.offset(x, -1, z);
                        BlockState bs = sl.getBlockState(checkPos);

                        if (!bs.isAir() && bs.getDestroySpeed(sl, checkPos) >= 0) {
                            sl.sendParticles(
                                    new net.minecraft.core.particles.BlockParticleOption(
                                            net.minecraft.core.particles.ParticleTypes.BLOCK, bs),
                                    checkPos.getX() + 0.5,
                                    checkPos.getY() + 1.0,
                                    checkPos.getZ() + 0.5,
                                    8,    // 個数
                                    0.3,  // X広がり
                                    0.2,  // Y広がり
                                    0.3,  // Z広がり
                                    0.3   // 速度
                            );
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────
        // summon_attack：円形にゾンビ召喚
        // ──────────────────────────────────────
        private void doSummon() {
            if (this.mob.level().isClientSide) return;

            int count = 4 + this.mob.random.nextInt(3);
            double radius = 5.0;

            // グラディウスのターゲットを取得
            LivingEntity gradiusTarget = this.mob.getTarget();

            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2 / count) * i;
                double sx = this.mob.getX() + Math.cos(angle) * radius;
                double sz = this.mob.getZ() + Math.sin(angle) * radius;

                BlockPos spawnPos = new BlockPos((int) sx, (int) this.mob.getY(), (int) sz);
                while (!this.mob.level().getBlockState(spawnPos).isAir()
                        && spawnPos.getY() < this.mob.level().getMaxBuildHeight()) {
                    spawnPos = spawnPos.above();
                }

                GhostKnightEntity ghostKnight = new GhostKnightEntity(
                        RagnarokEntities.GHOST_KNIGHT.get(), this.mob.level());
                ghostKnight.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(),
                        spawnPos.getZ() + 0.5, this.mob.random.nextFloat() * 360F, 0F);
                ghostKnight.finalizeSpawn((ServerLevel) this.mob.level(),
                        this.mob.level().getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null, null);
                this.mob.level().addFreshEntity(ghostKnight);

                // ── スポーン直後にターゲットをセット ──
                if (gradiusTarget != null) {
                    ghostKnight.setTarget(gradiusTarget);
                }
            }

            this.mob.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 1.2F);

            // 召喚エフェクト
            if (this.mob.level() instanceof ServerLevel sl) {
                for (int i = 0; i < 60; i++) {
                    double angle = Math.PI * 2 / 60 * i;
                    sl.sendParticles(ParticleTypes.PORTAL,
                            this.mob.getX() + Math.cos(angle) * 5,
                            this.mob.getY() + 1.0,
                            this.mob.getZ() + Math.sin(angle) * 5,
                            1, 0, 0.3, 0, 0.05);
                }
            }
        }

        // ──────────────────────────────────────
        // 突進：開始
        // ──────────────────────────────────────
        private void startCharge(LivingEntity t) {
            Vec3 dir = t.position().subtract(this.mob.position()).normalize();
            this.chargeVec = dir;
            this.chargingActive = false;
            this.chargeTimer = 0;
            this.mob.setChargePhase(1); // charge_start
            this.mob.getNavigation().stop();
            this.mob.playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.9F);
        }

        private void startJumpSlam(LivingEntity t) {
            // ターゲットの現在位置を着地目標に設定
            this.jumpTarget = t.position();
            this.jumpSlamTimer = 0;
            this.slamDone = false;
            this.mob.setJumpSlamPhase(1);
            this.mob.getNavigation().stop();
        }

        // ──────────────────────────────────────
        // 突進：毎tick処理
        // ──────────────────────────────────────
        // CHARGE_PHASEの値で「予備動作(1)→加速中(2)→着地硬直(3)」のように
        // 複数tickにまたがる状態を管理する、いわば「攻撃専用の小さなステートマシン」。
        // executeAttack()のswitchと違い、こちらは同期された1つの整数値
        // (setChargePhase)を進行させながら、tick()側で毎回phaseを見て分岐している。
        // 通常攻撃(attack1〜4)よりも「移動そのものが攻撃の一部」という性質が強いため、
        // ATTACK_STATEとは別の変数で管理する方が見通しが良い、という判断。
        private void tickCharge(LivingEntity t, int phase) {
            this.chargeTimer++;

            switch (phase) {
                // ── charge_start（予備動作）──
                case 1 -> {
                    // ── 予備動作中も重力を適用 ──
                    Vec3 mv1 = mob.getDeltaMovement();
                    if (!mob.onGround()) {
                        mob.setDeltaMovement(mv1.x, mv1.y - 0.08D, mv1.z);
                    }

                    if (this.chargeTimer >= chargeStartDuration) {
                        this.chargeTimer = 0;
                        this.chargingActive = true;
                        this.mob.setChargePhase(2);
                    }
                }

                // ── charge_loop（突進中）──
                case 2 -> {

                    // 重力を適用（地面にいない場合は下向きの速度を加算）
                    double currentY = this.mob.getDeltaMovement().y;
                    if (!this.mob.onGround()) {
                        currentY -= 0.08D; // バニラ準拠の重力
                    }

                    Vec3 mv = this.chargeVec.scale(CHARGE_SPEED);
                    // Y軸の速度に重力を反映させる
                    this.mob.setDeltaMovement(mv.x, currentY, mv.z);

                    if (mob.level() instanceof ServerLevel sl
                            && mob.isPhase2()) {

                        spawnTrail(sl);
                    }

                    // プレイヤー接触判定
                    AABB hitBox = this.mob.getBoundingBox().inflate(1.0);

                    if (hitBox.intersects(t.getBoundingBox())) {
                        onChargeHit();
                        return;
                    }

                    Vec3 vel = this.mob.getDeltaMovement();

                    boolean wallHit =
                            Math.abs(vel.x) < 0.1 &&
                                    Math.abs(vel.z) < 0.1 &&
                                    this.chargeTimer > 5;

                    if (wallHit) {
                        endCharge();
                        return;
                    }

                    if (this.chargeTimer > 40)
                        endCharge();
                }
                // ── charge_end（終了）──
                case 3 -> {
                    // ── 終了時も重力を適用（Vec3.ZEROで止めない）──
                    Vec3 mv3 = mob.getDeltaMovement();
                    if (!mob.onGround()) {
                        mob.setDeltaMovement(mv3.x * 0.8, mv3.y - 0.08D, mv3.z * 0.8);
                    } else {
                        mob.setDeltaMovement(0, 0, 0);
                    }

                    if (this.chargeTimer == 10 && !this.chargeAttackDone) {
                        doChargeSlash();
                        this.chargeAttackDone = true;
                    }

                    if (this.chargeTimer >= 25) {
                        this.mob.setChargePhase(0);
                        this.chargeTimer = 0;
                        this.chargingActive = false;
                        this.cooldown = COOLDOWN_CHARGE;
                    }
                }
            }
        }

        private void doChargeSlash() {

            AABB box = this.mob.getBoundingBox()
                    .inflate(4.0, 2.0, 4.0);

            for (LivingEntity e : getHittableEntities(box)) {

                e.invulnerableTime = 0;

                float dmg =
                        (float) this.mob.getAttributeValue(
                                Attributes.ATTACK_DAMAGE) * 1.2F;
                mob.breakShield(e, 100);

                e.hurt(
                        this.mob.damageSources().mobAttack(this.mob),
                        dmg);

                Vec3 kb =
                        e.position()
                                .subtract(this.mob.position())
                                .normalize()
                                .scale(1.0);

                e.setDeltaMovement(kb.x, 1.0, kb.z);
                e.hurtMarked = true;
            }

            this.mob.playSound(
                    SoundEvents.GENERIC_EXPLODE,
                    2.0F,
                    0.8F);
        }

        private void doJumpSlam() {
            // ── 基本ダメージ判定（全形態共通）──
            AABB box = mob.getBoundingBox().inflate(5.0);

            for (LivingEntity e : getHittableEntities(box)) {
                e.invulnerableTime = 0;
                float dmg = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3F;
                mob.breakShield(e, 100);
                e.hurt(mob.damageSources().mobAttack(mob), dmg);

                Vec3 kb = e.position().subtract(mob.position()).normalize().scale(3.0);
                e.setDeltaMovement(kb.x, 0.8, kb.z);
                e.hurtMarked = true;
            }

            // 着地衝撃波パーティクル（全形態共通）
            spawnExpandingShockwave();
            mob.playSound(SoundEvents.GENERIC_EXPLODE, 2.5F, 0.5F);
            mob.sendScreenShake(1F, 12);

            // ── 第二形態：爆発＋火柱 ──
            if (mob.isPhase2() && mob.level() instanceof ServerLevel sl) {

                // 爆発エフェクト
                sl.sendParticles(
                        ParticleTypes.EXPLOSION_EMITTER,
                        mob.getX(), mob.getY() + 0.5, mob.getZ(),
                        3, 1.0, 0.5, 1.0, 0.1
                );
                sl.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        mob.getX(), mob.getY() + 1.0, mob.getZ(),
                        60, 2.0, 1.0, 2.0, 0.2
                );

                // 周囲にランダム火柱（即時〜遅延）
                scheduleRandomFirePillars(0);  // 即時に6〜10本
                scheduleRandomFirePillars(8);  // 8tick後にさらに追加
            }
        }

        private void spawnExpandingShockwave() {

            if (!(this.mob.level() instanceof ServerLevel sl))
                return;

            for (int radius = 1;
                 radius <= 8;
                 radius++) {

                int finalRadius = radius;

                sl.getServer().tell(new net.minecraft.server.TickTask(
                        sl.getServer().getTickCount()
                                + radius * 2,

                        () -> {

                            for (int i = 0; i < 24; i++) {

                                double angle =
                                        Math.PI * 2
                                                * i / 24;

                                sl.sendParticles(
                                        ParticleTypes.SOUL_FIRE_FLAME,

                                        mob.getX()
                                                + Math.cos(angle)
                                                * finalRadius,

                                        mob.getY() + 0.2,

                                        mob.getZ()
                                                + Math.sin(angle)
                                                * finalRadius,

                                        1,
                                        0,
                                        0,
                                        0,
                                        0);
                            }
                        }));
            }
        }

        private void onChargeHit() {
            this.chargeEndYaw = this.mob.getYRot();

            endCharge();
        }

        /**
         * 突進終了
         */
        private void endCharge() {
            this.chargeTimer = 0;
            this.chargeAttackDone = false;
            this.mob.setDeltaMovement(Vec3.ZERO);
            this.mob.setChargePhase(3);
        }

        // tickChargeと同じ考え方のもう一つの複数フェーズ攻撃（ジャンプ切り）。
        // JUMP_SLAM_PHASEで「予備動作→滞空・落下→着地衝撃」を管理する。
        private void tickJumpSlam(LivingEntity target, int phase) {
            jumpSlamTimer++;

            switch (phase) {

                // phase1：予備動作（30tick）
                case 1 -> {
                    if (jumpSlamTimer >= 30) {
                        jumpSlamTimer = 0;

                        Vec3 toTarget = jumpTarget.subtract(mob.position());
                        double dist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);

                        // 水平速度：20ブロック先まで届くよう上限を引き上げ
                        double hSpeed = Math.max(0.6, Math.min(dist / 8.0, 3.5));
                        // 垂直速度：遠いほど高く跳ぶ
                        double vSpeed = 0.85 + dist * 0.04;

                        Vec3 dir = new Vec3(toTarget.x, 0, toTarget.z).normalize();
                        mob.setDeltaMovement(
                                dir.x * hSpeed,
                                vSpeed,
                                dir.z * hSpeed
                        );

                        mob.setJumpSlamPhase(2);
                    }
                }

                // phase2：飛行中（着地判定）
                case 2 -> {
                    // 重力を手動適用（放物線）
                    Vec3 mv = mob.getDeltaMovement();
                    mob.setDeltaMovement(mv.x, mv.y - 0.08, mv.z);

                    // 着地判定：地面に触れたか、jumpTargetに十分近づいたか
                    boolean nearTarget = mob.position().distanceTo(jumpTarget) < 2.0
                            && mob.getDeltaMovement().y < 0;
                    boolean onGround = mob.onGround();

                    if (nearTarget || onGround) {
                        jumpSlamTimer = 0;
                        // 着地位置にスナップ（ずれ防止）
                        mob.setDeltaMovement(Vec3.ZERO);
                        mob.setJumpSlamPhase(3);
                    }

                    // タイムアウト（100tick経過で強制着地）
                    if (jumpSlamTimer >= 100) {
                        jumpSlamTimer = 0;
                        mob.setDeltaMovement(Vec3.ZERO);
                        mob.setJumpSlamPhase(3);
                    }
                }

                // phase3：着地と同時に判定発生
                case 3 -> {
                    if (jumpSlamTimer == 5 && !slamDone) {
                        slamDone = true;
                        doJumpSlam();
                    }

                    if (jumpSlamTimer >= 20) {
                        mob.setJumpSlamPhase(0);
                        jumpSlamTimer = 0;
                        cooldown = 40;
                    }
                }
            }
        }

        // ガード（防御）もCHARGE/JUMP_SLAMと同じ「専用フェーズ変数」方式。
        // hurt()側で「一定条件を満たしたらガードに入る」という判定を行い、
        // ここでは純粋にガード中の演出・硬直だけを進行させている。
        private void tickGuard() {

            guardTimer++;

            Vec3 mv = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(0, mv.y, 0); // ガード中は水平方向の慣性を殺して足を完全に止める

            switch (mob.getGuardPhase()) {

                case 1 -> { // start

                    if (guardTimer >= 5) {

                        guardTimer = 0;
                        mob.setGuardPhase(2);
                    }
                }

                case 2 -> { // loop

                    if (guardTimer >= 20) {

                        guardTimer = 0;
                        mob.setGuardPhase(3);
                    }
                }

                case 3 -> { // end

                    if (guardTimer >= 5) {

                        guardTimer = 0;
                        mob.setGuardPhase(0);
                    }
                }
            }
        }

        // ──────────────────────────────────────
        // 攻撃終了
        // ──────────────────────────────────────
        private void finishAttack(int cd) {
            this.attackTimer = 0;
            this.cooldown = cd;
            this.mob.setAttackState(0);
            this.mob.setDeltaMovement(Vec3.ZERO);
        }

        // ──────────────────────────────────────
        // 攻撃対象フィルタ
        // ──────────────────────────────────────
        private List<LivingEntity> getHittableEntities(AABB box) {
            return this.mob.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != this.mob
                            && e.isAlive()
                            && isHostileToGradius(e)  // ← 変更
            );
        }

        // ──────────────────────────────────────
        // パーティクル
        // ──────────────────────────────────────
        private void spawnTrail(ServerLevel sl) {
            for (Vec3 p : mob.trail) {
                sl.sendParticles(
                        ParticleTypes.SOUL,
                        p.x, p.y + 0.5, p.z,
                        2,
                        0.1, 0.1, 0.1,
                        0.01
                );
            }
        }

        private void spawnSlashParticles(Vec3 look) {
            if (!(this.mob.level() instanceof ServerLevel sl)) return;
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18 - 90);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.mob.getX() + Math.cos(angle) * 3,
                        this.mob.getY() + 2.0,
                        this.mob.getZ() + Math.sin(angle) * 3,
                        1, 0, 0, 0, 0);
            }
        }

        private void spawnShockwaveParticles(Vec3 center, double arcDeg, double radius) {
            if (!(this.mob.level() instanceof ServerLevel sl)) return;
            int steps = (int) (arcDeg / 6);
            double startAngle = arcDeg < 360
                    ? Math.atan2(this.mob.getLookAngle().z, this.mob.getLookAngle().x) - Math.toRadians(arcDeg / 2)
                    : 0;

            for (int i = 0; i < steps; i++) {
                double angle = startAngle + Math.toRadians(arcDeg / steps * i);
                sl.sendParticles(ParticleTypes.EXPLOSION,
                        center.x + Math.cos(angle) * radius,
                        center.y,
                        center.z + Math.sin(angle) * radius,
                        1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.SMOKE,
                        center.x + Math.cos(angle) * (radius * 0.6),
                        center.y + 0.3,
                        center.z + Math.sin(angle) * (radius * 0.6),
                        2, 0.2, 0.1, 0.2, 0.02);
            }
        }

        // グラディウスに敵対しているかを判定
        private boolean isHostileToGradius(LivingEntity entity) {
            if (entity instanceof Player) return true;

            // グラディウスのターゲットは常に対象
            if (entity == this.target) return true;

            if (entity instanceof net.minecraft.world.entity.Mob hostileMob) {
                return hostileMob.getTarget() == this.mob;
            }
            return false;
        }
        private void spawnBlockWaveRing(ServerLevel level, BlockPos center,
                                        int currentRadius, double startAngle, double endAngle) {
            for (int x = -currentRadius; x <= currentRadius; x++) {
                for (int z = -currentRadius; z <= currentRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance >= currentRadius - 0.5 && distance <= currentRadius + 0.5) {

                        // ── 扇状チェック（startAngle == -1なら全周）──
                        if (startAngle > -Math.PI * 2) { // -1なら全周
                            double blockAngle = Math.atan2(z, x);

                            double arcSize  = angleDiff(endAngle, startAngle);
                            double diffFromStart = angleDiff(blockAngle, startAngle);

                            if (diffFromStart > arcSize) continue;
                        }

                        BlockPos targetPos = center.offset(x, -1, z);
                        BlockState state = level.getBlockState(targetPos);

                        if (!state.isAir() && state.getDestroySpeed(level, targetPos) >= 0) {
                            FallingBlockEntity fallingBlock =
                                    new FallingBlockEntity(EntityType.FALLING_BLOCK, level);
                            fallingBlock.setPos(
                                    targetPos.getX() + 0.5D,
                                    targetPos.getY() + 1.0D,
                                    targetPos.getZ() + 0.5D
                            );

                            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                            fallingBlock.saveWithoutId(tag);
                            tag.put("BlockState", net.minecraft.nbt.NbtUtils.writeBlockState(state));
                            tag.putInt("Time", 580);
                            tag.putBoolean("DropItem", false);
                            tag.putBoolean("NoPhysics", true);
                            fallingBlock.load(tag);

                            fallingBlock.noPhysics = true;
                            fallingBlock.setDeltaMovement(0, 0.4D, 0);

                            level.addFreshEntity(fallingBlock);
                            activeWaveBlocks.add(fallingBlock);
                        }
                    }
                }
            }
        }
        // 全周版（踏みつけ・飛び切りから呼ばれる）
        private void spawnBlockWaveRing(ServerLevel level, BlockPos center, int currentRadius) {
            spawnBlockWaveRing(level, center, currentRadius,
                    -Math.PI * 3, -Math.PI * 3); // 全周フラグ
        }
        private static double angleDiff(double a, double b) {
            double diff = a - b;
            while (diff < 0) diff += Math.PI * 2;
            while (diff > Math.PI * 2) diff -= Math.PI * 2;
            return diff;
        }
        // ── 予約済みブロック衝撃波 ──
        // ScheduledFirePillarと同じ「今すぐ実行せず、条件だけ持たせて後で処理する」方式。
        // こちらは「同心円状に広がる衝撃波」を表現するため、中心座標(center)・半径(radius)・
        // 角度範囲(startAngle〜endAngle)を持つ。扇状攻撃(叩きつけ)と全周攻撃(踏みつけ)を
        // 同じクラスで表現できるよう、角度範囲を「-π*3」という通常あり得ない値にすることで
        // 「全周（角度制限なし）」を表すフラグとして流用している。
        private static class ScheduledBlockWave {
            int remainingTicks;
            BlockPos center;
            int radius;
            double startAngle; // 開始角度（ラジアン）、-1なら全周
            double endAngle;   // 終了角度（ラジアン）

            // 全周コンストラクタ（踏みつけ・飛び切り）
            ScheduledBlockWave(int remainingTicks, BlockPos center, int radius) {
                this(remainingTicks, center, radius,
                        -Math.PI * 3, -Math.PI * 3); // -1 → -π*3（全周フラグ）
            }

            // 扇状コンストラクタ（叩きつけ第一形態）
            ScheduledBlockWave(int remainingTicks, BlockPos center, int radius,
                               double startAngle, double endAngle) {
                this.remainingTicks = remainingTicks;
                this.center = center;
                this.radius = radius;
                this.startAngle = startAngle;
                this.endAngle = endAngle;
            }
        }
        private void tickWaveBlockDamage() {
            if (mob.level().isClientSide()) return;

            activeWaveBlocks.removeIf(block -> {
                if (!block.isAlive()) return true;

                AABB hitBox = block.getBoundingBox().inflate(0.1);
                List<LivingEntity> hits = mob.level().getEntitiesOfClass(
                        LivingEntity.class, hitBox,
                        e -> e != mob
                                && e.isAlive()
                                && e.invulnerableTime <= 0  // ← 無敵時間チェック追加
                                && isHostileToGradius(e)
                );

                for (LivingEntity e : hits) {
                    // invulnerableTime = 0 は削除（無敵時間を尊重する）
                    e.hurt(
                            mob.damageSources().mobAttack(mob),
                            (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F
                    );
                    Vec3 kb = e.position().subtract(mob.position())
                            .normalize().scale(1.5);
                    e.setDeltaMovement(kb.x, 0.5, kb.z);
                    e.hurtMarked = true;
                }

                return false;
            });
        }
    }
}