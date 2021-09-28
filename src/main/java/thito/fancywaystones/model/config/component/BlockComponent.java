package thito.fancywaystones.model.config.component;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.*;
import thito.fancywaystones.*;
import thito.fancywaystones.config.*;
import thito.fancywaystones.model.config.*;

public class BlockComponent implements ComponentType {

    private BlockComponentData defaultData;

    public BlockComponent() {
        defaultData = new BlockComponentData(new ComponentData(new StyleRuleCompound(), new MapSection()));
    }

    @Override
    public BlockComponentData getDefaultData() {
        return defaultData;
    }

    @Override
    public ComponentHandler createHandler(WaystoneData waystoneData, Component component) {
        return new Handler(waystoneData, component);
    }

    @Override
    public void bakeData(ComponentData[] componentData) {
        for (int i = 0; i < componentData.length; i++) componentData[i] = new BlockComponentData(componentData[i]);
    }

    public static class BlockComponentData extends ComponentData {
        private final String type;

        public BlockComponentData(ComponentData other) {
            super(other);
            type = (String) getConfig().getList("type").orElse(ListSection.empty()).stream().filter(x -> {
                if (x instanceof String) {
                    try {
                        return XMaterial.valueOf((String) x).isSupported();
                    } catch (Throwable ignored) {
                    }
                }
                return false;
            }).findAny().orElse("AIR");
        }

        public void setBlockAt(WaystoneData wb, Location location) {
            if (!Bukkit.isPrimaryThread()) {
                if (!FancyWaystones.getPlugin().isEnabled()) return;
                Bukkit.getScheduler().runTask(FancyWaystones.getPlugin(), () -> setBlockAt(wb, location));
                return;
            }
            Block block = location.getBlock();
            try {
                block.setType(XMaterial.valueOf(type).parseMaterial());
            } catch (Throwable t) {
                t.printStackTrace();
            }
            block.setMetadata("FW:WD", new FixedMetadataValue(FancyWaystones.getPlugin(), wb));
//            try {
//                BlockData blockData = Bukkit.getServer().createBlockData(this.type);
//                location.getBlock().setBlockData(blockData);
//                return;
//            } catch (Throwable t) {
//                t.printStackTrace();
//            }
//
        }
    }

    public class Handler implements ComponentHandler {
        private WaystoneData waystoneData;
        private Component component;

        public Handler(WaystoneData waystoneData, Component component) {
            this.waystoneData = waystoneData;
            this.component = component;
            update(component.requestData(waystoneData, WaystoneState.INACTIVE), WaystoneState.INACTIVE, null);
        }

        public Component getComponent() {
            return component;
        }

        public WaystoneData getWaystoneData() {
            return waystoneData;
        }

        @Override
        public ComponentType getType() {
            return BlockComponent.this;
        }

        @Override
        public void update(ComponentData data, WaystoneState state, Player player) {
            ((BlockComponentData) data).setBlockAt(waystoneData, component.getLocation());
        }

        @Override
        public void destroy() {
            if (!Bukkit.isPrimaryThread()) {
                if (!FancyWaystones.getPlugin().isEnabled()) return;
                Bukkit.getScheduler().runTask(FancyWaystones.getPlugin(), this::destroy);
                return;
            }
            Block block = component.getLocation().getBlock();
            block.removeMetadata("FW:WD", FancyWaystones.getPlugin());
            block.setType(XMaterial.AIR.parseMaterial());
        }
    }
}
