package illa4257.i4test;

import illa4257.i4Utils.bytecode.ClassFile;
import illa4257.i4Utils.ir.IRClass;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;
import illa4257.i4Utils.logger.i4Logger;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.security.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JSTest {

    public static void main(String[] args) throws Exception {
        i4Logger.INSTANCE.unregisterAllHandlers().registerHandler(new AnsiColoredPrintStreamLogHandler(System.out))
                .inheritIO();
        try (final PrintStream out = new PrintStream(new File("i4TestApp/src/illa4257/i4test/bootstrap.js"))) {
            final List<String> classes = Arrays.asList(
                    Object.class.getName(),

                    Number.class.getName(),
                    Integer.class.getName(),
                    Long.class.getName(),
                    Float.class.getName(),
                    Double.class.getName(),
                    String.class.getName(),

                    "java.lang.AbstractStringBuilder",
                    "java/lang/ThreadGroup",
                    "java/lang/IllegalArgumentException",
                    "java/lang/IllegalThreadStateException",

                    "java/lang/ref/SoftReference",
                    "java/lang/ref/ReferenceQueue",

                    "java/lang/reflect/AccessibleObject",
                    "java/lang/reflect/ReflectPermission",
                    "java/lang/reflect/Field",

                    "java/util/Collections",
                    "java/util/Objects",
                    "java/util/AbstractQueue",
                    "java/util/ArrayList",

                    "java/util/concurrent/atomic/AtomicReferenceFieldUpdater",

                    "java/io/FileInputStream",
                    "java/io/InputStream",
                    "java/io/FileDescriptor",
                    "java/io/FileOutputStream",
                    "java/io/OutputStream",
                    "java/io/BufferedInputStream",
                    "java/io/FilterInputStream",

                    "java/security/AccessController",
                    "java/security/AccessControlContext",

                    Runnable.class.getName(),
                    Enum.class.getName(),
                    Guard.class.getName(),
                    Permission.class.getName(),
                    BasicPermission.class.getName(),
                    RuntimePermission.class.getName(),
                    Reference.class.getName(),
                    WeakReference.class.getName(),
                    Thread.class.getName(),
                    StringBuilder.class.getName(),
                    Math.class.getName(),
                    Arrays.class.getName(),
                    Stack.class.getName(),
                    AbstractCollection.class.getName(),
                    AbstractSet.class.getName(),
                    AbstractList.class.getName(),
                    AbstractMap.class.getName(),
                    Vector.class.getName(),
                    Class.class.getName(),
                    ClassLoader.class.getName(),
                    ProtectionDomain.class.getName(),
                    "sun.misc.SharedSecrets",
                    "sun.misc.Unsafe",
                    "sun.misc.VM",
                    "sun.reflect.Reflection",
                    "sun.security.util.Debug",
                    "sun/security/action/GetPropertyAction",
                    "sun/misc/Version",

                    Properties.class.getName(),
                    Hashtable.class.getName(),
                    Dictionary.class.getName(),
                    HashMap.class.getName(),
                    LinkedHashMap.class.getName(),
                    System.class.getName(),
                    CodeSource.class.getName(),
                    Serializable.class.getName(),
                    Comparable.class.getName(),
                    Throwable.class.getName(),
                    Exception.class.getName(),
                    RuntimeException.class.getName()
            );
            final IR2JS.PSW w = new IR2JS.PSW(out);
            w.w("var env = new AsyncJavaEnv(),").ln()
                    .t(1).w("mainThread=null,").ln()
                    .t(1).w("class_loader = {loaded:{}};").ln()
                    .w("(async () => {");
            final HashMap<String, IRClass> irClasses = new HashMap<>();
            final ArrayList<String> loaded = new ArrayList<>();
            try (final ZipInputStream zis = new ZipInputStream(Files.newInputStream(new File(
                    /*Arch.JVM.IS_WINDOWS ?
                    "C:\\Program Files\\Eclipse Adoptium\\jdk-8.0.482.8-hotspot\\jre\\lib\\rt.jar" :
                    "/usr/lib/jvm/java-8-openjdk/jre/lib/rt.jar"*/
                    "rt.zip"
            ).toPath()))) {
                final ArrayList<String> cnl = new ArrayList<>();
                for (final String c : classes)
                    cnl.add(c.replaceAll("\\.", "/"));
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
                final IRClass c = ClassFile.parse(Files.newInputStream(new File("out/production/i4TestApp/illa4257/i4test/Test.class").toPath())).toIRClass();
                irClasses.put(c.name, c);
            }
            /*{
                final IRClass c = ClassFile.parse(Files.newInputStream(new File("/home/illa4257/IdeaProjects/i4Platform/out/production/Test/Test.class").toPath())).toIRClass();
                irClasses.put(c.name, c);
            }
            {
                final IRClass c = ClassFile.parse(Files.newInputStream(new File("EdgeCase.class").toPath())).toIRClass();
                irClasses.put(c.name, c);
            }*/
            w.st(1);
            for (final IRClass c : irClasses.values())
                if (!loaded.contains(c.name))
                    loadClass(irClasses, loaded, c, w);

            w.ln().w("await JavaUtilities.initEnv(class_loader, env);");

            w.ln().w("for (let c of Object.values(class_loader.loaded))");
            w.ln().t(1).w("await regClass(class_loader, env, null, c);");

            w.ln().w("mainThread={};");
            w.ln().w("await env.initThread(mainThread);");

            w.ln().w("const tgc = await env.getClass(class_loader, mainThread, \"java/lang/ThreadGroup\");");
            w.ln().w("const tg = await env.alloc(tgc, mainThread);");
            w.ln().w("await tgc['<init>__V'](tgc, env, mainThread, tg);");

            w.ln().w("const tc = await env.getClass(class_loader, mainThread, \"java/lang/Thread\");");
            w.ln().w("await env.alloc(tc, mainThread, mainThread);");

            w.ln().w("let c = await env.getClass(class_loader, mainThread, \"java/lang/ClassLoader\");");
            w.ln().w("await env.alloc(c, mainThread, class_loader);");
            w.ln().w("await env.setField(class_loader,'isSystem',true);");

            w.ln().w("await env.setField(mainThread, 'priority', await env.getField(tc, 'NORM_PRIORITY'));");
            w.ln().w("await tc['<init>__Ljava/lang/ThreadGroup_2Ljava/lang/String_2V'](tc, env, mainThread, ")
                    .w("mainThread, tg, await JavaUtilities.javaStr(class_loader, env, mainThread, 'main'));");

            w.ln().w("const sys=await env.getClass(class_loader, mainThread, \"java/lang/System\");");
            w.ln().w("await sys['initializeSystemClass__V'](sys, env, mainThread);");

            //w.ln().w("await c['initSystemClassLoader__V'](c,env,mainThread);");
            //w.ln().w("await env.setField(class_loader,'assertionLock',locker);");
            //w.ln().w("await c['<init>__Ljava/lang/ClassLoader_2V'](c,env,mainThread,class_loader,null);");
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