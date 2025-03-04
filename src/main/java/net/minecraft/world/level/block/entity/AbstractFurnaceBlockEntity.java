package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeHolder, StackedContentsCompatible {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int SLOT_RESULT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    int litTime;
    int litDuration;
    int cookingProgress;
    int cookingTotalTime;
    protected final ContainerData dataAccess = new ContainerData() {
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    return AbstractFurnaceBlockEntity.this.litTime;

                case 1:
                    return AbstractFurnaceBlockEntity.this.litDuration;

                case 2:
                    return AbstractFurnaceBlockEntity.this.cookingProgress;

                case 3:
                    return AbstractFurnaceBlockEntity.this.cookingTotalTime;

                default:
                    return 0;
            }
        }

        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    AbstractFurnaceBlockEntity.this.litTime = p_58434_;
                    break;

                case 1:
                    AbstractFurnaceBlockEntity.this.litDuration = p_58434_;
                    break;

                case 2:
                    AbstractFurnaceBlockEntity.this.cookingProgress = p_58434_;
                    break;

                case 3:
                    AbstractFurnaceBlockEntity.this.cookingTotalTime = p_58434_;
            }
        }

        public int getCount() {
            return 4;
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;

    protected AbstractFurnaceBlockEntity(BlockEntityType<?> pType, BlockPos pWorldPosition, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType) {
        super(pType, pWorldPosition, pBlockState);
        this.recipeType = pRecipeType;
    }

    public static Map<Item, Integer> getFuel() {
        Map<Item, Integer> map = Maps.newLinkedHashMap();
        add(map, Items.LAVA_BUCKET, 20000);
        add(map, Blocks.COAL_BLOCK, 16000);
        add(map, Items.BLAZE_ROD, 2400);
        add(map, Items.COAL, 1600);
        add(map, Items.CHARCOAL, 1600);
        add(map, ItemTags.LOGS, 300);
        add(map, ItemTags.PLANKS, 300);
        add(map, ItemTags.WOODEN_STAIRS, 300);
        add(map, ItemTags.WOODEN_SLABS, 150);
        add(map, ItemTags.WOODEN_TRAPDOORS, 300);
        add(map, ItemTags.WOODEN_PRESSURE_PLATES, 300);
        add(map, Blocks.OAK_FENCE, 300);
        add(map, Blocks.BIRCH_FENCE, 300);
        add(map, Blocks.SPRUCE_FENCE, 300);
        add(map, Blocks.JUNGLE_FENCE, 300);
        add(map, Blocks.DARK_OAK_FENCE, 300);
        add(map, Blocks.ACACIA_FENCE, 300);
        add(map, Blocks.OAK_FENCE_GATE, 300);
        add(map, Blocks.BIRCH_FENCE_GATE, 300);
        add(map, Blocks.SPRUCE_FENCE_GATE, 300);
        add(map, Blocks.JUNGLE_FENCE_GATE, 300);
        add(map, Blocks.DARK_OAK_FENCE_GATE, 300);
        add(map, Blocks.ACACIA_FENCE_GATE, 300);
        add(map, Blocks.NOTE_BLOCK, 300);
        add(map, Blocks.BOOKSHELF, 300);
        add(map, Blocks.LECTERN, 300);
        add(map, Blocks.JUKEBOX, 300);
        add(map, Blocks.CHEST, 300);
        add(map, Blocks.TRAPPED_CHEST, 300);
        add(map, Blocks.CRAFTING_TABLE, 300);
        add(map, Blocks.DAYLIGHT_DETECTOR, 300);
        add(map, ItemTags.BANNERS, 300);
        add(map, Items.BOW, 300);
        add(map, Items.FISHING_ROD, 300);
        add(map, Blocks.LADDER, 300);
        add(map, ItemTags.SIGNS, 200);
        add(map, Items.WOODEN_SHOVEL, 200);
        add(map, Items.WOODEN_SWORD, 200);
        add(map, Items.WOODEN_HOE, 200);
        add(map, Items.WOODEN_AXE, 200);
        add(map, Items.WOODEN_PICKAXE, 200);
        add(map, ItemTags.WOODEN_DOORS, 200);
        add(map, ItemTags.BOATS, 1200);
        add(map, ItemTags.WOOL, 100);
        add(map, ItemTags.WOODEN_BUTTONS, 100);
        add(map, Items.STICK, 100);
        add(map, ItemTags.SAPLINGS, 100);
        add(map, Items.BOWL, 100);
        add(map, ItemTags.CARPETS, 67);
        add(map, Blocks.DRIED_KELP_BLOCK, 4001);
        add(map, Items.CROSSBOW, 300);
        add(map, Blocks.BAMBOO, 50);
        add(map, Blocks.DEAD_BUSH, 100);
        add(map, Blocks.SCAFFOLDING, 400);
        add(map, Blocks.LOOM, 300);
        add(map, Blocks.BARREL, 300);
        add(map, Blocks.CARTOGRAPHY_TABLE, 300);
        add(map, Blocks.FLETCHING_TABLE, 300);
        add(map, Blocks.SMITHING_TABLE, 300);
        add(map, Blocks.COMPOSTER, 300);
        add(map, Blocks.AZALEA, 100);
        add(map, Blocks.FLOWERING_AZALEA, 100);
        return map;
    }

    private static boolean isNeverAFurnaceFuel(Item pItem) {
        return ItemTags.NON_FLAMMABLE_WOOD.contains(pItem);
    }

    private static void add(Map<Item, Integer> pMap, Tag<Item> pItemTag, int pBurnTime) {
        for (Item item : pItemTag.getValues()) {
            if (!isNeverAFurnaceFuel(item)) {
                pMap.put(item, pBurnTime);
            }
        }
    }

    private static void add(Map<Item, Integer> pMap, ItemLike pItemTag, int pBurnTime) {
        Item item = pItemTag.asItem();

        if (isNeverAFurnaceFuel(item)) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw (IllegalStateException) Util.pauseInIde(new IllegalStateException("A developer tried to explicitly make fire resistant item " + item.getName((ItemStack) null).getString() + " a furnace fuel. That will not work!"));
            }
        } else {
            pMap.put(item, pBurnTime);
        }
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items);
        this.litTime = pTag.getShort("BurnTime");
        this.cookingProgress = pTag.getShort("CookTime");
        this.cookingTotalTime = pTag.getShort("CookTimeTotal");
        this.litDuration = this.getBurnDuration(this.items.get(1));
        CompoundTag compoundtag = pTag.getCompound("RecipesUsed");

        for (String s : compoundtag.getAllKeys()) {
            this.recipesUsed.put(new ResourceLocation(s), compoundtag.getInt(s));
        }
    }

    protected void saveAdditional(CompoundTag p_187452_) {
        super.saveAdditional(p_187452_);
        p_187452_.putShort("BurnTime", (short) this.litTime);
        p_187452_.putShort("CookTime", (short) this.cookingProgress);
        p_187452_.putShort("CookTimeTotal", (short) this.cookingTotalTime);
        ContainerHelper.saveAllItems(p_187452_, this.items);
        CompoundTag compoundtag = new CompoundTag();
        this.recipesUsed.forEach((p_187449_, p_187450_) ->
        {
            compoundtag.putInt(p_187449_.toString(), p_187450_);
        });
        p_187452_.put("RecipesUsed", compoundtag);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity) {
        boolean flag = pBlockEntity.isLit();
        boolean flag1 = false;

        if (pBlockEntity.isLit()) {
            --pBlockEntity.litTime;
        }

        ItemStack itemstack = pBlockEntity.items.get(1);

        if (pBlockEntity.isLit() || !itemstack.isEmpty() && !pBlockEntity.items.get(0).isEmpty()) {
            Recipe<?> recipe = pLevel.getRecipeManager().getRecipeFor((RecipeType<AbstractCookingRecipe>) pBlockEntity.recipeType, pBlockEntity, pLevel).orElse((AbstractCookingRecipe) null);
            int i = pBlockEntity.getMaxStackSize();

            if (!pBlockEntity.isLit() && canBurn(recipe, pBlockEntity.items, i)) {
                pBlockEntity.litTime = pBlockEntity.getBurnDuration(itemstack);
                pBlockEntity.litDuration = pBlockEntity.litTime;

                if (pBlockEntity.isLit()) {
                    flag1 = true;

                    if (!itemstack.isEmpty()) {
                        Item item = itemstack.getItem();
                        itemstack.shrink(1);

                        if (itemstack.isEmpty()) {
                            Item item1 = item.getCraftingRemainingItem();
                            pBlockEntity.items.set(1, item1 == null ? ItemStack.EMPTY : new ItemStack(item1));
                        }
                    }
                }
            }

            if (pBlockEntity.isLit() && canBurn(recipe, pBlockEntity.items, i)) {
                ++pBlockEntity.cookingProgress;

                if (pBlockEntity.cookingProgress == pBlockEntity.cookingTotalTime) {
                    pBlockEntity.cookingProgress = 0;
                    pBlockEntity.cookingTotalTime = getTotalCookTime(pLevel, pBlockEntity.recipeType, pBlockEntity);

                    if (burn(recipe, pBlockEntity.items, i)) {
                        pBlockEntity.setRecipeUsed(recipe);
                    }

                    flag1 = true;
                }
            } else {
                pBlockEntity.cookingProgress = 0;
            }
        } else if (!pBlockEntity.isLit() && pBlockEntity.cookingProgress > 0) {
            pBlockEntity.cookingProgress = Mth.clamp(pBlockEntity.cookingProgress - 2, 0, pBlockEntity.cookingTotalTime);
        }

        if (flag != pBlockEntity.isLit()) {
            flag1 = true;
            pState = pState.setValue(AbstractFurnaceBlock.LIT, Boolean.valueOf(pBlockEntity.isLit()));
            pLevel.setBlock(pPos, pState, 3);
        }

        if (flag1) {
            setChanged(pLevel, pPos, pState);
        }
    }

    private static boolean canBurn(@Nullable Recipe<?> p_155006_, NonNullList<ItemStack> p_155007_, int p_155008_) {
        if (!p_155007_.get(0).isEmpty() && p_155006_ != null) {
            ItemStack itemstack = p_155006_.getResultItem();

            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack itemstack1 = p_155007_.get(2);

                if (itemstack1.isEmpty()) {
                    return true;
                } else if (!itemstack1.sameItem(itemstack)) {
                    return false;
                } else if (itemstack1.getCount() < p_155008_ && itemstack1.getCount() < itemstack1.getMaxStackSize()) {
                    return true;
                } else {
                    return itemstack1.getCount() < itemstack.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean burn(@Nullable Recipe<?> p_155027_, NonNullList<ItemStack> p_155028_, int p_155029_) {
        if (p_155027_ != null && canBurn(p_155027_, p_155028_, p_155029_)) {
            ItemStack itemstack = p_155028_.get(0);
            ItemStack itemstack1 = p_155027_.getResultItem();
            ItemStack itemstack2 = p_155028_.get(2);

            if (itemstack2.isEmpty()) {
                p_155028_.set(2, itemstack1.copy());
            } else if (itemstack2.is(itemstack1.getItem())) {
                itemstack2.grow(1);
            }

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !p_155028_.get(1).isEmpty() && p_155028_.get(1).is(Items.BUCKET)) {
                p_155028_.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getBurnDuration(ItemStack pFuel) {
        if (pFuel.isEmpty()) {
            return 0;
        } else {
            Item item = pFuel.getItem();
            return getFuel().getOrDefault(item, 0);
        }
    }

    private static int getTotalCookTime(Level pLevel, RecipeType<? extends AbstractCookingRecipe> pRecipeType, Container pContainer) {
        return pLevel.getRecipeManager().getRecipeFor((RecipeType<AbstractCookingRecipe>) pRecipeType, pContainer, pLevel).map(AbstractCookingRecipe::getCookingTime).orElse(200);
    }

    public static boolean isFuel(ItemStack pStack) {
        return getFuel().containsKey(pStack.getItem());
    }

    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }

    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        if (pDirection == Direction.DOWN && pIndex == 1) {
            return pStack.is(Items.WATER_BUCKET) || pStack.is(Items.BUCKET);
        } else {
            return true;
        }
    }

    public int getContainerSize() {
        return this.items.size();
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public ItemStack getItem(int pIndex) {
        return this.items.get(pIndex);
    }

    public ItemStack removeItem(int pIndex, int pCount) {
        return ContainerHelper.removeItem(this.items, pIndex, pCount);
    }

    public ItemStack removeItemNoUpdate(int pIndex) {
        return ContainerHelper.takeItem(this.items, pIndex);
    }

    public void setItem(int pIndex, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pIndex);
        boolean flag = !pStack.isEmpty() && pStack.sameItem(itemstack) && ItemStack.tagMatches(pStack, itemstack);
        this.items.set(pIndex, pStack);

        if (pStack.getCount() > this.getMaxStackSize()) {
            pStack.setCount(this.getMaxStackSize());
        }

        if (pIndex == 0 && !flag) {
            this.cookingTotalTime = getTotalCookTime(this.level, this.recipeType, this);
            this.cookingProgress = 0;
            this.setChanged();
        }
    }

    public boolean stillValid(Player pPlayer) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return pPlayer.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == 2) {
            return false;
        } else if (pIndex != 1) {
            return true;
        } else {
            ItemStack itemstack = this.items.get(1);
            return isFuel(pStack) || pStack.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
        }
    }

    public void clearContent() {
        this.items.clear();
    }

    public void setRecipeUsed(@Nullable Recipe<?> pRecipe) {
        if (pRecipe != null) {
            ResourceLocation resourcelocation = pRecipe.getId();
            this.recipesUsed.addTo(resourcelocation, 1);
        }
    }

    @Nullable
    public Recipe<?> getRecipeUsed() {
        return null;
    }

    public void awardUsedRecipes(Player pPlayer) {
    }

    public void awardUsedRecipesAndPopExperience(ServerPlayer pPlayer) {
        List<Recipe<?>> list = this.getRecipesToAwardAndPopExperience(pPlayer.getLevel(), pPlayer.position());
        pPlayer.awardRecipes(list);
        this.recipesUsed.clear();
    }

    public List<Recipe<?>> getRecipesToAwardAndPopExperience(ServerLevel p_154996_, Vec3 p_154997_) {
        List<Recipe<?>> list = Lists.newArrayList();

        for (Entry<ResourceLocation> entry : this.recipesUsed.object2IntEntrySet()) {
            p_154996_.getRecipeManager().byKey(entry.getKey()).ifPresent((p_155023_) ->
            {
                list.add(p_155023_);
                createExperience(p_154996_, p_154997_, entry.getIntValue(), ((AbstractCookingRecipe) p_155023_).getExperience());
            });
        }

        return list;
    }

    private static void createExperience(ServerLevel p_154999_, Vec3 p_155000_, int p_155001_, float p_155002_) {
        int i = Mth.floor((float) p_155001_ * p_155002_);
        float f = Mth.frac((float) p_155001_ * p_155002_);

        if (f != 0.0F && Math.random() < (double) f) {
            ++i;
        }

        ExperienceOrb.award(p_154999_, p_155000_, i);
    }

    public void fillStackedContents(StackedContents pHelper) {
        for (ItemStack itemstack : this.items) {
            pHelper.accountStack(itemstack);
        }
    }
}
