/*
 * @(#) ClientMethodHander.java 2017年5月23日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.rtmp;

import java.util.Map;
import java.util.Timer;

import org.red5.client.net.rtmp.INetStreamEventHandler;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.IConnection;
import org.red5.server.net.rtmp.event.Notify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zzhao
 * @version 2017年5月23日
 */
public class ClientMethodHander implements INetStreamEventHandler {
    private Logger log = LoggerFactory.getLogger(ClientMethodHander.class);

    private Timer timer;

    public ClientMethodHander() {

    }

    public void onStatus(IConnection conn, ObjectMap<String, Object> status) {
        log.debug("onStatus: {}", status);
        String code = status.get("code").toString();
        if ("NetStream.Play.Stop".equals(code)) {
            log.debug("Playback stopped");
            conn.close();
        }
    }

    public void onPlayStatus(IConnection conn, Map<Object, Object> info) {
        log.info("onPlayStatus: {}", info);
    }

    public void onBWDone() {
        log.info("onBwDone");
    }

    public void onStreamEvent(Notify notify) {
//        // TODO Auto-generated method stub
//        log.info(String.format("onStreamEvent: %s", notify));
//        ObjectMap<?, ?> map = (ObjectMap<?, ?>) notify.getCall().getArguments()[0];
//        String code = (String) map.get("code");
//        log.info("code : " + code);
//        if (StatusCodes.NS_PLAY_STREAMNOTFOUND.equals(code)) {
//            log.info("Requested stream was not found");
//            client.getConnection().close();
//        } else if (StatusCodes.NS_PLAY_STOP.equals(code) || StatusCodes.NS_PLAY_COMPLETE.equals(code)) {
//            log.info("Source has stopped publishing or play is complete");
//            client.getConnection().close();
//        }
    }
}
