package illa4257.i4Utils.bytecode;

public class Attr {
    public short nameIndex;
    public byte[] info;

    public Attr(final short nameIndex, final byte[] info) {
        this.nameIndex = nameIndex;
        this.info = info;
    }
}