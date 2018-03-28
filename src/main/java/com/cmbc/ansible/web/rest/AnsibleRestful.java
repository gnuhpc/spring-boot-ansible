package com.cmbc.ansible.web.rest;


import com.cmbc.ansible.entity.Configuration;
import com.cmbc.ansible.util.JschToAnsible;
import com.cmbc.ansible.util.JschToFTPUtil;
import com.cmbc.ansible.util.OperaFileUtil;
import com.cmbc.ansible.util.ParsingResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


/**
 * Created by Administrator on 2017/3/8.
 */

@RestController
@RequestMapping("/v1/ansible")
@Api(value = "调用ansible执行任务API", description = "调用ansible执行任务API")
public class AnsibleRestful {
    private static Logger logger = Logger.getLogger(AnsibleRestful.class);
    @Autowired
    private Configuration configuration;

    @ApiOperation(value = "使用ansible执行脚本", notes = "解析用户传递的json参数组成执行命令")
    @ApiImplicitParam(name = "json", value = "脚本信息json字符串", required = true, dataType = "String")
    @RequestMapping(value = "/execPython", method = RequestMethod.POST)
    public String execPython(@RequestBody String json) {
        JSONObject jobList = JSONObject.fromObject(json);
        Integer jschTimeout = jobList.getInt("jschTimeout");
        String serverUser = configuration.getServerUser();
        String serverAddress = configuration.getServerAddress();
        String id_rsa = configuration.getId_rsa();
        String executeCommand = jobList.getString("executeCommand");
        JSONArray jsonArray = JSONArray.fromObject(jobList.getString("server"));
        JSONArray jsonArray1 = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        JSONArray iparray = new JSONArray();
        JSONObject jsonObject1 = new JSONObject();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jobInfiList = jsonArray.getJSONObject(i);
            String ip = jobInfiList.getString("app_ip");
            Integer id = jobInfiList.getInt("id");
            jsonObject1.put("ip", ip);
            jsonObject1.put("id", id);
            iparray.add(jsonObject1);
        }
        JschToAnsible jschToAnsible = new JschToAnsible();
        String results = jschToAnsible.execPlaybooks(executeCommand, serverAddress, serverUser, jschTimeout, id_rsa);
        JSONObject resultJson = JSONObject.fromObject(results);
        if (resultJson.containsKey("error")) {
            jsonObject.put("error", resultJson.getString("error"));
        } else {
            jsonObject.put("result", resultJson.getString("right"));
        }
        jsonObject.put("ip", iparray);
        jsonArray1.add(jsonObject);
        logger.info("执行linux命令成功");
        logger.info("获取配置文件参数：");
        return jsonArray1.toString();
    }

    @ApiOperation(value = "使用ansible copy模块分发文件", notes = "解析用户传递的json参数组成执行命令")
    @ApiImplicitParam(name = "json", value = "脚本信息json字符串", required = true, dataType = "String")
    @RequestMapping(value = "/sendFile", method = RequestMethod.POST)
    public String sendFile(@RequestBody String json) {
        JSONObject jsonList = JSONObject.fromObject(json);
        String jschTimeout = configuration.getJschTimeout();
        Integer timeOut = Integer.valueOf(jschTimeout);
        String serverUser = configuration.getServerUser();
        String serverAddress = configuration.getServerAddress();
        String id_rsa = configuration.getId_rsa();
        String executeCommand = jsonList.getString("executeCommand");
        JSONArray jsonArray = JSONArray.fromObject(jsonList.getString("job_info_ids"));
        JSONObject jsonObject1 = new JSONObject();
        JSONObject jsonOb = new JSONObject();
        JSONArray iparray = new JSONArray();
        JSONArray jsonArray1 = new JSONArray();
        JschToAnsible jschToAnsible = new JschToAnsible();
        String results = jschToAnsible.execPlaybooks(executeCommand, serverAddress, serverUser, timeOut, id_rsa);
        JSONObject resultJson = JSONObject.fromObject(results);
        jsonObject1.put("job_info_ids", jsonArray);
        iparray.add(jsonObject1);
        if (resultJson.containsKey("error")) {
            jsonOb.put("error", resultJson.getString("error"));
        } else {
            jsonOb.put("result", resultJson.getString("right"));
        }
        jsonOb.put("ip", iparray);
        jsonArray1.add(jsonOb);
        return jsonArray1.toString();
    }

    @ApiOperation(value = "使用ansible执行任务后处理脚本", notes = "解析用户传递的json参数组成执行命令")
    @ApiImplicitParam(name = "json", value = "脚本信息json字符串", required = true, dataType = "String")
    @RequestMapping(value = "/processResult", method = RequestMethod.POST)
    public String processResult(@RequestBody String json) {
        JSONObject jobList = JSONObject.fromObject(json);
        String jschTimeout = configuration.getJschTimeout();
        Integer timeOut = Integer.valueOf(jschTimeout);
        String serverUser = configuration.getServerUser();
        String serverAddress = configuration.getServerAddress();
        String id_rsa = configuration.getId_rsa();
        String executeCommand = jobList.getString("executeCommand");
        Integer processId = jobList.getInt("processId");
        JSONArray jsonArray1 = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        JschToAnsible jschToAnsible = new JschToAnsible();
        String results = jschToAnsible.execPlaybooks(executeCommand, serverAddress, serverUser, timeOut, id_rsa);
        JSONObject resultJson = JSONObject.fromObject(results);
        if (resultJson.containsKey("error")) {
            jsonObject.put("error", resultJson.getString("error"));
        } else {
            jsonObject.put("result", resultJson.getString("right"));
        }
        jsonObject.put("processId", processId);
        jsonArray1.add(jsonObject);
        logger.info("执行linux命令成功");
        return jsonArray1.toString();
    }


    @ApiOperation(value = "文件上传公共接口", notes = "解析用户传递的json参数上传文件")
    @ApiImplicitParam(name = "json", value = "上传文件参数json字符串", required = true, dataType = "String")
    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    public String uploadFile(@RequestBody String json) {
        JSONObject jsonList = JSONObject.fromObject(json);
        String user = jsonList.getString("user");
        String host = jsonList.getString("host");
        boolean coverFlag = jsonList.getBoolean("coverFileFlag");
        boolean makeDirFlag = jsonList.getBoolean("makeDirFlag");
        Integer port = 22;
        Integer timeout = jsonList.getInt("timeout");
        String remoteFilePath = jsonList.getString("remoteFilePath");
        String remoteFileName = jsonList.getString("remoteFileName");
        String remoteFile = remoteFilePath + "/" + remoteFileName;
        String fileContent = jsonList.getString("fileContent");
        InputStream new_fileInputStream = new ByteArrayInputStream(fileContent.getBytes());
        JSONObject jsonObject = new JSONObject();
        try {
            Integer fileSize = new_fileInputStream.available();
            if (fileSize > 102400) {

                String upladFailed = "上传失败,文件大小不能超过100KB!";
                jsonObject.put("uploadFailed", upladFailed);
                return jsonObject.toString();
            }
            JschToFTPUtil jschToFTPUtil = new JschToFTPUtil();
            String id_rsa = configuration.getId_rsa();
            JSONObject jsonResult = jschToFTPUtil.uploadFile(user, host, port, timeout, remoteFile, new_fileInputStream, coverFlag, makeDirFlag, id_rsa);
            return jsonResult.toString();
        } catch (IOException e) {
            e.printStackTrace();
            jsonObject.put("uploadFailed", "获取文件大小异常");
            return jsonObject.toString();
        }
    }


    @ApiOperation(value = "文件下载公共接口", notes = "解析用户传递的json参数上传文件")
    @ApiImplicitParam(name = "json", value = "上传文件参数json字符串", required = true, dataType = "String")
    @RequestMapping(value = "/downloadFile", method = RequestMethod.POST)
    public String downloadFile(@RequestBody String json) {
        JSONObject jsonList = JSONObject.fromObject(json);
        String user = jsonList.getString("user");
        String host = jsonList.getString("host");
        Integer port = 22;
        Integer timeout = jsonList.getInt("timeout");
        String remoteFile = jsonList.getString("remoteFile");
        JschToFTPUtil jschToFTPUtil = new JschToFTPUtil();
        String id_rsa = configuration.getId_rsa();
        JSONObject jsonObject = jschToFTPUtil.downloadFile(user, host, port, timeout, remoteFile, id_rsa);
        return jsonObject.toString();
    }


    @ApiOperation(value = "执行命令公共接口", notes = "解析用户传递的json参数上传文件")
    @ApiImplicitParam(name = "json", value = "上传文件参数json字符串", required = true, dataType = "String")
    @RequestMapping(value = "/executeCommand", method = RequestMethod.POST)
    public String executeCommand(@RequestBody String json) {
        JSONObject jsonList = JSONObject.fromObject(json);
        Integer timeOut = jsonList.getInt("timeout");
        String executeType = jsonList.getString("executeType");
        String executeCommand = jsonList.getString("executeCommand");
        String host = jsonList.getString("host");
        String user = jsonList.getString("user");
        String scriptTemplateRemote = configuration.getScriptTemplateRemote();
        String extraCommand = "";
        String titleCommand = "ansible-playbook --inventory-file=" + host + "," + " --user=" + user + " " + scriptTemplateRemote;
        String temporaryFile = configuration.getTemporaryFile();
        String temporaryFileName = "";
        if (executeType.equals("python")) {
            String work_content = "#!/usr/bin/env python\n" + executeCommand;
            OperaFileUtil.creatDir(temporaryFile);
            temporaryFileName = temporaryFile + "/" + new Date().getTime() + ".py";
            OperaFileUtil.creatFile(temporaryFileName, work_content);
            String displayName = temporaryFileName.substring(temporaryFileName.lastIndexOf("/") + 1);
            extraCommand = " --extra-vars " + '"' + "scriptLocalPath='" + temporaryFileName + "'" + " scriptRemotePath='" + "~/" + displayName + "'" + " ExecuteScript=" + "'python ~/" + displayName + "'" + " asyncSecs=" + timeOut + " pollSecs=2" + '"';
        }
        if (executeType.equals("shell")) {
            String work_content = "#!/bin/sh\n" + executeCommand;
            OperaFileUtil.creatDir(temporaryFile);
            temporaryFileName = temporaryFile + "/" + new Date().getTime() + ".sh";
            OperaFileUtil.creatFile(temporaryFileName, work_content);
            String displayName = temporaryFileName.substring(temporaryFileName.lastIndexOf("/") + 1);
            extraCommand = " --extra-vars " + '"' + "scriptLocalPath='" + temporaryFileName + "'" + " scriptRemotePath='" + "~/" + displayName + "'" + " ExecuteScript=" + "'sh ~/" + displayName + "'" + " asyncSecs=" + timeOut + " pollSecs=2" + '"';
        }
        String scriptCommand = titleCommand + extraCommand;
        String serverUser = configuration.getServerUser();
        String serverAddress = configuration.getServerAddress();
        String id_rsa = configuration.getId_rsa();
        JSONObject jsonObject = new JSONObject();
        JschToAnsible jschToAnsible = new JschToAnsible();
        String results = jschToAnsible.execPlaybooks(scriptCommand, serverAddress, serverUser, timeOut, id_rsa);
        JSONObject resultJson = JSONObject.fromObject(results);
        if (resultJson.containsKey("error")) {
            jsonObject.put("error", resultJson.getString("error"));
        } else {
            jsonObject.put("result", resultJson.getString("right"));
        }
        jsonObject.put("ip", host);
        ParsingResultUtil parsingResultUtil = new ParsingResultUtil();
        JSONObject jsonResult = parsingResultUtil.playBookResult(jsonObject.toString());
        OperaFileUtil.deleteFile(temporaryFileName);
        return jsonResult.toString();
    }

    @ApiOperation(value = "配置文件读取", notes = "解析用户传递的json参数")
    @ApiImplicitParam(name = "json", value = "读取配置文件参数json字符串", required = true, dataType = "String")
    @RequestMapping(value = "/readConfigFile", method = RequestMethod.POST)
    public String readConfigFile(@RequestBody String json) {
        JSONObject jsonList = JSONObject.fromObject(json);
        String user = jsonList.getString("user");
        String host = jsonList.getString("host");
        String remoteFile = jsonList.getString("remoteFile");
        String remoteFileType = jsonList.getString("remoteFileType");
        String id_rsa = configuration.getId_rsa();
        Integer port = 22;
        Integer timeout = jsonList.getInt("timeout");
        JschToFTPUtil jschToFTPUtil = new JschToFTPUtil();
//        Map<String, String> map = new HashMap<String, String>();
        String result = "";
        if (remoteFileType.equals("prop")) {
            result = jschToFTPUtil.readConfigFile(user, host, port, timeout, remoteFile, id_rsa);
        }
        return result;
    }

    @ApiOperation(value = "更新配置文件", notes = "解析用户传递的json参数")
    @ApiImplicitParam(name = "json", value = "读取配置文件参数json字符串", required = true, dataType = "String")
    @RequestMapping(value = "/updateConfigFile", method = RequestMethod.POST)
    public String writeConfigFile(@RequestBody String json) {
        JSONObject jsonList = JSONObject.fromObject(json);
        String user = jsonList.getString("user");
        String host = jsonList.getString("host");
        String remoteFile = jsonList.getString("remoteFile");
        String remoteFileType = jsonList.getString("remoteFileType");
        JSONArray configContentArray = jsonList.getJSONArray("configContent");
        JSONArray deleteKeyArray = jsonList.getJSONArray("deleteKey");
        Integer port = 22;
        Integer count = 0;
        Integer countErr = 0;
        Integer timeout = jsonList.getInt("timeout");
        JschToFTPUtil jschToFTPUtil = new JschToFTPUtil();
        String id_rsa = configuration.getId_rsa();
        if (remoteFileType.equals("prop")) {
            String temporaryFile = configuration.getTemporaryFile();
            String srcFile = temporaryFile + "/" + new Date().getTime() + ".properties";
            JSONObject jsonObject = jschToFTPUtil.updataConfigFile(user, host, port, timeout, remoteFile, srcFile, configContentArray, deleteKeyArray, id_rsa);
            if (jsonObject.containsKey("writeFailed")) {
                countErr++;
            }
            if (jsonObject.containsKey("writeSuccessful")) {
                count++;
            }
        }
        JSONObject new_json = new JSONObject();
        if (countErr == 0 && count != 0) {
            new_json.put("result", "success");
        } else {
            new_json.put("result", "failed");
        }
        return new_json.toString();
    }


    @ApiOperation(value = "用于连接测试", notes = "测试连接是否通畅，避免遗忘启动spring boot")
    @RequestMapping(value = "/connectTest", method = RequestMethod.POST)
    public String connectTest() {
        return "success";
    }
}
