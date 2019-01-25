package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;

import Utility.FileUtility;
import Utility.XMLUtility;

public class HexaLog extends BaseController {
    /**
     * 1.北祥 2.舊網銀-測試區106 3.舊網銀-過版區107 4.指定路徑 5.auto-測試區 6.auto-過版區
     */
    private static int _FileFleg = 6;
    // 日期
    private static int _Date = 20190116;
    // 指定路徑
    private static String _FilePath = "D:\\BOT\\06_SIT測試\\05_電文資料文件彙整\\01.台銀側錄電文\\old\\20181224\\";
    // 寫入檔案的根目錄
    protected static String _WriteDir = "D:/BOT/06_SIT測試/05_電文資料文件彙整/02.階段二_已整理的側錄電文/";
    // LoginEAI 檔案的根目錄
    private static String _WriteLoginEAI = "D:/BOT/IIS/EBOT_SIT/LoginEAI/";
    // Success => StatusCode
    protected static String[] _Success = {"0000", "C080", "C081"};
    // 是否寫入 Login
    private static Boolean _IsLogin = false;

    private static FileUtility _FileUtility = CreateFile();

    public static void main(String[] args) throws IOException {
        // 讀檔
        Map<String, String> file = InitNewBank();
        System.out.println("--- readFile rq ---");
        List<String> rq_list = readFile(file.get("rq"), "<RqXMLData>", "</RqXMLData>");
        System.out.println("rq size:" + rq_list.size());
        System.out.println("--- readFile rs ---");
        List<String> rs_list = readFile(file.get("rs"), "<RsXMLData>", "</RsXMLData>");
        System.out.println("rs size:" + rs_list.size());

        // 分類
        System.out.println("--- Classification ---");
        Map<String, String> rq_map = Classification(rq_list, true);
        Map<String, String> rs_map = Classification(rs_list, false);

        // 寫檔
        System.out.println("--- writeFile ---");
        writeFile(rq_map, rs_map);

        if (_IsLogin) {
            System.out.println("--- writeLogin ---");
            writeLogin(rq_map, rs_map);
        }

        printEnd();
    }

    /**
     * 設定讀取的log
     *
     * @return
     */
    private static Map<String, String> InitNewBank() {
        Map<String, String> map = new HashMap<>();

        String path = "D:/BOT/06_SIT測試/05_電文資料文件彙整/01.台銀側錄電文/";
        switch (_FileFleg) {
            case 1:
                path += _Date + "/北祥/";
                map.put("rq", path + "WEBLog.txt");
                map.put("rs", path + "WEBLog.txt");
                break;
            case 2:
                path += _Date + "/台銀/";
                map.put("rq", path + "106nbin.xml");
                map.put("rs", path + "106nbout.xml");
                break;
            case 3:
                // path += _Date + "/台銀/" + (_Date - 19110000) + "過版/";
                path += _Date + "/台銀/";
                map.put("rq", path + "107nbin.xml");
                map.put("rs", path + "107nbout.xml");
                break;
            case 4:
                // path += _Date + "/台銀/" + (_Date - 19110000) + "過版/";
                path += _Date + "/台銀/";
                map.put("rq", path + "nbin.xml");
                map.put("rs", path + "nbout.xml");
                break;
            case 5:
                path += _Date + "/台銀/";
                map.put("rq", path + "106_nbin.xml_" + _Date);
                map.put("rs", path + "106_nbout.xml_" + _Date);
                break;
            case 6:
                path += _Date + "/台銀/";
                map.put("rq", path + "107_nbin.xml_" + _Date);
                map.put("rs", path + "107_nbout.xml_" + _Date);
                break;
        }

        return map;
    }

    /**
     * 讀取檔案
     *
     * @param url
     * @param begin
     * @param end
     * @return
     * @throws IOException
     */
    protected static List<String> readFile(String url, String begin, String end) throws IOException {
        List<String> list = new ArrayList<String>();
        BufferedReader br = _FileUtility.read(url);
        try {
            String line = "";
            String xmlString = "";
            int beginIndex = 0;
            int endIndex = 0;
            do {
                beginIndex = line.indexOf(begin);
                endIndex = line.indexOf(end) + end.length();
                if (beginIndex > -1 && endIndex > end.length() - 1) {
                    // 有頭有尾
                    xmlString = line.substring(beginIndex, endIndex);
                    list.add(xmlString);
                    xmlString = "";
                } else if (beginIndex > -1) {
                    // 有頭沒尾
                    xmlString = line.substring(beginIndex);
                } else if (endIndex > end.length() - 1) {
                    // 沒頭有尾
                    xmlString += line.substring(0, endIndex);
                    list.add(xmlString);
                    xmlString = "";
                } else if (StringUtils.isNotEmpty(xmlString)) {
                    // 沒頭沒尾
                    xmlString += line;
                }
                line = br.readLine();
            } while (line != null);

        } finally {
            br.close();
        }

        return list;
    }

    /**
     * 分類
     *
     * @param list
     * @return
     */
    protected static Map<String, String> Classification(List<String> list, boolean debug) {
        Map<String, String> map = new TreeMap<>();
        for (String str : list) {
            try {
                String frnMsgID = XMLUtility.getFirstValue(str, "Header", "FrnMsgID");
                if (map.get(frnMsgID) != null) {
                    System.out.println("frnMsgID repeat: " + XMLUtility.getFirstValue(str, "Header", "SvcType") + " / "
                            + frnMsgID);
                    frnMsgID += "#";
                }
                map.put(frnMsgID, str);
            } catch (DocumentException e) {
                if (debug) {
                    System.err.println("Classification => DocumentException: " + str);
                }
            }
        }

        return map;
    }

    /**
     * 寫入檔案
     *
     * @param rq_map
     * @param rs_map
     */
    protected static void writeFile(Map<String, String> rq_map, Map<String, String> rs_map) {
        String noMerge_file = "";
        StringBuffer noMerge_body = new StringBuffer();

        for (Entry<String, String> entry : rq_map.entrySet()) {
            String frnMsgID = entry.getKey();
            String rq = entry.getValue();
            String file_path = "";
            String file_name = "";
            try {
                String rs = rs_map.get(frnMsgID);
                file_path = _WriteDir + getFilePath(rq, rs);
                file_name = getRqFileName(rq);
                _FileUtility.write(file_path + file_name, XMLUtility.format(rq), false);
                if (StringUtils.isNotEmpty(rs)) {
                    file_name = getRsFileName(rs, rq);
                    _FileUtility.write(file_path + file_name, XMLUtility.format(rs), false);
                } else {
                    // no Merge
                    if (StringUtils.isEmpty(noMerge_file)) {
                        noMerge_file = _WriteDir + file_name.split("_")[2].substring(0, 8) + "/noMerge.txt";
                    }
                    String temp = file_name + " / " + frnMsgID;
                    noMerge_body.append(temp + "\r\n");
                    System.out.println("no Merge: " + temp);
                }
            } catch (DocumentException e) {
                System.err.println("getFilePath or getRqFileName => DocumentException: " + frnMsgID);
            } catch (IOException e) {
                System.out.println("Controller.writeFile.write => IOException: " + file_path + file_name);
            }
        }

        // 寫入錯誤 Log
        try {
            if (StringUtils.isNotEmpty(noMerge_file) && StringUtils.isNotEmpty(noMerge_body.toString())) {
                _FileUtility.write(noMerge_file, noMerge_body.toString(), false);
            }
        } catch (IOException e) {
            System.out.println("noMerge_file: " + noMerge_file + "\tnoMerge_body: " + noMerge_body.toString());
        }
    }

    /**
     * 取得檔案路徑
     *
     * @param rq
     * @return
     * @throws DocumentException
     */
    protected static String getFilePath(String rq, String rs) throws DocumentException {
        String ip = XMLUtility.getFirstValue(rq, "Header", "ClientID");
        if (StringUtils.isEmpty(ip) || "::1".equals(ip))
            ip = "127.0.0.1";

        String id = XMLUtility.getFirstValue(rq, "AuthData", "CustPermId");
        id = StringUtils.defaultIfEmpty(id, "Unknown");

        String date = XMLUtility.getValue(rq, "Header", "ClientDtTm").get(0).substring(0, 8);
        String svcType = XMLUtility.getValue(rq, "Header", "SvcType").get(0);
        String time = XMLUtility.getValue(rq, "Header", "ClientDtTm").get(0).substring(8);

        String success = "_Fail";
        if (StringUtils.isNotEmpty(rs)) {
            String statusCode = XMLUtility.getFirstValue(rs, "Header", "StatusCode");
            for (String str : _Success) {
                if (StringUtils.equals(str, statusCode)) {
                    success = "";
                    break;
                }
            }
        }

        // 區分新舊網銀
        String np = "old";
        List<String> list = new ArrayList<>(Arrays.asList("192.168.100.163", "192.168.100.165"));
        if (list.contains(XMLUtility.getFirstValue(rq, "Header", "FrnIP"))) {
            np = "new";
        }

        String str = date + "/" + ip + "/" + np + "/" + id + "/" + date + "_" + time + "_" + svcType + success + "/";

        return str;
    }

    /**
     * 取得 rq 檔案名稱
     *
     * @param rq
     * @return
     * @throws DocumentException
     */
    protected static String getRqFileName(String rq) throws DocumentException {
        String svcType = XMLUtility.getFirstValue(rq, "Header", "SvcType");
        String svcCode = XMLUtility.getFirstValue(rq, "Header", "SvcCode");
        String clientDtTm = XMLUtility.getFirstValue(rq, "Header", "ClientDtTm");

        return svcType + "_" + svcCode + "_" + clientDtTm + ".txt";
    }

    /**
     * 取得 rs 檔案名稱
     *
     * @param rs
     * @return
     * @throws DocumentException
     */
    protected static String getRsFileName(String rs, String rq) throws DocumentException {
        String svcType = XMLUtility.getValue(rs, "Header", "SvcType").get(0);
        String svcCode = "";
        if (!"MsgErrorRs".equals(svcType)) {
            svcCode = XMLUtility.getFirstValue(rs, "Header", "SvcCode");
        } else {
            svcType = XMLUtility.getValue(rq, "Header", "SvcType").get(0).replace("Rq", "Rs").replace("rq", "rs");
            svcCode = XMLUtility.getFirstValue(rq, "Header", "SvcCode");
        }

        // debug
        if (StringUtils.isEmpty(svcCode)) {
            System.out.println("SvcCode is Empty = " + svcType);
        }

        String prcDtTm = XMLUtility.getValue(rs, "Header", "PrcDtTm").get(0);

        return svcType + "_" + svcCode + "_" + prcDtTm + ".txt";
    }

    /**
     * 寫入 LoginEAI
     *
     * @param rq_map
     * @param rs_map
     */
    private static void writeLogin(Map<String, String> rq_map, Map<String, String> rs_map) {
        Map<String, String> map = new TreeMap<>();
        Map<String, String> id_Map = new TreeMap<>();
        for (Entry<String, String> entry : rs_map.entrySet()) {
            String frnMsgID = entry.getKey();
            String rs = entry.getValue();

            try {
                String[] files = {"AcctGrpInqRs", "NPFrnCDInqRs", "SignonRs"};
                String svcType = XMLUtility.getFirstValue(rs, "Header", "SvcType");
                for (String file : files) {
                    if (file.equals(svcType) && "0000".equals(XMLUtility.getFirstValue(rs, "Header", "StatusCode"))) {
                        String id = XMLUtility.getFirstValue(rs, "Text", "CustPermId");
                        map.put(id + "/" + svcType + ".txt", rs);
                        id_Map.put(id, id);
                    }
                }
            } catch (DocumentException e) {
                System.err.println("getFilePath or getRqFileName => DocumentException: " + frnMsgID);
            }
        }

        for (Entry<String, String> entry : map.entrySet()) {
            String file_name = entry.getKey();
            String rs = entry.getValue();
            try {
                _FileUtility.setWriteLanguage(FileUtility.UTF_8);
                _FileUtility.write(_WriteLoginEAI + file_name, XMLUtility.format(rs), false);
            } catch (DocumentException e) {
                System.err.println("getFilePath or getRqFileName => DocumentException: " + file_name);
            } catch (IOException e) {
                System.out.println("Controller.writeFile.write => IOException: " + _WriteLoginEAI + file_name);
            }
        }
        for (String key : id_Map.keySet()) {
            System.out.println(key);
        }
    }

    /**
     * 設定讀/寫檔案的語系
     *
     * @return
     */
    private static FileUtility CreateFile() {
        FileUtility file = new FileUtility();
        if (_FileFleg == 1) {
            file.setReadLanguage(FileUtility.UTF_8);
        }
        return file;
    }
}
