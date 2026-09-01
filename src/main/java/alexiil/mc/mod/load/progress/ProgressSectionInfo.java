package alexiil.mc.mod.load.progress;

public class ProgressSectionInfo {
    public final String stageName;
    public final String modId;
    public long time;

    public ProgressSectionInfo(String stageName, long time) {
        this.stageName = stageName;
        this.modId = null;
        this.time = time;
    }

    public ProgressSectionInfo(String stageName, String modId, long time) {
        this.stageName = stageName;
        this.modId = modId;
        this.time = time;
    }

    @Override
    public String toString() {
        String pre;
        if (modId != null && !modId.isEmpty()) {
            pre = stageName + ": " + modId;
        } else {
            pre = stageName != null ? stageName : "Unknown";
        }
        return pre + " took " + time + "ms";
    }
}
