package illa4257.i4test;

import illa4257.i4Utils.bytecode.ClassFile;
import illa4257.i4Utils.ir.*;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;
import illa4257.i4Utils.logger.i4Logger;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BytecodeToIR {
    public static final i4Logger L = new i4Logger("Transpiler");


    public static void main(final String[] args) throws Exception {
        final PrintStream ps = System.out;
        L.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out)).inheritGlobalIO();

        ClassFile cf = null;
        try (final ZipInputStream zis = new ZipInputStream(Files.newInputStream(new File("/usr/lib/jvm/java-8-openjdk/jre/lib/rt.jar").toPath()))) {
            final String target = ClassLoader.class.getName().replaceAll("\\.", "/") + ".class";
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(target)) {
                    cf = ClassFile.parse(zis);
                }
            }
            if (cf == null)
                throw new RuntimeException("No CF");
        }
        //cf = ClassFile.parse(Files.newInputStream(new File("out/production/Test/Test.class").toPath()));
        final IRClass cls = cf.toIRClass();

        IR2JS.write(new IR2JS.PSW(ps), cls);
        /*for (final IRMethod m : cls.methods) {
            if (m.name.equals("equals")) {
                m.print();
                IR2JS.write(new IR2JS.PSW(ps), m.instructions);
            }
        }*/
    }
}