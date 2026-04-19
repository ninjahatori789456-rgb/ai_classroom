package com.remoteclassroom.backend.dto;

public class BatchDTO {
    private Long id;
    private String name;
    private String subject;
    private String batchCode;
    private String teacherName;

    public BatchDTO() {}

    public BatchDTO(Long id, String name, String subject, String batchCode, String teacherName) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.batchCode = batchCode;
        this.teacherName = teacherName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
}
