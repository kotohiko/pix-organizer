package com.jacob.po.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 *
 * @author Kotohiko
 * @version 1.0
 * @apiNote
 * @since Nov 30, 2025, 5:34 PM
 **/
@Data
@Setter
@Getter
public class SysModelEntity {

    private Integer id;

    private String idSrc;

    private String name;

    private String alias;

    private String type;

    private String country;

    private String gender;

    private Integer heightCm;

    private String bilibiliUrl;

    private String twitterUrl;

    private String twitterUrlUid;

    private String weiboUrl;

    private String xhsUrl;

    private String douyinUrl;

    private String url6;

    private String url7;

    private String url8;

    private String url9;

    private String qqNum;

    private String remark;

    private Boolean deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
