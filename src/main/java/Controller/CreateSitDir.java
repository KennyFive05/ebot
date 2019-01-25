package Controller;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CreateSitDir extends BaseController {
    private static String _TXN = "UR2001_FU0101_線上信託開戶申請";
    private static final int _CaseNum = 1;
    private static String _Date = "";
    private static final String _Dir = "D:/BOT/06_SIT測試/04_問題單檔案區/";

    public static void main(String[] args) {
        String root = init();

        List<String> list = new ArrayList<String>();

        // new / old
        List<String> path1 = new ArrayList<String>(Arrays.asList("New", "Old"));

        // case
        List<String> path2 = new ArrayList<String>();
        for (int i = 1; i <= _CaseNum; i++) {
            path2.add("Case " + StringUtils.leftPad(String.valueOf(i), 2, '0'));
        }

        // View / EAI
        List<String> path3 = new ArrayList<String>(Arrays.asList("View", "EAI"));

        list.addAll(loopFor(root, path1, path2, path3));

        for (String str : list) {
            try {
                CreateDir(str);
                System.out.println(str);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        printEnd();
    }

    /**
     * 初始化
     *
     * @return
     */
    private static String init() {
        String root = "";
        String txn_1 = GetTxn_1(_TXN).replace('/', '／');
        String txn_2 = _TXN.replace('/', '／');
        if (StringUtils.isEmpty(_Date)) {
            SimpleDateFormat sdFormat = new SimpleDateFormat("yyyyMMdd");
            _Date = sdFormat.format(new Date());
        }

        root = _Dir + txn_1 + "/" + txn_2 + "/" + _Date + "/";

        return root;
    }

    /**
     * 階層
     *
     * @param root
     * @param append
     * @return
     */
    @SafeVarargs
    private static List<String> loopFor(String root, List<String>... append) {
        return loopFor(root, 0, append);
    }

    /**
     * 階層
     *
     * @param root
     * @param count
     * @param append
     * @return
     */
    @SafeVarargs
    private static List<String> loopFor(String root, int count, List<String>... append) {
        List<String> result = new ArrayList<String>();
        if (append.length < 1) {
            return result;
        }

        List<String> args = append[count];
        for (String str : args) {
            String path = root + str + "/";
            if (count < append.length - 1) {
                result.addAll(loopFor(path, count + 1, append));
            } else {
                result.add(path);
            }
        }
        return result;
    }

    /**
     * 建立 dir
     *
     * @param fileName
     * @throws IOException
     */
    private static void CreateDir(String fileName) throws IOException {

        File newTxt = new File(fileName);
        // 檢查檔案是否已存在
        if (!newTxt.exists()) {
            // 建立目錄
            newTxt.mkdirs();
        }

    }

    private static String GetTxn_1(String txn) {
        String txn_1 = "other";

        switch (txn.substring(0, 4)) {
            case "UR05":
                txn_1 = "05_申請_設定";
                break;
            case "UR14":
                txn_1 = "14_黃金存摺";
                break;
            case "UR20":
                txn_1 = "20_基金／債券服務";
                break;
            case "UR29":
                txn_1 = "29_網路櫃檯";
                break;
            case "UR31":
                txn_1 = "31_公債票券查詢";
                break;
        }
        return txn_1;
    }
}