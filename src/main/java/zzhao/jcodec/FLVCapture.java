/*
 * @(#) FLVCapture.java 2017年5月25日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.jcodec;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.mina.core.buffer.IoBuffer;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVReader;

/**
 *
 * @author zzhao
 * @version 2017年5月25日
 */
public class FLVCapture {

    private static int AVC_PAYLOAD_OFFSET = 5;
    

    public static void main(String[] args) {
        try {
            FLVReader reader = new FLVReader(new File("e:/test1.flv"));

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
                            int offset = 6; // sps length start
                            int spsLength = (int) (0xff00 & tmp[offset]) + (int) (0xff & tmp[offset + 1]);
                            offset += 2;
                            byte[] spsData = new byte[spsLength];
                            System.arraycopy(tmp, offset, spsData, 0, spsLength);
                            offset += spsLength;
                            List<ByteBuffer> tmpList = new ArrayList<ByteBuffer>();
                            ByteBuffer spsBuffer = ByteBuffer.wrap(spsData);
                            spsBuffer.get();// skip naul header
                            tmpList.add(spsBuffer);
                            decoder.addSps(tmpList);

                            offset += 1; // skip pps number
                            int ppsLength = (int) (0xff00 & tmp[offset]) + (int) (0xff & tmp[offset + 1]);
                            offset += 2;
                            byte[] ppsData = new byte[ppsLength];
                            System.arraycopy(tmp, offset, ppsData, 0, ppsLength);
                            tmpList.clear();
                            ByteBuffer ppsBuffer = ByteBuffer.wrap(ppsData);
                            ppsBuffer.get(); // skip naul header
                            tmpList.add(ppsBuffer);
                            decoder.addPps(tmpList);
                            
                        } else if (avcType == 0x01) {
                            // for avc frames
                            ByteBuffer avcPayload = ByteBuffer.wrap(tmp);
                            avcPayload.position(0);
                            byte[][] picData = Picture.create(480, 288, ColorSpace.YUV420J).getData();
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
        List<ByteBuffer> nalUnits = new ArrayList<ByteBuffer>();

        while (buf.remaining() > 4) {
            int length = buf.getInt();
            ByteBuffer nalUnit = ByteBuffer.allocate(length);
            for (int i = 0; i < length; i++) {
                nalUnit.put(buf.get());
            }
            nalUnit.flip();
            nalUnits.add(nalUnit);
        }

        return nalUnits;
    }

}
