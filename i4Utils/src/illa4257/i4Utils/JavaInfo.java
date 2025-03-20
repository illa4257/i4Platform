package illa4257.i4Utils;

import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class JavaInfo {
    public static final JavaInfo CURRENT;

    static {
        CURRENT = new JavaInfo(Integer.parseInt(System.getProperty("java.specification.version")), Arch.JVM,
                File.pathSeparator,
                new File(System.getProperty("java.home"), "bin/" + (Arch.JVM.IS_WINDOWS ? "java.exe" : "java")),
                getDistribution(System.getProperty("java.vendor"), System.getProperty("java.vendor.version"),
                        System.getProperty("java.runtime.name")));
    }

    public final int majorVersion;
    public final Arch arch;
    public final String pathSeparator;
    public final File path;
    public final Distribution distribution;

    public enum Distribution {
        /** OpenJDK */
        OPEN_JDK("OpenJDK"),
        /** AdoptOpenJDK */
        ADOPT_OPEN_JDK("AdoptOpenJDK"),

        /** GraalVM */
        GRAALVM("GraalVM"),

        /** Oracle */
        ORACLE("Oracle"),

        UNKNOWN("Unknown");

        public final String displayName;

        Distribution(final String displayName) {
            this.displayName = displayName;
        }
    }

    public JavaInfo(final int majorVersion, final Arch arch, final String pathSeparator, final File path, final Distribution distribution) {
        this.majorVersion = majorVersion;
        this.arch = arch;
        this.pathSeparator = pathSeparator;
        this.path = path;
        this.distribution = distribution;
    }

    public static Distribution getDistribution(String vendor, String vendorVersion, String runtimeName) {
        if (vendor == null)
            vendor = "null";
        if (vendor.equalsIgnoreCase("AdoptOpenJDK"))
            return Distribution.ADOPT_OPEN_JDK;
        runtimeName = runtimeName != null ? runtimeName.toLowerCase() : "null";
        vendorVersion = vendorVersion != null ? vendorVersion.toLowerCase() : "null";
        if (vendor.equalsIgnoreCase("Oracle Corporation")) {
            if (vendorVersion.contains("graalvm"))
                return Distribution.GRAALVM;
            if (runtimeName.contains("java(tm) se"))
                return Distribution.ORACLE;
        }
        vendor = vendor.toLowerCase();
        if (vendor.contains("graalvm") || vendorVersion.contains("graalvm"))
            return Distribution.GRAALVM;
        if (runtimeName.equals("openjdk runtime environment"))
            return Distribution.OPEN_JDK;
        i4Logger.INSTANCE.log(Level.WARN, "Unknown java distribution: " + vendor + " / " + vendorVersion + " / " + runtimeName);
        return Distribution.UNKNOWN;
    }

    public static JavaInfo check(final File java) throws Exception {
        if (!java.exists())
            throw new Exception("Not exist!");
        final File e;
        if (java.isFile())
            e = java;
        else {
            final File t1 = new File(java, Arch.JVM.IS_WINDOWS ? "java.exe" : "java"), t2 = new File(java, Arch.JVM.IS_WINDOWS ? "bin/java.exe" : "bin/java");
            if (t1.exists() && t1.isFile())
                e = t1;
            else if (t2.exists() && t2.isFile())
                e = t2;
            else
                throw new Exception("Not exist!");
        }
        final ProcessBuilder b = new ProcessBuilder(e.getAbsolutePath(), "-XshowSettings:properties", "-version");
        final Process p = b.start();
        final ConcurrentHashMap<String, String> variables = new ConcurrentHashMap<>();
        final Thread processKiller = new Thread(() -> {
            try {
                Thread.sleep(15000);
                p.destroyForcibly();
            } catch (final InterruptedException ignored) {
            } catch (final Exception ex) {
                i4Logger.INSTANCE.log(ex);
            }
        });
        processKiller.setName("Process Killer");
        processKiller.start();
        try (final Scanner s = new Scanner(p.getInputStream())) {
            scan(variables, s);
        }
        try (final Scanner s = new Scanner(p.getErrorStream())) {
            scan(variables, s);
        }
        try {
            p.waitFor();
            processKiller.interrupt();
        } catch (final Exception ex) {
            p.destroyForcibly();
            i4Logger.INSTANCE.log(ex);
        }

        if (
                !variables.containsKey("os.name") ||
                !variables.containsKey("os.arch") ||
                !variables.containsKey("java.vendor") ||
                !variables.containsKey("path.separator") ||
                !variables.containsKey("java.specification.version")
        )
            throw new Exception("No information.");
        if (variables.get("java.specification.version").startsWith("1."))
            variables.put("java.specification.version", variables.get("java.specification.version").substring(2));

        return new JavaInfo(Integer.parseInt(variables.get("java.specification.version")),
                new Arch(variables.get("os.name"), variables.get("os.arch"), variables.get("java.vendor")),
                variables.get("path.separator"), e, getDistribution(variables.get("java.vendor"), variables.get("java.vendor.version"), variables.get("java.runtime.name")));
    }

    private static void scan(final ConcurrentHashMap<String, String> vars, final Scanner s) {
        String l, k;
        while (s.hasNextLine()) {
            l = s.nextLine();
            if (!l.contains("="))
                continue;
            if (l.startsWith(" ") || l.startsWith("\t")) {
                int i = 1;
                for (; i < l.length(); i++)
                    if (l.charAt(i) != ' ' && l.charAt(i) != '\t')
                        break;
                l = l.substring(i);
            }
            int i = l.indexOf('=');
            k = l.substring(0, i);
            l = l.substring(i + 1);
            if (k.endsWith(" ") || k.endsWith("\t")) {
                for (i = k.length() - 2; i >= 0; i--)
                    if (k.charAt(i) != ' ' && k.charAt(i) != '\t')
                        break;
                k = k.substring(0, i + 1);
            }
            if (k.isEmpty())
                continue;
            if (l.startsWith(" ") || l.startsWith("\t")) {
                for (i = 1; i < l.length(); i++)
                    if (l.charAt(i) != ' ' && l.charAt(i) != '\t')
                        break;
                l = l.substring(i);
            }
            vars.put(k, l);
        }
    }
}