package net.minecraft.world.entity.animal.horse;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractHorse extends Animal implements ContainerListener, PlayerRideableJumping, Saddleable {
    public static final int EQUIPMENT_SLOT_OFFSET = 400;
    public static final int CHEST_SLOT_OFFSET = 499;
    public static final int INVENTORY_SLOT_OFFSET = 500;
    private static final Predicate<LivingEntity> PARENT_HORSE_SELECTOR = (p_30636_) ->
    {
        return p_30636_ instanceof AbstractHorse && ((AbstractHorse) p_30636_).isBred();
    };
    private static final TargetingConditions MOMMY_TARGETING = TargetingConditions.forNonCombat().range(16.0D).ignoreLineOfSight().selector(PARENT_HORSE_SELECTOR);
    private static final Ingredient FOOD_ITEMS = Ingredient.a(Items.WHEAT, Items.SUGAR, Blocks.HAY_BLOCK.asItem(), Items.APPLE, Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> DATA_ID_OWNER_UUID = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int FLAG_TAME = 2;
    private static final int FLAG_SADDLE = 4;
    private static final int FLAG_BRED = 8;
    private static final int FLAG_EATING = 16;
    private static final int FLAG_STANDING = 32;
    private static final int FLAG_OPEN_MOUTH = 64;
    public static final int INV_SLOT_SADDLE = 0;
    public static final int INV_SLOT_ARMOR = 1;
    public static final int INV_BASE_COUNT = 2;
    private int eatingCounter;
    private int mouthCounter;
    private int standCounter;
    public int tailCounter;
    public int sprintCounter;
    protected boolean isJumping;
    protected SimpleContainer inventory;
    protected int temper;
    protected float playerJumpPendingScale;
    private boolean allowStandSliding;
    private float eatAnim;
    private float eatAnimO;
    private float standAnim;
    private float standAnimO;
    private float mouthAnim;
    private float mouthAnimO;
    protected boolean canGallop = true;
    protected int gallopSoundCounter;

    protected AbstractHorse(EntityType<? extends AbstractHorse> p_30531_, Level p_30532_) {
        super(p_30531_, p_30532_);
        this.maxUpStep = 1.0F;
        this.createInventory();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D, AbstractHorse.class));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.a(Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE), false));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_FLAGS, (byte) 0);
        this.entityData.define(DATA_ID_OWNER_UUID, Optional.empty());
    }

    protected boolean getFlag(int p_30648_) {
        return (this.entityData.get(DATA_ID_FLAGS) & p_30648_) != 0;
    }

    protected void setFlag(int p_30598_, boolean p_30599_) {
        byte b0 = this.entityData.get(DATA_ID_FLAGS);

        if (p_30599_) {
            this.entityData.set(DATA_ID_FLAGS, (byte) (b0 | p_30598_));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte) (b0 & ~p_30598_));
        }
    }

    public boolean isTamed() {
        return this.getFlag(2);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_ID_OWNER_UUID).orElse((UUID) null);
    }

    public void setOwnerUUID(@Nullable UUID pUniqueId) {
        this.entityData.set(DATA_ID_OWNER_UUID, Optional.ofNullable(pUniqueId));
    }

    public boolean isJumping() {
        return this.isJumping;
    }

    public void setTamed(boolean pTamed) {
        this.setFlag(2, pTamed);
    }

    public void setIsJumping(boolean pJumping) {
        this.isJumping = pJumping;
    }

    protected void onLeashDistance(float pDistance) {
        if (pDistance > 6.0F && this.isEating()) {
            this.setEating(false);
        }
    }

    public boolean isEating() {
        return this.getFlag(16);
    }

    public boolean isStanding() {
        return this.getFlag(32);
    }

    public boolean isBred() {
        return this.getFlag(8);
    }

    public void setBred(boolean pBreeding) {
        this.setFlag(8, pBreeding);
    }

    public boolean isSaddleable() {
        return this.isAlive() && !this.isBaby() && this.isTamed();
    }

    public void equipSaddle(@Nullable SoundSource p_30546_) {
        this.inventory.setItem(0, new ItemStack(Items.SADDLE));

        if (p_30546_ != null) {
            this.level.playSound((Player) null, this, SoundEvents.HORSE_SADDLE, p_30546_, 0.5F, 1.0F);
        }
    }

    public boolean isSaddled() {
        return this.getFlag(4);
    }

    public int getTemper() {
        return this.temper;
    }

    public void setTemper(int pTemper) {
        this.temper = pTemper;
    }

    public int modifyTemper(int p_30654_) {
        int i = Mth.clamp(this.getTemper() + p_30654_, 0, this.getMaxTemper());
        this.setTemper(i);
        return i;
    }

    public boolean isPushable() {
        return !this.isVehicle();
    }

    private void eating() {
        this.openMouth();

        if (!this.isSilent()) {
            SoundEvent soundevent = this.getEatingSound();

            if (soundevent != null) {
                this.level.playSound((Player) null, this.getX(), this.getY(), this.getZ(), soundevent, this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
        }
    }

    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        if (pFallDistance > 1.0F) {
            this.playSound(SoundEvents.HORSE_LAND, 0.4F, 1.0F);
        }

        int i = this.calculateFallDamage(pFallDistance, pMultiplier);

        if (i <= 0) {
            return false;
        } else {
            this.hurt(pSource, (float) i);

            if (this.isVehicle()) {
                for (Entity entity : this.getIndirectPassengers()) {
                    entity.hurt(pSource, (float) i);
                }
            }

            this.playBlockFallSound();
            return true;
        }
    }

    protected int calculateFallDamage(float pDistance, float pDamageMultiplier) {
        return Mth.ceil((pDistance * 0.5F - 3.0F) * pDamageMultiplier);
    }

    protected int getInventorySize() {
        return 2;
    }

    protected void createInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(this.getInventorySize());

        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = simplecontainer.getItem(j);

                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.inventory.addListener(this);
        this.updateContainerEquipment();
    }

    protected void updateContainerEquipment() {
        if (!this.level.isClientSide) {
            this.setFlag(4, !this.inventory.getItem(0).isEmpty());
        }
    }

    public void containerChanged(Container pInvBasic) {
        boolean flag = this.isSaddled();
        this.updateContainerEquipment();

        if (this.tickCount > 20 && !flag && this.isSaddled()) {
            this.playSound(SoundEvents.HORSE_SADDLE, 0.5F, 1.0F);
        }
    }

    public double getCustomJump() {
        return this.getAttributeValue(Attributes.JUMP_STRENGTH);
    }

    @Nullable
    protected SoundEvent getEatingSound() {
        return null;
    }

    @Nullable
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        if (this.random.nextInt(3) == 0) {
            this.stand();
        }

        return null;
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        if (this.random.nextInt(10) == 0 && !this.isImmobile()) {
            this.stand();
        }

        return null;
    }

    @Nullable
    protected SoundEvent getAngrySound() {
        this.stand();
        return null;
    }

    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
        if (!pBlock.getMaterial().isLiquid()) {
            BlockState blockstate = this.level.getBlockState(pPos.above());
            SoundType soundtype = pBlock.getSoundType();

            if (blockstate.is(Blocks.SNOW)) {
                soundtype = blockstate.getSoundType();
            }

            if (this.isVehicle() && this.canGallop) {
                ++this.gallopSoundCounter;

                if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
                    this.playGallopSound(soundtype);
                } else if (this.gallopSoundCounter <= 5) {
                    this.playSound(SoundEvents.HORSE_STEP_WOOD, soundtype.getVolume() * 0.15F, soundtype.getPitch());
                }
            } else if (soundtype == SoundType.WOOD) {
                this.playSound(SoundEvents.HORSE_STEP_WOOD, soundtype.getVolume() * 0.15F, soundtype.getPitch());
            } else {
                this.playSound(SoundEvents.HORSE_STEP, soundtype.getVolume() * 0.15F, soundtype.getPitch());
            }
        }
    }

    protected void playGallopSound(SoundType p_30560_) {
        this.playSound(SoundEvents.HORSE_GALLOP, p_30560_.getVolume() * 0.15F, p_30560_.getPitch());
    }

    public static AttributeSupplier.Builder createBaseHorseAttributes() {
        return Mob.createMobAttributes().add(Attributes.JUMP_STRENGTH).add(Attributes.MAX_HEALTH, 53.0D).add(Attributes.MOVEMENT_SPEED, (double) 0.225F);
    }

    public int getMaxSpawnClusterSize() {
        return 6;
    }

    public int getMaxTemper() {
        return 100;
    }

    protected float getSoundVolume() {
        return 0.8F;
    }

    public int getAmbientSoundInterval() {
        return 400;
    }

    public void openInventory(Player pPlayerEntity) {
        if (!this.level.isClientSide && (!this.isVehicle() || this.hasPassenger(pPlayerEntity)) && this.isTamed()) {
            pPlayerEntity.openHorseInventory(this, this.inventory);
        }
    }

    public InteractionResult fedFood(Player p_30581_, ItemStack p_30582_) {
        boolean flag = this.handleEating(p_30581_, p_30582_);

        if (!p_30581_.getAbilities().instabuild) {
            p_30582_.shrink(1);
        }

        if (this.level.isClientSide) {
            return InteractionResult.CONSUME;
        } else {
            return flag ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
    }

    protected boolean handleEating(Player pPlayer, ItemStack pStack) {
        boolean flag = false;
        float f = 0.0F;
        int i = 0;
        int j = 0;

        if (pStack.is(Items.WHEAT)) {
            f = 2.0F;
            i = 20;
            j = 3;
        } else if (pStack.is(Items.SUGAR)) {
            f = 1.0F;
            i = 30;
            j = 3;
        } else if (pStack.is(Blocks.HAY_BLOCK.asItem())) {
            f = 20.0F;
            i = 180;
        } else if (pStack.is(Items.APPLE)) {
            f = 3.0F;
            i = 60;
            j = 3;
        } else if (pStack.is(Items.GOLDEN_CARROT)) {
            f = 4.0F;
            i = 60;
            j = 5;

            if (!this.level.isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                flag = true;
                this.setInLove(pPlayer);
            }
        } else if (pStack.is(Items.GOLDEN_APPLE) || pStack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            f = 10.0F;
            i = 240;
            j = 10;

            if (!this.level.isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                flag = true;
                this.setInLove(pPlayer);
            }
        }

        if (this.getHealth() < this.getMaxHealth() && f > 0.0F) {
            this.heal(f);
            flag = true;
        }

        if (this.isBaby() && i > 0) {
            this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);

            if (!this.level.isClientSide) {
                this.ageUp(i);
            }

            flag = true;
        }

        if (j > 0 && (flag || !this.isTamed()) && this.getTemper() < this.getMaxTemper()) {
            flag = true;

            if (!this.level.isClientSide) {
                this.modifyTemper(j);
            }
        }

        if (flag) {
            this.eating();
            this.gameEvent(GameEvent.EAT, this.eyeBlockPosition());
        }

        return flag;
    }

    protected void doPlayerRide(Player pPlayer) {
        this.setEating(false);
        this.setStanding(false);

        if (!this.level.isClientSide) {
            pPlayer.setYRot(this.getYRot());
            pPlayer.setXRot(this.getXRot());
            pPlayer.startRiding(this);
        }
    }

    protected boolean isImmobile() {
        return super.isImmobile() && this.isVehicle() && this.isSaddled() || this.isEating() || this.isStanding();
    }

    public boolean isFood(ItemStack pStack) {
        return FOOD_ITEMS.test(pStack);
    }

    private void moveTail() {
        this.tailCounter = 1;
    }

    protected void dropEquipment() {
        super.dropEquipment();

        if (this.inventory != null) {
            for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.inventory.getItem(i);

                if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack)) {
                    this.spawnAtLocation(itemstack);
                }
            }
        }
    }

    public void aiStep() {
        if (this.random.nextInt(200) == 0) {
            this.moveTail();
        }

        super.aiStep();

        if (!this.level.isClientSide && this.isAlive()) {
            if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
                this.heal(1.0F);
            }

            if (this.canEatGrass()) {
                if (!this.isEating() && !this.isVehicle() && this.random.nextInt(300) == 0 && this.level.getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
                    this.setEating(true);
                }

                if (this.isEating() && ++this.eatingCounter > 50) {
                    this.eatingCounter = 0;
                    this.setEating(false);
                }
            }

            this.followMommy();
        }
    }

    protected void followMommy() {
        if (this.isBred() && this.isBaby() && !this.isEating()) {
            LivingEntity livingentity = this.level.getNearestEntity(AbstractHorse.class, MOMMY_TARGETING, this, this.getX(), this.getY(), this.getZ(), this.getBoundingBox().inflate(16.0D));

            if (livingentity != null && this.distanceToSqr(livingentity) > 4.0D) {
                this.navigation.createPath(livingentity, 0);
            }
        }
    }

    public boolean canEatGrass() {
        return true;
    }

    public void tick() {
        super.tick();

        if (this.mouthCounter > 0 && ++this.mouthCounter > 30) {
            this.mouthCounter = 0;
            this.setFlag(64, false);
        }

        if ((this.isControlledByLocalInstance() || this.isEffectiveAi()) && this.standCounter > 0 && ++this.standCounter > 20) {
            this.standCounter = 0;
            this.setStanding(false);
        }

        if (this.tailCounter > 0 && ++this.tailCounter > 8) {
            this.tailCounter = 0;
        }

        if (this.sprintCounter > 0) {
            ++this.sprintCounter;

            if (this.sprintCounter > 300) {
                this.sprintCounter = 0;
            }
        }

        this.eatAnimO = this.eatAnim;

        if (this.isEating()) {
            this.eatAnim += (1.0F - this.eatAnim) * 0.4F + 0.05F;

            if (this.eatAnim > 1.0F) {
                this.eatAnim = 1.0F;
            }
        } else {
            this.eatAnim += (0.0F - this.eatAnim) * 0.4F - 0.05F;

            if (this.eatAnim < 0.0F) {
                this.eatAnim = 0.0F;
            }
        }

        this.standAnimO = this.standAnim;

        if (this.isStanding()) {
            this.eatAnim = 0.0F;
            this.eatAnimO = this.eatAnim;
            this.standAnim += (1.0F - this.standAnim) * 0.4F + 0.05F;

            if (this.standAnim > 1.0F) {
                this.standAnim = 1.0F;
            }
        } else {
            this.allowStandSliding = false;
            this.standAnim += (0.8F * this.standAnim * this.standAnim * this.standAnim - this.standAnim) * 0.6F - 0.05F;

            if (this.standAnim < 0.0F) {
                this.standAnim = 0.0F;
            }
        }

        this.mouthAnimO = this.mouthAnim;

        if (this.getFlag(64)) {
            this.mouthAnim += (1.0F - this.mouthAnim) * 0.7F + 0.05F;

            if (this.mouthAnim > 1.0F) {
                this.mouthAnim = 1.0F;
            }
        } else {
            this.mouthAnim += (0.0F - this.mouthAnim) * 0.7F - 0.05F;

            if (this.mouthAnim < 0.0F) {
                this.mouthAnim = 0.0F;
            }
        }
    }

    private void openMouth() {
        if (!this.level.isClientSide) {
            this.mouthCounter = 1;
            this.setFlag(64, true);
        }
    }

    public void setEating(boolean p_30662_) {
        this.setFlag(16, p_30662_);
    }

    public void setStanding(boolean pRearing) {
        if (pRearing) {
            this.setEating(false);
        }

        this.setFlag(32, pRearing);
    }

    private void stand() {
        if (this.isControlledByLocalInstance() || this.isEffectiveAi()) {
            this.standCounter = 1;
            this.setStanding(true);
        }
    }

    public void makeMad() {
        if (!this.isStanding()) {
            this.stand();
            SoundEvent soundevent = this.getAngrySound();

            if (soundevent != null) {
                this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
            }
        }
    }

    public boolean tameWithName(Player pPlayer) {
        this.setOwnerUUID(pPlayer.getUUID());
        this.setTamed(true);

        if (pPlayer instanceof ServerPlayer) {
            CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayer) pPlayer, this);
        }

        this.level.broadcastEntityEvent(this, (byte) 7);
        return true;
    }

    public void travel(Vec3 pTravelVector) {
        if (this.isAlive()) {
            if (this.isVehicle() && this.canBeControlledByRider() && this.isSaddled()) {
                LivingEntity livingentity = (LivingEntity) this.getControllingPassenger();
                this.setYRot(livingentity.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(livingentity.getXRot() * 0.5F);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.yBodyRot;
                float f = livingentity.xxa * 0.5F;
                float f1 = livingentity.zza;

                if (f1 <= 0.0F) {
                    f1 *= 0.25F;
                    this.gallopSoundCounter = 0;
                }

                if (this.onGround && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
                    f = 0.0F;
                    f1 = 0.0F;
                }

                if (this.playerJumpPendingScale > 0.0F && !this.isJumping() && this.onGround) {
                    double d0 = this.getCustomJump() * (double) this.playerJumpPendingScale * (double) this.getBlockJumpFactor();
                    double d1 = d0 + this.getJumpBoostPower();
                    Vec3 vec3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3.x, d1, vec3.z);
                    this.setIsJumping(true);
                    this.hasImpulse = true;

                    if (f1 > 0.0F) {
                        float f2 = Mth.sin(this.getYRot() * ((float) Math.PI / 180F));
                        float f3 = Mth.cos(this.getYRot() * ((float) Math.PI / 180F));
                        this.setDeltaMovement(this.getDeltaMovement().add((double) (-0.4F * f2 * this.playerJumpPendingScale), 0.0D, (double) (0.4F * f3 * this.playerJumpPendingScale)));
                    }

                    this.playerJumpPendingScale = 0.0F;
                }

                this.flyingSpeed = this.getSpeed() * 0.1F;

                if (this.isControlledByLocalInstance()) {
                    this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    super.travel(new Vec3((double) f, pTravelVector.y, (double) f1));
                } else if (livingentity instanceof Player) {
                    this.setDeltaMovement(Vec3.ZERO);
                }

                if (this.onGround) {
                    this.playerJumpPendingScale = 0.0F;
                    this.setIsJumping(false);
                }

                this.calculateEntityAnimation(this, false);
                this.tryCheckInsideBlocks();
            } else {
                this.flyingSpeed = 0.02F;
                super.travel(pTravelVector);
            }
        }
    }

    protected void playJumpSound() {
        this.playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("EatingHaystack", this.isEating());
        pCompound.putBoolean("Bred", this.isBred());
        pCompound.putInt("Temper", this.getTemper());
        pCompound.putBoolean("Tame", this.isTamed());

        if (this.getOwnerUUID() != null) {
            pCompound.putUUID("Owner", this.getOwnerUUID());
        }

        if (!this.inventory.getItem(0).isEmpty()) {
            pCompound.put("SaddleItem", this.inventory.getItem(0).save(new CompoundTag()));
        }
    }

    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setEating(pCompound.getBoolean("EatingHaystack"));
        this.setBred(pCompound.getBoolean("Bred"));
        this.setTemper(pCompound.getInt("Temper"));
        this.setTamed(pCompound.getBoolean("Tame"));
        UUID uuid;

        if (pCompound.hasUUID("Owner")) {
            uuid = pCompound.getUUID("Owner");
        } else {
            String s = pCompound.getString("Owner");
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
        }

        if (uuid != null) {
            this.setOwnerUUID(uuid);
        }

        if (pCompound.contains("SaddleItem", 10)) {
            ItemStack itemstack = ItemStack.of(pCompound.getCompound("SaddleItem"));

            if (itemstack.is(Items.SADDLE)) {
                this.inventory.setItem(0, itemstack);
            }
        }

        this.updateContainerEquipment();
    }

    public boolean canMate(Animal pOtherAnimal) {
        return false;
    }

    protected boolean canParent() {
        return !this.isVehicle() && !this.isPassenger() && this.isTamed() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel p_149506_, AgeableMob p_149507_) {
        return null;
    }

    protected void setOffspringAttributes(AgeableMob p_149509_, AbstractHorse p_149510_) {
        double d0 = this.getAttributeBaseValue(Attributes.MAX_HEALTH) + p_149509_.getAttributeBaseValue(Attributes.MAX_HEALTH) + (double) this.generateRandomMaxHealth();
        p_149510_.getAttribute(Attributes.MAX_HEALTH).setBaseValue(d0 / 3.0D);
        double d1 = this.getAttributeBaseValue(Attributes.JUMP_STRENGTH) + p_149509_.getAttributeBaseValue(Attributes.JUMP_STRENGTH) + this.generateRandomJumpStrength();
        p_149510_.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(d1 / 3.0D);
        double d2 = this.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) + p_149509_.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) + this.generateRandomSpeed();
        p_149510_.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(d2 / 3.0D);
    }

    public boolean canBeControlledByRider() {
        return this.getControllingPassenger() instanceof LivingEntity;
    }

    public float getEatAnim(float p_30664_) {
        return Mth.lerp(p_30664_, this.eatAnimO, this.eatAnim);
    }

    public float getStandAnim(float p_30668_) {
        return Mth.lerp(p_30668_, this.standAnimO, this.standAnim);
    }

    public float getMouthAnim(float p_30534_) {
        return Mth.lerp(p_30534_, this.mouthAnimO, this.mouthAnim);
    }

    public void onPlayerJump(int pJumpPower) {
        if (this.isSaddled()) {
            if (pJumpPower < 0) {
                pJumpPower = 0;
            } else {
                this.allowStandSliding = true;
                this.stand();
            }

            if (pJumpPower >= 90) {
                this.playerJumpPendingScale = 1.0F;
            } else {
                this.playerJumpPendingScale = 0.4F + 0.4F * (float) pJumpPower / 90.0F;
            }
        }
    }

    public boolean canJump() {
        return this.isSaddled();
    }

    public void handleStartJump(int pJumpPower) {
        this.allowStandSliding = true;
        this.stand();
        this.playJumpSound();
    }

    public void handleStopJump() {
    }

    protected void spawnTamingParticles(boolean p_30670_) {
        ParticleOptions particleoptions = p_30670_ ? ParticleTypes.HEART : ParticleTypes.SMOKE;

        for (int i = 0; i < 7; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(particleoptions, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
        }
    }

    public void handleEntityEvent(byte pId) {
        if (pId == 7) {
            this.spawnTamingParticles(true);
        } else if (pId == 6) {
            this.spawnTamingParticles(false);
        } else {
            super.handleEntityEvent(pId);
        }
    }

    public void positionRider(Entity pPassenger) {
        super.positionRider(pPassenger);

        if (pPassenger instanceof Mob) {
            Mob mob = (Mob) pPassenger;
            this.yBodyRot = mob.yBodyRot;
        }

        if (this.standAnimO > 0.0F) {
            float f3 = Mth.sin(this.yBodyRot * ((float) Math.PI / 180F));
            float f = Mth.cos(this.yBodyRot * ((float) Math.PI / 180F));
            float f1 = 0.7F * this.standAnimO;
            float f2 = 0.15F * this.standAnimO;
            pPassenger.setPos(this.getX() + (double) (f1 * f3), this.getY() + this.getPassengersRidingOffset() + pPassenger.getMyRidingOffset() + (double) f2, this.getZ() - (double) (f1 * f));

            if (pPassenger instanceof LivingEntity) {
                ((LivingEntity) pPassenger).yBodyRot = this.yBodyRot;
            }
        }
    }

    protected float generateRandomMaxHealth() {
        return 15.0F + (float) this.random.nextInt(8) + (float) this.random.nextInt(9);
    }

    protected double generateRandomJumpStrength() {
        return (double) 0.4F + this.random.nextDouble() * 0.2D + this.random.nextDouble() * 0.2D + this.random.nextDouble() * 0.2D;
    }

    protected double generateRandomSpeed() {
        return ((double) 0.45F + this.random.nextDouble() * 0.3D + this.random.nextDouble() * 0.3D + this.random.nextDouble() * 0.3D) * 0.25D;
    }

    public boolean onClimbable() {
        return false;
    }

    protected float getStandingEyeHeight(Pose pPose, EntityDimensions pSize) {
        return pSize.height * 0.95F;
    }

    public boolean canWearArmor() {
        return false;
    }

    public boolean isWearingArmor() {
        return !this.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
    }

    public boolean isArmor(ItemStack pStack) {
        return false;
    }

    private SlotAccess createEquipmentSlotAccess(final int p_149503_, final Predicate<ItemStack> p_149504_) {
        return new SlotAccess() {
            public ItemStack get() {
                return AbstractHorse.this.inventory.getItem(p_149503_);
            }

            public boolean set(ItemStack p_149528_) {
                if (!p_149504_.test(p_149528_)) {
                    return false;
                } else {
                    AbstractHorse.this.inventory.setItem(p_149503_, p_149528_);
                    AbstractHorse.this.updateContainerEquipment();
                    return true;
                }
            }
        };
    }

    public SlotAccess getSlot(int pSlot) {
        int i = pSlot - 400;

        if (i >= 0 && i < 2 && i < this.inventory.getContainerSize()) {
            if (i == 0) {
                return this.createEquipmentSlotAccess(i, (p_149518_) ->
                {
                    return p_149518_.isEmpty() || p_149518_.is(Items.SADDLE);
                });
            }

            if (i == 1) {
                if (!this.canWearArmor()) {
                    return SlotAccess.NULL;
                }

                return this.createEquipmentSlotAccess(i, (p_149516_) ->
                {
                    return p_149516_.isEmpty() || this.isArmor(p_149516_);
                });
            }
        }

        int j = pSlot - 500 + 2;
        return j >= 2 && j < this.inventory.getContainerSize() ? SlotAccess.forContainer(this.inventory, j) : super.getSlot(pSlot);
    }

    @Nullable
    public Entity getControllingPassenger() {
        return this.getFirstPassenger();
    }

    @Nullable
    private Vec3 getDismountLocationInDirection(Vec3 p_30562_, LivingEntity p_30563_) {
        double d0 = this.getX() + p_30562_.x;
        double d1 = this.getBoundingBox().minY;
        double d2 = this.getZ() + p_30562_.z;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Pose pose : p_30563_.getDismountPoses()) {
            blockpos$mutableblockpos.set(d0, d1, d2);
            double d3 = this.getBoundingBox().maxY + 0.75D;

            while (true) {
                double d4 = this.level.getBlockFloorHeight(blockpos$mutableblockpos);

                if ((double) blockpos$mutableblockpos.getY() + d4 > d3) {
                    break;
                }

                if (DismountHelper.isBlockFloorValid(d4)) {
                    AABB aabb = p_30563_.getLocalBoundsForPose(pose);
                    Vec3 vec3 = new Vec3(d0, (double) blockpos$mutableblockpos.getY() + d4, d2);

                    if (DismountHelper.canDismountTo(this.level, p_30563_, aabb.move(vec3))) {
                        p_30563_.setPose(pose);
                        return vec3;
                    }
                }

                blockpos$mutableblockpos.move(Direction.UP);

                if (!((double) blockpos$mutableblockpos.getY() < d3)) {
                    break;
                }
            }
        }

        return null;
    }

    public Vec3 getDismountLocationForPassenger(LivingEntity pLivingEntity) {
        Vec3 vec3 = getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) pLivingEntity.getBbWidth(), this.getYRot() + (pLivingEntity.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F));
        Vec3 vec31 = this.getDismountLocationInDirection(vec3, pLivingEntity);

        if (vec31 != null) {
            return vec31;
        } else {
            Vec3 vec32 = getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) pLivingEntity.getBbWidth(), this.getYRot() + (pLivingEntity.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F));
            Vec3 vec33 = this.getDismountLocationInDirection(vec32, pLivingEntity);
            return vec33 != null ? vec33 : this.position();
        }
    }

    protected void randomizeAttributes() {
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        if (pSpawnData == null) {
            pSpawnData = new AgeableMob.AgeableMobGroupData(0.2F);
        }

        this.randomizeAttributes();
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    public boolean hasInventoryChanged(Container p_149512_) {
        return this.inventory != p_149512_;
    }
}
