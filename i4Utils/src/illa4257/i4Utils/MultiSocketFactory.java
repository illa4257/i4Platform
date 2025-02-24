package illa4257.i4Utils;

import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiSocketFactory implements Closeable {
    private static final byte[] VERSION = new byte[] { 1, 0, 0, 0 };

    protected static final byte
            /* Methods */
            RESERVE = 0, ACCEPT = 1, CONNECT = 2, CLOSE = 3,

            /* Operations */
            RAW = 0, SINGLE_BYTE = 1, SET_BUFFER_SIZE_MODE = 3, SET_BUFFER_SIZE = 4;

    private final int port, connectionTimeout, timeout;
    private final byte[] appId;
    private final SocketAddress addr;

    private final Object locker = new Object();
    private Thread t = null;

    /**
     *
     * @param appId Application ID, the length should be between 1 byte to 256.
     * @param port Port
     */
    public MultiSocketFactory(final SocketAddress address, final byte[] appId, final int port, final int connectionTimeout, final int timeout) {
        if (appId.length == 0 || appId.length > 256)
            throw new IllegalArgumentException("Application ID is too short or too long");
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.timeout = timeout;
        this.appId = appId;
        this.addr = address;
    }

    /**
     * Local connection.
     * @param appId Application ID, the length should be between 1 byte to 256.
     * @param port Port
     */
    public MultiSocketFactory(final byte[] appId, final int port) {
        this(new InetSocketAddress("127.0.0.1", port), appId, port, 0, 0);
    }

    private static void log(final Throwable ex) {
        final i4Logger l = i4Logger.INSTANCE;
        l.log(Level.ERROR, l.prefix(Level.ERROR, "MultiSocket"), ex);
    }

    private final ConcurrentHashMap<String, Socket> servers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Socket> clients = new ConcurrentHashMap<>();

    private SyncVar<ServerSocket> server = new SyncVar<>();

    public void host() {
        synchronized (locker) {
            if (t != null)
                return;
            try {
                final ServerSocket server = new ServerSocket();
                try {
                    server.bind(addr);
                    if (!this.server.setIfNull(server)) {
                        server.close();
                        return;
                    }
                } catch (final Exception ex) {
                    server.close();
                    throw ex;
                }
                final AtomicBoolean isNotShuttingDown = new AtomicBoolean(true);
                new Thread(() -> {
                    final SecureRandom r = new SecureRandom();
                    try (final ServerSocket serv = server) {
                        while (!serv.isClosed()) {
                            final Socket s = serv.accept();
                            new Thread(() -> {
                                try (final CloseableSyncVar<Socket> ac = new CloseableSyncVar<>(s)) {
                                    s.setTcpNoDelay(true);
                                    final InputStream is = s.getInputStream();
                                    final OutputStream os = s.getOutputStream();
                                    final byte[] ver = new byte[VERSION.length];
                                    int a;
                                    for (int i = 0; i < ver.length; i++) {
                                        a = is.read();
                                        if (a == -1)
                                            throw new IOException("End");
                                        ver[i] = (byte) a;
                                    }
                                    for (int i = 0; i < VERSION.length; i++)
                                        if (VERSION[i] != ver[i]) {
                                            os.write(0);
                                            os.flush();
                                            return;
                                        }
                                    a = is.read();
                                    if (a == -1)
                                        throw new IOException("End");
                                    final byte[] applicationIdArr = new byte[a + 1];
                                    for (int i = 0; i < applicationIdArr.length; i++) {
                                        a = is.read();
                                        if (a == -1)
                                            throw new IOException("End");
                                        applicationIdArr[i] = (byte) a;
                                    }
                                    final String applicationId = Arrays.toString(applicationIdArr);
                                    os.write(1);
                                    os.flush();
                                    a = is.read();
                                    if (a == -1)
                                        throw new IOException("End");
                                    if (a == RESERVE) {
                                        final Socket sm = servers.putIfAbsent(applicationId, s);
                                        System.out.println("Reserving, old = " + sm);
                                        if (sm == null) {
                                            os.write(1);
                                            os.flush();
                                            try {
                                                if (is.read() == CLOSE)
                                                    System.out.println("Stop reserving");
                                            } catch (final Exception ex) {
                                                log(ex);
                                            }
                                            servers.remove(applicationId).close();
                                            if (servers.isEmpty())
                                                close();
                                            return;
                                        }
                                    } else if (a == CONNECT) {
                                        final Socket sm = servers.get(applicationId);
                                        System.out.println("Connecting to " + sm);
                                        if (sm == null) {
                                            os.write(0);
                                            os.flush();
                                            return;
                                        }
                                        synchronized (sm) {
                                            final OutputStream osm = sm.getOutputStream();
                                            while (true) {
                                                final int b = r.nextInt();
                                                System.out.println("Accept code: " + b);
                                                if (clients.putIfAbsent(b, s) != null)
                                                    continue;
                                                osm.write(0);
                                                IO.writeBEInteger(osm, b);
                                                osm.flush();
                                                break;
                                            }
                                        }
                                        ac.preventClosing.set(true);
                                        return;
                                    } else if (a == ACCEPT) {
                                        try (final Socket client = clients.remove(IO.readBEInteger(is))) {
                                            if (client == null)
                                                return;
                                            final InputStream clientIS = client.getInputStream();
                                            final OutputStream clientOS = client.getOutputStream();
                                            clientOS.write(1);
                                            clientOS.flush();

                                            final Thread ct = new Thread(() -> {
                                                try {
                                                    transfer(os, clientIS);
                                                } catch (final Exception ex) {
                                                    log(ex);
                                                }
                                            });
                                            ct.start();
                                            try {
                                                transfer(clientOS, is);
                                            } catch (final IOException ex) {
                                                log(ex);
                                            }
                                            ct.join();
                                            try {
                                                client.close();
                                            } catch (final Exception ex) {
                                                log(ex);
                                            }
                                            return;
                                        }
                                    }
                                    os.write(0);
                                    os.flush();
                                } catch (final Exception ex) {
                                    log(ex);
                                }
                            }).start();
                        }
                    } catch (final Exception ex) {
                        log(ex);
                    }
                    this.server.setIfEquals(null, server);
                    synchronized (locker) {
                        if (t == null)
                            return;
                        if (isNotShuttingDown.get())
                            Runtime.getRuntime().removeShutdownHook(t);
                        t = null;
                    }
                }).start();
                Runtime.getRuntime().addShutdownHook(t = new Thread(() -> {
                    isNotShuttingDown.set(false);
                    try {
                        server.close();
                    } catch (final Exception ex) {
                        log(ex);
                    }
                }));
            } catch (final Exception ignored) {
            }
        }
    }

    private static void transfer(final OutputStream to, final InputStream from) throws IOException {
        try {
            boolean autoSize = true;
            int r, cs;
            byte[] d = new byte[32];
            while (true) {
                final byte o = IO.readByte(from);
                switch (o) {
                    case SINGLE_BYTE:
                        to.write(IO.readByte(from));
                        to.flush();
                        break;
                    case RAW:
                        int length = IO.readBEInteger(from);
                        if (length == 0)
                            break;
                        if (autoSize) {
                            cs = 32;
                            if (length > 1024 * 1024)
                                cs = 10 * 1024 * 1024;
                            else if (length > 1024)
                                cs = 10 * 1024;
                            else if (length > 64)
                                cs = 128;
                            if (d.length != cs)
                                d = new byte[cs];
                        }
                        for (; length > 0; length -= r) {
                            if (length > d.length)
                                r = from.read(d);
                            else
                                r = from.read(d, 0, length);
                            if (r == -1)
                                throw new EOFException("End");
                            to.write(d, 0, r);
                        }
                        to.flush();
                        break;
                    case SET_BUFFER_SIZE_MODE:
                        autoSize = IO.readByte(from) > 0;
                        break;
                    case SET_BUFFER_SIZE:
                        autoSize = false;
                        d = new byte[IO.readBEInteger(from)];
                        break;
                }
            }
        } finally {
            try {
                to.close();
            } catch (final Exception ex) {
                log(ex);
            }
            try {
                from.close();
            } catch (final Exception ex) {
                log(ex);
            }
        }
    }

    protected Socket internalConnect() {
        final Socket s = new Socket();
        try {
            s.setTcpNoDelay(true);
        } catch (final SocketException ex) {
            log(ex);
        }
        try {
            s.connect(addr, connectionTimeout);
        } catch (final IOException ex) {
            log(ex);
            return null;
        }
        try {
            s.setSoTimeout(timeout);
        } catch (final SocketException ex) {
            log(ex);
        }
        try {
            final OutputStream os = s.getOutputStream();
            os.write(VERSION);
            os.write(appId.length - 1);
            os.write(appId);
            os.flush();
            final InputStream is = s.getInputStream();
            final int a = is.read();
            if (a == -1)
                throw new IOException("End");
            if (a == 0)
                return null;
            if (a == 1)
                return s;
            throw new Exception("Unknown code " + a);
        } catch (final Exception ex) {
            log(ex);
            return null;
        }
    }

    public MultiSocketServer reserve() {
        host();
        final Socket s = internalConnect();
        if (s == null)
            return null;
        try {
            final OutputStream os = s.getOutputStream();
            os.write(RESERVE);
            os.flush();
            final InputStream is = s.getInputStream();
            final int a = is.read();
            if (a == -1)
                throw new IOException("End");
            if (a == 1) {
                System.out.println("Reserved");
                return new MultiSocketServer(this, s);
            }
            if (a == 0)
                return null;
            throw new Exception("Unknown code " + a);
        } catch (final Exception ex) {
            log(ex);
            return null;
        }
    }

    public MultiSocket connect() {
        final Socket s = internalConnect();
        if (s == null)
            return null;
        try {
            final OutputStream os = s.getOutputStream();
            os.write(CONNECT);
            os.flush();
            final InputStream is = s.getInputStream();
            final int a = is.read();
            if (a == -1)
                throw new IOException("End");
            if (a == 1)
                return new MultiSocket(s);
            if (a == 0) {
                try {
                    s.close();
                } catch (final Exception ignored) {}
                return null;
            }
            throw new Exception("Unknown code " + a);
        } catch (final Exception ex) {
            try {
                s.close();
            } catch (final Exception ignored) {}
            log(ex);
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        final ServerSocket s = server.getAndSet(null);
        if (s != null)
            s.close();
    }
}