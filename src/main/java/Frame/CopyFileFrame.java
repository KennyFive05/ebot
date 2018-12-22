package Frame;

import Controller.CopyFile;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

public class CopyFileFrame {

    CopyFile copyFile = new CopyFile();

    public static void main(String[] args) {
        CopyFileFrame gui = new CopyFileFrame();
        gui.run();
    }

    public void run() {
        JFrame jframe = new JFrame();
        jframe.setSize(800, 500);
        jframe.setLayout(null);
        jframe.setTitle("換版工具");
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.setLocation(0, 0); // 設定視窗開啟時左上角的座標，也可帶入Point物件
        jframe.setLocationRelativeTo(null); // 設定開啟的位置和某個物件相同，帶入null則會在畫面中間開啟

        Container cp = jframe.getContentPane();

        // 過版清單
        JLabel n11 = new JLabel("過版清單(txt)", JLabel.RIGHT);
        n11.setBounds(10, 10, 100, 30);
        cp.add(n11);

        JTextField n12 = new JTextField(copyFile.getTxtFile());
        n12.setBounds(120, 10, 400, 30);
        cp.add(n12);

        JButton n13 = new JButton("選擇檔案");
        n13.setBounds(520, 10, 100, 30);
        cp.add(n13);

        n12.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                copyFile.setTxtFile(n12.getText());
            }
        });

        n13.addActionListener(ae -> {
            JFileChooser fileChooser = new JFileChooser();//宣告filechooser
            fileChooser.setCurrentDirectory(new java.io.File(n12.getText()));
            int returnValue = fileChooser.showOpenDialog(null);//叫出filechooser
            //判斷是否選擇檔案
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();//指派給File
                n12.setText(selectedFile.getPath());
            }
        });

        // 資料來源
        JLabel n21 = new JLabel("來源資料夾", JLabel.RIGHT);
        n21.setBounds(10, 50, 100, 30);
        cp.add(n21);

        JTextField n22 = new JTextField(copyFile.getFromPath());
        n22.setBounds(120, 50, 400, 30);
        cp.add(n22);

        JButton n23 = new JButton("選擇資料夾");
        n23.setBounds(520, 50, 100, 30);
        cp.add(n23);

        n22.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                copyFile.setFromPath(n22.getText());
            }
        });

        n23.addActionListener(ae -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File(n22.getText()));
            fileChooser.setDialogTitle("資料來源");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                n22.setText(fileChooser.getSelectedFile().toString());
            }
        });

        // 目的資料夾
        JLabel n31 = new JLabel("目的資料夾", JLabel.RIGHT);
        n31.setBounds(10, 90, 100, 30);
        cp.add(n31);

        JTextField n32 = new JTextField(copyFile.getToPath());
        n32.setBounds(120, 90, 400, 30);
        cp.add(n32);

        JButton n33 = new JButton("選擇資料夾");
        n33.setBounds(520, 90, 100, 30);
        cp.add(n33);

        n32.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                copyFile.setToPath(n32.getText());
            }
        });

        n33.addActionListener(ae -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File(n32.getText()));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                n32.setText(fileChooser.getSelectedFile().toString());
            }
        });

        // 版號
        JLabel n41 = new JLabel("是否要產生版號", JLabel.RIGHT);
        JCheckBox n42 = new JCheckBox();
        n41.setBounds(10, 130, 100, 30);
        cp.add(n41);

        n42.setSelected(copyFile.isVersion());
        n42.setBounds(120, 130, 30, 30);
        cp.add(n42);

        n42.addActionListener(ae -> copyFile.setVersion(n42.isSelected()));

        // 分隔線
        JLabel m00 = new JLabel("-----------------------------------------------------------------------------------------------------------------------------------------------------------");
        m00.setBounds(10, 170, 650, 10);
        cp.add(m00);

        // textArea
        JLabel m01 = new JLabel("訊息說明", JLabel.RIGHT);
        m01.setBounds(10, 200, 100, 30);
        cp.add(m01);

        JTextArea m02 = new JTextArea("Ready...\r\n");
//        m02.setBounds(120, 200, 500, 200);
//        m02.setEditable(false); //禁止輸入
        m02.setLineWrap(true); //自動換行
        m02.setWrapStyleWord(true); //斷行不斷字
        JScrollPane sbrText = new JScrollPane(m02);
        sbrText.setBounds(120, 200, 500, 200);
        cp.add(sbrText);

        // 功能鍵
        JButton f01 = new JButton("執行");
        f01.setBounds(650, 10, 100, 50);
        cp.add(f01);

        f01.addActionListener(ae -> m02.append(copyFile.run(true)));

        JButton f02 = new JButton("保存");
        f02.setBounds(650, 70, 100, 50);
        cp.add(f02);

        f02.addActionListener(ae -> {
            m02.append(copyFile.saveProperties() + "\r\n");
        });

        JButton f03 = new JButton("Check");
        f03.setBounds(650, 130, 100, 50);
        cp.add(f03);

        f03.addActionListener(ae -> m02.append(copyFile.run(false)));

//        JButton f03 = new JButton("測試");
//        f03.setBounds(650, 130, 100, 50);
//        cp.add(f03);
//
//        f03.addActionListener(ae -> System.out.println("Hello"));
//
//        JButton f04 = new JButton("測試2");
//        f04.setBounds(650, 190, 100, 50);
//        cp.add(f04);
//
//        f04.addActionListener(ae -> copyFile.test2());
//
//        JButton f05 = new JButton("測試3");
//        f05.setBounds(650, 250, 100, 50);
//        cp.add(f05);
//
//        f05.addActionListener(ae -> copyFile.test());

        // 必須在最後，不然畫面啟始會顯示不完全
        jframe.setVisible(true);
    }
}