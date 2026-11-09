package ru.turtlebot.nav;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import ru.turtlebot.config.BotConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Проверки проходимости и защиты кладок. Кэш живёт в рамках одного поиска/тика. */
public final class Walkability {
    private static final Set<Block> HAZARD = Set.of(
            Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.COBWEB,
            Blocks.SWEET_BERRY_BUSH, Blocks.WITHER_ROSE, Blocks.POWDER_SNOW, Blocks.MAGMA_BLOCK,
            Blocks.CACTUS, Blocks.POINTED_DRIPSTONE);
    private static final Set<Block> BAD_GROUND = Set.of(
            Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.HONEY_BLOCK,
            Blocks.SOUL_SAND, Blocks.SLIME_BLOCK, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
            Blocks.FROSTED_ICE, Blocks.CACTUS, Blocks.TURTLE_EGG);

    private final World world;
    private final int margin;
    private boolean strict = true;
    private final Map<Long, Boolean> zoneStrict = new HashMap<>();
    private final Map<Long, Boolean> zoneLoose = new HashMap<>();
    private final Map<Long, Boolean> standCache = new HashMap<>();

    public Walkability(World world, BotConfig cfg) {
        this.world = world;
        this.margin = cfg.eggMargin;
    }

    public void autoStrict(BlockPos start) {
        strict = !eggZone(start, true);
        standCache.clear();
    }

    public boolean passable(BlockPos p) {
        if (p.getY() < world.getBottomY() || p.getY() >= world.getTopY()) return false;
        if (!world.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) return false;
        BlockState s = world.getBlockState(p);
        return s.getCollisionShape(world, p).isEmpty()
                && s.getFluidState().isEmpty()
                && !HAZARD.contains(s.getBlock());
    }

    public boolean bodyClear(BlockPos feet) {
        return passable(feet) && passable(feet.up());
    }

    public boolean diagOk(BlockPos cell) {
        return bodyClear(cell) && !eggZone(cell, strict);
    }

    private boolean groundOk(BlockPos p) {
        if (!world.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) return false;
        BlockState s = world.getBlockState(p);
        return s.isSideSolidFullSquare(world, p, Direction.UP)
                && s.getFluidState().isEmpty()
                && !BAD_GROUND.contains(s.getBlock());
    }

    private boolean lavaNear(BlockPos p) {
        for (Direction d : Direction.Type.HORIZONTAL) {
            if (world.getFluidState(p.offset(d)).isIn(FluidTags.LAVA)) return true;
        }
        return false;
    }

    public boolean standable(BlockPos p) {
        Boolean c = standCache.get(p.asLong());
        if (c != null) return c;
        boolean ok = passable(p) && passable(p.up()) && groundOk(p.down())
                && !lavaNear(p) && !eggZone(p, strict);
        standCache.put(p.asLong(), ok);
        return ok;
    }

    private boolean egg(BlockPos p) {
        return world.getBlockState(p).isOf(Blocks.TURTLE_EGG);
    }

    public boolean eggZone(BlockPos p, boolean strictMode) {
        Map<Long, Boolean> cache = strictMode ? zoneStrict : zoneLoose;
        Boolean c = cache.get(p.asLong());
        if (c != null) return c;
        boolean found = false;
        int m = strictMode ? margin : 0;
        int dyMax = strictMode ? 1 : 0;
        BlockPos.Mutable mp = new BlockPos.Mutable();
        outer:
        for (int dx = -m; dx <= m; dx++)
            for (int dz = -m; dz <= m; dz++)
                for (int dy = -1; dy <= dyMax; dy++) {
                    mp.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
                    if (!world.getChunkManager().isChunkLoaded(mp.getX() >> 4, mp.getZ() >> 4)) continue;
                    if (egg(mp)) { found = true; break outer; }
                }
        cache.put(p.asLong(), found);
        return found;
    }
}
