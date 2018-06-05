/*
 * @(#) VideoCapture.java 2017年5月24日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.processor;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.mina.core.buffer.IoBuffer;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.red5.codec.VideoCodec;
import org.red5.server.api.event.IEvent;
import org.red5.server.net.rtmp.RTMPType;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.FrameType;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zzhao.jcodec.AWTUtil;

/**
 * ref docs:
 * https://wuyuans.com/2012/08/flv-format/
 * http://www.rosoo.net/a/201405/16980.html
 * @author zzhao
 * @version 2017年5月24日
 */
public class VideoCapture implements StreamProcessHandler {

    private static Logger log = LoggerFactory.getLogger(VideoSaver.class);

    private static int AVC_PAYLOAD_OFFSET = 5;
    
    private static String folder = "e:/videoCapture";

    private int start;
    private int length;

    private H264Decoder decoder;

    private boolean decodeInit = false;

    public VideoCapture(int start, int length) {
        this.start = start;
        this.length = length;
    }

    public void dispatchEvent(IEvent event) {
        try {
            RTMPMessage msg = RTMPMessage.build((IRTMPEvent) event);
            if (msg.getBody().getDataType() == RTMPType.TYPE_VIDEO_DATA.getType()) {
                VideoData video = (VideoData) msg.getBody();
                // current only support avc
                if (video.getFrameType() == FrameType.KEYFRAME && video.getCodecId() == VideoCodec.AVC.getId()) {
                    if (video.getTimestamp() == 0) {
                        // init decoder with pps and sps
                        initDecoder(video);
                    } else {
                        capture(video);
                    }
                }
            }
        } catch (Throwable e) {
            log.error("dispathc event failed!", e);
        }
    }

    private void initDecoder(VideoData video) {
    	
    	File dir = new File(folder);
    	if(!dir.exists()){
    		dir.mkdir();
    	}
    	
        IoBuffer data = video.getData().asReadOnlyBuffer();
        int bodySIze = data.limit();
        byte avcType = data.get(1);
        if (avcType == 0x00) {
            byte[] tmp = new byte[bodySIze - AVC_PAYLOAD_OFFSET];
            data.position(AVC_PAYLOAD_OFFSET); // move video tag header and avc NALU
            data.get(tmp, 0, bodySIze - AVC_PAYLOAD_OFFSET);

            int offset = 6; // sps length start
            int spsLength = (int) (0xff00 & tmp[offset]) + (int) (0xff & tmp[offset + 1]);
            offset += 2;
            byte[] spsData = new byte[spsLength];
            System.arraycopy(tmp, offset, spsData, 0, spsLength);
            offset += spsLength;
            List<ByteBuffer> tmpList = new ArrayList<ByteBuffer>();
            ByteBuffer spsBuffer = ByteBuffer.wrap(spsData);

            spsBuffer.get(); // remove naul header
            tmpList.add(spsBuffer);
            decoder.addSps(tmpList);

            offset += 1; // skip pps number
            int ppsLength = (int) (0xff00 & tmp[offset]) + (int) (0xff & tmp[offset + 1]);
            offset += 2;
            byte[] ppsData = new byte[ppsLength];
            System.arraycopy(tmp, offset, ppsData, 0, ppsLength);
            tmpList.clear();
            ByteBuffer ppsBuffer = ByteBuffer.wrap(ppsData);
            ppsBuffer.get(); // remove naul header
            tmpList.add(ppsBuffer);
            decoder.addPps(tmpList);
        }
        decodeInit = true;
    }

    private void capture(VideoData video) throws IOException {
        if(decodeInit){
            IoBuffer data = video.getData().asReadOnlyBuffer();
            int bodySIze = data.limit();
            byte avcType = data.get(1);
            if (avcType == 0x01) {
                byte[] tmp = new byte[bodySIze - AVC_PAYLOAD_OFFSET];
                data.position(AVC_PAYLOAD_OFFSET); // move video tag header and avc NALU
                data.get(tmp, 0, bodySIze - AVC_PAYLOAD_OFFSET);
                ByteBuffer avcPayload = ByteBuffer.wrap(tmp);
                avcPayload.position(0);

                byte[][] picData = Picture.create(480, 288, ColorSpace.YUV420J).getData();
                List<ByteBuffer> nalUnits = extractNALUnits(avcPayload);

                Picture pic = decoder.decodeFrameFromNals(nalUnits, picData);
                ImageIO.write(AWTUtil.toBufferedImage(pic), "jpg", new File(folder + "/capture_" + video.getTimestamp() + ".jpeg"));
            }
        }
    }

    public void init() {
        decoder = new H264Decoder();
    }

    public void close() {
        // TODO Auto-generated method stub
    }

    private List<ByteBuffer> extractNALUnits(ByteBuffer buf) {
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
