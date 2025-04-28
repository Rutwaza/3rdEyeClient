package client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class Client extends JFrame {

    private JTextField ipField;
    private JTextField nameField;
    private JButton startButton;
    private JLabel statusLabel;
    private AtomicBoolean isStreaming = new AtomicBoolean(false);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        setTitle("ScreenShare Client");
        setSize(400, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(6, 1));

        ipField = new JTextField("127.0.0.1");
        nameField = new JTextField("Anonymous");
        startButton = new JButton("Start Sharing");
        statusLabel = new JLabel("Status: Idle");

        add(new JLabel("Server IP:"));
        add(ipField);
        add(new JLabel("Your Name/Nickname:"));
        add(nameField);
        add(startButton);
        add(statusLabel);

        startButton.addActionListener(e -> startStreaming());

        setVisible(true);
    }

    private void startStreaming() {
        String serverIP = ipField.getText().trim();
        String nickname = nameField.getText().trim();

        if (serverIP.isEmpty() || nickname.isEmpty()) {
            statusLabel.setText("Status: Enter all fields!");
            return;
        }

        new Thread(() -> {
            try {
                statusLabel.setText("Status: Connecting...");
                Socket socket = new Socket(serverIP, 5000);

                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(nickname);

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                String command = dis.readUTF();

                if ("start_screenshare".equals(command)) {
                    statusLabel.setText("Status: Streaming...");

                    Robot robot = new Robot();
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

                    OutputStream os = socket.getOutputStream();
                    DataOutputStream imageDos = new DataOutputStream(os);

                    isStreaming.set(true);
                    while (isStreaming.get()) {
                        BufferedImage image = robot.createScreenCapture(screenRect);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(0.8f); // better compression quality

                        writer.setOutput(new MemoryCacheImageOutputStream(baos));
                        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
                        writer.dispose();

                        byte[] imageBytes = baos.toByteArray();
                        imageDos.writeInt(imageBytes.length);
                        imageDos.write(imageBytes);
                        imageDos.flush();

                        Thread.sleep(100); // faster frames
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                statusLabel.setText("Status: Error - " + ex.getMessage());
            }
        }).start();
    }
}
