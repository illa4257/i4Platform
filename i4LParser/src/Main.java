import i4l.I4LJavaExport;
import i4l.I4LParser;
import i4l.I4LParserConfig;
import i4l.I4LResolver;
import i4l.common.I4LPkg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Exception {
        final ArrayList<Object> code = new ArrayList<>();
        //scan(new File("I4LCommon/src"), code);
        //scan(new File("I4LParser/src"), code);
        scan(new File("GLTest/src/GLFW.i4s"), code);

        final I4LResolver r = new I4LResolver();
        r.resolve(code);

        if (!r.errors.isEmpty()) {
            for (final String e : r.errors)
                System.err.println("[ERR] " + e);
            return;
        }

        r.unlink(code);

        I4LJavaExport.export(code, new File("Test/src"));
    }

    public static void scan(final File dir, final ArrayList<Object> code) throws Exception {
        if (dir.isFile())
            try (final BufferedReader r = new BufferedReader(new FileReader(dir))) {
                code.add(new I4LPkg(""));
                final I4LParser p = new I4LParser(r, new I4LParserConfig()
                        .setCode(code));
                if (p.exception != null) {
                    System.out.println("Line: " + p.line + ", Offset = " + p.off);
                    System.err.println("at " + dir.getPath() + ":" + p.line + ":" + p.off);
                    //p.exception.printStackTrace();
                    throw p.exception;
                }
            }
        else {
            final File[] l = dir.listFiles();
            if (l == null)
                return;
            for (final File f : l)
                scan(f, code);
        }
    }
}