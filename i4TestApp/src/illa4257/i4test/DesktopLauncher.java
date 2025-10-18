package illa4257.i4test;

import illa4257.i4Framework.swing.SwingFramework;
import illa4257.i4Utils.Recycler;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;
import illa4257.i4test.NioRunner.Session;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static illa4257.i4test.i4Test.L;

public class DesktopLauncher {
    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    public static final Recycler<ByteBuffer>
            b1024 = new Recycler<>(() -> ByteBuffer.allocate(1024), Buffer::clear),
            b32kb = new Recycler<>(() -> ByteBuffer.allocate(32768), Buffer::clear);

    public static ByteBuffer nextTier(final ByteBuffer buffer) {
        if (buffer.capacity() == 0)
            return b1024.get();
        if (buffer.capacity() == 1024)
            return b32kb.get();
        return buffer;
    }

    public static void recycle(final ByteBuffer buffer) {
        switch (buffer.capacity()) {
            case 1024:
                b1024.recycle(buffer);
                break;
            case 32768:
                b32kb.recycle(buffer);
                break;
        }
    }

    public static ExecutorService sslPool = Executors.newFixedThreadPool(2);

    public static void main(final String[] args) throws Exception {
        L.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out));
        i4Test.init(new SwingFramework("illa4257.i4Test"));

        final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(
                createKeyManagers("niotest.p12", "changeit", "changeit"),
                null, null
        );

        new Thread(() -> {
            try (final ServerSocket server = new ServerSocket()) {
                server.bind(new InetSocketAddress(12345));
                while (!server.isClosed()) {
                    final Socket socket = server.accept();
                    final Socket socket1 = new Socket();
                    socket1.connect(new InetSocketAddress("127.0.0.1", 1234));
                    new Thread(() -> {
                        try (final Socket s = socket; final Socket s1 = socket1) {
                            final byte[] b = new byte[1024];
                            final InputStream is = s.getInputStream();
                            final OutputStream os = s1.getOutputStream();
                            int d;
                            while ((d = is.read(b)) != -1) {
                                os.write(b, 0, d);
                                os.flush();
                                Thread.sleep(1);
                            }
                        } catch (final Exception ex) {
                            L.e(ex);
                        }
                    }).start();
                    new Thread(() -> {
                        try (final Socket s = socket; final Socket s1 = socket1) {
                            final byte[] b = new byte[1024];
                            final InputStream is = s1.getInputStream();
                            final OutputStream os = s.getOutputStream();
                            int d;
                            while ((d = is.read(b)) != -1) {
                                os.write(b, 0, d);
                                os.flush();
                                Thread.sleep(1);
                            }
                        } catch (final Exception ex) {
                            L.e(ex);
                        }
                    }).start();
                }
            } catch (final Exception ex) {
                L.e(ex);
            }
        }) {{
            setDaemon(true);
            setName("Proxy");
        }}.start();

        try (final ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(1234));

            final NioRunner runner = new NioRunner(sslPool, new ArrayBuffMgr(new int[] { 1024, 32 * 1024, 64 * 1024, 128 * 1024 }, 0)) {
                @Override
                public void accept(SelectionKey key) throws IOException {
                    SocketChannel client = serverChannel.accept();
                    client.configureBlocking(false);

                    final SSLEngine engine = sslContext.createSSLEngine();
                    engine.setUseClientMode(false);
                    engine.beginHandshake();
                    final Session s = new Session();
                    s.engine = engine;
                    register(client).attach(s);
                }
            };
            runner.register(serverChannel);
            runner.run();
        }

        /*if (key.isReadable() || key.isWritable()) {

                        if (s.engine != null) {
                            if (globalNetIn.capacity() < s.engine.getSession().getPacketBufferSize()) {
                                globalNetIn = ByteBuffer.allocate(s.engine.getSession().getPacketBufferSize());
                                globalNetOut = ByteBuffer.allocate(s.engine.getSession().getPacketBufferSize());
                            }
                            if (globalAppIn.capacity() < s.engine.getSession().getApplicationBufferSize())
                                globalAppIn = ByteBuffer.allocate(s.engine.getSession().getApplicationBufferSize());

                            ByteBuffer netIn  = globalNetIn, netOut = globalNetOut, appIn  = globalAppIn;

                            if (!s.handshakeComplete) {
                                boolean progress = true;
                                while (progress) {
                                    System.out.println(s.engine.getHandshakeStatus());
                                    progress = false;
                                    switch (s.engine.getHandshakeStatus()) {
                                        case NEED_WRAP:
                                            netOut.clear();
                                            SSLEngineResult wrapResult = s.engine.wrap(EMPTY_BUFFER, netOut);
                                            netOut.flip();
                                            while (netOut.hasRemaining()) {
                                                client.write(netOut);
                                            }
                                            progress = true;
                                            break;

                                        case NEED_UNWRAP:
                                            int bytesRead = client.read(netIn);
                                            if (bytesRead == -1) {
                                                key.cancel();
                                                client.close();
                                                continue;
                                            }
                                            netIn.flip();
                                            SSLEngineResult unwrapResult = s.engine.unwrap(netIn, appIn);
                                            netIn.compact();
                                            progress = true;
                                            break;

                                        case NEED_TASK:
                                            Runnable task;
                                            while ((task = s.engine.getDelegatedTask()) != null) task.run();
                                            progress = true;
                                            break;

                                        case FINISHED:
                                        case NOT_HANDSHAKING:
                                            s.handshakeComplete = true;
                                            key.interestOps(SelectionKey.OP_READ);
                                            break;
                                    }
                                }
                                continue;
                            }

                            int read = client.read(netIn);
                            if (read == -1) {
                                key.cancel();
                                client.close();
                                continue;
                            }

                            netIn.flip();
                            while (netIn.hasRemaining()) {
                                SSLEngineResult result = s.engine.unwrap(netIn, appIn);
                                if (result.getStatus() == SSLEngineResult.Status.OK) {
                                    appIn.flip();
                                    process(s, appIn, result.bytesProduced());
                                    appIn.compact();
                                } else if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                                    System.out.println("UNDERFLOW");
                                    break;
                                }
                            }
                            netIn.compact();
                        } else {
                            final int read = client.read(globalAppIn);
                            globalAppIn.flip();
                            process(s, globalAppIn, read);
                            globalAppIn.clear();
                        }
                    }*/
    }

    public static boolean process(final Session session, final ByteBuffer appIn, final int length) {
        byte[] data = new byte[length];
        appIn.get(data);
        System.out.println("Received: " + new String(data));
        return true;
    }

    protected static KeyManager[] createKeyManagers(String filepath, String keystorePassword, String keyPassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (final InputStream keyStoreIS = Files.newInputStream(Paths.get(filepath))) {
            keyStore.load(keyStoreIS, keystorePassword.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keyPassword.toCharArray());
        return kmf.getKeyManagers();
    }

    protected static TrustManager[] createTrustManagers(String filepath, String keystorePassword) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (final InputStream trustStoreIS = Files.newInputStream(Paths.get(filepath))) {
            trustStore.load(trustStoreIS, keystorePassword.toCharArray());
        }
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(trustStore);
        return trustFactory.getTrustManagers();
    }
}
