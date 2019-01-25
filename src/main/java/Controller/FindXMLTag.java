package Controller;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;

import Model.FileVo;
import Utility.FileUtility;
import Utility.XMLUtility;

public class FindXMLTag extends BaseController {

    private static FileUtility _FileUtility = getFileUtility();

    public static void main(String[] arg) throws IOException {
        String path = "D:/BOT/IIS/EBOT_SIT/LoginEAI/";
        List<String> FILTER = new ArrayList<String>(Arrays.asList("AcctGrpInqRs"));
        List<String> blacklist = new ArrayList<String>();
        FileUtility fileUtility = new FileUtility();

        System.out.println("--- getFileList ---");
        List<FileVo> fileList = fileUtility.getFileList(path, true, FILTER, blacklist);
        System.out.println("fileList size: " + fileList.size());
//        for (FileVo vo : fileList) {
//            System.out.println(vo.getPath());
//        }

        System.out.println("--- readFile ---");
        Map<FileVo, String> map = readFile(fileList);

        System.out.println("--- printTag ---");
        printTag(map);

        printEnd();
    }

    private static Map<FileVo, String> readFile(List<FileVo> fileList) {
        Map<FileVo, String> map = new HashMap<FileVo, String>();
        for (FileVo vo : fileList) {
            try {
                map.put(vo, _FileUtility.readAll(vo.getPath() + vo.getName()));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.out.println(vo);
                e.printStackTrace();
            }
        }
        return map;
    }

    private static Map<FileVo, String> readFile(List<FileVo> fileList, String begin, String end) {
        Map<FileVo, String> map = new HashMap<FileVo, String>();
        for (FileVo vo : fileList) {
            try {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(vo.getPath() + vo.getName()), "UTF-8"));
                if (!StringUtils.isEmpty(begin) && !StringUtils.isEmpty(end)) {
                    String line = "";
                    String xmlString = "";
                    int beginIndex = 0;
                    int endIndex = 0;
                    int count = 1;
                    do {
                        beginIndex = line.indexOf(begin);
                        endIndex = line.indexOf(end) + end.length();
                        if (beginIndex > -1 && endIndex > end.length() - 1) {
                            // 有頭有尾
                            xmlString = line.substring(beginIndex, endIndex);
                            FileVo tVo = new FileVo();
                            tVo.setName(vo.getName() + "_" + count);
                            tVo.setPath(vo.getPath());
                            map.put(tVo, xmlString);
                            xmlString = "";
                        } else if (beginIndex > -1) {
                            // 有頭沒尾
                            xmlString = line.substring(beginIndex);
                        } else if (endIndex > end.length() - 1) {
                            // 沒頭有尾
                            xmlString += line.substring(0, endIndex);
                            FileVo tVo = new FileVo();
                            tVo.setName(vo.getName() + "_" + count);
                            tVo.setPath(vo.getPath());
                            map.put(tVo, xmlString);
                            xmlString = "";
                        } else if (StringUtils.isNotEmpty(xmlString)) {
                            // 沒頭沒尾
                            xmlString += line;
                        }
                        line = br.readLine();
                    } while (line != null);
                } else {
                    StringBuffer sb = new StringBuffer();
                    String str = br.readLine();
                    while (str != null) {
                        sb.append(str);
                        str = br.readLine();
                    }
                    map.put(vo, sb.toString());
                }
                br.close();
            } catch (IOException e) {
                System.err.println("readFile error: " + vo.getPath() + vo.getName());
            }
        }
        return map;
    }

    private static void printTag(Map<FileVo, String> map) {
        for (Entry<FileVo, String> ent : map.entrySet()) {
            try {
                String xml = ent.getValue();
                Map<String, String> map2 = new HashMap<String, String>();
                FileVo vo = ent.getKey();
//                String AcctId = XMLUtility.getFirstValue(xml, "AcctId");
//                if ("983072300189".equals(AcctId)) {
//                    System.out.println(vo);
//                }
                List<String> rec = XMLUtility.getValue(xml, "AcctId");
                for (String AcctId : rec) {
                    if ("983072300189".equals(AcctId)) {
                        System.out.println(vo);
                    }
                }
            } catch (Exception e) {
                // System.out.println(ent.getValue());
                System.out.println("DocumentException error");
            }
        }
    }

    private static FileUtility getFileUtility() {
        FileUtility file = new FileUtility();
//        file.setReadLanguage(FileUtility.UTF_8);
        return file;
    }
}
