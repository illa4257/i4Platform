package illa4257.i4Utils;

import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

import java.util.Scanner;

public class Arch {
    public static final Arch JVM = new Arch(System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"), System.getProperty("java.vendor")),
            REAL;

    static {
        REAL = detectRealArch();
    }

    public static String wmicGet(final String group, final String property) {
        try {
            final Process p = new ProcessBuilder("wmic", group, "get", property).redirectErrorStream(true).start();
            try (final Scanner s = new Scanner(p.getInputStream())) {
                if (property.equalsIgnoreCase(s.nextLine().trim())) {
                    s.nextLine();
                    return s.nextLine().trim();
                }
            } finally {
                p.destroyForcibly();
            }
        } catch (final Exception ex) {
            i4Logger.INSTANCE.log(ex);
        }
        return null;
    }

    private static Arch detectRealArch() {
        if (JVM.IS_WINDOWS) {
            String ver = JVM.osVersion;
            if (ver.indexOf('.') == ver.lastIndexOf('.') && ver.indexOf('.') != -1) {
                final String buildNumber = wmicGet("OS", "BuildNumber");
                if (buildNumber != null)
                    ver += '+' + buildNumber;
            }
            if (JVM.IS_32BIT) {
                String a = wmicGet("cpu", "Architecture");
                if (a != null)
                    switch (a) {
                        case "0":
                            a = "x86";
                            break;
                        case "5":
                            a = wmicGet("OS", "OSArchitecture");
                            if ("64-bit".equalsIgnoreCase(a)) {
                                a = "aarch64";
                                break;
                            } else if ("32-bit".equalsIgnoreCase(a)) {
                                a = "arm";
                                break;
                            } else {
                                i4Logger.INSTANCE.log(Level.WARN, "Unknown bit number for ARM processor in Windows: " + a);
                            }
                            return new Arch(JVM.osName, ver, JVM.arch, JVM.vendor);
                        case "9":
                            a = "amd64";
                            break;
                        default:
                            i4Logger.INSTANCE.log(Level.WARN, "Unknown Windows architecture code: " + a);
                            return new Arch(JVM.osName, ver, JVM.arch, JVM.vendor);
                    }
                return new Arch(JVM.osName, ver, a, JVM.vendor);
            }
            return new Arch(JVM.osName, ver, JVM.arch, JVM.vendor);
        } else
            try {
                final Process p = Runtime.getRuntime().exec(new String[] { "uname", "-m" });
                try (final Scanner s = new Scanner(p.getInputStream())) {
                    return new Arch(JVM.osName, JVM.osVersion, s.nextLine(), JVM.vendor);
                } finally {
                    p.destroyForcibly();
                }
            } catch (final Exception ex) {
                i4Logger.INSTANCE.log(ex);
            }
        return JVM;
    }

    /** Original data */
    public final String osName, osVersion, arch, vendor;
    public final SemVer osVer;

    /** Architecture */
    public final boolean
            IS_X86,
            IS_X86_32,
            IS_X86_64,

            IS_ARM,
            IS_ARM32,
            IS_ARM64,

            IS_CHEERPJ;

    /** CPU bits */
    public final boolean
            IS_32BIT,
            IS_64BIT;

    /** Operating System */
    public final boolean
            IS_WINDOWS,
            IS_LINUX,
            IS_ANDROID,
            IS_MACOS;

    /** Platform */
    public final boolean
            IS_DESKTOP,
            IS_MOBILE,
            IS_WEB;

    @SuppressWarnings("AssignmentUsedAsCondition")
    public Arch(String osName, final String osVersion, String arch, final String vendor) {
        this.osName = osName;
        this.osVer = new SemVer(this.osVersion = osVersion);
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

            IS_CHEERPJ = arch.equalsIgnoreCase("cheerpj");
        } else
            IS_CHEERPJ = IS_ARM = IS_ARM64 = IS_ARM32 = IS_X86 = IS_X86_64 = IS_X86_32 = false;

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
        IS_WEB = IS_CHEERPJ;
    }
}