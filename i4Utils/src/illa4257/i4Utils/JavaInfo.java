package illa4257.i4Utils;

import illa4257.i4Utils.logger.i4Logger;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class JavaInfo {
    public final int majorVersion;
    public final Arch arch;
    public final String pathSeparator;
    public final File path;

    public JavaInfo(final int majorVersion, final Arch arch, final String pathSeparator, final File path) {
        this.majorVersion = majorVersion;
        this.arch = arch;
        this.pathSeparator = pathSeparator;
        this.path = path;
    }

    public static JavaInfo check(final File java) throws Exception {
        if (!java.exists())
            throw new Exception("Not exist!");
        final File e;
        if (java.isFile())
            e = java;
        else {
            final File t1 = new File(java, OS.ARCH.IS_WINDOWS ? "java.exe" : "java"), t2 = new File(java, OS.ARCH.IS_WINDOWS ? "bin/java.exe" : "bin/java");
            if (t1.exists() && t1.isFile())
                e = t1;
            else if (t2.exists() && t2.isFile())
                e = t2;
            else
                throw new Exception("Not exist!");
        }
        final Process p = new ProcessBuilder(e.getAbsolutePath(), "-XshowSettings:properties", "-version").start();
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
        p.waitFor();
        processKiller.interrupt();

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
                variables.get("path.separator"), e);
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