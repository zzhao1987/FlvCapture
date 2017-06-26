/*
 * @(#) FLVWriter3.java 2017年5月24日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.processor;

import java.io.FileOutputStream;
import java.io.IOException;

import org.red5.io.ITag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zzhao
 * @version 2017年5月24日
 */
public class FLVWriter3 {

    private static Logger log = LoggerFactory.getLogger(FLVWriter3.class);

    private FileOutputStream fos;

    public FLVWriter3(String file) throws IOException {
        fos = new FileOutputStream(file);
        fos.write(FLVUtils.getHeaderByte(true, true));
    }

    public void writeTag(ITag tag) {
        try{
            fos.write(FLVUtils.getTagByte(tag));
        }catch(Throwable e){
            log.error("failed to write tag", e);
        }
    }

    public void close() {
        try {
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
