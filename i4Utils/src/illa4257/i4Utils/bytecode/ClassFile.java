package illa4257.i4Utils.bytecode;

import illa4257.i4Utils.io.IO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

public class ClassFile {
    public static final int MAGIC = 0xCAFEBABE;

    public static class StrTag {
        public short stringIndex;

        public StrTag(final short stringIndex) { this.stringIndex = stringIndex; }
    }

    public static class ClsTag {
        public short nameIndex;

        public ClsTag(final short nameIndex) { this.nameIndex = nameIndex; }
    }

    public static class NameAndType {
        public short nameIndex, descriptorIndex;

        public NameAndType(final short nameIndex, final short descriptorIndex) {
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }
    }

    public static class Ref {
        public short classIndex, nameAndTypeIndex;

        public Ref(final short classIndex, final short nameAndTypeIndex) {
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }
    }

    public static class FieldRef extends Ref {
        public FieldRef(short classIndex, short nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }
    }

    public static class MethodRef extends Ref {
        public MethodRef(short classIndex, short nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }
    }

    public static class Method {
        public short accessFlags, nameIndex, descriptorIndex;
        public Collection<Attr> attributes = new ArrayList<>();

        public Method(final short accessFlags, final short nameIndex, final short descriptorIndex) {
            this.accessFlags = accessFlags;
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }
    }

    public static ClassFile parse(final InputStream inputStream) throws IOException {
        if (IO.readBEInt(inputStream) != MAGIC)
            throw new UnsupportedOperationException("Not a class file");

        final ClassFile cls = new ClassFile();
        cls.minorVersion = IO.readBEShort(inputStream);
        cls.majorVersion = IO.readBEShort(inputStream);

        short poolSize = IO.readBEShort(inputStream);
        for (poolSize--; poolSize != 0; poolSize--) {
            final byte tag = IO.readByte(inputStream);
            switch (tag) {
                case 1:
                    cls.constantPool.add(new String(IO.readByteArray(inputStream, IO.readBEShort(inputStream)),
                            StandardCharsets.UTF_8));
                    break;
                case 7:
                    cls.constantPool.add(new ClsTag(IO.readBEShort(inputStream)));
                    break;
                case 8:
                    cls.constantPool.add(new StrTag(IO.readBEShort(inputStream)));
                    break;
                case 9:
                    cls.constantPool.add(new FieldRef(IO.readBEShort(inputStream), IO.readBEShort(inputStream)));
                    break;
                case 10:
                    cls.constantPool.add(new MethodRef(IO.readBEShort(inputStream), IO.readBEShort(inputStream)));
                    break;
                case 12:
                    cls.constantPool.add(new NameAndType(IO.readBEShort(inputStream), IO.readBEShort(inputStream)));
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown tag: " + tag);
            }
        }

        cls.accessFlags = IO.readBEShort(inputStream);
        cls.thisIndex = IO.readBEShort(inputStream);
        cls.superIndex = IO.readBEShort(inputStream);

        if (IO.readBEShort(inputStream) != 0) // Interfaces
            throw new RuntimeException("Not supported!");

        if (IO.readBEShort(inputStream) != 0) // Fields
            throw new RuntimeException("Not supported!");

        for (short methodsCount = IO.readBEShort(inputStream); methodsCount != 0; methodsCount--) {
            final Method m = new Method(IO.readBEShort(inputStream), IO.readBEShort(inputStream), IO.readBEShort(inputStream));
            for (short attributesCount = IO.readBEShort(inputStream); attributesCount != 0; attributesCount--)
                m.attributes.add(new Attr(IO.readBEShort(inputStream), IO.readByteArray(inputStream, IO.readBEInt(inputStream))));
            cls.methods.add(m);
        }

        for (short attributesCount = IO.readBEShort(inputStream); attributesCount != 0; attributesCount--) {
            final short nameIndex = IO.readBEShort(inputStream);
            final int len = IO.readBEInt(inputStream);
            if ("SourceFile".equals(cls.constantPool.get(nameIndex - 1)))
                cls.fileNameIndex = IO.readBEShort(inputStream);
            else
                cls.attributes.add(new Attr(nameIndex, IO.readByteArray(inputStream, len)));
        }

        return cls;
    }

    public short majorVersion = 0, minorVersion = 0, accessFlags = 0, thisIndex = -1, superIndex = -1, fileNameIndex = -1;
    public ArrayList<Object> constantPool = new ArrayList<>();
    public Collection<Attr> attributes = new ArrayList<>();
    public Collection<Method> methods = new ArrayList<>();
}