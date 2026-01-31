package illa4257.i4test;

import illa4257.i4Framework.awt.AWTFramework;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;
import illa4257.i4Utils.logger.i4Logger;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
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
        i4Test.init(new AWTFramework("illa4257.i4Test"));
        if (false) {
            i4Test.start();
            return;
        }

        final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(
                createKeyManagers("niotest.p12", "a12d29f29838193b1537e17e1f6e794e", "a12d29f29838193b1537e17e1f6e794e"),
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

            final NioRunner runner = new NioRunner(sslPool, new ArrayBuffMgr(new int[] { 1024, 32 * 1024, 64 * 1024, 128 * 1024, 256 * 1024, 1024 * 1024 }, 0)) {
                @Override
                public void accept(SelectionKey key) throws IOException {
                    System.out.println("Accept");
                    SocketChannel client = serverChannel.accept();
                    client.configureBlocking(false);

                    final SSLEngine engine = sslContext.createSSLEngine();
                    final SSLParameters parameters = engine.getSSLParameters();
                    //noinspection Since15
                    parameters.setApplicationProtocols(new String[] { "http/1.1" });
                    //parameters.setApplicationProtocols(new String[] { "h2", "http/1.1" });
                    engine.setSSLParameters(parameters);
                    engine.setUseClientMode(false);
                    engine.beginHandshake();
                    final NioHttpSession s = new NioHttpSession() {
                        @Override
                        public void onContentStart() throws Exception {
                            final ByteBuffer b = mgr.get();
                            if (path.equalsIgnoreCase("/favicon.ico") || path.startsWith("/.")) {
                                responseLine = STATUS[404];
                                response = new byte[0];
                                writeStatus(b);
                            } else {
                                response = ("Hello, world! You're at " + path).getBytes(StandardCharsets.US_ASCII);
                                writeStatus(b);
                            }
                            writeHeader(b, "Server", "test");
                            writeEndHeaders(b);
                            b.flip();
                            //write(b);
                            closeRequest(b);
                        }
                    };
                    s.reset();
                    s.engine = engine;
                    s.engine = null;
                    s.mgr = mgr;
                    s.runner = this;
                    s.client = client;
                    (s.key = register(client)).attach(s);
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
