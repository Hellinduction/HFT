package club.hellin.hft.webserver;

import club.hellin.hft.Main;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class WebServer {
    private HttpServer server = null;

    public WebServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(Main.host, Main.port), 0);
            final HttpContext context = this.server.createContext("/");

            context.setHandler(exchange -> {
                try {
                    final Headers headers = exchange.getRequestHeaders();
                    final String ip = exchange.getRemoteAddress().getAddress().getHostAddress();

                    final OutputStream out = exchange.getResponseBody();

                    if (!Main.ipWhitelist.contains(ip) || !exchange.getRequestMethod().equals("POST")) {
                        this.fail(exchange);
                        return;
                    }

                    final String name = headers.getFirst("File-Name");
//                    System.out.println(headers.getFirst("File-Location"));
//                    final String dir = headers.getFirst("File-Location");

                    final File fileToSaveTo = new File(Main.rootFilePath, String.format("%s", name));

                    if (fileToSaveTo.exists() && !Main.override) {
                        this.fail(exchange);
                        return;
                    }

                    final File parent = fileToSaveTo.getParentFile().getAbsoluteFile();
                    if (!parent.exists())
                        parent.mkdirs();

                    if (!fileToSaveTo.exists())
                        fileToSaveTo.createNewFile();

                    final InputStream in = exchange.getRequestBody();
                    Files.copy(in, fileToSaveTo.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    in.close();
                    exchange.sendResponseHeaders(200, 0);

                    final String response = String.format("Saved file %s to %s", name, fileToSaveTo.getAbsolutePath());
                    System.out.println(response);
                    out.write(response.getBytes());
                    out.close();
                } catch (final Exception exception) {
                    exception.printStackTrace();
                    this.fail(exchange);
                }
            });
        } catch (final Exception exception) {
            exception.printStackTrace();
        }

        this.server.start();
        System.out.println(String.format("Attempted to start webserver on port %s.", Main.port));
    }

    private void fail(final HttpExchange exchange) {
        try {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        } catch (final Exception ignored) {}
    }
}