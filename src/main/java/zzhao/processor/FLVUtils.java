/*
 * @(#) FLVUtils.java 2017年5月24日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.processor;

import java.nio.ByteBuffer;

import org.red5.io.ITag;
import org.red5.io.flv.FLVHeader;
import org.red5.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zzhao
 * @version 2017年5月24日
 */
public class FLVUtils {

    private static Logger log = LoggerFactory.getLogger(FLVUtils.class);

    /**
     * Length of the flv header in bytes
     */
    private final static int HEADER_LENGTH = 9;

    /**
     * Length of the flv tag in bytes
     */
    private final static int TAG_HEADER_LENGTH = 11;

    /**
     * For now all recorded streams carry a stream id of 0.
     */
    private final static byte[] DEFAULT_STREAM_ID = new byte[] {(byte) (0 & 0xff), (byte) (0 & 0xff), (byte) (0 & 0xff)};

    public static byte[] getHeaderByte(boolean hasVideo, boolean hasAudio) {
        FLVHeader flvHeader = new FLVHeader();
        flvHeader.setFlagAudio(hasAudio);
        flvHeader.setFlagVideo(hasVideo);
        // create a buffer
        byte[] data = new byte[HEADER_LENGTH + 4];
        ByteBuffer header = ByteBuffer.wrap(data);
        flvHeader.write(header);
        return data;
    }

    public static byte[] getTagByte(ITag tag) {
        int bodySize = tag.getBodySize();
        int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;

        byte[] data = new byte[totalTagSize];
        ByteBuffer tagBuffer = ByteBuffer.wrap(data);
        // Data Type
        IOUtils.writeUnsignedByte(tagBuffer, tag.getDataType()); // 1
        // Body Size - Length of the message. Number of bytes after StreamID to end of tag
        // (Equal to length of the tag - 11)
        IOUtils.writeMediumInt(tagBuffer, bodySize); // 3
        // Timestamp
        IOUtils.writeExtendedMediumInt(tagBuffer, tag.getTimestamp()); // 4
        // Stream id
        tagBuffer.put(DEFAULT_STREAM_ID); // 3
        tagBuffer.put(tag.getBody().buf());
        // we add the tag size
        tagBuffer.putInt(TAG_HEADER_LENGTH + bodySize);
        return data;
    }

}
