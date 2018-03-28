package com.cmbc.ansible.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by rtdl-liruidong on 2017/3/21.
 */
public class OperaFileUtil {
    private static Logger logger = Logger.getLogger(OperaFileUtil.class);

    /**
     * 读取文件内容
     *
     * @param filePath
     * @return
     */
    public static String readTxtFile(String filePath) {
        File file = new File(filePath);
        JSONArray array = new JSONArray();
        JSONObject json = new JSONObject();
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            BufferedReader bufferedReader = new BufferedReader(reader);
            String lineTxt = null;
            while ((lineTxt = bufferedReader.readLine()) != null) {
                array.add(lineTxt);
            }
            reader.close();
            json.put("result", array);
        } catch (Exception e) {
            e.printStackTrace();
            json.put("result", "error");
            logger.error("读取文件异常：" + ExceptionUtil.print(e));
        }
        return json.toString();
    }

    /**
     * 创建文件夹
     *
     * @param destDirName
     * @return
     */
    public static boolean creatDir(String destDirName) {
        File dir = new File(destDirName);
        Boolean bool = false;
        if (!dir.exists()) {
            dir.mkdirs();
            bool = true;
        }
        return bool;
    }

    /**
     * 创建文件
     *
     * @param fileName
     * @param fileContent
     * @return
     */
    public static boolean creatFile(String fileName, String fileContent) {
        File file = new File(fileName);
        Boolean bool = false;
        try {
            if (!file.exists()) {
                file.createNewFile();
                bool = true;
            }
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(fileContent);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("创建文件异常：" + ExceptionUtil.print(e));
        }
        return bool;
    }

    /**
     * 删除文件
     *
     * @param fileName
     * @return
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            logger.info("删除文件失败：" + fileName + "不存在");
            return false;
        } else {
            if (file.isFile()) {
                file.delete();
                return true;
            }
            return false;
        }
    }


    /**
     * 压缩文件(zip)
     *
     * @param srcfile
     * @return
     */
    public static ZipOutputStream zipFiles(String[] srcfile, String zipName) {
        String fileAddrName = zipName.substring(0, zipName.lastIndexOf("/"));
        creatDir(fileAddrName);
        File zipFile = new File(zipName);
        byte[] buf = new byte[1024];
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            for (int i = 0; i < srcfile.length; i++) {
                File new_file = new File(srcfile[i]);
                FileInputStream inputStream = new FileInputStream(new_file);
                zipOutputStream.putNextEntry(new ZipEntry(new_file.getName()));
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    zipOutputStream.write(buf, 0, len);
                }
                zipOutputStream.closeEntry();
                inputStream.close();
            }
            zipOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipOutputStream;
    }

}
