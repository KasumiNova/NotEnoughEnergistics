package com.github.vfyjxf.nee.helper;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.me.ItemRepo;
import appeng.util.item.AEItemStack;
import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.utils.IngredientStatus;
import com.github.vfyjxf.nee.utils.ItemUtils;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A tool class to get the ingredients available in ae for the current recipe.
 * TODO:Wireless Crafting Term support
 */
public class RecipeAnalyzer {

    private static boolean shouldCleanCache = false;

    @Nonnull
    private static List<IAEItemStack> craftableCache = new ArrayList<>();
    @Nonnull
    private static List<IAEItemStack> allStacksCache = new ArrayList<>();
    private final GuiMEMonitorable term;
    private final boolean craftableOnly;
    private final boolean isWireless;
    /**
     * Contains player inventory and crafting grid.
     * Not contain stacks in the network.
     */
    private final List<ItemStack> availableItems = new ArrayList<>();
    private long lastUpdateTime;

    public RecipeAnalyzer(GuiPatternTerm patternTerm) {
        this.term = patternTerm;
        this.craftableOnly = true;
        this.isWireless = false;
        if (shouldCleanCache) clearCache();

        if (craftableCache.isEmpty()) {
            craftableCache = getStorage().stream()
                    .filter(IAEItemStack::isCraftable)
                    .collect(Collectors.toList());
        }
    }

    public RecipeAnalyzer(GuiCraftingTerm craftingTerm) {
        this(craftingTerm, shouldCleanCache);
    }

    public RecipeAnalyzer(GuiCraftingTerm craftingTerm, boolean cleanCache) {
        this.term = craftingTerm;
        this.craftableOnly = false;
        this.isWireless = false;
        if (cleanCache) clearCache();
        if (allStacksCache.isEmpty()) allStacksCache = getStorage();
        NotEnoughEnergistics.logger.debug("Network Storage Item:" + allStacksCache.size());
    }

    public static void setCleanCache(boolean cleanCache) {
        RecipeAnalyzer.shouldCleanCache = cleanCache;
    }

    public List<RecipeIngredient> analyzeRecipe(IRecipeLayout recipeLayout) {
        Stream<List<IGuiIngredient<ItemStack>>> merged = mergeIngredients(recipeLayout).stream();
        if (craftableOnly) {
            //Craftable mode is only used for highlighted rendering, so we don't need an identifier.
            return merged.filter(ingredient -> craftableCache.parallelStream()
                            .anyMatch(craftable -> ingredient.get(0)
                                    .getAllIngredients()
                                    .parallelStream()
                                    .anyMatch(stack -> ItemUtils.matches(stack, craftable.getDefinition()))
                            )
                    ).map(ingredient -> new RecipeIngredient(IngredientStatus.CRAFTABLE, ItemStack.EMPTY, ingredient))
                    .collect(Collectors.toList());
        } else {
            //fist check EXISTS and available ingredient,if not enough,use craftable ingredient
            return merged.map(this::getExistData).collect(Collectors.toList());
        }

    }

    public void addAvailableIngredient(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) return;
        boolean find = availableItems.stream()
                .anyMatch(itemStack -> IngredientMerger.unlimitedMergeStack(itemStack, stack));
        if (!find) availableItems.add(stack.copy());
    }

    public void update() {
        if (NEEConfig.getUpdateIntervalTime() < 0) return;

        if (System.currentTimeMillis() - lastUpdateTime > NEEConfig.getUpdateIntervalTime()) {
            lastUpdateTime = System.currentTimeMillis();
            //TODO:
        }
    }

    public static class RecipeIngredient {

        private final IngredientStatus status;
        private final ItemStack identifier;
        private final List<IGuiIngredient<ItemStack>> merged;
        private final int missingCount;
        private final int missingSlots;

        public RecipeIngredient(IngredientStatus status, ItemStack identifier, List<IGuiIngredient<ItemStack>> merged, int missingCount) {
            this.status = status;
            this.identifier = identifier;
            this.merged = merged;
            this.missingCount = Math.max(missingCount, 0);
            this.missingSlots = getMissingSlots(missingCount);
        }

        private int getMissingSlots(int missingCount) {
            int count = 0;
            for (int i = 0, mergedSize = merged.size(); i < mergedSize; i++) {
                IGuiIngredient<ItemStack> ingredient = merged.get(i);
                ItemStack first = ItemUtils.getFirstStack(ingredient);
                if ((count += first.getCount()) >= missingCount) return i + 1;
            }
            return 0;
        }

        public RecipeIngredient(IngredientStatus status, ItemStack identifier, List<IGuiIngredient<ItemStack>> merged) {
            this(status, identifier, merged, 0);
        }

        public IngredientStatus getStatus() {
            return status;
        }

        public ItemStack getIdentifier() {
            return identifier;
        }

        public IAEItemStack createAeStack() {
            IAEItemStack stack = AEItemStack.fromItemStack(identifier);
            if (stack == null) return null;
            stack.setStackSize(missingCount);
            return stack;
        }

        public void drawHighlight(Minecraft minecraft, Color color, int xOffset, int yOffset) {
            for (int i = 0; i < missingSlots && i < merged.size(); i++) {
                IGuiIngredient<ItemStack> ingredient = merged.get(i);
                ingredient.drawHighlight(minecraft, color, xOffset, yOffset);
            }
        }

    }

    private void clearCache() {
        craftableCache = Collections.emptyList();
        allStacksCache = Collections.emptyList();
    }

    private List<List<IGuiIngredient<ItemStack>>> mergeIngredients(IRecipeLayout recipeLayout) {
        List<List<IGuiIngredient<ItemStack>>> merged = new ArrayList<>();
        recipeLayout.getItemStacks()
                .getGuiIngredients()
                .values()
                .stream()
                .filter(IGuiIngredient::isInput)
                .filter(ingredient -> !ingredient.getAllIngredients().isEmpty())
                .forEach(ingredient -> {
                    boolean matches = merged.stream().anyMatch(list -> {
                        if (list.isEmpty()) return false;
                        IGuiIngredient<ItemStack> first = list.get(0);
                        if (matches(first, ingredient)) {
                            list.add(ingredient);
                            return true;
                        } else return false;
                    });
                    if (!matches) merged.add(new ArrayList<>(Collections.singleton(ingredient)));
                });
        return merged;
    }

    private RecipeIngredient getExistData(List<IGuiIngredient<ItemStack>> ingredients) {
        //use available ingredient in player inventory first
        int required = ingredients.stream().mapToInt(stacks -> ItemUtils.getFirstStack(stacks).getCount()).sum();
        IGuiIngredient<ItemStack> first = ingredients.get(0);
        ItemStack identifier = availableItems.stream()
                .filter(stack -> first.getAllIngredients().parallelStream().anyMatch(stack1 -> ItemUtils.matches(stack, stack1)))
                .findAny()
                .orElse(ItemStack.EMPTY);
        if (!identifier.isEmpty() && identifier.getCount() >= required) {
            return new RecipeIngredient(IngredientStatus.EXISTS, identifier.copy(), ingredients);
        } else {
            int count = identifier.getCount();
            Stream<IAEItemStack> stackStream = allStacksCache.parallelStream();
            if (identifier.isEmpty()) {
                stackStream = stackStream.filter(stack -> first.getAllIngredients().parallelStream().anyMatch(guiStack -> ItemUtils.matches(stack.getDefinition(), guiStack)));
            } else {
                stackStream = stackStream.filter(stack -> first.getAllIngredients().parallelStream().anyMatch(guiStack -> ItemUtils.matches(stack.getDefinition(), identifier) || ItemUtils.matches(stack.getDefinition(), guiStack)));
            }
            IAEItemStack aeStack = stackStream
                    .findAny()
                    .orElse(null);
            if (aeStack == null) {
                return new RecipeIngredient(IngredientStatus.MISSING, ItemStack.EMPTY, ingredients);
            }
            count += aeStack.getStackSize();
            int missingCount = required - count;
            IngredientStatus status = missingCount > 0 ? IngredientStatus.CRAFTABLE : IngredientStatus.EXISTS;
            return new RecipeIngredient(status, aeStack.getDefinition().copy(), ingredients, missingCount);

        }

    }

    private boolean matches(IGuiIngredient<ItemStack> ingredient1, IGuiIngredient<ItemStack> ingredient2) {
        ItemStack first1 = ItemUtils.getFirstStack(ingredient1);
        ItemStack first2 = ItemUtils.getFirstStack(ingredient2);
        return ItemUtils.matches(first1, first2) && ingredient1.getAllIngredients().size() == ingredient2.getAllIngredients().size();
    }

    private List<IAEItemStack> getStorage() {
        ItemRepo repo = ObfuscationReflectionHelper.getPrivateValue(GuiMEMonitorable.class, term, "repo");
        if (repo == null) return Collections.emptyList();
        IItemList<IAEItemStack> all = ObfuscationReflectionHelper.getPrivateValue(ItemRepo.class, repo, "list");
        if (all == null) return Collections.emptyList();
        else return StreamSupport.stream(all.spliterator(), false).collect(Collectors.toList());
    }

}
