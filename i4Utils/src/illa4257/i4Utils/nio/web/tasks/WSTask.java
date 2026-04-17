package illa4257.i4Utils.nio.web.tasks;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.nio.web.WSProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.locks.ReentrantLock;

public class WSTask extends Task implements WSProtocol {
    private static final ThreadLocal<byte[]> mask = ThreadLocal.withInitial(() -> new byte[4]);

    public ByteBuffer buffer = null;
    public boolean r = false, n = false, masking = false;
    public static final SecureRandom RND = new SecureRandom();

    public final ReentrantLock lock = new ReentrantLock();

    public void mask(final byte[] arr) {
        final byte[] mask = WSTask.mask.get();
        for (int i = 0; i < arr.length; i++)
            arr[i] ^= mask[i % 4];
    }

    public void writeType(final ByteBuffer buffer, final boolean isFinal, final int type) {
        buffer.put((byte) (isFinal ? type | 0x80 : type));
    }

    public void writeLength(final ByteBuffer buffer, final boolean masking, final long length) {
        if (length >= 0 && length <= 125) {
            buffer.put((byte) (masking ? length | 0x80 : length));
            return;
        }
        if (length >= 0 && length < 65536) {
            buffer.put((byte) (masking ? 0xFE : 0x7E));
            IO.writeBEShort(buffer, (int) length);
            return;
        }
        buffer.put((byte) (masking ? 0xFF : 0x7F));
        IO.writeBELong(buffer, length);
    }

    @Override
    public void sendText(final String text) throws IOException {
        final boolean masking = this.masking;
        final byte[] arr = text.getBytes(StandardCharsets.UTF_8);

        final boolean singlePacket = arr.length <= BuffLand.BIGGEST;
        if (!singlePacket)
            throw new RuntimeException("Not supported long packets");
        final ByteBuffer hb = worker.land.bl0(), b = worker.land.auto(arr.length);

        writeType(hb, true, 0x01);
        writeLength(hb, masking, arr.length);
        if (masking) {
            final byte[] mask = WSTask.mask.get();
            RND.nextBytes(mask);
            hb.put(mask);
            mask(arr);
        }
        b.put(arr);
        hb.flip();
        b.flip();

        lock.lock();
        try {
            write(hb, true);
            write(b, true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sendBinary(final ByteBuffer binary, final boolean recycle) throws IOException {
        final boolean masking = this.masking;
        final ByteBuffer hb = worker.land.bl0();
        final int l = binary.remaining();

        writeType(hb, true, 0x02);
        writeLength(hb, masking, l);
        if (masking) {
            final byte[] mask = WSTask.mask.get();
            RND.nextBytes(mask);
            hb.put(mask);
            final int e = binary.position() + l;
            for (int i = binary.position(), k = 0; i < e; i++, k++)
                binary.put(i, (byte) (binary.get(i) ^ mask[k & 3]));
        }
        hb.flip();
        lock.lock();
        try {
            write(hb, true);
            write(binary, recycle);
        } finally {
            lock.unlock();
        }
    }

    private final byte[] maskRead = new byte[4];
    private int state = 0, b0;
    private boolean finalRead, maskingRead, z0;
    private byte frameType;
    private int i0, i1;
    private volatile long l0 = 0;

    @Override
    public void tick() throws Exception {
        ByteBuffer b = buffer != null ? buffer : worker.bl1;
        final int l = transport.read(b);
        if (l == -1) {
            if (buffer != null)
                worker.land.recycle(buffer);
            else
                b.clear();
            transport.close();
            return;
        }
        b.flip();
        while (true)
            switch (state) {
                case 0: {
                    if (!b.hasRemaining()) {
                        b.clear();
                        return;
                    }
                    byte s = b.get();
                    if (finalRead = (s & 0x80) != 0)
                        s ^= (byte) 0x80;
                    switch (frameType = s) {
                        case 1: // Text
                            state = 1;
                            i0 = 7;
                            break;
                        case 2: // Binary
                            state = 1;
                            i0 = 7;
                            break;
                        case 9: // Ping
                            state = 1;
                            i0 = 6;
                            i1 = 10;
                            z0 = false;
                            continue;
                        case 10: // Pong
                            if (!finalRead) {
                                if (r && n) {
                                    r = false;
                                    continue;
                                }
                                if (buffer != null)
                                    worker.land.recycle(buffer);
                                else
                                    b.clear();
                                transport.close();
                                return;
                            }
                            state = 1;
                            i0 = 5;
                            continue;
                        default:
                            throw new RuntimeException("Not supported packet code: " + s + " | " + finalRead);
                    }
                    //break;
                }
                case 1: {
                    if (!b.hasRemaining()) {
                        b.clear();
                        return;
                    }
                    byte s = b.get();
                    if (maskingRead = (s & 0x80) != 0)
                        s ^= (byte) 0x80;
                    if (s <= 125) {
                        if (maskingRead) {
                            state = 4;
                            b0 = 0;
                        } else
                            state = i0;
                        l0 = s;
                        continue;
                    }
                    throw new RuntimeException("Length " + s);
                }
                case 2:
                case 3:
                    throw new RuntimeException("Stub");
                case 4: // READ MASK
                    while (b0 < 4) {
                        if (!b.hasRemaining()) {
                            b.clear();
                            return;
                        }
                        maskRead[b0++] = b.get();
                    }
                    state = i0;
                    b0 = 0;
                    break;
                case 5: // SKIP
                    if (l0 != 0) {
                        if (!b.hasRemaining()) {
                            b.clear();
                            return;
                        }
                        if (l0 > b.remaining()) {
                            l0 -= b.remaining();
                            b.clear();
                            return;
                        }
                        b.position(b.position() + (int) l0);
                    }
                    state = 0;
                    break;
                case 6: // COPY REPLY
                    final boolean masking = this.masking;
                    ByteBuffer buf = null;
                    if (i1 != 0) {
                        if (masking) {
                            final byte[] mask = WSTask.mask.get();
                            RND.nextBytes(mask);
                            if (maskingRead)
                                for (byte i = 0; i < 4; i++)
                                    maskRead[i] ^= mask[i];
                            else
                                System.arraycopy(mask, 0, maskRead, 0, 4);
                        }
                        buf = worker.land.bl0();
                        writeType(buf, true, i1);
                        writeLength(buf, masking, l0);
                        buf.flip();
                    }
                    lock.lock();
                    try {
                        if (buf != null) {
                            i1 = 0;
                            write(buf, true);
                        }
                        if (buffer == null) {
                            buffer = b;
                            worker.bl1 = worker.land.bl1();
                        }
                        if (maskingRead || masking) {
                            if (!b.hasRemaining()) {
                                b.clear();
                                final int l1 = transport.read(b);
                                if (l1 == -1)
                                    throw new IOException("EOS");
                                b.flip();
                            }
                            {
                                final int limit = b.position() + (int) Math.min(b.remaining(), l0);
                                b0 = 0;
                                for (int p = b.position(); p < limit; p++, b0++) {
                                    if (b0 >= 4)
                                        b0 = 0;
                                    b.put(p, (byte) (b.get(p) ^ maskRead[b0]));
                                }
                            }
                            if (l0 >= 0 && l0 <= b.remaining()) {
                                if (l0 < b.remaining()) {
                                    final ByteBuffer b2 = b.slice();
                                    b2.limit(b2.position() + (int) l0);
                                    int delta = b2.remaining();
                                    transport.write(b2);
                                    delta = delta - b2.remaining();
                                    l0 -= delta;
                                    b.position(b.position() + delta);
                                } else {
                                    int delta = b.remaining();
                                    transport.write(b);
                                    delta = delta - b.remaining();
                                    l0 -= delta;
                                }
                                if (l0 == 0) {
                                    b.compact();
                                    return;
                                }
                            }
                            final ByteBuffer b1 = b;
                            queue(new QTask() {
                                @Override
                                public void tick() throws Exception {
                                    if (!b1.hasRemaining()) {
                                        b1.clear();
                                        final int l = transport.read(b1);
                                        if (l == -1)
                                            throw new IOException("EOS");
                                        b1.flip();
                                        if (l0 == 0) {
                                            transport.interestOps(SelectionKey.OP_READ);
                                            return;
                                        }
                                        if (maskingRead || masking) {
                                            final int limit = b.position() + (int) Math.min(b1.remaining(), l0);
                                            for (int p = b.position(); p < limit; p++, b0++) {
                                                if (b0 >= 4)
                                                    b0 = 0;
                                                b1.put(p, (byte) (b.get(p) ^ maskRead[b0]));
                                            }
                                        }
                                    }
                                    transport.interestOps(SelectionKey.OP_WRITE);
                                    if (l0 >= 0 && l0 <= b1.remaining()) {
                                        if (l0 < b1.remaining()) {
                                            final ByteBuffer b2 = b1.slice();
                                            b2.limit(b2.position() + (int) l0);
                                            int delta = b2.remaining();
                                            transport.write(b2);
                                            delta = delta - b2.remaining();
                                            l0 -= delta;
                                            b1.position(b1.position() + delta);
                                        } else {
                                            int delta = b1.remaining();
                                            transport.write(b1);
                                            delta = delta - b1.remaining();
                                            l0 -= delta;
                                        }
                                        if (l0 == 0) {
                                            b1.compact();
                                            complete();
                                            return;
                                        }
                                    }
                                    final int initial = b1.remaining();
                                    transport.write(b1);
                                    l0 -= initial - b1.remaining();
                                }
                            }, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                            return;
                        } else {
                            b.compact();
                            if (copyReply(b, l0)) {
                                l0 = 0;
                                return;
                            }
                            l0 = 0;
                            break;
                        }
                    } finally {
                        state = 0;
                        lock.unlock();
                    }
                case 7:
                    return;
            }
    }

    @Override
    public byte getFrameType() {
        return frameType;
    }

    @Override
    public long remainingFrameBytes() {
        return l0;
    }

    @Override
    public void skipRest() {
        if (state == 7)
            state = 5;
    }
}