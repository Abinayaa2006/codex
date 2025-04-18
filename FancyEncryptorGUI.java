import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;

public class FancyEncryptorGUI extends JFrame {
    private JTextField filePathField;
    private JComboBox<String> algoBox;
    private JTextField aesKeyField;
    private File encryptedFilePath;

    public FancyEncryptorGUI() {
        setTitle("Fancy Encryptor ✨");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(7, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(255, 240, 245));

        JLabel titleLabel = new JLabel("Fancy Encryptor ✨", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.MAGENTA);
        panel.add(titleLabel);

        filePathField = new JTextField();
        filePathField.setBorder(BorderFactory.createTitledBorder("📁 Enter file path"));
        panel.add(filePathField);

        algoBox = new JComboBox<>(new String[]{"AES", "RSA"});
        algoBox.setBorder(BorderFactory.createTitledBorder("🔐 Choose Algorithm"));
        panel.add(algoBox);

        aesKeyField = new JTextField();
        aesKeyField.setBorder(BorderFactory.createTitledBorder("🔑 AES Key (16 characters)"));
        panel.add(aesKeyField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4, 10, 10));

        JButton encryptButton = new JButton("Encrypt");
        JButton decryptButton = new JButton("Decrypt");
        JButton viewEncryptedButton = new JButton("View Encrypted File");
        JButton viewDecryptedButton = new JButton("View Decrypted File");

        buttonPanel.add(encryptButton);
        buttonPanel.add(decryptButton);
        buttonPanel.add(viewEncryptedButton);
        buttonPanel.add(viewDecryptedButton);
        panel.add(buttonPanel);

        JPanel sharePanel = new JPanel();
        sharePanel.setLayout(new GridLayout(1, 2, 10, 10));
        JButton emailButton = new JButton("📧 Send via Email");
        JButton cloudButton = new JButton("☁️ Upload to Cloud");
        sharePanel.add(emailButton);
        sharePanel.add(cloudButton);
        panel.add(sharePanel);

        add(panel);

        encryptButton.addActionListener(e -> encrypt());
        decryptButton.addActionListener(e -> decrypt());
        viewEncryptedButton.addActionListener(e -> openFile(encryptedFilePath));
        viewDecryptedButton.addActionListener(e -> openFile(new File(filePathField.getText() + ".decrypted")));
        emailButton.addActionListener(e -> sendViaEmail());
        cloudButton.addActionListener(e -> openCloudFolder());
    }

    private void encrypt() {
        try {
            String algo = algoBox.getSelectedItem().toString();
            Path path = Paths.get(filePathField.getText());
            byte[] data = Files.readAllBytes(path);

            if (algo.equals("AES")) {
                String key = aesKeyField.getText();
                if (key.length() != 16) {
                    JOptionPane.showMessageDialog(this, "AES key must be 16 characters.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encrypted = cipher.doFinal(data);

                encryptedFilePath = new File(path.toString() + ".aes.enc");
                Files.write(encryptedFilePath.toPath(), encrypted);
                JOptionPane.showMessageDialog(this, "Encrypted with AES!\nSaved as: " + encryptedFilePath.getName());

            } else {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                KeyPair keyPair = keyGen.generateKeyPair();

                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (int i = 0; i < data.length; i += 245) {
                    int len = Math.min(245, data.length - i);
                    outputStream.write(cipher.doFinal(data, i, len));
                }

                encryptedFilePath = new File(path.toString() + ".rsa.enc");
                Files.write(encryptedFilePath.toPath(), outputStream.toByteArray());
                JOptionPane.showMessageDialog(this, "Encrypted with RSA!\nSaved as: " + encryptedFilePath.getName());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Encryption failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void decrypt() {
        try {
            String algo = algoBox.getSelectedItem().toString();
            Path path = Paths.get(filePathField.getText());
            byte[] encrypted = Files.readAllBytes(path);

            if (algo.equals("AES")) {
                String key = aesKeyField.getText();
                if (key.length() != 16) {
                    JOptionPane.showMessageDialog(this, "AES key must be 16 characters.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                byte[] decrypted = cipher.doFinal(encrypted);

                File decryptedFile = new File(path.toString() + ".decrypted");
                Files.write(decryptedFile.toPath(), decrypted);
                JOptionPane.showMessageDialog(this, "Decrypted! Saved as: " + decryptedFile.getName());
            } else {
                JOptionPane.showMessageDialog(this, "RSA decryption requires the private key. (Not implemented here)", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Decryption failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openFile(File file) {
        try {
            if (file != null && file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                JOptionPane.showMessageDialog(this, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to open file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendViaEmail() {
        if (encryptedFilePath != null) {
            try {
                String subject = "Encrypted File";
                String body = "Hi, I'm sharing an encrypted file with you securely.";
                String uriStr = String.format("mailto:?subject=%s&body=%s",
                        URLEncoder.encode(subject, "UTF-8"),
                        URLEncoder.encode(body, "UTF-8"));

                Desktop.getDesktop().mail(new URI(uriStr));
                JOptionPane.showMessageDialog(this, "Mail client opened. Attach the file manually!", "Info", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open mail client: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "No encrypted file available to send.", "Info", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openCloudFolder() {
        if (encryptedFilePath != null) {
            try {
                Desktop.getDesktop().open(encryptedFilePath.getParentFile());
                JOptionPane.showMessageDialog(this, "Now upload the encrypted file to Google Drive.", "Upload Info", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open file location: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "No encrypted file to upload.", "Info", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FancyEncryptorGUI().setVisible(true));
    }
}
