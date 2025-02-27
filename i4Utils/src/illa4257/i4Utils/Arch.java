package illa4257.i4Utils;

import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

import java.util.Scanner;

public class Arch {
    public static final Arch JVM = new Arch(System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("java.vendor")),
            REAL;

    static {
        REAL = detectRealArch();
    }

    private static Arch detectRealArch() {
        if (JVM.IS_32BIT)
            if (JVM.IS_WINDOWS)
                try {
                    final Process p = new ProcessBuilder("wmic", "cpu", "get", "Architecture").start();
                    try (final Scanner s = new Scanner(p.getInputStream())) {
                        if ("Architecture".equalsIgnoreCase(s.nextLine().trim())) {
                            String a = s.nextLine().trim();
                            switch (a) {
                                case "0":
                                    a = "x86";
                                    break;
                                case "5":
                                    final Process p2 = new ProcessBuilder("wmic", "OS", "get", "OSArchitecture").start();
                                    try (final Scanner s2 = new Scanner(p2.getInputStream())) {
                                        if ("OSArchitecture".equalsIgnoreCase(s2.nextLine().trim())) {
                                            a = s2.nextLine().trim();
                                            if ("64-bit".equalsIgnoreCase(a)) {
                                                a = "aarch64";
                                                break;
                                            } else if ("32-bit".equalsIgnoreCase(a)) {
                                                a = "arm";
                                                break;
                                            } else {
                                                i4Logger.INSTANCE.log(Level.WARN, "Unknown bit number for ARM processor in Windows: " + a);
                                            }
                                        }
                                    } finally {
                                        p2.destroyForcibly();
                                    }
                                    return JVM;
                                case "9":
                                    a = "amd64";
                                    break;
                                default:
                                    i4Logger.INSTANCE.log(Level.WARN, "Unknown Windows architecture code: " + a);
                                    return JVM;
                            }
                            return new Arch(JVM.osName, a, JVM.vendor);
                        }
                    } finally {
                        p.destroyForcibly();
                    }
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.log(ex);
                }
            else
                try {
                    final Process p = Runtime.getRuntime().exec(new String[] { "uname", "-m" });
                    try (final Scanner s = new Scanner(p.getInputStream())) {
                        return new Arch(JVM.osName, s.nextLine(), JVM.vendor);
                    } finally {
                        p.destroyForcibly();
                    }
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.log(ex);
                }
        return JVM;
    }

    public final String osName, arch, vendor;

    public final boolean
            IS_X86,
            IS_X86_32,
            IS_X86_64,

            IS_ARM,
            IS_ARM32,
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
        this.osName = osName;
        this.arch = arch;
        this.vendor = vendor;

        if (arch != null) {
            arch = arch.toLowerCase();

            // Architecture
            IS_X86_64 = arch.equals("x86_64") || arch.equals("amd64");
            IS_X86_32 = arch.equals("x86") || arch.equals("i386") || arch.equals("i486") || arch.equals("i586") ||
                        arch.equals("i686");
            IS_X86 = IS_X86_64 || IS_X86_32;

            IS_ARM64 = arch.equals("arm64") || arch.equals("aarch64");
            IS_ARM32 = arch.equals("arm") || arch.equals("armv7l");
            IS_ARM = IS_ARM64 || IS_ARM32;
        } else
            IS_ARM = IS_ARM64 = IS_ARM32 = IS_X86 = IS_X86_64 = IS_X86_32 = false;

        // Bits
        IS_64BIT = IS_X86_64 || IS_ARM64;
        IS_32BIT = IS_X86_32 || IS_ARM32;

        // Operating System
        if (osName != null) {
            osName = osName.toLowerCase();
            if (IS_WINDOWS = osName.contains("win"))
                IS_MACOS = IS_LINUX = false;
            else if (IS_LINUX = osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
                IS_MACOS = false;
            else
                IS_MACOS = osName.contains("mac");
        } else
            IS_MACOS = IS_LINUX = IS_WINDOWS = false;

        IS_ANDROID = IS_LINUX && vendor != null && vendor.toLowerCase().contains("android");

        // Platforms
        IS_MOBILE = IS_ANDROID;
        IS_DESKTOP = !IS_MOBILE && (IS_WINDOWS || IS_LINUX || IS_MACOS);
    }
}