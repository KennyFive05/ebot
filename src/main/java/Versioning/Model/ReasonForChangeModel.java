package Versioning.Model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ReasonForChangeModel {
    /**
     * 序號
     */
    @Getter
    @Setter
    private int id;

    /**
     * 通報單號/問題單號
     */
    @Getter
    @Setter
    private String number;

    /**
     * 換版原因
     */
    @Getter
    @Setter
    private String reason;

    /**
     * 線上問題單號
     */
    @Getter
    @Setter
    private String onlineNumber;

    /**
     * UAT 問題單
     */
    @Getter
    @Setter
    private String uatNumber;

    /**
     * 程式清單(String)
     */
    @Getter
    @Setter
    private String sPrograms;

    /**
     * 程式清單(List)
     */
    @Getter
    @Setter
    private List<ProgramModel> programs;

    @Override
    public String toString() {
        return "ReasonForChangeModel{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", reason='" + reason + '\'' +
                ", sPrograms='" + sPrograms + '\'' +
                '}';
    }
}
