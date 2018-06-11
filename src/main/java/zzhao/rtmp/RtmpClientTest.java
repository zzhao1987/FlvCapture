/*
 * @(#) RtmpClientTest.java 2017年5月22日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.rtmp;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.red5.client.net.rtmp.ClientExceptionHandler;
import org.red5.client.net.rtmp.INetStreamEventHandler;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.net.rtmp.event.Notify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zzhao.processor.StreamProcessHandler;
import zzhao.processor.VideoCapture;
import zzhao.processor.VideoSaver;

/**
 *
 * @author zzhao
 * @version 2017年5月22日
 * 
 * https://ptrthomas.wordpress.com/2008/04/19/how-to-record-rtmp-flash-video-streams-using-red5/
 */
public class RtmpClientTest {

    private static Logger log = LoggerFactory.getLogger(RtmpClientTest.class);

    private static Timer timer = new Timer();

    private static String sourceStreamName = "hks";
    private static String host = "live.hkstv.hk.lxdns.com";
    		//"live.hkstv.hk.lxdns.com";
    private static String application = "live";
    private static int port = 1935;

    private static RTMPClient client;

    private static int start = 0;
    private static int length = 6000;

    private static Number streamId;

    private static StreamProcessHandler handler;

    public static void main(String[] args) throws IOException {
        handler = new VideoSaver(start, length);
        handler.init();
                        
        client = new RTMPClient();

        client.setStreamEventDispatcher(handler);

        client.setStreamEventHandler(new INetStreamEventHandler() {
            public void onStreamEvent(Notify notify) {

            }
        });
        client.setServiceProvider(new ClientMethodHander());
        client.setConnectionClosedHandler(new Runnable() {
            public void run() {
                log.info("Connection closed");
            }
        });
        client.setExceptionHandler(new ClientExceptionHandler() {
            public void handleException(Throwable throwable) {
                log.error("exception:", throwable);
            }
        });

        IPendingServiceCallback connectCallback = new IPendingServiceCallback() {
            public void resultReceived(IPendingServiceCall call) {
                log.info("connectCallback");
                ObjectMap<?, ?> map = (ObjectMap<?, ?>) call.getResult();
                String code = (String) map.get("code");
                log.info("Response code: {}", code);
                if ("NetConnection.Connect.Rejected".equals(code)) {
                    System.out.printf("Rejected: %s\n", map.get("description"));
                    client.disconnect();
                } else if ("NetConnection.Connect.Success".equals(code)) {
                    // 1. Wait for onBWDone
                    timer.schedule(new BandwidthStatusTask(), 2000L);
                }
            }
        };

        client.connect(host, port, application, connectCallback);
        if (client.getConnection() == null) {
            log.info("connection failed!");
            return;
        }
        while (!client.getConnection().isClosed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        log.debug("Client not connected");
        timer.cancel();
        log.info("Exit");
        client.disconnect();
        handler.close();
    }

    private static final class StreamEventDispatcher implements IEventDispatcher {

        public void dispatchEvent(IEvent event) {

        }
    }



    /**
     * Creates a "stream" via playback, this is the source stream.
     */
    private static final class CreateStreamCallback implements IPendingServiceCallback {

        public void resultReceived(IPendingServiceCall call) {
            System.out.println("resultReceived: " + call);
            streamId = (Number) call.getResult();
            System.out.println("stream id: " + streamId);
            timer.schedule(new StopTask(), 10000L);
            client.play(streamId, sourceStreamName, start, length);
        }

    }

    /**
     * Continues to check for onBWDone
     */
    private static final class BandwidthStatusTask extends TimerTask {

        public void run() {
            // check for onBWDone
            System.out.println("Bandwidth check done: " + client.isBandwidthCheckDone());
            // cancel this task
            this.cancel();

            // create a task to wait for subscribed
            timer.schedule(new PlayStatusTask(), 1000L);
            // 2. send FCSubscribe
            client.subscribe(new SubscribeStreamCallBack(), new Object[] {sourceStreamName});
        }

    }

    private static final class PlayStatusTask extends TimerTask {

        public void run() {
            // checking subscribed
            System.out.println("Subscribed: " + client.isSubscribed());
            // cancel this task
            this.cancel();
            // 3. create stream
            client.createStream(new CreateStreamCallback());
        }

    }

    private static final class StopTask extends TimerTask {
        public void run() {
            // cancel this task
            System.out.println("start to stop!");
            this.cancel();
            // 3. create stream
//            IClientStream stream = client.getConnection().getStreamById(streamId);
//            stream.stop();
            client.getConnection().close();
        }
    }

    /**
     * Handles result from subscribe call.
     */
    private static final class SubscribeStreamCallBack implements IPendingServiceCallback {

        public void resultReceived(IPendingServiceCall call) {
            System.out.println("resultReceived: " + call);
        }

    }
}
