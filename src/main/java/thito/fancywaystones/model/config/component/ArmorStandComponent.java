package thito.fancywaystones.model.config.component;

import org.bukkit.*;
import org.bukkit.configuration.serialization.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.util.*;
import thito.fancywaystones.*;
import thito.fancywaystones.config.*;
import thito.fancywaystones.model.config.*;
import thito.fancywaystones.protocol.*;

import java.util.*;
import java.util.function.Consumer;

public class ArmorStandComponent implements ComponentType {

    private ComponentData defaultData;

    public ArmorStandComponent() {
        defaultData = new ArmorStandComponentData(new ComponentData(new StyleRuleCompound(), new MapSection()));
    }

    @Override
    public ComponentData getDefaultData() {
        return defaultData;
    }

    @Override
    public void bakeData(ComponentData[] componentData) {
        for (int i = 0; i < componentData.length; i++) componentData[i] = new ArmorStandComponentData(componentData[i]);
    }

    @Override
    public ComponentHandler createHandler(WaystoneData waystoneData, Component component) {
        return new Handler(component, waystoneData);
    }

    public static class ArmorStandComponentData extends ComponentData {
        public static final MapSection DEFAULT_ITEM;
        static {
            DEFAULT_ITEM = new MapSection();
            DEFAULT_ITEM.set("==", "org.bukkit.inventory.ItemStack");
            DEFAULT_ITEM.set("type", "AIR");
            DEFAULT_ITEM.set("amount", 1);
        }
        private ArmorStandMeta meta = new ArmorStandMeta();
        public ArmorStandComponentData(ComponentData other) {
            super(other);
            meta.setHelmet((ItemStack) ConfigurationSerialization.deserializeObject(validateItemStack(getConfig().getMap("helmet").orElse(DEFAULT_ITEM))));
            meta.setBoots((ItemStack) ConfigurationSerialization.deserializeObject(validateItemStack(getConfig().getMap("chestplate").orElse(DEFAULT_ITEM))));
            meta.setLeggings((ItemStack) ConfigurationSerialization.deserializeObject(validateItemStack(getConfig().getMap("leggings").orElse(DEFAULT_ITEM))));
            meta.setBoots((ItemStack) ConfigurationSerialization.deserializeObject(validateItemStack(getConfig().getMap("boots").orElse(DEFAULT_ITEM))));
            meta.setCustomNameVisible(getConfig().getBoolean("custom-name-visible").orElse(false));
            meta.setCustomName(getConfig().getString("custom-name").orElse(null));
            meta.setMarker(getConfig().getBoolean("marker").orElse(true));
            meta.setSmall(getConfig().getBoolean("small").orElse(false));
            meta.setInvisible(getConfig().getBoolean("invisible").orElse(true));
            meta.setNoBasePlate(getConfig().getBoolean("no-base-plate").orElse(false));
            meta.setArms(getConfig().getBoolean("arms").orElse(false));
            meta.setHeadPose(fromConfig(getConfig().getMap("head-pose").orElse(MapSection.empty())));
            meta.setBodyPose(fromConfig(getConfig().getMap("body-pose").orElse(MapSection.empty())));
            meta.setLeftArmPose(fromConfig(getConfig().getMap("left-arm-pose").orElse(MapSection.empty())));
            meta.setRightArmPose(fromConfig(getConfig().getMap("right-arm-pose").orElse(MapSection.empty())));
            meta.setLeftLegPose(fromConfig(getConfig().getMap("left-leg-pose").orElse(MapSection.empty())));
            meta.setRightLegPose(fromConfig(getConfig().getMap("right-leg-pose").orElse(MapSection.empty())));
        }

        public ArmorStandMeta getMeta() {
            return meta;
        }

        private static Map<String, Object> validateItemStack(Map<String, Object> map) {
            map.putIfAbsent("==", map.getOrDefault("class", "org.bukkit.inventory.ItemStack"));
            map.put("v", Bukkit.getUnsafe().getDataVersion());
            Object type = map.get("type");
            if (type instanceof String) {
                String[] split = ((String) type).split(";");
                for (String s : split) {
                    try {
                        map.put("type", XMaterial.valueOf(s).parseMaterial().name());
                        return map;
                    } catch (Throwable ignored) {
                    }
                }
            }
            map.put("type", "AIR");
            return map;
        }

        private static EulerAngle fromConfig(Section section) {
            return new EulerAngle(section.getDouble("x").orElse(0d),
                    section.getDouble("y").orElse(0d),
                    section.getDouble("z").orElse(0d));
        }
    }

    public class Handler implements ComponentHandler {
        private Component component;
        private FakeArmorStand armorStand;
        private WaystoneData waystoneData;

        public Handler(Component component, WaystoneData data) {
            this.component = component;
            waystoneData = data;
            armorStand = new FakeArmorStand(component.getLocation());
            armorStand.setAsyncMetaFactory((player, cons) -> {
                supplyIsActive(player, isActive -> {
                    component.update(data, isActive ? WaystoneState.ACTIVE : WaystoneState.INACTIVE, player);
                });
            });
            armorStand.spawn();
        }

        public void supplyIsActive(Player player, Consumer<Boolean> result) {
            if (!waystoneData.getType().isActivationRequired()) {
                result.accept(true);
            } else {
                FancyWaystones.getPlugin().submitIO(() -> {
                    PlayerData playerData = WaystoneManager.getManager().getPlayerData(player);
                    result.accept(playerData.knowWaystone(this.waystoneData));
                });
            }
        }

        public Component getComponent() {
            return component;
        }

        @Override
        public ComponentType getType() {
            return ArmorStandComponent.this;
        }

        @Override
        public void update(ComponentData data, WaystoneState state, Player player) {
            armorStand.update(player, ((ArmorStandComponentData) data).getMeta());
        }

        @Override
        public void destroy() {
            armorStand.remove();
        }
    }
}
