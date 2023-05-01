package club.hellin.hft;

import club.hellin.hft.util.Config;
import club.hellin.hft.util.Mode;
import club.hellin.hft.webserver.WebServer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class Main {
    public static Mode mode = null;

    public static String host;
    public static int port;

    public static URL url; // Only used in CLIENT mode
    public static List<String> ipWhitelist; // Used only in SERVER mode
    public static String rootFilePath; // Used only in SERVER mode
    public static boolean override; // Used only in SERVER mode

    public static void main(final String[] args) {
        try {
            mode = Config.getMode();
            System.out.println(String.format("Started HFT in %s mode.", mode.name()));

            host = Config.getHost();
            port = Config.getPort();

            if (mode == Mode.CLIENT) {
                if (args.length == 0) {
                    System.out.println("Provide a file path!");
                    System.exit(0);
                    return;
                }

                url = new URL(String.format("%s://%s:%s/", Config.getProtocol(), host, port));
                final File file = new File(String.join(" ", args));

                if (!file.exists()) {
                    System.out.println("Specified file does not exist.");
                    System.exit(0);
                    return;
                }

                transfer(file);
                return;
            }

            if (mode == Mode.SERVER) {
                ipWhitelist = Config.getIpWhitelist();
                rootFilePath = Config.getRootFilePath();
                override = Config.shouldOverride();

                new WebServer();
            }
        } catch (final Exception exception) {
            exception.printStackTrace();
            System.exit(0);
        }
    }

    private static void transfer(final File file) throws IOException {
        final boolean isDir = file.isDirectory();

        if (isDir) {
            Files.walk(Paths.get(file.getAbsolutePath(), new String[0]), new FileVisitOption[0]).forEach(path -> {
                try {
                    final File f = path.toFile();

                    if (f.isDirectory())
                        return;

                    transfer(f);
                } catch (final Exception exception) {
                    exception.printStackTrace();
                }
            });
            return;
        }

        final long size = file.length();
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Length", String.valueOf(size));
        conn.setRequestProperty("Content-Type", URLConnection.guessContentTypeFromName(file.getName()));

        /**
         * I know these aren't real headers, but they still work so idc
         */
        conn.setRequestProperty("File-Name", file.getName());
        conn.setRequestProperty("File-Location", file.getParentFile().getAbsolutePath());

        conn.setDoOutput(true);
        final OutputStream out = conn.getOutputStream();
        Files.copy(file.toPath(), out);

        final InputStream in = conn.getInputStream();
        final String response = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).readLine();

        out.close();
        in.close();

        System.out.println(response);
    }
}