package illa4257.i4Utils;

public class Arch {
    public static final Arch INSTANCE = new Arch(System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("java.vendor"));

    public final boolean
            IS_X86,
            IS_X86_64,
            IS_ARM,
            IS_ARM64;

    /** CPU bits */
    public final boolean
            IS_32BIT,
            IS_64BIT;

    /** Operating Systems */
    public final boolean
            IS_WINDOWS,
            IS_LINUX,
            IS_ANDROID,
            IS_MACOS;

    /** Platforms */
    public final boolean
            IS_DESKTOP,
            IS_MOBILE;

    @SuppressWarnings("AssignmentUsedAsCondition")
    public Arch(String osName, String arch, final String vendor) {
        if (arch != null) {
            arch = arch.toLowerCase();

            // Architecture
            IS_X86_64 = arch.equals("amd64");
            IS_X86 = IS_X86_64 || arch.equals("x86");

            IS_ARM64 = arch.equals("aarch64");
            IS_ARM = IS_ARM64 || arch.equals("arm");
        } else
            IS_ARM = IS_ARM64 = IS_X86 = IS_X86_64 = false;

        // Bits
        IS_64BIT = IS_X86_64 || IS_ARM64;
        IS_32BIT = !IS_64BIT;

        // Operating System
        if (osName == null)
            IS_MACOS = IS_LINUX = IS_WINDOWS = false;
        else if (IS_WINDOWS = (osName = osName.toLowerCase()).contains("win"))
            IS_MACOS = IS_LINUX = false;
        else if (IS_LINUX = osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
            IS_MACOS = false;
        else
            IS_MACOS = osName.contains("mac");

        IS_ANDROID = IS_LINUX && vendor != null && vendor.toLowerCase().contains("android");

        // Platforms
        IS_MOBILE = IS_ANDROID;
        IS_DESKTOP = !IS_MOBILE && (IS_WINDOWS || IS_LINUX || IS_MACOS);
    }
}