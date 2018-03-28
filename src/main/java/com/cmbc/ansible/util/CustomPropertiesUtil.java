package com.cmbc.ansible.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Created by rtdl-liruidong on 2017/10/16.
 */
public class CustomPropertiesUtil extends Properties {
    private static final long serialVersionUID = 1l;

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        customStore0(new BufferedWriter(new OutputStreamWriter(out, "8859_1")), comments, true);
    }

    private void customStore0(BufferedWriter bw, String comments, boolean escUnicode) throws IOException {
        bw.write("#" + new Date().toString() + " " + comments);
        bw.newLine();
        synchronized (this) {
            for (Enumeration e = keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                String val = (String) get(key);
                bw.write(key + "=" + val);
                bw.newLine();
            }
        }
        bw.flush();
    }
}
