/*
 * @(#) VideoSaver.java 2017年5月23日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.processor;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.flv.impl.Tag;
import org.red5.server.api.event.IEvent;
import org.red5.server.net.rtmp.RTMPType;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.IStreamData;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zzhao
 * @version 2017年5月23日
 */
public class VideoSaver implements StreamProcessHandler {

    private static Logger log = LoggerFactory.getLogger(VideoSaver.class);

    private FLVWriter3 writer;

    private int start;
    private int length;

    public VideoSaver(int start, int length) {
        this.start = start;
        this.length = length;
    }

    public void dispatchEvent(IEvent event) {

        RTMPMessage msg = RTMPMessage.build((IRTMPEvent) event);
        if (msg.getBody().getDataType() == RTMPType.TYPE_AUDIO_DATA.getType()) {
            AudioData audio = (AudioData) msg.getBody();
            if (audio.getTimestamp() == 0 || audio.getTimestamp() >= start) {
                // writeData(audio.getTimestamp() - start, audio);
            }
        } else if (msg.getBody().getDataType() == RTMPType.TYPE_VIDEO_DATA.getType()) {
            VideoData video = (VideoData) msg.getBody();
            log.info(video.toString() + "  " + video.getFrameType().toString());
            if (video.getTimestamp() == 0 || video.getTimestamp() >= start) {
                writeData(video.getTimestamp() - start, video);
            }
        } else {
            log.info("get msg : " + msg.getMessageType());
        }
    }

    public void init() {
        try {
            writer = new FLVWriter3("e:/test.flv");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        writer.close();
    }

    @SuppressWarnings("rawtypes")
    private void writeData(int timeStamp, IRTMPEvent event) {
        ITag tag = new Tag();
        tag.setTimestamp(timeStamp);
        tag.setDataType(event.getDataType());
        IoBuffer data = ((IStreamData) event).getData().asReadOnlyBuffer();
        tag.setBodySize(data.limit());
        tag.setBody(data);
        try {
            writer.writeTag(tag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
