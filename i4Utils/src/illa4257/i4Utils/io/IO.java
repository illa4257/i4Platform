package illa4257.i4Utils.io;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class IO {
    public static final int BUFFER_SIZE = 1024 * 1024;

    private interface IReader {
        byte[] run(final InputStream inputStream) throws IOException;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IReader detectReader() {
        try {
            final Method m = InputStream.class.getMethod("readAllBytes");
            return is -> {
                try {
                    return (byte[]) m.invoke(is);
                } catch (final Throwable ex) {
                    if (ex instanceof InvocationTargetException) {
                        final Throwable e = ((InvocationTargetException) ex).getTargetException();
                        if (e instanceof NullPointerException)
                            throw (NullPointerException) e;
                        throw (IOException) e;
                    }
                    throw new IOException(ex);
                }
            };
        } catch (final NoSuchMethodException ignored) {}
        try {
            final Class c = Class.forName("sun.misc.IOUtils");
            try {
                final Method m = c.getDeclaredMethod("readFully", InputStream.class);
                return is -> {
                    try {
                        return (byte[]) m.invoke(null, is);
                    } catch (final Throwable ex) {
                        if (ex instanceof InvocationTargetException) {
                            final Throwable e = ((InvocationTargetException) ex).getTargetException();
                            if (e instanceof NullPointerException)
                                throw (NullPointerException) e;
                            throw (IOException) e;
                        }
                        throw new IOException(ex);
                    }
                };
            } catch (final NoSuchMethodException ignored) {}
            try {
                final Method m = c.getDeclaredMethod("readAllBytes", InputStream.class);
                return is -> {
                    try {
                        return (byte[]) m.invoke(null, is);
                    } catch (final Throwable ex) {
                        if (ex instanceof InvocationTargetException) {
                            final Throwable e = ((InvocationTargetException) ex).getTargetException();
                            if (e instanceof NullPointerException)
                                throw (NullPointerException) e;
                            throw (IOException) e;
                        }
                        throw new IOException(ex);
                    }
                };
            } catch (final NoSuchMethodException ignored) {}
            try {
                final Method m = c.getDeclaredMethod("readFully", InputStream.class, int.class, boolean.class);
                return is -> {
                    try {
                        return (byte[]) m.invoke(null, is, -1, true);
                    } catch (final Throwable ex) {
                        if (ex instanceof InvocationTargetException) {
                            final Throwable e = ((InvocationTargetException) ex).getTargetException();
                            if (e instanceof NullPointerException)
                                throw (NullPointerException) e;
                            throw (IOException) e;
                        }
                        throw new IOException(ex);
                    }
                };
            } catch (final NoSuchMethodException ignored) {}
        } catch (final ClassNotFoundException ignored) {}
        return is -> {
            if (is == null)
                throw new NullPointerException("InputStream is null!");
            final ByteArrayOutputStream r = new ByteArrayOutputStream();
            final byte[] buff = new byte[BUFFER_SIZE];
            for (int len = is.read(buff, 0, buff.length); len > -1; len = is.read(buff, 0, buff.length))
                r.write(buff, 0, len);
            return r.toByteArray();
        };
    }

    private static final IReader reader;

    static {
        reader = detectReader();
    }

    public static byte[] readFully(final InputStream inputStream, final boolean close) throws IOException {
        try {
            return reader.run(inputStream);
        } finally {
            if (close)
                inputStream.close();
        }
    }

    public static byte[] readFully(final InputStream inputStream) throws IOException {
        try {
            return reader.run(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public static byte[] readFully(final File path) throws IOException {
        try (final InputStream fis = Files.newInputStream(path.toPath())) {
            return reader.run(fis);
        }
    }

    public static byte[] toBytes(final Charset charset, final char[] charArray) {
        return (charset != null ? charset : Charset.defaultCharset()).encode(CharBuffer.wrap(charArray)).array();
    }

    /**
     * Tries to read a byte, if it reaches the end, it throws IOException.
     * @param is InputStream
     * @return byte
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static byte readByte(final InputStream is) throws IOException {
        final int r = is.read();
        if (r == -1)
            throw new EOFException("End of stream reached while trying to read a byte.");
        return (byte) r;
    }

    /**
     * Tries to read a byte, if it reaches the end, it throws IOException.
     * @param is InputStream
     * @return byte
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static int readByteI(final InputStream is) throws IOException {
        final int r = is.read();
        if (r == -1)
            throw new EOFException("End of stream reached while trying to read a byte.");
        return r;
    }

    /**
     * Reads the whole array, if it fails then it throws IOException.
     * @param stream InputStream
     * @param array Result
     * @param length Number of bytes to read
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static void readByteArray(final InputStream stream, final byte[] array, final int length) throws IOException {
        if (length > array.length)
            throw new IndexOutOfBoundsException(length + " > " + array.length);
        for (int i = 0; i < length; i++)
            array[i] = readByte(stream);
    }

    /**
     * Reads the whole array, if it fails then it throws IOException.
     * @param stream InputStream
     * @param array Result
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static void readByteArray(final InputStream stream, final byte[] array) throws IOException {
        for (int i = 0; i < array.length; i++)
            array[i] = readByte(stream);
    }

    /**
     * Creates and reads the whole array, if it fails then it throws IOException.
     * @param stream InputStream
     * @param length Length of the byte array
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static byte[] readByteArray(final InputStream stream, final int length) throws IOException {
        final byte[] array = new byte[length];
        readByteArray(stream, array);
        return array;
    }

    /**
     * Reads short in big endian order.
     *
     * <pre>
     * {@code (readByte(stream) << 8) + readByte(stream) }
     * </pre>
     *
     * @param stream InputStream
     * @return Integer
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static int readBEShortI(final InputStream stream) throws IOException {
        return (readByteI(stream) << 8) + readByteI(stream);
    }

    /**
     * Reads short in big endian order.
     *
     * <pre>
     * {@code (readByte(stream) << 8) + readByte(stream) }
     * </pre>
     *
     * @param stream InputStream
     * @return Integer
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static short readBEShort(final InputStream stream) throws IOException {
        return (short) ((readByteI(stream) << 8) + readByteI(stream));
    }

    /**
     * Reads integer in big endian order.
     *
     * <pre>
     * {@code (readByte(stream) << 24) + (readByte(stream) << 16) + (readByte(stream) << 8) + readByte(stream) }
     * </pre>
     *
     * @param stream InputStream
     * @return Integer
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static int readBEInteger(final InputStream stream) throws IOException {
        return (readByteI(stream) << 24) + (readByteI(stream) << 16) + (readByteI(stream) << 8) + readByteI(stream);
    }

    /**
     * Reads long in big endian order.
     *
     * <pre>
     * {@code
     * ((long) readByte(stream) << 56) +
     * ((long) readByte(stream) << 48) +
     * ((long) readByte(stream) << 40) +
     * ((long) readByte(stream) << 32) +
     * ((long) readByte(stream) << 24) +
     * ((long) readByte(stream) << 16) +
     * ((long) readByte(stream) <<  8) +
     *  (long) readByte(stream)
     * }
     * </pre>
     *
     * @param stream InputStream
     * @return Integer
     * @throws IOException if any input stream throws IOException, or if it reaches the end.
     */
    public static long readBELong(final InputStream stream) throws IOException {
        return ((long) readByteI(stream) << 56) +
               ((long) readByteI(stream) << 48) +
               ((long) readByteI(stream) << 40) +
               ((long) readByteI(stream) << 32) +
               ((long) readByteI(stream) << 24) +
               ((long) readByteI(stream) << 16) +
               ((long) readByteI(stream) <<  8) +
                (long) readByteI(stream);
    }

    /**
     * Writes in big-endian order.
     *
     * <pre>
     * {@code
     * stream.write(number >> 8);
     * stream.write(number);
     * }
     * </pre>
     *
     * @param stream OutputStream
     * @param number Short
     */
    public static void writeBEShort(final OutputStream stream, int number) throws IOException {
        stream.write(number >> 8);
        stream.write(number);
    }

    /**
     * Writes in big-endian order.
     *
     * <pre>
     * {@code
     * stream.write(number >> 24);
     * stream.write(number >> 16);
     * stream.write(number >> 8);
     * stream.write(number);
     * }
     * </pre>
     *
     * @param stream OutputStream
     * @param number Integer
     */
    public static void writeBEInteger(final OutputStream stream, int number) throws IOException {
        stream.write(number >> 24);
        stream.write(number >> 16);
        stream.write(number >> 8);
        stream.write(number);
    }

    /**
     * Writes in big-endian order.
     *
     * <pre>
     * {@code
     * stream.write((int) (number >> 56));
     * stream.write((int) (number >> 48));
     * stream.write((int) (number >> 40));
     * stream.write((int) (number >> 32));
     * stream.write((int) (number >> 24));
     * stream.write((int) (number >> 16));
     * stream.write((int) (number >> 8));
     * stream.write((int) number);
     * }
     * </pre>
     *
     * @param stream OutputStream
     * @param number Long
     */
    public static void writeBELong(final OutputStream stream, long number) throws IOException {
        stream.write((int) (number >> 56));
        stream.write((int) (number >> 48));
        stream.write((int) (number >> 40));
        stream.write((int) (number >> 32));
        stream.write((int) (number >> 24));
        stream.write((int) (number >> 16));
        stream.write((int) (number >> 8));
        stream.write((int) number);
    }
}