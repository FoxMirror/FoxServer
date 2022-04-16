package foxlaunch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class LibrariesDownloader {
    private static List<String> librariesSources = new ArrayList<>();

    public static void setupDownloadSource() {
        try {
            String str = sendRequest("https://catserver.moe/api/libraries_sources/");
            for (String s : str.split("\\|")) {
                if (s.startsWith("http://") || s.startsWith("https://")) {
                    librariesSources.add(s);
                }
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }

        if (librariesSources.size() == 0) {
            librariesSources.add("http://sv.catserver.moe:8001/dl/");
            librariesSources.add("http://sv2.catserver.moe:8001/dl/");
            librariesSources.add("http://cdn.catserver.moe/dl/");
        }
    }

    public static void tryDownload(File file, String sha256) {
        Iterator<String> iterator = librariesSources.iterator();
        while (iterator.hasNext()) {
            String downloadUrl = iterator.next() + file.getName();
            try {
                if (sha256 == null && downloadUrl.startsWith("http://")) {
                    System.out.println(String.format("[Warning] Trying to download a file (%s) that missing SHA256 from http protocol, possible security risk!", file.getName()));
                }

                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                new Downloader(downloadUrl, file);

                if (!file.exists() || (sha256 != null && !Objects.equals(Utils.getFileSHA256(file), sha256))) {
                    System.out.println(String.format(LanguageUtils.I18nToString("launch.lib_failure_check"), file.getName(), downloadUrl));
                }

                return;
            } catch (IOException e) {
                System.out.println(String.format(LanguageUtils.I18nToString("launch.lib_failure_download"), e.toString(), downloadUrl));
                if (e instanceof ConnectException || e instanceof SocketTimeoutException) iterator.remove();
            }
        }
    }

    private static String sendRequest(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "Close");
        connection.connect();

        String result = "";
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        }

        return result;
    }

    static class Downloader {
        public Downloader(String downloadUrl, File saveFile) throws IOException {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setRequestMethod("GET");

            System.out.println(String.format(LanguageUtils.I18nToString("launch.lib_downloading"), saveFile.getName(), getSize(connection.getContentLengthLong())));

            ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
            FileOutputStream fos = new FileOutputStream(saveFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            rbc.close();
            fos.close();

            connection.disconnect();
        }

        private String getSize(long size) {
            if (size >= 1048576L) {
                return size / 1048576.0F + " MB";
            }
            if (size >= 1024) {
                return size / 1024.0F + " KB";
            }
            return size + " B";
        }
    }
}
