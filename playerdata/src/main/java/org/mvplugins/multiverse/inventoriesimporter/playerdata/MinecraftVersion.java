package org.mvplugins.multiverse.inventoriesimporter.playerdata;

import org.jspecify.annotations.NonNull;

public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {

    private static final MinecraftVersion ZERO = new MinecraftVersion(0, 0, 0);
    private static final MinecraftVersion INFINITE = new MinecraftVersion(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    public static MinecraftVersion fromString(String versionString) {
        String[] parts = versionString.split("\\.");
        int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return new MinecraftVersion(major, minor, patch);
    }

    public static MinecraftVersion zero() {
        return ZERO;
    }

    public static MinecraftVersion infinite() {
        return INFINITE;
    }

    public static MinecraftVersion.Range range(@NonNull MinecraftVersion min, @NonNull MinecraftVersion max) {
        return new Range(min, max);
    }

    public static MinecraftVersion.Range range(@NonNull String min, @NonNull String max) {
        return new Range(fromString(min), fromString(max));
    }

    public static MinecraftVersion.Range rangeAtLeast(@NonNull String min) {
        return new Range(fromString(min), INFINITE);
    }

    public static MinecraftVersion.Range rangeAtLeast(@NonNull MinecraftVersion min) {
        return new Range(min, INFINITE);
    }

    public static MinecraftVersion.Range rangeAtMost(@NonNull String max) {
        return new Range(ZERO, fromString(max));
    }

    public static MinecraftVersion.Range rangeAtMost(@NonNull MinecraftVersion max) {
        return new Range(ZERO, max);
    }

    @Override
    public int compareTo(@NonNull MinecraftVersion o) {
        if (this.major != o.major) {
            return Integer.compare(this.major, o.major);
        }
        if (this.minor != o.minor) {
            return Integer.compare(this.minor, o.minor);
        }
        return Integer.compare(this.patch, o.patch);
    }

    public static class Range {
        private final MinecraftVersion min;
        private final MinecraftVersion max;

        public Range(MinecraftVersion min, MinecraftVersion max) {
            this.min = min;
            this.max = max;
        }

        public boolean includes(MinecraftVersion version) {
            return (version.compareTo(min) >= 0) && (version.compareTo(max) <= 0);
        }
    }
}
