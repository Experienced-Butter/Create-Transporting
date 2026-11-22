package com.experiencedbutter.villagetransit.village;

import com.experiencedbutter.villagetransit.config.VTConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.phys.AABB;

import java.util.stream.Stream;

/**
 * Village survey. Runs on the server thread only, is bounded by
 * {@link VTConfig.Snapshot#censusRadius} and refuses to report numbers for a partially loaded
 * village, because unloaded chunks contain no entities and would make the village look smaller
 * than it is.
 */
public final class VillageCensus {

    private VillageCensus() {
    }

    public record Result(boolean valid,
                         int villagers,
                         int employed,
                         int workstations,
                         int beds,
                         boolean incomplete,
                         String reason,
                         String villageId) {

        public static Result incomplete(String reason) {
            return new Result(false, 0, 0, 0, 0, true, reason, "");
        }

        public static Result invalid(String reason) {
            return new Result(false, 0, 0, 0, 0, false, reason, "");
        }
    }

    public static Result scan(ServerLevel level, BlockPos center) {
        VTConfig.Snapshot cfg = VTConfig.get();
        int radius = cfg.censusRadius;

        boolean fullyLoaded = isAreaLoaded(level, center, radius);
        if (!fullyLoaded && cfg.requireFullyLoadedCensus) {
            return Result.incomplete("area_not_loaded");
        }

        net.minecraft.world.level.levelgen.structure.StructureStart start = level.structureManager()
                .getStructureWithPieceAt(center, StructureTags.VILLAGE);
        boolean inStructure = start != null && start.isValid();
        String villageId = "";
        if (inStructure) {
            net.minecraft.world.level.ChunkPos chunkPos = start.getChunkPos();
            villageId = chunkPos.x + "," + chunkPos.z;
        }

        if (cfg.checkMode == VTConfig.VillageCheckMode.STRUCTURE_ONLY && !inStructure) {
            return Result.invalid("not_in_village_structure");
        }
        if (cfg.checkMode == VTConfig.VillageCheckMode.STRUCTURE_AND_POI && !inStructure) {
            return Result.invalid("not_in_village_structure");
        }

        int beds = 0;
        int workstations = 0;
        try (Stream<PoiRecord> pois = level.getPoiManager().getInSquare(
                holder -> holder.is(PoiTypes.HOME) || holder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE),
                center, radius, PoiManager.Occupancy.ANY)) {
            for (PoiRecord record : (Iterable<PoiRecord>) pois::iterator) {
                if (record.getPoiType().is(PoiTypes.HOME)) {
                    beds++;
                } else {
                    workstations++;
                }
            }
        }

        boolean poiOk = beds >= cfg.minBeds || workstations >= cfg.minWorkstations;
        if (cfg.checkMode != VTConfig.VillageCheckMode.STRUCTURE_ONLY && !poiOk) {
            return Result.invalid("not_enough_village_pois");
        }

        AABB box = new AABB(center).inflate(radius);
        int villagers = 0;
        int employed = 0;
        for (Villager villager : level.getEntitiesOfClass(Villager.class, box, v -> v.isAlive() && !v.isBaby()
                && !v.getPersistentData().getBoolean(com.experiencedbutter.villagetransit.train.SeatNpcs.TAG))) {
            villagers++;
            VillagerProfession profession = villager.getVillagerData().getProfession();
            if (profession != VillagerProfession.NONE && profession != VillagerProfession.NITWIT) {
                employed++;
            }
        }

        return new Result(true, villagers, employed, workstations, beds, !fullyLoaded, "", villageId);
    }

    /** Cheap chunk-state check; never forces a chunk to load. */
    public static boolean isAreaLoaded(ServerLevel level, BlockPos center, int radius) {
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }
}
