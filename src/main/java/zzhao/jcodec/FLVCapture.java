/*
 * @(#) FLVCapture.java 2017年5月25日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.jcodec;

import static org.jcodec.codecs.h264.H264Utils.unescapeNAL;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.mina.core.buffer.IoBuffer;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVReader;

import com.google.common.collect.Lists;

/**
 *
 * @author zzhao
 * @version 2017年5月25日
 */
public class FLVCapture {

    private static int AVC_PAYLOAD_OFFSET = 5;
    private static int pWidth = 2048;
    private static int pHeight = 2048;

    public static void main(String[] args) {
        try {
            FLVReader reader = new FLVReader(new File("e:/test2.flv"));

            
            H264Decoder decoder = new H264Decoder();
            int count = 0;
            while (reader.hasMoreTags()) {
                ITag tag = reader.readTag();
                if (tag.getDataType() == 0x09) {
                    IoBuffer data = tag.getBody();
                    int tagMata = (int) data.array()[0];
                    // keyframe and avc formate
                    if (((tagMata & 0xf0) == 0x10) && ((tagMata & 0x0f) == 0x07)) {
                        byte avcType = data.array()[1];
                        byte[] tmp = new byte[tag.getBodySize() - AVC_PAYLOAD_OFFSET];
                        data.position(AVC_PAYLOAD_OFFSET); // move video tag header and avc NALU
                        data.get(tmp, 0, tag.getBodySize() - AVC_PAYLOAD_OFFSET);

                        if (avcType == 0x00) {
                            ByteBuffer buffer = ByteBuffer.wrap(tmp);
                            initDecoder(buffer, decoder);
                            
                        } else if (avcType == 0x01) {
                            // for avc frames
                            ByteBuffer avcPayload = ByteBuffer.wrap(tmp);
                            avcPayload.position(0);
                            byte[][] picData = Picture.create(pWidth, pHeight, ColorSpace.YUV420J).getData();
                            List<ByteBuffer> nalUnits = extractNALUnits(avcPayload);
                            Picture pic = decoder.decodeFrameFromNals(nalUnits, picData);
                            ImageIO.write(AWTUtil.toBufferedImage(pic), "jpg", new File("e:/test_" + count + ".jpeg"));
                            count++;
                        }
                    }
                }
            }
            reader.close();

        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private static List<ByteBuffer> extractNALUnits(ByteBuffer buf) {
        buf = buf.duplicate();
        List<ByteBuffer> nalUnits = new LinkedList<>();

        while (buf.remaining() > 4) {
            int length = buf.getInt();
            byte[] tmp = new byte[length];
            buf.get(tmp);
            nalUnits.add(ByteBuffer.wrap(tmp));
        }

        return nalUnits;
    }
    
    private static boolean initDecoder(ByteBuffer buffer, H264Decoder decoder){
        // skip version and profile-level-id
        buffer.getInt();
        // skip ff
        buffer.get();
        byte tmp =buffer.get();
        int spsNumber =  tmp & 0x1f;
        List<ByteBuffer> spsList = Lists.newLinkedList();
        for(int i = 0; i < spsNumber; i++){
            int spsLen = buffer.getShort();
            byte[] spsData = new byte[spsLen - 1];
            buffer.get();  // skip naul header
            buffer.get(spsData);
            spsList.add(ByteBuffer.wrap(spsData));
            
            ByteBuffer clone = ByteBuffer.wrap(spsData);
            unescapeNAL(clone);
            SeqParameterSet sps = SeqParameterSet.read(clone);
            pWidth = sps.picWidthInMbsMinus1 + 1 << 4;
            pHeight = SeqParameterSet.getPicHeightInMbs(sps) << 4;
          
        }
 
        int ppsNumber = buffer.get();
        List<ByteBuffer> ppsList = Lists.newLinkedList();
        for(int i = 0; i < ppsNumber; i++){
            int ppsLen = buffer.getShort();
            byte[] ppsData = new byte[ppsLen - 1];
            buffer.get();  // skip naul header
            buffer.get(ppsData);
            ppsList.add(ByteBuffer.wrap(ppsData));
        }
        
        decoder.addSps(spsList);
        decoder.addPps(ppsList);

        return  true;
    }

}
