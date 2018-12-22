package Controller;

import Model.FileVo;
import Utility.FileUtility;
import Utility.NumberUtility;

import java.io.IOException;
import java.util.*;

public class findAll {
    private static FileUtility _FileUtility = new FileUtility();
    private static String _Path_1 = "C:\\PSC\\BOT\\EBOT-UAT\\";
    private static String _Path_2 = "D:\\BOT\\IIS\\Bak\\EBOT-UAT_Release\\";

    public static void main(String[] args) {
        try {
            new FileUtility().write("D:/txtFile.txt", List2String(findAll(_Path_1)), false);
//            new FileUtility().write("D:/txtFile.txt", match(_Path_1, _Path_2), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> findAll(String path) {
        List<String> list = new ArrayList<>();
        List<FileVo> fileList = _FileUtility.getFileList(path, true, Arrays.asList(""), new CopyFile().getBlacklist());
        fileList.forEach(e -> list.add(e.getPath().replace(path, "\\") + e.getName()));
        return list;
    }

    public static String List2String(List<String> list) {
        StringBuilder sb = new StringBuilder();
        list.forEach(s -> sb.append(s).append("\r\n"));
        return sb.toString();
    }

    public static String match(String path1, String path2) {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();

        List<String> list_1 = findAll(path1);
        List<String> list_2 = findAll(path2);
        Map<String, Integer> map = new TreeMap<>();
        list_1.forEach(s -> map.put(s, 1));
        list_2.forEach(s -> {
            Integer count = NumberUtility.null2Integer(map.get(s));
            map.put(s, count + 1);
        });

        map.forEach((key, value) -> {
            if (value == 1) {
                sb1.append(String.format("%s\r\n", key));
            } else {
                sb2.append(String.format("%s\r\n", key));
            }
        });

        return "-- 唯一 --\r\n" + sb1.toString() + "\r\n-- 重覆 -- \r\n" + sb2.toString();
    }
}
