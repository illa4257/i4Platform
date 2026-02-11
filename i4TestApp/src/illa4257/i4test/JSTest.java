package illa4257.i4test;

import illa4257.i4Utils.bytecode.ClassFile;
import illa4257.i4Utils.ir.IRClass;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;
import illa4257.i4Utils.logger.i4Logger;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JSTest {
    public static void main(String[] args) throws Exception {
        i4Logger.INSTANCE.unregisterAllHandlers().registerHandler(new AnsiColoredPrintStreamLogHandler(System.out))
                .inheritIO();
        try (final PrintStream out = new PrintStream(new File("i4TestApp/src/illa4257/i4test/bootstrap.js"))) {
            final List<Class<?>> classes = Arrays.asList(
                    Object.class,
                    String.class,
                    Arrays.class,
                    Stack.class,
                    AbstractCollection.class,
                    AbstractSet.class,
                    AbstractList.class,
                    AbstractMap.class,
                    Vector.class,
                    ClassLoader.class,
                    ProtectionDomain.class,
                    sun.misc.SharedSecrets.class,
                    sun.misc.Unsafe.class,
                    sun.reflect.Reflection.class,
                    HashMap.class,
                    LinkedHashMap.class,
                    System.class
            );
            final IR2JS.PSW w = new IR2JS.PSW(out);
            w.w("var env = new AsyncJavaEnv();").ln()
                    .w("var class_loader = {loaded:{}};").ln()
                    .w("(async () => {");
            final HashMap<String, IRClass> irClasses = new HashMap<>();
            final ArrayList<String> loaded = new ArrayList<>();
            try (final ZipInputStream zis = new ZipInputStream(Files.newInputStream(new File("/usr/lib/jvm/java-8-openjdk/jre/lib/rt.jar").toPath()))) {
                final ArrayList<String> cnl = new ArrayList<>();
                for (final Class<?> c : classes)
                    cnl.add(c.getName().replaceAll("\\.", "/"));
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    if (!e.getName().endsWith(".class"))
                        continue;
                    boolean n = true;
                    for (final String s : cnl)
                        if (e.getName().equals(s + ".class") || e.getName().startsWith(s + "$")) {
                            n = false;
                            break;
                        }
                    if (n)
                        continue;
                    final IRClass c = ClassFile.parse(zis).toIRClass();
                    irClasses.put(c.name, c);
                }
            }
            {
                final IRClass c = ClassFile.parse(Files.newInputStream(new File("/home/illa4257/IdeaProjects/i4Platform/out/production/Test/Test.class").toPath())).toIRClass();
                irClasses.put(c.name, c);
            }
            {
                final IRClass c = ClassFile.parse(Files.newInputStream(new File("EdgeCase.class").toPath())).toIRClass();
                irClasses.put(c.name, c);
            }
            w.st(1);
            for (final IRClass c : irClasses.values())
                if (!loaded.contains(c.name))
                    loadClass(irClasses, loaded, c, w);
            w.ln().w("let c = await env.getClass(class_loader, null, \"java/lang/ClassLoader\");");
            w.ln().w("await env.alloc(c, class_loader);");
            w.ln().w("await class_loader.cls[\"<init>()void\"](c, env, null, class_loader);");
            w.st(0).ln().w("})();");
        }
    }

    private static void loadClass(final HashMap<String, IRClass> irClasses, final ArrayList<String> loaded, final IRClass c, final IR2JS.W w) {
        if (c.superName != null && !loaded.contains(c.superName) && irClasses.containsKey(c.superName))
            loadClass(irClasses, loaded, irClasses.get(c.superName), w);
        loaded.add(c.name);
        w.ln().w("await addClass(class_loader,env,null,");
        try {
            IR2JS.write(new IR2JS.SW(w, 1), c);
        } catch (final RuntimeException e) {
            System.err.println("Error writing class " + c.name);
            throw e;
        }
        w.w(");");
    }
}