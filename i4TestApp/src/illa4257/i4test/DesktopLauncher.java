package illa4257.i4test;

import illa4257.i4Framework.swing.SwingFramework;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
                            final byte[] b = new byte[64];
                            final InputStream is = s.getInputStream();
                            final OutputStream os = s1.getOutputStream();
                            int d;
                            while ((d = is.read(b)) != -1) {
                                os.write(b, 0, d);
                                os.flush();
                                Thread.sleep(100);
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

                @Override
                public void processData(final SelectionKey key) throws IOException {
                    key.interestOps(SelectionKey.OP_READ);
                    final SocketChannel client = (SocketChannel) key.channel();
                    final Session s = (Session) key.attachment();
                    ByteBuffer ai = s.appIn != null ? s.appIn : appIn;
                    if (s.engine != null) {
                        ByteBuffer ni = s.netIn != null ? s.netIn : netIn;
                        final int l = client.read(ni);
                        if (l == -1)
                            throw new IOException("Closed");
                        ni.flip();
                        while (true) {
                            final SSLEngineResult r = s.engine.unwrap(ni, ai);
                            switch (r.getStatus()) {
                                case OK:
                                    if (ni.hasRemaining()) {
                                        ni.compact();
                                        if (ni == netIn)
                                            netIn = mgr.get(s.engine.getSession().getPacketBufferSize());
                                        s.netIn = ni;
                                    } else
                                        ni.clear();
                                    break;
                                case BUFFER_OVERFLOW:
                                    final ByteBuffer nb = mgr.nextTier(ai, s.engine.getSession().getApplicationBufferSize());
                                    ai.flip();
                                    nb.put(ai);
                                    if (ai == appIn)
                                        ai.clear();
                                    else
                                        mgr.recycle(ai);
                                    s.appIn = ai = nb;
                                    continue;
                                default:
                                    throw new RuntimeException("Unknown unwrap status: " + r.getStatus());
                            }
                            break;
                        }
                    } else {
                        client.read(ai);
                    }
                    ai.flip();

                    final byte[] d = new byte[ai.remaining()];
                    ai.get(d);
                    System.out.println(new String(d));
                }
            };
            runner.register(serverChannel);
            runner.run();
        }
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
