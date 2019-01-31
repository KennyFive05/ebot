package Versioning.Model;

import lombok.Getter;
import lombok.Setter;

public class ProgramModel {

    /**
     * 路徑
     */
    @Getter
    @Setter
    private String path;

    /**
     * 名稱
     */
    @Getter
    @Setter
    private String name;

    /**
     * 異動
     */
    @Getter
    @Setter
    private String status;

    /**
     * 項目編號
     */
    @Getter
    @Setter
    private int Id;

    /**
     * 錯誤訊息
     */
    @Getter
    @Setter
    private String ErrorMessage;

    @Override
    public String toString() {
        return "ProgramModel{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", Id=" + Id +
                ", ErrorMessage='" + ErrorMessage + '\'' +
                '}';
    }
}
