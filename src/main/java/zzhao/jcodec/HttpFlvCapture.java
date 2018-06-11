package zzhao.jcodec;

import static org.jcodec.codecs.h264.H264Utils.unescapeNAL;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import com.google.common.collect.Lists;

public class HttpFlvCapture {

    private final static byte[] FLV_SIGNATURE = new byte[]{'F','L','V'};
    private final static int FLV_Tag_HEADER_SIZE = 11;
    private final static byte FLV_AUDIO_TYPE = 8;
    private final static byte FLV_VIDEO_TYPE = 9;
    private final static byte FLV_VIDEO_KEY_FRAME = 1;
    private final static byte FLV_VIDEO_AVC = 7;
    private final static byte FLV_AVC_HEADER = 0;
    private final static byte FLV_AVC_NALU = 1;
    private final static int FLV_AVC_VIDEO_OFFSET = 5;  // video header + video packaet header
    private final static int FLV_AVC_SPS_OFFSET = 6;
   
    private H264Decoder decoder;
    private int pictureHeight = 2048;
    private int pictureWidth = 2048; 
    private int captureCount = 0;
    
    
    public HttpFlvCapture(){
        
    }
    
    public boolean processFlvStream(DataInputStream flvStream) throws IOException{
        if(!processFlvHeader(flvStream)){
            return false;
        }
        
        while(processFlvTag(flvStream)){};
        
        return true;
    }


    private boolean processFlvHeader(DataInputStream flvStream) throws IOException{

        byte[] header = new byte[9];
        if(readEnoughBytes(flvStream, header, 9)){
            if(!bytesCompare(header, FLV_SIGNATURE, 3)){
                return false;
            }
            
            return true;
        }
        
        return false;
    }
    

    private boolean processFlvTag(DataInputStream flvStream)  throws IOException{
        flvStream.readInt(); // skip previousTagSize
        
        byte[] tagHeader = new byte[FLV_Tag_HEADER_SIZE];
        if(!readEnoughBytes(flvStream, tagHeader, FLV_Tag_HEADER_SIZE)){
            return false;
        }
        
        
        int tagDataSize = ((tagHeader[1] & 0xff) << 16) + ((tagHeader[2] & 0xff) << 8) + (tagHeader[3] & 0xff);
        byte[] tagData = new byte[tagDataSize];
        if(!readEnoughBytes(flvStream, tagData, tagDataSize)){
            return false;
        }

        if(tagHeader[0] == FLV_VIDEO_TYPE){
            processVideoData(tagData);
        }
        
        
        return true;
    }
    
    
    private boolean processVideoData(byte[] videoData) throws IOException{
        byte frameType = (byte)((videoData[0] & 0xf0) >> 4);
        byte frameDecoder = (byte)(videoData[0] & 0x0f);
        
        // only process  keyframe and avc formate

        if(frameType == FLV_VIDEO_KEY_FRAME && frameDecoder == FLV_VIDEO_AVC){
            byte avcType = videoData[1];
            switch (avcType) {
                case FLV_AVC_HEADER:
                    ByteBuffer buffer = ByteBuffer.wrap(videoData, FLV_AVC_VIDEO_OFFSET, videoData.length - FLV_AVC_VIDEO_OFFSET);
                    if(initDecoder(buffer)){
                        return true;
                    }
                    break;
                case FLV_AVC_NALU:
                    buffer = ByteBuffer.wrap(videoData, FLV_AVC_VIDEO_OFFSET, videoData.length - FLV_AVC_VIDEO_OFFSET);
                    doCapture(buffer);
                    break;  
                    
                default:
                    break;
            }
        }

        return false;   
    }
    
    private boolean initDecoder(ByteBuffer buffer){
        if(decoder == null){
            decoder = new H264Decoder();
        }

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
            pictureWidth = sps.picWidthInMbsMinus1 + 1 << 4;
            pictureHeight = SeqParameterSet.getPicHeightInMbs(sps) << 4;
          
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
    
    private void doCapture(ByteBuffer data) throws IOException{
        if(decoder == null){
            return;
        }
        
        byte[][] picData = Picture.create(pictureWidth, pictureHeight, ColorSpace.YUV420J).getData();
        List<ByteBuffer> nalUnits = extractNALUnits(data);
        Picture pic = decoder.decodeFrameFromNals(nalUnits, picData);
        String imagePath = "e:/test_" + captureCount + ".jpeg";
        ImageIO.write(AWTUtil.toBufferedImage(pic), "jpg", new File(imagePath));
        System.out.println(imagePath);
        captureCount++;
    }
    
    
    private static boolean readEnoughBytes(DataInputStream is, byte[] dst, int length) throws IOException{
        int offset = 0;
        int left = length;
        int n;
        while(left > 0){
            n = is.read(dst, offset, left);
            if(n == -1){
                // no data
                return false;
            }
            left = left - n;
            offset = offset + n;
        }
        return true;
    }
    
    private static boolean bytesCompare(byte[] array1, byte[] array2, int len){
        if(array1 == array2){
            return true;
        }
        
        if(array1 == null || array2 == null){
            return false;
        }
        
        if(array1.length < len || array2.length < len){
            return false;
        }
        
        for(int i = 0; i < len; i++){
            if(array1[i] != array2[i]){
                return false;
            }
        }
        
        return true;
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
    
    public static void main(String[] args) {

        CloseableHttpClient client = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet("http://hdl.9158.com/live/b06c7246f0a11f45b9376e8f741d8878.flv");

        try {
            CloseableHttpResponse response1 = client.execute(httpGet);
            HttpEntity entity = response1.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                HttpFlvCapture capture = new HttpFlvCapture();
                capture.processFlvStream(new DataInputStream(inputStream));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }
}
