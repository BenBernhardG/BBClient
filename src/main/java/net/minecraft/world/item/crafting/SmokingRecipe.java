package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class SmokingRecipe extends AbstractCookingRecipe
{
    public SmokingRecipe(ResourceLocation pId, String pGroup, Ingredient pIngredient, ItemStack pResult, float pExperience, int pCookingTime)
    {
        super(RecipeType.SMOKING, pId, pGroup, pIngredient, pResult, pExperience, pCookingTime);
    }

    public ItemStack getToastSymbol()
    {
        return new ItemStack(Blocks.SMOKER);
    }

    public RecipeSerializer<?> getSerializer()
    {
        return RecipeSerializer.SMOKING_RECIPE;
    }
}
