package com.cmbc.ansible.util;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;


import java.util.List;

/**
 * Created by rtdl-liruidong on 2017/4/1.
 */

public class ParsingResultUtil {

    private static Logger logger = Logger.getLogger(ParsingResultUtil.class);


    /**
     * 解析ansible-playbook返回结果
     *
     * @param result
     */

    public JSONObject playBookResult(String result) {
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("stderr", "");
        jsonResult.put("stdout", "");
        try {
            JSONObject resultList = JSONObject.fromObject(result);
            if (resultList.containsKey("result")) {
                JSONObject jsonObj = JSONObject.fromObject(resultList.getString("result"));
                JSONArray jsonPlayBook = JSONArray.fromObject(jsonObj.getString("plays"));
                String ip = resultList.getString("ip");
                for (int i = 0; i < jsonPlayBook.size(); i++) {
                    JSONObject playBook = jsonPlayBook.getJSONObject(i);
                    JSONArray tasks = JSONArray.fromObject(playBook.getString("tasks"));
                    for (int c = 0; c < tasks.size(); c++) {
                        JSONObject playResult = tasks.getJSONObject(c);
                        JSONObject hosts = JSONObject.fromObject(playResult.getString("hosts"));
                        if (hosts.containsKey(ip)) {
                            JSONObject resultObject = JSONObject.fromObject(hosts.getString(ip));
                            if (resultObject.containsKey("stderr")) {
                                jsonResult.put("stderr", resultObject.getString("stderr"));
                            }
                            if (resultObject.containsKey("stdout")) {
                                jsonResult.put("stdout", resultObject.getString("stdout"));
                            }
                        } else {
                            jsonResult.put("stderr", "没有采集到结果");
                        }
                    }
                }
            }else{
                jsonResult.put("stderr", "没有采集到结果");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("解析结果错误，请联系管理员：" + result);
            jsonResult.put("stderr", "解析结果错误，请联系管理员：" + result);
        }
        return jsonResult;
    }

}
