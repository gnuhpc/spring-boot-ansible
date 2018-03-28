package com.cmbc.ansible.util;


import com.jcraft.jsch.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * Created by rtdl-liruidong on 2017/2/21.
 */
public class JschToAnsible {
    private static Logger logger = Logger.getLogger(JschToAnsible.class);

    /**
     * 执行ansible命令(私钥方式)
     *
     * @param command
     * @param host
     * @param user
     * @param timeout
     * @param id_rsa
     * @return
     */
    public String execPlaybooks(String command, String host, String user, Integer timeout, String id_rsa) {
        JSONObject js = new JSONObject();
        String keyFile = id_rsa;
        String passphrase = "111111";
        JSch jsch = new JSch();
        Session session = null;
        try {
            jsch.addIdentity(keyFile);
            session = jsch.getSession(user, host, 22);
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("连接主机异常" + ExceptionUtil.print(e));
            js.put("error", ExceptionUtil.print(e));
            return js.toString();
        }
        UserInfo ui = new MyUserInfo(passphrase);
        session.setUserInfo(ui);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("userauth.gssapi-with-mic", "no");
        Integer maxInt = Integer.MAX_VALUE;
        config.put("max_input_buffer_size", maxInt.toString());
        session.setConfig(config);
        try {
            session.connect(timeout);
        } catch (JSchException e) {
            e.printStackTrace();
            logger.error("连接linux主机异常" + ExceptionUtil.print(e));
            js.put("error", ExceptionUtil.print(e));
            return js.toString();
        }
        BufferedReader reader = null;
        BufferedReader readerErr = null;
        Channel channel = null;
        String buf = null;
        String error = null;
        StringBuffer buffer = new StringBuffer();
        StringBuffer bufferErr = new StringBuffer();
        try {
            if (command != null) {
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                ((ChannelExec) channel).setErrStream(System.err);
                channel.connect();
                InputStream in = channel.getInputStream();
                InputStream err = ((ChannelExec) channel).getErrStream();
                reader = new BufferedReader(new InputStreamReader(in));
                readerErr = new BufferedReader(new InputStreamReader(err));
                while ((error = readerErr.readLine()) != null) {
                    bufferErr.append(error.trim());
                }
                while ((buf = reader.readLine()) != null) {
                    buffer.append(buf.trim());
                }
            }
        } catch (JSchException e) {
            e.printStackTrace();
            logger.warn("执行linux命令异常" + ExceptionUtil.print(e));
            js.put("error", ExceptionUtil.print(e));
            return js.toString();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("执行linux命令出现异常" + ExceptionUtil.print(e));
            js.put("error", ExceptionUtil.print(e));
            return js.toString();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("异常关闭流" + ExceptionUtil.print(e));
                js.put("error", ExceptionUtil.print(e));
                return js.toString();
            }
            channel.disconnect();
            session.disconnect();
        }
        JSONObject jsonObject = new JSONObject();
        if (bufferErr != null && !"".equals(bufferErr.toString())) {
            jsonObject.put("error", bufferErr.toString());
        } else {
            jsonObject.put("right", buffer.toString());
        }
        return jsonObject.toString();
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
        }
    }


}
