package net.rk4z.s1.swiftbase.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;

public class Main {

    public static void main(String[] args) {
        File currentJar = getCurrentJarLocation();
        File directory = currentJar.getParentFile();
        SwingUtilities.invokeLater(() -> createAndShowGUI(directory));
    }

    public static File getCurrentJarLocation() {
        try {
            URI uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            return new File(uri);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get current jar location", e);
        }
    }

    public static void createAndShowGUI(File baseDir) {
        JFrame frame = new JFrame("SwiftBase Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridLayout(6, 2, 5, 5));

        JLabel platformLabel = new JLabel("プラットフォームを選択");
        JComboBox<String> platformComboBox = new JComboBox<>(new String[]{"fabric", "paper"});
        mainPanel.add(platformLabel);
        mainPanel.add(platformComboBox);

        JLabel jarPathLabel = new JLabel("Jarファイルパス");
        JTextField jarPathField = new JTextField();
        mainPanel.add(jarPathLabel);
        mainPanel.add(jarPathField);

        JLabel ramLabel = new JLabel("RAM設定 (例: 512m, 1g)");
        JTextField ramField = new JTextField("512m");
        mainPanel.add(ramLabel);
        mainPanel.add(ramField);

        JLabel jvmOptionsLabel = new JLabel("追加JVMオプション");
        JTextField jvmOptionsField = new JTextField();
        mainPanel.add(jvmOptionsLabel);
        mainPanel.add(jvmOptionsField);

        JButton startButton = new JButton("起動");
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        mainPanel.add(startButton);
        mainPanel.add(progressBar);

        DefaultListModel<String> jarListModel = new DefaultListModel<>();
        JList<String> jarList = new JList<>(jarListModel);
        jarList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        frame.add(new JScrollPane(jarList), BorderLayout.CENTER);

        jarList.addListSelectionListener(e -> {
            if (!jarList.isSelectionEmpty()) {
                jarPathField.setText(jarList.getSelectedValue());
            }
        });

        frame.add(mainPanel, BorderLayout.NORTH);

        File librariesDir = new File(baseDir, "libraries/net/rk4z/s1/swiftbase");
        List<File> jars = findJars(baseDir);
        for (File jar : jars) {
            jarListModel.addElement(jar.getAbsolutePath());
        }

        startButton.addActionListener(e -> {
            String platform = (String) platformComboBox.getSelectedItem();
            String jarPath = jarPathField.getText();
            if (jarPath.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Jarファイルを入力してください！", "エラー", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String ram = ramField.getText();
            String jvmOptions = jvmOptionsField.getText();

            File platformDir = new File(librariesDir, Objects.requireNonNull(platform));
            File coreDir = new File(librariesDir, "core");
            platformDir.mkdirs();
            coreDir.mkdirs();

            SwingUtilities.invokeLater(() -> {
                downloadDependencies(progressBar, platform, platformDir, coreDir);
                try {
                    String classpath = buildClasspath(platformDir, coreDir);

                    String[] jvmOptionArray = jvmOptions.split(" ");

                    List<String> command = new ArrayList<>();
                    command.add("java");
                    command.add("-Xmx" + ram);
                    command.add("-cp");
                    command.add(classpath);
                    command.add(jarPath);

                    for (String option : jvmOptionArray) {
                        if (!option.isEmpty()) {
                            command.add(option);
                        }
                    }

                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    processBuilder.inheritIO().start();

                    frame.dispose();
                    JOptionPane.showMessageDialog(null, "プロセスを起動しました！", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "プロセスの起動に失敗しました: " + ex.getMessage(), "エラー", JOptionPane.ERROR_MESSAGE);
                }
            });
        });

        frame.setSize(600, 400);
        frame.setVisible(true);
    }

    public static List<File> findJars(File directory) {
        List<File> jars = new ArrayList<>();
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        jars.addAll(findJars(file));
                    } else if (file.getName().endsWith(".jar")) {
                        jars.add(file);
                    }
                }
            }
        }
        return jars;
    }

    public static String buildClasspath(File platformDir, File coreDir) {
        String classpathSeparator = System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":";
        StringBuilder classpathBuilder = new StringBuilder();

        File[] platformJars = platformDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (platformJars != null) {
            for (File jar : platformJars) {
                classpathBuilder.append(jar.getAbsolutePath()).append(classpathSeparator);
            }
        }

        File[] coreJars = coreDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (coreJars != null) {
            for (File jar : coreJars) {
                classpathBuilder.append(jar.getAbsolutePath()).append(classpathSeparator);
            }
        }

        return classpathBuilder.toString();
    }

    public static void downloadDependencies(JProgressBar progressBar, String platform, File platformDir, File coreDir) {
        String baseUrl = "https://repo.maven.apache.org/maven2/net/rk4z/s1/";
        String[][] dependencies = {
                {"swiftbase-" + platform, platformDir.getAbsolutePath()},
                {"swiftbase-core", coreDir.getAbsolutePath()}
        };

        int progress = 0;
        int progressStep = 100 / dependencies.length;

        for (String[] dependency : dependencies) {
            String subPath = dependency[0];
            File targetDir = new File(dependency[1]);

            String metadataUrl = baseUrl + subPath + "/maven-metadata.xml";
            String latestVersion = fetchLatestVersion(metadataUrl);
            String jarUrl = baseUrl + subPath + "/" + latestVersion + "/" + subPath + "-" + latestVersion + ".jar";

            downloadFile(jarUrl, new File(targetDir, subPath + "-" + latestVersion + ".jar"));
            progress += progressStep;
            progressBar.setValue(progress);
        }

        progressBar.setValue(100);
    }

    public static String fetchLatestVersion(String metadataUrl) {
        try {
            URL url = new URL(metadataUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try (InputStream inputStream = connection.getInputStream()) {
                return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(inputStream)
                        .getElementsByTagName("latest")
                        .item(0)
                        .getTextContent();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch latest version", e);
        }
    }

    public static void downloadFile(String fileUrl, File outputFile) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + fileUrl, e);
        }
    }
}
