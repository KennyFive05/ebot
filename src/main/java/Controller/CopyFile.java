package Controller;

import Model.FileVo;
import Utility.FileUtility;
import Utility.NumberUtility;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CopyFile extends BaseController {

    @Getter
    @Setter
    private String txtFile;
    @Getter
    @Setter
    private String fromPath;
    @Getter
    @Setter
    private String toPath;
    @Getter
    @Setter
    private boolean isVersion;
    private FileUtility _FileUtility = new FileUtility();
    private String _PropertiesPath;

    public static void main(String[] args) {
        CopyFile uv = new CopyFile();
        uv.run(true);
    }

    public CopyFile() {
        try {
            _FileUtility.setReadLanguage(FileUtility.UTF_8);
            _FileUtility.setWriteLanguage(FileUtility.UTF_8);

            Properties properties = new Properties();
            File file = new File("CopyFile.properties");
            if (file.isFile()) {
                _PropertiesPath = file.getAbsolutePath();
            } else {
                _PropertiesPath = this.getClass().getClassLoader().getResource("CopyFile.properties").getPath();
            }
            properties.load(new FileInputStream(_PropertiesPath));
            txtFile = properties.getProperty("txtFile").replace('/', '\\');
            fromPath = properties.getProperty("fromPath");
            toPath = properties.getProperty("toPath");
            isVersion = Boolean.valueOf(properties.getProperty("isVersion"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * save properties
     *
     * @return
     */
    public String saveProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("Save...OK\r\n");
        try {
            Properties properties = new Properties();
            properties.setProperty("txtFile", txtFile);
            properties.setProperty("fromPath", fromPath);
            properties.setProperty("toPath", toPath);
            properties.setProperty("isVersion", String.valueOf(isVersion));
            OutputStream outStrem = new FileOutputStream(_PropertiesPath);
            properties.store(outStrem, "");

            sb.append("txtFile= ").append(txtFile).append("\r\n");
            sb.append("fromPath= ").append(fromPath).append("\r\n");
            sb.append("toPath= ").append(toPath).append("\r\n");
            sb.append("isVersion= ").append(isVersion).append("\r\n");
        } catch (IOException ex) {
            ex.printStackTrace();
            sb = new StringBuilder(ex.getMessage());
        }

        return sb.toString();
    }

    /**
     * 主要流程
     *
     * @param isCopy false: 僅檢查 過版清單(txtFile) 與 來源資料夾(fromPath) 是否相符
     * @return
     */
    public String run(boolean isCopy) {
        String result;

        try {
            // Check Path
            fromPath = replacePath(fromPath);
            toPath = replacePath(toPath);

            // 版次
            String filePath = toPath;
            if (isVersion) {
                _FileUtility.setWriteLanguage(FileUtility.UTF_8);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                filePath = String.format("%s%s_v", toPath, sdf.format(getGMT8().getTime()));
                int version = 1;
                while (_FileUtility.isLivebyDir(filePath + version))
                    version++;
                filePath = filePath + version + "\\";
            }
            String logFile = filePath + "log.md";

            // 換版清單
            Map<String, Integer> map = getTxtMap();
            _FileUtility.write(logFile, printTxnMap(map), true);

            // Check list 清單是不是一對一
            List<FileVo> checkList = checkList(map);
            _FileUtility.write(logFile, printCheckList(map, checkList), true);

            // copy
            if(isCopy) {
                _FileUtility.write(logFile, copyList(checkList, filePath), true);
            }

            result = String.format("已完成, 詳細 log 請參考 %s\r\n", logFile);
        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }

        return result;
    }

    /**
     * 檢查 Path 結尾是否為'\'
     *
     * @param path
     * @return
     */
    private String replacePath(String path) {
        path = path.replace('/', '\\');
        if (!"\\".equals(path.substring(path.length() - 1))) {
            path += '\\';
        }
        return path;
    }

    /**
     * 換版清單
     *
     * @return
     */
    public Map<String, Integer> getTxtMap() throws IOException {
        Map<String, Integer> map = new TreeMap<>(String::compareTo);
        BufferedReader br = _FileUtility.read(txtFile);
        String line = br.readLine();
        while (StringUtils.isNotEmpty(line)) {
            map.put(line, NumberUtility.null2Integer(map.get(line)) + 1);
            line = br.readLine();
        }

        return map;
    }

    /**
     * 列印是否重覆的內容
     *
     * @param map
     */
    public String printTxnMap(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        List<String> list = new ArrayList<>();
        sb.append(String.format("## 過版清單(txtFile) 共 %d 筆\r\n", map.size()));
        map.entrySet().forEach(entry -> {
            sb.append(String.format("* %s\r\n", entry.getKey()));
            if (entry.getValue() > 1) {
                list.add(entry.getKey());
            }
        });

        if (list.size() > 0) {
            sb.append("\r\n### 重覆的\r\n");
            list.stream().map(str -> String.format("* %s >>> %d\r\n", str, map.get(str))).forEach(sb::append);
        }
        sb.append("- - -\r\n");

        return sb.toString();
    }

    /**
     * 檢查 list 清單是不是一對一
     *
     * @param txnMap
     * @return
     */
    public List<FileVo> checkList(Map<String, Integer> txnMap) {
        List<String> txnList = new ArrayList<>();
        txnMap.forEach((key, value) -> txnList.add(key));
        List<FileVo> fileList = _FileUtility.getFileList(fromPath, true, txnList, getBlacklist());
        List<FileVo> list = new ArrayList<>();
        for (String str : txnList) {
            fileList.stream().filter(vo -> ("\\" + vo.getPath() + vo.getName() + "\0").contains((String.format("\\%s\u0000", str).replace("\\\\", "\\")))).forEach(vo -> list.add(vo));
        }
        Collections.sort(list, Comparator.comparing(FileVo::getPath).thenComparing(FileVo::getName));

        return list;
    }

    /**
     * 例外清單
     *
     * @return
     */
    public static List<String> getBlacklist() {
        return Arrays.asList("Debug\\", ".vs\\", "bin\\", "Release\\",
                ".vspscc", ".user", "Thumbs.db",
                "LoginEAI\\", "Web.Debug.config", "Web.Release.config",
                "\\Models\\Pershing.EBot.Models.Communication\\說明.txt",
                "\\Models\\Pershing.EBot.Models.Global\\ClassDiagram1.cd"
        );
    }

    /**
     * 列印 Check List
     *
     * @param txnMap
     * @param fileList
     * @return
     */
    public String printCheckList(Map<String, Integer> txnMap, List<FileVo> fileList) {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> map = new TreeMap<>();
        sb.append(String.format("## 來源資料夾(fromPath) 共 %d 筆", fileList.size()));
        if (txnMap.size() != fileList.size()) {
            sb.append(" ,**與 過版清單(txtFile) 筆數不合**");
        }
        sb.append("\r\n");
        for (FileVo vo : fileList) {
            map.put(vo.getName(), NumberUtility.null2Integer(map.get(vo.getName())) + 1);
            sb.append(String.format("* %s%s\r\n", vo.getPath(), vo.getName()));
        }

        if (txnMap.size() != fileList.size()) {
            sb.append("\r\n### 來源資料夾(fromPath) 找不到相關的檔案\r\n");
            txnMap.forEach((key, value) -> {
                AtomicBoolean bool = new AtomicBoolean(false);
                for (FileVo vo : fileList) {
                    if ((vo.getPath() + vo.getName()).contains(key)) {
                        bool.set(true);
                        break;
                    }
                }
                if (!bool.get()) {
                    sb.append(String.format("* %s\r\n", key));
                }
            });

            sb.append("\r\n### 來源資料夾(fromPath) 對應多個檔案\r\n");
            map.entrySet().stream().filter(entry -> entry.getValue() > 1).map(entry -> String.format("* %s >>> %d\r\n", entry.getKey(), entry.getValue())).forEach(sb::append);
        }
        sb.append("- - -\r\n");

        return sb.toString();
    }

    /**
     * copy File
     *
     * @param fileList
     * @return
     */
    private String copyList(List<FileVo> fileList, String filePath) {
        StringBuilder sb = new StringBuilder("## UAT 換版清單\r\n");
        final int[] count = {1};
        fileList.forEach(v -> {
            try {
                String source = v.getPath() + v.getName();
                Path path = _FileUtility.copy(source, source.replace(fromPath, filePath));
                sb.append(String.format("%d.\t%s\\\t%s\r\n", count[0], path.getParent().toString().replace(filePath.substring(0, filePath.length() - 1), ""), path.getFileName()));
                count[0]++;
            } catch (IOException e) {
                e.printStackTrace();
                sb.append(String.format("> %s\r\n\r\n", e));
            }
        });

        return sb.toString();
    }
}
