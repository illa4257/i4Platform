package illa4257.i4test;

import illa4257.i4Utils.bytecode.ClassFile;
import illa4257.i4Utils.ir.IRClass;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JSTest {
    public static void main(String[] args) throws Exception {
        try (final PrintStream out = new PrintStream(new File("i4TestApp/src/illa4257/i4test/bootstrap.js"))) {
            final List<Class<?>> classes = Arrays.asList(
                    /*Object.class,
                    Stack.class,
                    AbstractCollection.class,
                    AbstractList.class,
                    Vector.class,
                    ClassLoader.class,
                    ProtectionDomain.class,
                    System.class*/
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
                    cnl.add(c.getName().replaceAll("\\.", "/") + ".class");
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    if (!e.getName().endsWith(".class"))
                        continue;
                    if (!cnl.contains(e.getName()))
                        continue;
                    final IRClass c = ClassFile.parse(zis).toIRClass();
                    irClasses.put(c.name, c);
                }
            }
            {
                final IRClass c = ClassFile.parse(Files.newInputStream(new File("/home/illa4257/IdeaProjects/i4Platform/out/production/Test/Test.class").toPath())).toIRClass();
                irClasses.put(c.name, c);
            }
            w.st(1);
            for (final IRClass c : irClasses.values())
                if (!loaded.contains(c.name))
                    loadClass(irClasses, loaded, c, w);
            w.ln().w("let c = await env.getClass(class_loader, \"java/lang/ClassLoader\");");
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
        IR2JS.write(new IR2JS.MW(new IR2JS.SW(w, 1)), c);
        w.w(");");
    }
}