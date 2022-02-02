package com.zener.brewery.config;

import com.zener.brewery.Brewery;

import lombok.Getter;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Brewery.MOD_ID)
public class ModConfig implements ConfigData {

    @Getter
    @ConfigEntry.Gui.CollapsibleObject
    Heaters heaters = new Heaters();

    @Getter
    @ConfigEntry.Gui.CollapsibleObject
    HeaterStrengths heaterStrengths = new HeaterStrengths();
    
    public static class HeaterStrengths {
        @Getter int fire = 3;
        @Getter int soul_fire = 1;
        @Getter int lava = 3;
        @Getter int campfire = 6;
        @Getter int soul_campfire = 3;
        @Getter int lava_cauldron = 3;
        @Getter int furnace = 12;
        @Getter int smoker = 6;
        @Getter int blast_furnace = 24;
    }

    public static class Heaters {
        @Getter boolean fire = true;
        @Getter boolean soul_fire = true;
        @Getter boolean lava = true;
        @Getter boolean campfire = true;
        @Getter boolean soul_campfire = true;
        @Getter boolean lava_cauldron = true;
        @Getter boolean furnace = true;
        @Getter boolean smoker = true;
        @Getter boolean blast_furnace = true;
    }
    
}
