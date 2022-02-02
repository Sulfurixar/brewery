package com.zener.brewery.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zener.brewery.Brewery;
import com.zener.brewery.config.ModConfig.HeaterStrengths;
import com.zener.brewery.config.ModConfig.Heaters;
import com.zener.brewery.matcher.CauldronMatcher.Result;
import com.zener.brewery.mixin.RecipeManagerAccessor;
import com.zener.brewery.recipes.RecipeTypesRegistry;
import com.zener.brewery.recipes.cauldron.CauldronRecipe;
import com.zener.brewery.recipes.cauldron.FailedOutput;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientManager;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientType;
import com.zener.brewery.translatables.CauldronText;

import lombok.Getter;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.LavaCauldronBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CauldronBlockEntity extends LootableContainerBlockEntity {

    private static final int HEAT_TIME = 720*20;
    private int current_heat_time = 0;
    @SuppressWarnings("unused")
    private boolean previous_heat_status = false;
    private boolean heat_status = false;
    @Getter private boolean boiling = false;
    @Getter private byte heat_strength = 0;
    private Heaters heaterConfig = Brewery.CONFIG.getHeaters();
    private HeaterStrengths heaterStrengths = Brewery.CONFIG.getHeaterStrengths();
    @Getter private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(CauldronRecipe.INPUT_SLOTS, ItemStack.EMPTY);
    private byte inputCooldown = 0;
    private long cook_time = 0;

    private class HeaterData {
        @Getter private final int heat;
        @Getter private final boolean enabled;
        
        public HeaterData(int heat, boolean enabled) {
            this.heat = heat;
            this.enabled = enabled;
        }
    }

    private Map<Block, HeaterData> heaters = new HashMap<>() {{
        put(Blocks.FIRE, new HeaterData(heaterStrengths.getFire(), heaterConfig.isFire()));
        put(Blocks.CAMPFIRE, new HeaterData(heaterStrengths.getCampfire(), heaterConfig.isCampfire()));
        put(Blocks.SOUL_FIRE, new HeaterData(heaterStrengths.getSoul_fire(), heaterConfig.isSoul_fire()));
        put(Blocks.SOUL_CAMPFIRE, new HeaterData(heaterStrengths.getSoul_campfire(), heaterConfig.isSoul_campfire()));
        put(Blocks.LAVA_CAULDRON, new HeaterData(heaterStrengths.getLava_cauldron(), heaterConfig.isLava_cauldron()));
        put(Blocks.FURNACE, new HeaterData(heaterStrengths.getFurnace(), heaterConfig.isFurnace()));
        put(Blocks.SMOKER, new HeaterData(heaterStrengths.getSmoker(), heaterConfig.isSmoker()));
        put(Blocks.BLAST_FURNACE, new HeaterData(heaterStrengths.getBlast_furnace(), heaterConfig.isBlast_furnace()));
    }};

    public CauldronBlockEntity(BlockPos pos, BlockState state) {
        super(Brewery.CAULDRON_BLOCK_ENTITY, pos, state);
    }

    private int hasSpace(PlayerEntity player, TranslatableText msg) {
        int openSlot = this.getEmptySlot();
        if (openSlot == -1) {
            player.sendMessage(msg, true);
        }
        return openSlot;
    }

    public boolean playerInteract(PlayerEntity player, Hand hand, int level) {

        if (inputCooldown > 0) return false;
        inputCooldown = 1;

        if (!this.isEmpty()) {
            DefaultedList<ItemStack> list = getInvStackList();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).isEmpty()) continue;
                ItemStack item = list.get(i);
                invStackProps props = new invStackProps(item);
                if (props.hasUp()) {
                    if (props.getUp() <= 0) {
                        this.removeStack(i);
                    }
                }
            }
        }

        ItemStack playerStack = player.getMainHandStack();

        if (playerStack.isOf(Items.POTION)) {
            NbtCompound nbt = playerStack.getOrCreateNbt();
            if (nbt.contains("Potion") && nbt.get("Potion").getType() == NbtType.STRING) {
                if (nbt.getString("Potion").equals("minecraft:water")) {
                    if (level == 3) return false;
                    if (!this.isEmpty()) {
                        for (ItemStack item : this.getInvStackList()) {
                            int down = 1;
                            invStackProps props = new invStackProps(item);
                            if (props.hasDown()) {
                                down = props.getDown();
                            }
                            down+=1;
                            props.setDown(down);
                        }
                    }
                    return false;
                }
            }
        }

        if (playerStack.isOf(Items.WATER_BUCKET)) {
            if (level == 3) return false; 
            if (!this.isEmpty()) {
                for (ItemStack item : this.getInvStackList()) {
                    int down = 1;
                    invStackProps props = new invStackProps(item);
                    if (props.hasDown()) {
                        down = props.getDown();
                    }
                    down+=(3-level);
                    props.setDown(down);
                }
            }
            return false;
        }

        if (!isBoiling()) {
            if (playerStack.isOf(Items.GLASS_BOTTLE) && !this.isEmpty()) {
                int openSlot = player.getInventory().getEmptySlot();
                if (openSlot == -1 && playerStack.getCount() > 1) {
                    player.sendMessage(CauldronText.INVENTORY_FULL, true);
                    return true;
                }
    
                // update resource count by parts in solution
                for (ItemStack item : this.getInvStackList()) {
                    if (item.isEmpty()) continue;
                    double up = 1;
                    int down = 1;
                    invStackProps props = new invStackProps(item);
                    if (props.hasUp()) {
                        up = props.getUp();
                    }
    
                    if (props.hasDown()) {
                        down = props.getDown();
                    }
    
                    double new_up = up - 1/(3*(double)down);
    
                    props.setUp(new_up);
    
                }
    
                return false;
    
            }
        }

        if (playerStack.isOf(Items.GLASS_BOTTLE) && !this.isEmpty()) {
            int openSlot = player.getInventory().getEmptySlot();
            if (openSlot == -1 && playerStack.getCount() > 1) {
                player.sendMessage(CauldronText.INVENTORY_FULL, true);
                return true;
            }

            RecipeManager recipeManager = player.getWorld().getRecipeManager();
            RecipeManagerAccessor accessor = (RecipeManagerAccessor) recipeManager;
            Map<CauldronRecipe, Result> results = new HashMap<CauldronRecipe, Result>();

            accessor.callGetAllOfType(RecipeTypesRegistry.CAULDRON_RECIPE_SERIALIZER.type()).forEach((id, recipe) -> {
                Result match = ((CauldronRecipe) recipe).advancedMatches(this).getResult();
                results.put((CauldronRecipe) recipe, match);
            });

            double min = results.entrySet().stream().mapToDouble((r) -> r.getValue().result).min().getAsDouble();

            Map<CauldronRecipe, Result> filtered = 
                results.entrySet().stream().filter((r) -> r.getValue().result == min)
                .collect(HashMap::new, (m, x) -> m.put(x.getKey(), x.getValue()), HashMap::putAll);

            CauldronRecipe best_match;

            if (filtered.size() > 1) {
                int highest = filtered.entrySet().stream().mapToInt((r) -> r.getValue().getIngredientCount()).max().getAsInt();
                Map<CauldronRecipe, Result> last = 
                    filtered.entrySet().stream().filter((r) -> r.getValue().getIngredientCount() == highest)
                    .collect(HashMap::new, (m, x) -> m.put(x.getKey(), x.getValue()), HashMap::putAll);
                if (last.size() > 1) {
                    best_match = last.keySet().toArray(new CauldronRecipe[last.size()])[(int) (Math.random() * last.size())];
                } else {
                    best_match = last.keySet().toArray(new CauldronRecipe[last.size()])[0];
                }
            } else {
                best_match = filtered.keySet().toArray(new CauldronRecipe[filtered.size()])[0];
            }

            // Caulculate probabilities
            /*  R - ingredient mismatch
            //  c - ingredient count
            //  t - cooking time
            //  T - temperature 
            //  d - difficulty
            //  C - ingredient match ratio
            //  D - difficulty percentage
            */
            
            double d = best_match.getDifficulty();
            double C = filtered.get(best_match).calculate();
            double Co = Math.max(0, 1 - C*d/100);
            double T = best_match.getTemperature();
            double dT = this.heat_strength;
            double Tt = Math.max(0, 1 - Math.abs(T-dT)*d/100);
            double dt = this.cook_time/20/60;
            double t = best_match.getCook_time();
            double tt = Math.max(0, 1 - Math.abs(dt-t)*d/100);

            // Magic percentage that determines the completion percentage of our brew!
            double output = Co*Tt*tt;

            double output_max = Math.max(0, Math.min(100, (output + 1/d)*100)); 

            // find out the closest result
            Map<Double, FailedOutput> fs = best_match.getFailedOutputs().stream()
            .collect(HashMap::new, (m, f) -> m.put(Math.abs(output_max-f.getCompletionPercentage()), f), HashMap::putAll);
            fs.put(output_max, Brewery.DEFAULT_FAILED_OUTPUT);
            fs.put(Math.abs(100 - output_max), new FailedOutput(best_match.getOutput(), 100));

            double lowest_dist = fs.entrySet().stream().mapToDouble((e) -> e.getKey()).min().getAsDouble();

            List<FailedOutput> fs_res = fs.entrySet().stream().filter((e) -> e.getKey() == lowest_dist).collect(ArrayList::new, (l, e) -> l.add(e.getValue()), ArrayList::addAll);

            FailedOutput best_output;
            if (fs_res.size() > 1) {
                best_output = fs_res.get((int) (Math.random() * fs_res.size()));
            } else {
                best_output = fs_res.get(0);
            }

            ItemStack ns = new ItemStack(best_output.getOutput().getItem(), 1);
            NbtCompound final_nbt = best_output.getOutput().getOrCreateNbt();
            NbtCompound nbt_results = new NbtCompound();
            nbt_results.putDouble("Composition", Co);
            nbt_results.putDouble("Temperature", Tt);
            nbt_results.putDouble("Time", tt);
            nbt_results.putDouble("Confidence", output_max);
            final_nbt.put("r", nbt_results);
            ns.setNbt(final_nbt);

            playerStack.decrement(1);

            player.getInventory().insertStack(ns);

            // update resource count by parts in solution
            for (ItemStack item : this.getInvStackList()) {
                if (item.isEmpty()) continue;
                double up = 1;
                int down = 1;
                invStackProps props = new invStackProps(item);
                if (props.hasUp()) {
                    up = props.getUp();
                }

                if (props.hasDown()) {
                    down = props.getDown();
                }

                double new_up = up - 1/(3*(double)down);

                props.setUp(new_up);

            }

            return false;
        }

        if (playerStack.isOf(Items.CLOCK)) {
            player.sendMessage(CauldronText.TIME.shallowCopy().append(": "+String.valueOf((this.cook_time/20)/60)), true);
            return false;
        }

        Brewery.getNbtIngredientManager();
        List<? extends NbtIngredient> ingredientList = NbtIngredientManager.getAllMatches(NbtIngredientType.CAULDRON, player, world);
        if (ingredientList.size() == 0) return false;   

        int openSlot = hasSpace(player, CauldronText.CAULDRON_FULL);
        if (openSlot == -1) return false;

        ItemStack new_stack = playerStack.copy();
        new_stack.setCount(1);
        this.setStack(openSlot, new_stack);

        playerStack.decrement(1);

        return false;
    }

    public static class invStackProps {

        private ItemStack item;

        public invStackProps(ItemStack item) {
            this.item = item;
        }

        public boolean hasBrewery() {
            NbtCompound nbt = item.getOrCreateNbt(); 
            return (nbt.contains(Brewery.MOD_ID) && nbt.get(Brewery.MOD_ID).getType() == NbtType.COMPOUND);
        }
    
        public boolean hasUp() {
            if (!hasBrewery()) return false;
            NbtCompound nbt = item.getNbt().getCompound(Brewery.MOD_ID);
            return (nbt.contains("up") && nbt.get("up").getType() == NbtType.DOUBLE);
        }
    
        public boolean hasDown() {
            if (!hasBrewery()) return false;
            NbtCompound nbt = item.getNbt().getCompound(Brewery.MOD_ID);
            return (nbt.contains("down") && nbt.get("down").getType() == NbtType.INT);
        }
    
        public double getUp() {
            return item.getNbt().getCompound(Brewery.MOD_ID).getDouble("up");
        }
    
        public int getDown() {
            return item.getNbt().getCompound(Brewery.MOD_ID).getInt("down");
        }

        public void setUp(double up) {
            NbtCompound nbt;
            NbtCompound brewery;
            if (hasBrewery()) {
                nbt = item.getNbt();
                brewery = item.getNbt().getCompound(Brewery.MOD_ID);
            } else {
                nbt = item.getOrCreateNbt();
                brewery = new NbtCompound();
            }

            brewery.putDouble("up", up);
            nbt.put(Brewery.MOD_ID, brewery);

            item.setNbt(nbt);
        }

        public void setDown(int down) {
            NbtCompound nbt;
            NbtCompound brewery;
            if (hasBrewery()) {
                nbt = item.getNbt();
                brewery = item.getNbt().getCompound(Brewery.MOD_ID);
            } else {
                nbt = item.getOrCreateNbt();
                brewery = new NbtCompound();
            }

            brewery.putInt("down", down);
            nbt.put(Brewery.MOD_ID, brewery);

            item.setNbt(nbt);
        }

        public boolean hasR() {
            NbtCompound nbt = item.getOrCreateNbt();
            return (nbt.contains("r") && nbt.get("r").getType() == NbtType.COMPOUND);
        }

        public boolean hasConfidence() {
            if (!hasR()) return false;
            NbtCompound nbt = item.getNbt().getCompound("r");
            return (nbt.contains("Confidence") && nbt.get("Confidence").getType() == NbtType.DOUBLE);
        }

        public double getConfidence() {
            return item.getNbt().getCompound("r").getDouble("Confidence");
        }

        public double calculateRandomPurity() {
            if (!hasConfidence()) return 1;
            return ((Math.random() * (1 - getConfidence())) + getConfidence());
        }

    }

    public int getEmptySlot() {
        for (int i = 0; i < this.inventory.size(); ++i) {
            if (!this.inventory.get(i).isEmpty()) continue;
            return i;
        }
        return -1;
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        heat_status = nbt.getBoolean("HeatStatus");
        heat_strength = nbt.getByte("HeatStrength");
        current_heat_time = nbt.getInt("HeatTime");
        boiling = nbt.getBoolean("Boiling");
        cook_time = nbt.getLong("CookTime");
        if (!this.deserializeLootTable(nbt)) {
            Inventories.readNbt(nbt, this.inventory);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("HeatStatus", heat_status);
        nbt.putByte("HeatStrength", heat_strength);
        nbt.putInt("HeatTime", current_heat_time);
        nbt.putBoolean("Boiling", boiling);
        nbt.putLong("CookTime", cook_time);
        if (!this.serializeLootTable(nbt)) {
            Inventories.writeNbt(nbt, this.inventory);
        }
    }

    private boolean isHeated(World world, BlockPos pos) {

        // check that we're actually checking a block inside the world
        if (!World.isValid(pos.down())) return false;

        BlockState heaterState = world.getBlockState(pos.down());
        // make sure there is something below
        if (heaterState.isAir()) return false;

        if (heaterConfig.isLava()) {
            FluidState fluidState = world.getFluidState(pos.down());

            if (fluidState.getFluid() instanceof LavaFluid) {
                heat_strength = (byte)(fluidState.getLevel()*heaterStrengths.getLava());
                return true;
            }
        }

        Block block = heaterState.getBlock();
        for (Map.Entry<Block, HeaterData> entry : heaters.entrySet()) {
            if (!block.equals(entry.getKey())) continue;

            if (block instanceof AbstractFireBlock) {
                heat_strength = (byte)entry.getValue().getHeat();
                return true;
            }

            if (block instanceof CampfireBlock) {
                if (CampfireBlock.isLitCampfire(heaterState)) {
                    heat_strength = (byte)entry.getValue().getHeat();
                    return true;
                }
            }

            if (block instanceof LavaCauldronBlock) {
                LavaCauldronBlock lcb = (LavaCauldronBlock)block;
                if (lcb.isFull(heaterState)) {
                    heat_strength = (byte)entry.getValue().getHeat();
                    return true;
                }
            }

            if (block instanceof AbstractFurnaceBlock) {
                if (heaterState.contains(AbstractFurnaceBlock.LIT) && heaterState.get(AbstractFurnaceBlock.LIT) != false) {
                    heat_strength = (byte)entry.getValue().getHeat();
                    return true;
                }
            }
        }

        return false;
    }

    public final boolean isCooking() {
        return !this.isEmpty() && isBoiling();
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, CauldronBlockEntity blockEntity) {
        // always make sure to decrease the input cooldown
        if (blockEntity.inputCooldown > 0) blockEntity.inputCooldown -= 1;
        if (blockEntity.isCooking()) {
            blockEntity.cook_time += 1;
        } else {
            blockEntity.cook_time = 0;
        }

        blockEntity.heat_status = blockEntity.isHeated(world, pos);
        // no heat
        if (!blockEntity.heat_status) {
            blockEntity.current_heat_time -= 1;
            if (blockEntity.current_heat_time < 0) blockEntity.current_heat_time = 0;

            if (blockEntity.boiling && blockEntity.current_heat_time < HEAT_TIME * 0.75) blockEntity.boiling = false;
            blockEntity.previous_heat_status = blockEntity.heat_status;
            return;
        }

        blockEntity.previous_heat_status = blockEntity.heat_status;

        blockEntity.current_heat_time += blockEntity.heat_strength;
        if (blockEntity.current_heat_time > HEAT_TIME) {
            blockEntity.current_heat_time = HEAT_TIME;
            blockEntity.boiling = true;
        }

        // if boiling
        if (!blockEntity.boiling) return;

        cookEffect((ServerWorld)world, pos, state);

    }

    private static void cookEffect(ServerWorld world, BlockPos pos, BlockState state) {
        ParticleEffect p = ParticleTypes.SPLASH;
        world.spawnParticles(p, (double)(pos.up().getX()+0.5), (double)(pos.up().getY()+state.get(LeveledCauldronBlock.LEVEL)*0.2-0.6), (double)(pos.up().getZ()+0.5), 1, 0.1D, 0.0D, 0.1D, 0.0D);
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return this.inventory;
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> list) {
        this.inventory = list;
    }

    @Override
    protected Text getContainerName() {
        return new TranslatableText(Brewery.MOD_ID+".cauldron");
    }

    @Override
    protected ScreenHandler createScreenHandler(int var1, PlayerInventory var2) {
        return null;
    }
}
