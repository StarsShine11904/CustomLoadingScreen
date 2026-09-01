package alexiil.mc.mod.load.progress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class LongTermProgressTracker {

    public final String[] modIds;
    public final ProgressSectionInfo[] infos;

    public static LongTermProgressTracker load() {
        File file = FabricLoader.getInstance().getConfigDir().toFile();
        file = new File(file, "customloadingscreen_timings.nbt");
        if (file.isFile()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                NbtCompound nbt = NbtIo.readCompressed(fis);
                return new LongTermProgressTracker(nbt);
            } catch (IOException io) {
                return null;
            }
        }
        return null;
    }

    public static void save(List<ProgressSectionInfo> infos) {
        if (infos == null || infos.isEmpty()) return;

        NbtCompound nbt = new NbtCompound();
        NbtList sectionList = new NbtList();

        for (ProgressSectionInfo info : infos) {
            NbtCompound sectionTag = new NbtCompound();
            sectionTag.putString("stage", info.stageName != null ? info.stageName : "");
            if (info.modId != null) {
                sectionTag.putString("modId", info.modId);
            }
            sectionTag.putLong("time", info.time);
            sectionList.add(sectionTag);
        }
        nbt.put("sections", sectionList);

        File file = FabricLoader.getInstance().getConfigDir().toFile();
        file = new File(file, "customloadingscreen_timings.nbt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            NbtIo.writeCompressed(nbt, fos);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private LongTermProgressTracker(NbtCompound nbt) throws IOException {
        NbtList list = nbt.getList("sections", 10); // 10 is CompoundTag type ID
        infos = new ProgressSectionInfo[list.size()];

        for (int i = 0; i < list.size(); i++) {
            NbtCompound sectionTag = list.getCompound(i);
            String stage = sectionTag.getString("stage");
            String modId = sectionTag.contains("modId") ? sectionTag.getString("modId") : null;
            long time = sectionTag.getLong("time");
            infos[i] = new ProgressSectionInfo(stage, modId, time);
        }
        modIds = new String[0];
    }
}
