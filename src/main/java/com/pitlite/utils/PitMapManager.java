package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.block.Block;

public class PitMapManager {
    public enum PitMap {
        GENESIS, CORALS, CASTLE, ELEMENTS, FOUR_SEASONS, UNKNOWN
    }

    private static PitMap cachedMap = PitMap.UNKNOWN;
    private static boolean hasChecked = false;

    public static void detectMap() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            cachedMap = PitMap.UNKNOWN;
            hasChecked = false;
            return;
        }

        Block genesisCheck = mc.theWorld.getBlockState(new BlockPos(14, 85, -13)).getBlock();
        if (genesisCheck == Blocks.nether_brick) {
            cachedMap = PitMap.GENESIS;
            hasChecked = true;
            return;
        }

        BlockPos castlePos = new BlockPos(18, 95, -9);
        Block castleCheck = mc.theWorld.getBlockState(castlePos).getBlock();
        if (castleCheck == Blocks.double_stone_slab) {
            int meta = castleCheck.getMetaFromState(mc.theWorld.getBlockState(castlePos));
            if (meta == 8) {
                cachedMap = PitMap.CASTLE;
                hasChecked = true;
                return;
            }
        }

        Block elementsCheck = mc.theWorld.getBlockState(new BlockPos(-7, 113, -10)).getBlock();
        if (elementsCheck == Blocks.sandstone) {
            cachedMap = PitMap.ELEMENTS;
            hasChecked = true;
            return;
        }

        Block seasonsCheck = mc.theWorld.getBlockState(new BlockPos(-87, 85, -51)).getBlock();
        if (seasonsCheck == Blocks.planks) {
            int seasonsMeta = mc.theWorld.getBlockState(new BlockPos(-87, 85, -51)).getBlock()
                    .getMetaFromState(mc.theWorld.getBlockState(new BlockPos(-87, 85, -51)));
            if (seasonsMeta == 2) {
                cachedMap = PitMap.FOUR_SEASONS;
                hasChecked = true;
                return;
            }
        }

        Block coralsCheck = mc.theWorld.getBlockState(new BlockPos(-2, 113, 11)).getBlock();
        if (coralsCheck == Blocks.sand) {
            cachedMap = PitMap.CORALS;
            hasChecked = true;
            return;
        }

        cachedMap = PitMap.UNKNOWN;
    }

    public static PitMap getCurrentMap() {
        if (!hasChecked) {
            detectMap();
        }
        return cachedMap;
    }

    public static void reset() {
        cachedMap = PitMap.UNKNOWN;
        hasChecked = false;
    }

    public static boolean isInSpawn(double x, double y, double z) {
        if (getCurrentMap() == PitMap.FOUR_SEASONS) {
            return isInBox(x, y, z, 50, -50, 111, 138, -50, 50);
        } else if (getCurrentMap() == PitMap.GENESIS) {
            return isInBox(x, y, z, -50, 50, 85, 108, -50, 50);
        } else if (getCurrentMap() == PitMap.ELEMENTS) {
            return isInBox(x, y, z, -50, 50, 111, 122, -50, 50);
        } else if (getCurrentMap() == PitMap.CASTLE) {
            return isInBox(x, y, z, -22, 25, 90, 115, -24, 23);
        } else if (getCurrentMap() == PitMap.CORALS) {
            return isInBox(x, y, z, -18, 18, 114, 123, -21, 20);
        }
        return false;
    }

    public static boolean isInSewer(double x, double y, double z) {
        return getZone(x, y, z).equals("Sewer");
    }

    private static boolean isInBox(double x, double y, double z, double x1, double x2, double y1, double y2, double z1,
            double z2) {
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxZ = Math.max(z1, z2);
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public static String getZone(double x, double y, double z) {
        PitMap map = getCurrentMap();

        if (map == PitMap.FOUR_SEASONS) {
            if (isInBox(x, y, z, 18, -16, 111, 138, -20, 17)) {
                return "Spawn";
            }
            if (isInBox(x, y, z, -104, 102, 172, 500, -99, 110)) {
                return "Overspawn";
            }
            if (isInBox(x, y, z, -12, 13, 78, 111, -12, 13)) {
                return "Pit";
            }
            if (z > 0) {
                return (x >= 0) ? "Winter" : "Spring";
            } else {
                return (x >= 0) ? "Autumn" : "Summer";
            }
        }

        if (map == PitMap.GENESIS) {
            if (isInBox(x, y, z, -30, 22, 85, 108, -34, 20)) {
                return "Spawn";
            }
            if (isInBox(x, y, z, -78, -61, 64, 70, 62, 78)) {
                return "Angel";
            }
            if (isInBox(x, y, z, 57, 81, 64, 81, -56, -81)) {
                return "Demon";
            }
            if (isInBox(x, y, z, -93, 106, 126, 500, -90, 88)) {
                return "Overspawn";
            }
            double distSqCenter = x * x + z * z;
            if (distSqCenter > 35 * 35) {
                if (z > 0) {
                    return (x > 0) ? "Garden" : "Palace";
                } else {
                    return (x > 0) ? "Fortress" : "Badlands";
                }
            }
            return "Pit";
        }

        if (map == PitMap.ELEMENTS) {
            if (isInBox(x, y, z, -16, 20, 111, 122, -16, 23)) {
                return "Spawn";
            }
            if (isInBox(x, y, z, -98, 105, 145, 500, -96, 99)) {
                return "Overspawn";
            }
            if (isInBox(x, y, z, -12, 13, 79, 111, -13, 14)) {
                return "Pit";
            }
            if (z > 0) {
                return (x >= 0) ? "Lava" : "Sky";
            } else {
                return (x >= 0) ? "Mountains" : "Water";
            }
        }

        if (map == PitMap.CASTLE) {
            if (isInBox(x, y, z, -22, 25, 90, 115, -24, 23)) {
                return "Spawn";
            }
            if (isInBox(x, y, z, -10, 131, 44, 62, 9, 87)) {
                return "Sewer";
            }
            if (getDistSq(x, z, 0, 0) > 20 * 20) {
                if (z > 0) {
                    return (x >= 0) ? "City" : "Port";
                } else {
                    return (x >= 0) ? "Farm" : "Forest";
                }
            }
            if (y >= 124) {
                return "Overspawn";
            }
            return "Pit";
        }

        if (map == PitMap.CORALS) {
            if (isInBox(x, y, z, -18, 18, 114, 123, -21, 20)) {
                return "Spawn";
            }
            if (getDistSq(x, z, 0, 0) > 19 * 19) {
                if (z > 0) {
                    return (x >= 0) ? "Geyser" : "Shipwreck";
                } else {
                    return (x >= 0) ? "Temple" : "Seaweed";
                }
            }
            if (isInBox(x, y, z, -98, 91, 145, 500, -119, 124)) {
                return "Overspawn";
            }
            return "Pit";
        }

        return "???";
    }

    private static double getDistSq(double x, double z, double tx, double tz) {
        return (x - tx) * (x - tx) + (z - tz) * (z - tz);
    }
}
