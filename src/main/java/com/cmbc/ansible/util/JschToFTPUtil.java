package com.cmbc.ansible.util;

import com.jcraft.jsch.*;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.*;
import java.util.*;


/**
 * Created by rtdl-liruidong on 2017/8/8.
 */
public class JschToFTPUtil {
    private static Logger logger = Logger.getLogger(JschToFTPUtil.class);


    public JSONObject uploadFile(String username, String host, Integer port, Integer timeout, String remoteFile, InputStream inputStream, boolean coverFlag, boolean makeDirFlag, String id_rsa) {
        JSONObject jsonObject = new JSONObject();
        String passphrase = "111111";
        JSch jsch = new JSch();
        Session session = null;
        try {
            jsch.addIdentity(id_rsa);
            session = jsch.getSession(username, host, port);
        } catch (JSchException e) {
            e.printStackTrace();
        }
        UserInfo ui = new MyUserInfo(passphrase);
        session.setUserInfo(ui);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        try {
            session.connect(timeout);
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("连接linux主机异常" + ExceptionUtil.print(e));
            jsonObject.put("uploadFailed", "连接主机异常");
            return jsonObject;
        }
        Channel channel = null;
        ChannelSftp sftp = null;
        try {
            channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftp = (ChannelSftp) channel;
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("建立sftp甬道异常" + ExceptionUtil.print(e));
            jsonObject.put("uploadFailed", "建立sftp甬道异常");
            return jsonObject;
        }
        String remoteFileName = remoteFile.substring(remoteFile.lastIndexOf("/") + 1);
        String remoteFilePath = remoteFile.substring(0, remoteFile.lastIndexOf("/"));
        if (makeDirFlag) {
            try {
                sftp.cd(remoteFilePath);
            } catch (SftpException e) {
                e.printStackTrace();
                try {
                    String[] folders = remoteFilePath.split("/");
                    for (int i = 0; i < folders.length; i++) {
                        sftp.mkdir(folders[i]);
                        sftp.cd(folders[i]);
                    }
                } catch (SftpException e1) {
                    e1.printStackTrace();
                    logger.error("建立上级目录失败" + ExceptionUtil.print(e));
                    jsonObject.put("uploadFailed", "建立上级目录失败");
                    return jsonObject;
                }
            }
        } else {
            try {
                sftp.cd(remoteFilePath);
            } catch (SftpException e) {
                e.printStackTrace();
                logger.error("没有找到上级目录" + ExceptionUtil.print(e));
                jsonObject.put("uploadFailed", "没有找到上级目录");
            }
            return jsonObject;
        }
        boolean flag = true;
        if (coverFlag) {
            try {
                sftp.put(inputStream, remoteFileName);
                inputStream.close();
            } catch (SftpException e) {
                e.printStackTrace();
                logger.error("上传文件异常" + ExceptionUtil.print(e));
                jsonObject.put("uploadFailed", "上传文件失败");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                session.disconnect();
                channel.disconnect();
            }
        } else {
            String old_remoteFile = "old_" + new Date().getTime() + "_" + remoteFileName;
            try {
                sftp.rename(remoteFileName, old_remoteFile);
            } catch (SftpException e) {
                e.printStackTrace();
                try {
                    sftp.put(inputStream, remoteFileName);
                    inputStream.close();
                    sftp.quit();
                    jsonObject.put("uploadSuccessful", "上传文件成功");
                    flag = false;
                } catch (SftpException e1) {
                    e1.printStackTrace();
                    logger.error("上传文件异常" + ExceptionUtil.print(e1));
                } catch (IOException e1) {
                    e1.printStackTrace();
                    logger.error("上传文件异常" + ExceptionUtil.print(e1));
                }
            }
            try {
                if (flag) {
                    sftp.put(inputStream, remoteFileName);
                    inputStream.close();
                    sftp.quit();
                    jsonObject.put("uploadSuccessful", "上传文件成功");
                }
            } catch (SftpException e) {
                e.printStackTrace();
                logger.error("上传文件异常" + ExceptionUtil.print(e));
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("上传文件异常" + ExceptionUtil.print(e));
            } finally {
                session.disconnect();
                channel.disconnect();
            }
        }
        return jsonObject;
    }

    /**
     * 文件下载
     *
     * @param username
     * @param host
     * @param port
     * @param timeout
     * @param remoteFile
     * @return
     */
    public JSONObject downloadFile(String username, String host, Integer port, Integer timeout, String remoteFile, String id_rsa) {
        JSONObject jsonObject = new JSONObject();
        String passphrase = "111111";
        JSch jsch = new JSch();
        Session session = null;
        try {
            jsch.addIdentity(id_rsa);
            session = jsch.getSession(username, host, port);
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("连接linux主机异常" + ExceptionUtil.print(e));
        }
        UserInfo ui = new MyUserInfo(passphrase);
        session.setUserInfo(ui);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        try {
            session.connect(timeout);
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("连接linux主机异常" + ExceptionUtil.print(e));
            jsonObject.put("downloadFailed", "连接主机异常");
            return jsonObject;
        }
        Channel channel = null;
        ChannelSftp sftp = null;
        try {
            channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftp = (ChannelSftp) channel;
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("建立sftp甬道异常" + ExceptionUtil.print(e));
            jsonObject.put("downloadFailed", "建立sftp甬道异常");
            return jsonObject;
        }
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = null;
        String buf = null;
        try {
            InputStream inputStream = sftp.get(remoteFile);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((buf = reader.readLine()) != null) {
                buffer.append(buf.trim());
            }
            inputStream.close();
            jsonObject.put("fileContent", buffer.toString());
            sftp.quit();
        } catch (SftpException e) {
            e.printStackTrace();
            logger.error("下载文件异常" + ExceptionUtil.print(e));
            jsonObject.put("downloadFailed", "No such file");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.error("下载文件异常" + ExceptionUtil.print(e));
            jsonObject.put("downloadFailed", "下载文件异常");
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("下载文件异常" + ExceptionUtil.print(e));
            jsonObject.put("downloadFailed", "下载文件异常");
        } finally {
            session.disconnect();
            channel.disconnect();
        }
        return jsonObject;
    }

    /**
     * 读取配置文件
     *
     * @param username
     * @param host
     * @param port
     * @param timeout
     * @param remoteFile
     * @return
     */
    public String readConfigFile(String username, String host, Integer port, Integer timeout, String remoteFile, String id_rsa) {
        Map<String, String> map = new HashMap<String, String>();
        JSONArray resultArray = new JSONArray();
        String passphrase = "111111";
        JSch jsch = new JSch();
        Session session = null;
        try {
            jsch.addIdentity(id_rsa);
            session = jsch.getSession(username, host, port);
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("连接主机异常" + ExceptionUtil.print(e));
        }
        UserInfo ui = new MyUserInfo(passphrase);
        session.setUserInfo(ui);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        try {
            session.connect(timeout);
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("连接linux主机异常" + ExceptionUtil.print(e));
            map.put("Exception", "连接linux主机异常");
        }
        Channel channel = null;
        ChannelSftp sftp = null;
        try {
            channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftp = (ChannelSftp) channel;
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("建立sftp甬道异常" + ExceptionUtil.print(e));
            map.put("Exception", "建立sftp甬道异常");
        }
        try {
            InputStream inputStream = sftp.get(remoteFile);
            Properties properties = new Properties();
            properties.load(inputStream);
            Enumeration en = properties.propertyNames();
            String result = "";
            while (en.hasMoreElements()) {
                String key = (String) en.nextElement();
                String property = properties.getProperty(key);
                result = key + "=" + property;
                resultArray.add(result);
            }
            sftp.quit();
            inputStream.close();
        } catch (SftpException e) {
            e.printStackTrace();
            logger.error("文件读取异常" + ExceptionUtil.print(e));
            map.put("Exception", "No such file");
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("文件转换异常" + ExceptionUtil.print(e));
            map.put("Exception", "文件转换异常");
        } finally {
            session.disconnect();
            channel.disconnect();
        }
        return resultArray.toString();
    }

    /**
     * 修改配置文件
     *
     * @param username
     * @param host
     * @param port
     * @param timeout
     * @param remoteFile
     * @param srcFile
     * @param configContentArray
     * @return
     */
    public JSONObject updataConfigFile(String username, String host, Integer port, Integer timeout, String remoteFile, String srcFile, JSONArray configContentArray, JSONArray deleteKeyArray, String id_rsa) {

        JSONObject jsonObject = new JSONObject();
        String passphrase = "111111";
        JSch jsch = new JSch();
        Session session = null;
        try {
            jsch.addIdentity(id_rsa);
            session = jsch.getSession(username, host, port);
        } catch (JSchException e) {
            e.printStackTrace();
        }
        UserInfo ui = new MyUserInfo(passphrase);
        session.setUserInfo(ui);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        try {
            session.connect(timeout);
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("连接linux主机异常" + ExceptionUtil.print(e));
            jsonObject.put("writeFailed", "连接主机异常");
            return jsonObject;
        }
        Channel channel = null;
        ChannelSftp sftp = null;
        try {
            channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftp = (ChannelSftp) channel;
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("建立sftp甬道异常" + ExceptionUtil.print(e));
            jsonObject.put("writeFailed", "连接主机异常");
            return jsonObject;
        }
        try {
            String srcFilePath = srcFile.substring(0, srcFile.lastIndexOf("/"));
            OperaFileUtil.creatDir(srcFilePath);
            InputStream inputStream = sftp.get(remoteFile);
            String remoteFilePath = remoteFile.substring(0, remoteFile.lastIndexOf("/"));
            String remoteFileName = remoteFile.substring(remoteFile.lastIndexOf("/") + 1);
            String remoteBackup = remoteFilePath + "/" + new Date().getTime() + "_" + remoteFileName;
            OutputStream outputStream1 = new FileOutputStream(srcFile);
            int n;
            while ((n = inputStream.read()) != -1) {
                outputStream1.write(n);
            }
            sftp.put(srcFile, remoteBackup);
            inputStream.close();
            outputStream1.close();
            inputStream.close();
            outputStream1.close();
            CustomPropertiesUtil properties = new CustomPropertiesUtil();
            FileInputStream fis = new FileInputStream(srcFile);
            properties.load(fis);
            fis.close();
            for (int i = 0; i < configContentArray.size(); i++) {
                String kyeAndValue = (String) configContentArray.get(i);
                String[] kyeAndValues = kyeAndValue.split("=");
                String keyName = kyeAndValues[0];
                String keyValue = kyeAndValues[1];
                properties.setProperty(keyName, keyValue);
            }
            for (int i = 0; i < deleteKeyArray.size(); i++) {
                String deleteKey = (String) deleteKeyArray.get(i);
                properties.remove(deleteKey);
            }
            FileOutputStream fos = new FileOutputStream(srcFile);
            properties.store(fos, "");
            fos.close();
            sftp.put(srcFile, remoteFile);
            sftp.quit();
            OperaFileUtil.deleteFile(srcFile);
            jsonObject.put("writeSuccessful", "修改成功");
        } catch (SftpException e) {
            e.printStackTrace();
            logger.error("文件读取异常" + ExceptionUtil.print(e));
            jsonObject.put("writeFailed", "No such file");
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("文件更新异常" + ExceptionUtil.print(e));
            jsonObject.put("writeFailed", "文件更新异常");
        } finally {
            session.disconnect();
            channel.disconnect();
        }
        return jsonObject;
    }


    public static class MyUserInfo implements UserInfo {
        private String passphrase = null;

        public MyUserInfo(String passphrase) {
            this.passphrase = passphrase;
        }

        @Override
        public String getPassphrase() {
            return passphrase;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptPassword(String s) {
            return false;
        }

        @Override
        public boolean promptPassphrase(String s) {
            return false;
        }

        @Override
        public boolean promptYesNo(String s) {
            return false;
        }

        @Override
        public void showMessage(String s) {
            System.out.println(s);
        }
    }

}
