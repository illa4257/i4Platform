package illa4257.i4Utils.lists;

import illa4257.i4Utils.IO;

import java.nio.charset.Charset;
import java.util.Arrays;

public class MutableCharArray {
    private final Object locker = new Object();
    public final int pageSize;

    private static class CharArrayPage {
        private final Object locker = new Object();
        private final char[] charArray;
        private int offset, length;
        private CharArrayPage next = null;

        public CharArrayPage(final int length) {
            this.charArray = new char[length];
            offset = 0;
            this.length = 0;
        }

        public CharArrayPage(final char[] charArray, final int offset, final int length) {
            this.charArray = charArray;
            if (offset > charArray.length) {
                this.offset = charArray.length;
                this.length = 0;
                return;
            }
            this.offset = offset;
            this.length = length + offset > charArray.length ? charArray.length - offset : length;
        }

        public CharArrayPage(final char[] charArray) {
            this.charArray = charArray;
            this.offset = 0;
            this.length = charArray.length;
        }

        public char getChar(final int i) {
            if (i < 0)
                throw new IndexOutOfBoundsException();
            synchronized (locker) {
                if (i < length)
                    return charArray[offset + i];
                throw new IndexOutOfBoundsException();
            }
        }

        public CharArrayPage clear() {
            synchronized (locker) {
                Arrays.fill(charArray, '\0');
                length = offset = 0;
                final CharArrayPage n = next;
                next = null;
                return n;
            }
        }
    }

    private CharArrayPage page = null;

    public MutableCharArray() { this.pageSize = 8; }
    public MutableCharArray(final int pageSize) { this.pageSize = pageSize; }

    public boolean isEmpty() { synchronized (locker) { return page == null; } }
    public int size() {
        synchronized (locker) {
            if (page == null)
                return 0;
            CharArrayPage p = page;
            int r = p.length;
            while ((p = p.next) != null)
                r += p.length;
            return r;
        }
    }

    public void clear() {
        synchronized (locker) {
            while (page != null)
                page = page.clear();
        }
    }

    public void remove(int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException();
        synchronized (locker) {
            if (page == null)
                throw new IndexOutOfBoundsException();
            CharArrayPage p = page, pp = null;
            while (p != null) {
                if (index == 0) {
                    p.offset++;
                    if (--p.length == 0)
                        if (pp == null)
                            page = p.clear();
                        else
                            pp.next = p.clear();
                    return;
                }
                if (p.length - 1 == index) {
                    p.length--;
                    return;
                }
                if (p.length > index) {
                    final CharArrayPage t = new CharArrayPage(Arrays.copyOfRange(p.charArray, p.offset + index + 1, p.offset + p.length));
                    t.next = p.next;
                    p.length = index;
                    p.next = t;
                    return;
                }
                index -= p.length;
                pp = p;
                p = p.next;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    public void removeRange(int from, int to) {
        if (from < 0 || from > to)
            throw new IllegalArgumentException();
        synchronized (locker) {
            if (page == null) {
                if (from > 0 || to > 0)
                    throw new IndexOutOfBoundsException();
                return;
            }
            if (to > size())
                throw new IndexOutOfBoundsException();
            CharArrayPage p = page, pp = null;
            while (p != null) {
                if (from == 0) {
                    if (p.length >= to) {
                        p.offset += to;
                        p.length -= to;
                        if (p.length == 0)
                            if (pp == null)
                                page = p.clear();
                            else
                                pp.next = p.clear();
                        return;
                    }
                    to -= p.length;
                    p = p.clear();
                    if (pp == null)
                        page = p;
                    else
                        pp.next = p;
                    continue;
                }
                if (p.length == to) {
                    p.length = from;
                    return;
                }
                if (p.length > from && to > p.length) {
                    to -= p.length;
                    p.length = from;
                    from = 0;
                    pp = p;
                    p = p.next;
                    continue;
                }
                from = Math.max(from - p.length, 0);
                to -= p.length;
                pp = p;
                p = p.next;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    public void add(final char ch) {
        synchronized (locker) {
            if (page == null) {
                page = new CharArrayPage(pageSize);
                page.charArray[0] = ch;
                page.length++;
                return;
            }
            CharArrayPage p = page, pp = page;
            while ((p = p.next) != null)
                pp = p;
            if (pp.offset + pp.length < pp.charArray.length) {
                pp.charArray[pp.offset + (pp.length++)] = ch;
            } else {
                p = new CharArrayPage(pageSize);
                p.charArray[0] = ch;
                p.length++;
                pp.next = p;
            }
        }
    }

    public void add(final char ch, int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException();
        synchronized (locker) {
            if (page == null) {
                if (index > 0)
                    throw new IndexOutOfBoundsException();
                page = new CharArrayPage(pageSize);
                page.charArray[0] = ch;
                page.length++;
                return;
            }
            CharArrayPage p = page, pp = null;
            while (p != null) {
                if (index == 0) {
                    if (p.offset == 0) {
                        final CharArrayPage t = new CharArrayPage(pageSize);
                        t.charArray[0] = ch;
                        t.offset = 0;
                        t.length = 1;
                        t.next = p;
                        if (pp == null)
                            page = t;
                        else
                            pp.next = t;
                        return;
                    }
                    p.charArray[--p.offset] = ch;
                    return;
                }
                if (index == p.length && p.offset + p.length < p.charArray.length) {
                    p.charArray[p.offset + (p.length++)] = ch;
                    return;
                }
                if (index < p.length) {
                    final CharArrayPage t = new CharArrayPage(Arrays.copyOfRange(p.charArray, p.offset + index, p.offset + p.length));
                    p.length = index + 1;
                    p.charArray[index] = ch;
                    t.next = p.next;
                    p.next = t;
                    return;
                }
                index -= p.length;
                pp = p;
                p = p.next;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    public void add(final char[] charArray) { addDirect(charArray.clone()); }
    public void addDirect(final char[] charArray) {
        synchronized (locker) {
            if (page == null) {
                page = new CharArrayPage(charArray);
                return;
            }
            CharArrayPage l = page, a;
            while ((a = l.next) != null)
                l = a;
            l.next = new CharArrayPage(charArray);
        }
    }

    public void add(final char[] charArray, final int offset, final int length) { addDirect(charArray.clone(), offset, length); }
    public void addDirect(final char[] charArray, final int offset, final int length) {
        synchronized (locker) {
            if (page == null) {
                page = new CharArrayPage(charArray, offset, length);
                return;
            }
            CharArrayPage l = page, a;
            while ((a = l.next) != null)
                l = a;
            l.next = new CharArrayPage(charArray, offset, length);
        }
    }

    public void add(final char[] charArray, final int index) { addDirect(charArray.clone(), index); }
    public void addDirect(final char[] charArray, int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException();
        synchronized (locker) {
            if (page == null) {
                page = new CharArrayPage(charArray);
                return;
            }
            CharArrayPage p = page, pp = null;
            while (p != null) {
                if (index == 0) {
                    final CharArrayPage t = new CharArrayPage(charArray);
                    t.next = p;
                    if (pp == null)
                        page = t;
                    else
                        pp.next = t;
                    return;
                }
                if (p.length == index) {
                    final CharArrayPage t = new CharArrayPage(charArray);
                    t.next = p.next;
                    p.next = t;
                    return;
                }
                if (p.length > index) {
                    final CharArrayPage s = new CharArrayPage(Arrays.copyOfRange(p.charArray, p.offset + index, p.offset + p.length));
                    s.next = p.next;
                    p.length = index;
                    final CharArrayPage t = new CharArrayPage(charArray);
                    t.next = s;
                    p.next = t;
                    return;
                }

                index -= p.length;
                pp = p;
                p = p.next;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    public void add(final char[] charArray, final int index, final int offset, final int length) { addDirect(charArray.clone(), index, offset, length); }
    public void addDirect(final char[] charArray, int index, final int offset, final int length) {
        if (index < 0)
            throw new IndexOutOfBoundsException();
        synchronized (locker) {
            if (page == null) {
                page = new CharArrayPage(charArray, offset, length);
                return;
            }
            CharArrayPage p = page, pp = null;
            while (p != null) {
                if (index == 0) {
                    final CharArrayPage t = new CharArrayPage(charArray, offset, length);
                    t.next = p;
                    if (pp == null)
                        page = t;
                    else
                        pp.next = t;
                    return;
                }
                if (p.length == index) {
                    final CharArrayPage t = new CharArrayPage(charArray, offset, length);
                    t.next = p.next;
                    p.next = t;
                    return;
                }
                if (p.length > index) {
                    final CharArrayPage s = new CharArrayPage(Arrays.copyOfRange(p.charArray, p.offset + index, p.offset + p.length));
                    s.next = p.next;
                    p.length = index;
                    final CharArrayPage t = new CharArrayPage(charArray, offset, length);
                    t.next = s;
                    p.next = t;
                    return;
                }

                index -= p.length;
                pp = p;
                p = p.next;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    public Character getChar(int i, final Character defaultValue) {
        if (i < 0)
            return defaultValue;
        synchronized (locker) {
            if (page == null)
                return defaultValue;
            if (i < page.length)
                return page.charArray[page.offset + i];
            CharArrayPage c = page;
            while ((c = c.next) != null)
                if (i < c.length)
                    return c.charArray[c.offset + i];
                else
                    i -= c.length;
            return defaultValue;
        }
    }

    /**
     * Retrieves a character at a specific index.
     * @param i index
     * @return char
     */
    public char getChar(int i) {
        if (i < 0)
            throw new IndexOutOfBoundsException();
        synchronized (locker) {
            if (page == null)
                throw new IndexOutOfBoundsException();
            if (i < page.length)
                return page.charArray[page.offset + i];
            i -= page.length;
            CharArrayPage c = page;
            while ((c = c.next) != null) {
                if (i < c.length)
                    return c.charArray[c.offset + i];
                else
                    i -= c.length;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Returns the text contained in this MutableCharArray.
     * For stronger security, it is recommended that the returned character array be cleared after use by setting each character to zero.
     */
    public char[] getChars() {
        synchronized (locker) {
            if (page == null)
                return new char[0];
            final char[] r = new char[size()];
            CharArrayPage c = page;
            int i = c.length;
            System.arraycopy(c.charArray, c.offset, r, 0, i);
            while ((c = c.next) != null) {
                System.arraycopy(c.charArray, c.offset, r, i, c.length);
                i += c.length;
            }
            return r;
        }
    }

    public byte[] getBytes(final Charset charset) { return IO.toBytes(charset, getChars()); }

    /**
     * Returns the text contained in this MutableCharArray.
     * <p>
     *     Note: Not recommended for sensitive information because you can’t clear String.
     *     For sensitive information, use the {@link #getChars()} method.
     * </p>
     *
     * @return String
     */
    public String getString() { return new String(getChars()); }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(super.toString() + " [");
        synchronized (locker) {
            CharArrayPage p = page;
            if (p == null)
                return b.append(']').toString();
            b.append(p.charArray.length).append(", ").append(p.offset).append('-').append(p.length);
            while ((p = p.next) != null)
                b.append(" | ").append(p.charArray.length).append(", ").append(p.offset).append('-').append(p.length);
        }
        return b.append(']').toString();
    }
}