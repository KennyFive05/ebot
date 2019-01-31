package Versioning.Controller;

import Controller.BaseController;
import Controller.CopyFile;
import Model.FileVo;
import Utility.NumberUtility;
import Versioning.Model.ProgramModel;
import Versioning.Model.ReasonForChangeModel;
import Utility.CommonUtility;
import Utility.FileUtility;
import Versioning.Model.UpdateModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Update extends BaseController {
    public final String ERROR = "ERROR";
    public static final String[] MODE = {"SIT", "UAT", "PROD"};
    //    public static final String SIT = "SIT";
//    public static final String UAT = "UAT";
//    public static final String PROD = "PROD";
    private FileUtility _FileUtility;
    private String _PropertiesPath;

    public static void main(String[] args) throws Exception {
        Update update = new Update();
        update.run(update.LoadProperties());
    }

    /**
     * load properties
     *
     * @return
     * @throws IOException
     */
    public UpdateModel LoadProperties() throws IOException {
        UpdateModel data = new UpdateModel();
        Properties properties = new Properties();
        File file = new File("Update.properties");
        if (file.isFile()) {
            _PropertiesPath = file.getAbsolutePath();
        } else {
            _PropertiesPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Update.properties")).getPath();
        }
        properties.load(new InputStreamReader(new FileInputStream(_PropertiesPath), "utf-8"));

        data.setMode(properties.getProperty("mode"));
        data.setFromExecl(properties.getProperty("fromExecl").replace('/', '\\'));
        data.setFromPath(addDirEnd(properties.getProperty("fromPath").replace('/', '\\')));
        data.setToPath(addDirEnd(properties.getProperty("toPath").replace('/', '\\')));
        data.setProdPath(addDirEnd(properties.getProperty("prodPath").replace('/', '\\')));
        data.setVersion(Boolean.valueOf(properties.getProperty("isVersion")));
        data.setCopy(Boolean.valueOf(properties.getProperty("isCopy")));

        return data;
    }

    /**
     * save properties
     *
     * @return
     */
    public String saveProperties(UpdateModel data) {
        try {
            Properties properties = new Properties();
            properties.setProperty("mode", data.getMode());
            properties.setProperty("fromExecl", data.getFromExecl());
            properties.setProperty("fromPath", data.getFromPath());
            properties.setProperty("toPath", data.getToPath());
            properties.setProperty("prodPath", data.getProdPath());
            properties.setProperty("isVersion", String.valueOf(data.isVersion()));
            properties.setProperty("isCopy", String.valueOf(data.isCopy()));
            OutputStream outStrem = new FileOutputStream(_PropertiesPath);
            properties.store(outStrem, "");
            return String.format("Properties 儲存成功: %s", _PropertiesPath);
        } catch (IOException ex) {
            ex.printStackTrace();
            return String.format("Properties 儲存失敗: %s\r\n%s", _PropertiesPath, ex.getMessage());
        }
    }

    /**
     * 主流程
     *
     * @param data
     * @throws Exception
     */
    public String run(UpdateModel data) throws Exception {
        data = init(data);

        // 讀取檔案並檢查是否為 execl
        System.out.println("-- getWorkBook --");
        Workbook workbook = getWorkBook(data);

        // 取得所有程式清單
        System.out.println("-- getProgramModel --");
        Map<String, FileVo> fileMap = getProgramModel(data.getFromPath());

        // 取得各項目
        System.out.println("-- getReasonForChange --");
        List<ReasonForChangeModel> fileList = getReasonForChange(data, workbook, fileMap);

        if (data.isCopy()) {
            // Copy File
            System.out.println("-- copyFile --");
            fileList = copyFile(data, fileList);
        }

        if (data.getMode().equals(MODE[2])) {
            // 產生Execl
            System.out.println("-- CreateExecl --");
            String execl = CreateExecl(fileList, data.getToPath(), data.getProdPath());
            System.out.println(execl);
        }

        // ErrorMessage
        System.out.println("-- ErrorMessage --");
        String error = CreateErrorMessage(fileList, data.getToPath());
        System.out.println(error);

        System.out.println("-- END --");

        return error;
    }

    /**
     * 初始化
     *
     * @param data
     * @return
     */
    private UpdateModel init(UpdateModel data) {
        _FileUtility = new FileUtility();
        _FileUtility.setReadLanguage(FileUtility.UTF_8);
        _FileUtility.setWriteLanguage(FileUtility.UTF_8);

        data.setToPath(addVersion(data, addDirEnd(data.getToPath())));
        data.setFromPath(addDirEnd(data.getFromPath()));
        data.setProdPath(addDirEnd(data.getProdPath()));

        return data;
    }

    /**
     * 檢查目錄結尾是否為'\\'
     *
     * @param path
     * @return
     */
    private String addDirEnd(String path) {
        if (path.lastIndexOf('\\') != path.length() - 1) {
            path = path + "\\";
        }
        return path;
    }

    /**
     * 產生版號
     *
     * @param rq
     * @return
     */
    private String addVersion(UpdateModel rq, String path) {
        // 版次
        if (rq.isVersion()) {
            _FileUtility.setWriteLanguage(FileUtility.UTF_8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            path = String.format("%s%s_v", path, sdf.format(getGMT8().getTime()));
            int version = 1;
            while (_FileUtility.isLivebyDir(path + version))
                version++;
            path = path + version + "\\";
        }
        return path;
    }

    /**
     * 新增 row 的各欄位
     *
     * @param row
     * @param array
     */
    private void addRow(Row row, String... array) {
        for (int i = 0; i < array.length; i++) {
            row.createCell(i).setCellValue(array[i]);
        }
    }

    /**
     * 依檔案取得 xls 或 xlsx
     *
     * @param rq
     * @return
     * @throws IOException
     */
    private Workbook getWorkBook(UpdateModel rq) throws IOException {
        String execl = rq.getFromExecl();

        //創建Workbook工作薄對象，表示整個excel
        Workbook workbook;
        File file = new File(execl);

        //獲得文檔名
        String fileName = file.getName().toLowerCase();

        //獲取excel文檔的io流
        InputStream is = new FileInputStream(file);
        //根據文檔後綴名不同(xls和xlsx)獲得不同的Workbook實現類對象
        if (fileName.endsWith(".xls")) {
            workbook = new HSSFWorkbook(is);
        } else if (fileName.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(is);
        } else {
            throw new IOException(fileName + "不是excel文檔");
        }

        if (rq.isVersion()) {
            String toName = rq.getToPath() + execl.substring(execl.lastIndexOf("\\") + 1);
            _FileUtility.copy(execl, toName);
            System.out.println("execl 已複制: " + toName);
        }

        return workbook;
    }

    /**
     * 取得所有程式清單
     *
     * @param path
     * @return
     */
    private Map<String, FileVo> getProgramModel(String path) {
        Map<String, FileVo> fileMap = new TreeMap<>();
        List<FileVo> list = _FileUtility.getFileList(path, true, null, CopyFile.getBlacklist());
        list.forEach(vo -> {
            vo.setPath(vo.getPath().replace(path, "\\"));
            fileMap.put(vo.getPath() + vo.getName(), vo);
        });
        return fileMap;
    }

    /**
     * 取得各項目
     *
     * @param rq
     * @param workbook
     * @param fileMap
     * @return
     */
    private List<ReasonForChangeModel> getReasonForChange(UpdateModel rq, Workbook workbook, Map<String, FileVo> fileMap) {
        List<ReasonForChangeModel> list = new LinkedList<>();
        Sheet sheet = workbook.getSheetAt(workbook.getSheetIndex("換版單"));

        // 取得各標題對應的欄位 index
        Row row = sheet.getRow(0);
        List<String> cells = new LinkedList<>();
        for (int j = 0; j < row.getLastCellNum(); j++) {
            if (CellType.STRING == row.getCell(j).getCellType()) {
                cells.add(row.getCell(j).toString());
            } else {
                cells.add("");
            }
        }

        for (int i = 1; i < sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);

            // 塞選資料 & 檢查項目是否為空值
            if (MODE[0].equals(rq.getMode())) {
                if (!checkCellValue(row, cells, "SIT待換版") || checkCellValue(row, cells, "SIT換版日"))
                    continue;
            } else if (MODE[1].equals(rq.getMode())) {
                if (!checkCellValue(row, cells, "UAT待換版") || checkCellValue(row, cells, "UAT換版日"))
                    continue;
            } else if (MODE[2].equals(rq.getMode())) {
                if (!checkCellValue(row, cells, "Prod待換版") || checkCellValue(row, cells, "Prod換版日"))
                    continue;
            }
            if (!checkCellValue(row, cells, "序號", "通報單", "項目", "程式清單")) {
                continue;
            }

            // 資料
            ReasonForChangeModel model = new ReasonForChangeModel();
            try {
                model.setId(new BigDecimal(getCell(row, cells, "序號")).intValue());
            } catch (Exception ex) {
                continue;
            }
            model.setNumber(getCell(row, cells, "通報單"));
            model.setReason(getCell(row, cells, "項目"));
            model.setOnlineNumber(getCell(row, cells, "線上問題單號"));
            model.setUatNumber(getCell(row, cells, "UAT單號"));
            model.setSPrograms(getCell(row, cells, "程式清單"));

            // 程式清單fileMap
            List<ProgramModel> programs = new LinkedList<>();
            String[] array = model.getSPrograms().replace("/", "\\").split("\n");
            for (String str : array) {
                // 空白行
                if (StringUtils.isBlank(str))
                    continue;

                str = str.replace(' ', '\t');
                String[] temp = str.split("\t");
                temp = Arrays.stream(temp).filter(s -> !"".equals(s)).toArray(String[]::new);

                ProgramModel programModel = new ProgramModel();
                programModel.setId(model.getId());
                if (temp.length == 2) {
                    int beginIndex = temp[0].indexOf("\\", 1);
                    while (fileMap.get(temp[0]) == null && beginIndex > 0) {
                        temp[0] = temp[0].substring(beginIndex);
                        beginIndex = temp[0].indexOf("\\", 1);
                    }
                    if (fileMap.get(temp[0]) != null) {
                        FileVo vo = fileMap.get(temp[0]);
                        programModel.setPath(vo.getPath());
                        programModel.setName(vo.getName());
                        programModel.setStatus(temp[1]);

                        // 如果有新加入檔案，一定要調整 .csproj
                        if ("加入".equals(programModel.getStatus())) {
                            programs.add(programModel);
                            programModel = new ProgramModel();
                            programModel.setId(model.getId());
                            programModel.setStatus("編輯");
                            if (vo.getPath().contains("Pershing.EBot.Utility")) {
                                programModel.setPath("\\Library\\Pershing.EBot.Utility\\");
                                programModel.setName("Pershing.EBot.Utility.csproj");
                            } else if (vo.getPath().contains("Pershing.EBot.Models.Communication")) {
                                programModel.setPath("\\Models\\Pershing.EBot.Models.Communication\\");
                                programModel.setName("Pershing.EBot.Models.Communication.csproj");
                            } else if (vo.getPath().contains("Pershing.EBot.Models.Global")) {
                                programModel.setPath("\\Models\\Pershing.EBot.Models.Global\\");
                                programModel.setName("Pershing.EBot.Models.Global.csproj");
                            } else {
                                programModel.setPath("\\Pershing.EBot.Project\\Pershing.EBot.Project\\");
                                programModel.setName("Pershing.EBot.Project.csproj");
                            }
                        }
                    } else {
                        programModel.setName(str);
                        programModel.setStatus(ERROR);
                        programModel.setErrorMessage("getReasonForChange error: 無法找到對應的程式, 請確認交易路徑或名稱是否正確 or 更新專案至最新版!");
                    }
                } else {
                    programModel.setName(str);
                    programModel.setStatus(ERROR);
                    programModel.setErrorMessage("getReasonForChange error: 無法解析的交易路徑！");
                }
                programs.add(programModel);
            }
            model.setPrograms(programs);
            list.add(model);
        }

        return list;
    }

    /**
     * Copy File
     *
     * @param rq
     * @param fileList
     * @return
     */
    private List<ReasonForChangeModel> copyFile(UpdateModel rq, List<ReasonForChangeModel> fileList) {
        String toPath = rq.getToPath();
        if (rq.isVersion()) {
            toPath = toPath + toPath.substring(toPath.lastIndexOf('\\', toPath.length() - 2) + 1);
        }
        List<String> black = Arrays.asList("Web.config", "Pershing.EBot.Project.csproj", "EBotCCardAPI.js", "AstarWebUI.js");
        for (ReasonForChangeModel file : fileList) {
            for (ProgramModel programModel : file.getPrograms()) {
                String name = programModel.getName();
                if (ERROR.equals(programModel.getStatus())) {
                    continue;
                } else if (!MODE[2].equals(rq.getMode()) && black.contains(name)) {
                    programModel.setStatus(ERROR);
                    programModel.setErrorMessage("copyFile error: 此檔需手動比對");
                    continue;
                }

                String path = programModel.getPath() + name;
                path = (rq.getFromPath() + path).replace("\\\\", "\\");
                try {
                    _FileUtility.copy(path, path.replace(rq.getFromPath(), toPath), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                    programModel.setStatus(ERROR);
                    programModel.setErrorMessage("copyFile error: " + e.getMessage());
                }

            }
        }
        return fileList;
    }

    /**
     * 取得 cell
     *
     * @param row
     * @param cells
     * @param cellName
     * @return
     */
    private String getCell(Row row, List<String> cells, String cellName) {
        String result = "";
        int index = cells.indexOf(cellName);
        if (index > -1) {
            Cell cell = row.getCell(index);
            if (cell != null) {
                if (CellType.STRING == cell.getCellType() || CellType.NUMERIC == cell.getCellType()) {
                    result = NumberUtility.format("9", cell.toString().trim(), "ZZZ9.Z");
                }
            }
        }

        return result;
    }

    /**
     * 檢查項目是否為空值, 皆有值回傳 true
     *
     * @param row
     * @param cells
     * @param array
     * @return
     */
    private boolean checkCellValue(Row row, List<String> cells, String... array) {
        boolean flag = true;
        for (String str : array) {
            if ("".equals(getCell(row, cells, str))) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    /**
     * 產生 .xlsx
     *
     * @param fileList
     * @param toPath
     * @param prodPath
     * @return
     * @throws IOException
     */
    private String CreateExecl(List<ReasonForChangeModel> fileList, String toPath, String prodPath) throws IOException {
        List<ReasonForChangeModel> newfileList = new LinkedList<>();
        newfileList.addAll(fileList);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("換版原因");
        for (int i = 0; i < newfileList.size(); i++) {
            Row row = sheet.createRow((short) i);
            newfileList.sort(Comparator.comparing(ReasonForChangeModel::getNumber).thenComparing(ReasonForChangeModel::getId));
            ReasonForChangeModel model = newfileList.get(i);
            model.setId(i + 1);
            model.getPrograms().forEach(file -> file.setId(model.getId()));
            String[] array = {model.getId() + ".", model.getNumber(), model.getReason()};
            addRow(row, array);
        }

        sheet = workbook.createSheet("異動程式清單");
        Row row = sheet.createRow((short) 0);
        String[] array = {"序號", "目錄", "程式名稱", "異動", "項目編號"};
        addRow(row, array);
        List<ProgramModel> newPrograms = new LinkedList<>();
        newfileList.forEach(file -> newPrograms.addAll(file.getPrograms()));
        newPrograms.removeIf(p -> ERROR.equals(p.getStatus()));
        newPrograms.sort(Comparator.comparing(ProgramModel::getPath).thenComparing(ProgramModel::getName).thenComparing(ProgramModel::getId));
        String ids = "";
        short count = 1;
        for (int i = 0; i < newPrograms.size(); i++) {
            ProgramModel model = newPrograms.get(i);
            if ("".equals(ids)) {
                ids = String.valueOf(model.getId());
            } else if (!ids.contains(String.valueOf(model.getId()))) {
                ids = String.format("%s\r\n%d", ids, model.getId());
            }
            /* 寫入execl
             * 1. 最後一筆
             * 2. 跟下一筆的檔案(path+name)不同
             */
            if (i == newPrograms.size() - 1 || !(model.getPath() + model.getName()).equals(newPrograms.get(i + 1).getPath() + newPrograms.get(i + 1).getName())) {
                row = sheet.createRow(count);
                array = new String[]{String.format("%d.", count++), model.getPath(), model.getName(), model.getStatus(), ids};
                addRow(row, array);
                ids = "";
            }
        }

        sheet = workbook.createSheet("交版程式清單");
        row = sheet.createRow((short) 0);
        array = new String[]{"序號", "目錄", "程式名稱", "異動", "項目編號"};
        addRow(row, array);
        List<ProgramModel> list = createSheetByProd(newPrograms, prodPath);
        list.sort(Comparator.comparing(ProgramModel::getPath).thenComparing(ProgramModel::getName).thenComparing(ProgramModel::getId).thenComparing(ProgramModel::getStatus));
        count = 1;
        for (int i = 0; i < list.size(); i++) {
            ProgramModel model = list.get(i);
            if ("".equals(ids)) {
                ids = String.valueOf(model.getId());
            } else if (!ids.contains(String.valueOf(model.getId()))) {
                ids = String.format("%s\r\n%d", ids, model.getId());
            }
            /* 寫入execl
             * 1. 最後一筆
             * 2. 跟下一筆的檔案(path+name)不同
             */
            if (i == list.size() - 1 || !(model.getPath() + model.getName()).equals(list.get(i + 1).getPath() + list.get(i + 1).getName())) {
                row = sheet.createRow(count);
                array = new String[]{String.valueOf(count++) + ".", model.getPath(), model.getName(), model.getStatus(), ids};
                addRow(row, array);
                ids = "";
            }
        }

        String toFileName = toPath + "上線申請書.xlsx";
        _FileUtility.CreateDir(toFileName);
        FileOutputStream fileOut = new FileOutputStream(toFileName);
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
        return String.format("execl 已產生: %s", toFileName);
    }

    /**
     * 交版程式清單
     *
     * @param newPrograms
     * @param prodPath
     * @return
     */
    private List<ProgramModel> createSheetByProd(List<ProgramModel> newPrograms, String prodPath) {
        List<ProgramModel> list = new ArrayList<>();

        for (ProgramModel model : newPrograms) {

            // 根目錄直接 copy 的目錄
            String[] names = new String[]{"Content\\", "DemoPage\\", "Scripts\\", "Views\\"};
            for (String name : names) {
                if (model.getPath().contains(name)) {
                    ProgramModel newModel = createPrograms(model);
                    newModel.setPath(model.getPath().substring(model.getPath().indexOf("\\" + name)));
                    list.add(newModel);
                }
            }

            // 根目錄檔案
            names = new String[]{"packages.config", "Web.config"};
            for (String name : names) {
                if (model.getName().contains(name)) {
                    ProgramModel newModel = createPrograms(model);
                    newModel.setPath("\\");
                    list.add(newModel);
                }
            }

            // DLL
            if (model.getPath().contains("DLL\\")) {
                ProgramModel newModel = createPrograms(model);
                newModel.setPath("\\bin\\");
                list.add(newModel);
            }

            // Global
            if (model.getPath().contains("Pershing.EBot.Models.Global")) {
                ProgramModel newModel = createPrograms(model);
                newModel.setPath("\\bin\\");
                newModel.setName("Pershing.EBot.Models.Global.dll");
                list.add(newModel);
                newModel = createPrograms(model);
                newModel.setPath("\\bin\\en-US\\");
                newModel.setName("Pershing.EBot.Models.Global.resources.dll");
                list.add(newModel);
                newModel = createPrograms(model);
                newModel.setPath("\\bin\\zh-CN\\");
                newModel.setName("Pershing.EBot.Models.Global.resources.dll");
                list.add(newModel);
            }

            // 平台 DLL
            List<String> nameList = Arrays.asList("Pershing.EBot.Utility.csproj", "Pershing.EBot.Models.Communication.csproj", "Pershing.EBot.Models.Global.csproj", "Pershing.EBot.Project.csproj");
            if (nameList.contains(model.getName())) {
                ProgramModel newModel = createPrograms(model);
                newModel.setPath("\\bin\\");
                newModel.setName(model.getName().replace(".csproj", ".dll"));
                list.add(newModel);
            }

            // 平台 Controller
            nameList = Arrays.asList("CommonController.cs", "ErrorController.cs", "HomeController.cs", "HomeController_Secretary.cs");
            if (nameList.contains(model.getName())) {
                ProgramModel newModel = createPrograms(model);
                newModel.setPath("\\bin\\");
                newModel.setName("Pershing.EBot.Project.dll");
                list.add(newModel);
            }

            if (model.getName().contains("Controller")) {
                ProgramModel newModel = createPrograms(model);
                newModel.setPath("\\bin\\");
                newModel.setName(model.getName().substring(0, 6) + ".dll");
                if (_FileUtility.isLivebyFile(prodPath + "bin\\" + newModel.getName())) {
                    list.add(newModel);
                }
            }

            // 交易 View
            if (model.getPath().contains("Views\\")) {
                List<FileVo> voList = _FileUtility.getFileList(prodPath + "bin\\", false, Collections.singletonList(model.getName().toLowerCase()), Collections.singletonList(".dll"));
                ProgramModel newModel = createPrograms(model);
                newModel.setPath("\\bin\\");
                newModel.setName("EBOT.dll");
                list.add(newModel);
                voList.forEach(vo -> {
                    ProgramModel newModel2 = createPrograms(model);
                    newModel2.setPath("\\bin\\");
                    newModel2.setName(vo.getName());
                    list.add(newModel2);

                });
            }
        }

        return list;
    }

    /**
     * new ProgramModel & copy data
     *
     * @param model
     * @return
     */
    private ProgramModel createPrograms(ProgramModel model) {
        ProgramModel newModel = new ProgramModel();
        newModel.setPath(model.getPath());
        newModel.setName(model.getName());
        newModel.setStatus(model.getStatus());
        newModel.setId(model.getId());
        newModel.setErrorMessage(model.getErrorMessage());
        return newModel;
    }

    /**
     * 建立 error Log
     *
     * @param fileList
     * @param toPath
     * @throws IOException
     */
    private String CreateErrorMessage(List<ReasonForChangeModel> fileList, String toPath) throws IOException {
        String logFile = toPath + "log.txt";
        StringBuilder sb = new StringBuilder();
        List<ProgramModel> list = new LinkedList<>();
        fileList.forEach(file -> list.addAll(file.getPrograms().stream().filter(p -> ERROR.equals(p.getStatus())).collect(Collectors.toList())));
        list.forEach(CommonUtility::null2Empty);
        list.sort(Comparator.comparing(ProgramModel::getErrorMessage).thenComparing(ProgramModel::getId).thenComparing(ProgramModel::getPath).thenComparing(ProgramModel::getName));
        String message = "";
        for (ProgramModel model : list) {
            if (!message.equals(model.getErrorMessage())) {
                message = model.getErrorMessage();
                sb.append("\r\n").append(String.format("## %s", message)).append("\r\n");
            }
            sb.append(String.format("%d. %s%s", model.getId(), StringUtils.trimToEmpty(model.getPath()), model.getName())).append("\r\n");
        }
        sb.append("\r\n- - -\r\n");

        sb.append("\r\n## 換版清單序號\r\n");
        List<ReasonForChangeModel> newfileList = new LinkedList<>();
        newfileList.addAll(fileList);
        newfileList.sort(Comparator.comparing(ReasonForChangeModel::getId));
        newfileList.forEach(file -> sb.append(file.getId()).append("\r\n"));
        sb.append("\r\n## 線上問題單號\r\n");
        newfileList = new LinkedList<>();
        newfileList.addAll(fileList);
        newfileList.removeIf(file -> StringUtils.isBlank(file.getOnlineNumber()));
        newfileList.sort(Comparator.comparing(ReasonForChangeModel::getOnlineNumber).thenComparing(ReasonForChangeModel::getId));
        newfileList.forEach(file -> {
            String[] numbers = file.getOnlineNumber().split("\n");
            Arrays.stream(numbers).forEach(number -> sb.append(number).append("\r\n"));
        });
        sb.append("\r\n## UAT單號\r\n");
        newfileList = new LinkedList<>();
        newfileList.addAll(fileList);
        newfileList.removeIf(file -> StringUtils.isBlank(file.getUatNumber()));
        newfileList.sort(Comparator.comparing(ReasonForChangeModel::getUatNumber).thenComparing(ReasonForChangeModel::getId));
        newfileList.forEach(file -> {
            String[] numbers = file.getUatNumber().split("\n");
            Arrays.stream(numbers).forEach(number -> sb.append(number).append("\r\n"));
        });

        _FileUtility.write(logFile, sb.toString(), false);
        return String.format("log 已產生: %s", logFile);
    }

}
