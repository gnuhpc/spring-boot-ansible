package com.cmbc.ansible.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by rtdl-liruidong on 2017/5/12.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ansibleRestful")
public class Configuration {

    private String serverAddress;

    private String serverUser;

    private String id_rsa;

    private String jschTimeout;

    private String temporaryFile;

    private String scriptTemplateRemote;

}
