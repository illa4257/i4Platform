package illa4257.i4test;

import illa4257.i4Utils.bytecode.Attr;
import illa4257.i4Utils.bytecode.ClassFile;
import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;
import illa4257.i4Utils.logger.i4Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;

public class BytecodeToJS {
    public static final i4Logger L = new i4Logger("Transpiler");

    public static void main(final String[] args) throws Exception {
        final PrintStream ps = System.out;
        L.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out)).inheritGlobalIO();

        ClassFile cf = ClassFile.parse(Files.newInputStream(new File("out/production/Test/Test.class").toPath()));

        ps.print("class ");
        ps.print(cf.constantPool.get(((ClassFile.ClsTag)  cf.constantPool.get(cf.thisIndex - 1)).nameIndex - 1));
        ps.println(" {");
        for (final ClassFile.Method m : cf.methods) {
            ps.print("\t");
            {
                String n = (String) cf.constantPool.get(m.nameIndex - 1);
                if (n.equals("<init>"))
                    n = "constructor";
                ps.print(n);
            }
            ps.println("() {");

            for (final Attr attr : m.attributes) {
                if (!"Code".equals(cf.constantPool.get(attr.nameIndex - 1)))
                    continue;
                try (final ByteArrayInputStream is = new ByteArrayInputStream(attr.info)) {
                    IO.readBEShort(is); // Max Stack

                    ps.print("\t\tconst operandStack = []");
                    {
                        final int ln = IO.readBEShort(is); // Max Locals
                        if (ln > 0) {
                            ps.println(",");
                            ps.print("\t\t\tlocals = new Array(");
                            ps.print(ln);
                            ps.println(");");
                        } else
                            ps.println(";");
                    }

                    int codeLen = IO.readBEInt(is);
                    for (; codeLen != 0; codeLen--) {
                        ps.print("\t\t");
                        final int b = is.read();
                        switch (b) {
                            case 42: // aload_0
                                ps.println("operandStack.push(this);");
                                break;

                            case 183: // invokespecial
                                codeLen -= 2;
                                final ClassFile.MethodRef ref = (ClassFile.MethodRef) cf.constantPool.get(IO.readBEShort(is) - 1);
                                final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                ps.println(cf.constantPool.get(nt.nameIndex - 1));
                                ps.println(cf.constantPool.get(nt.descriptorIndex - 1));
                                break;

                            default:
                                throw new Exception("Unknown operation: " + b);
                        }
                    }
                }
            }

            ps.println("\t}");
        }
        ps.println("}");
    }
}