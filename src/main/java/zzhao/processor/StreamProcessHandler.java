/*
 * @(#) StreamProcessHandler.java 2017年5月23日
 * 
 * Copyright 2016 NetEase.com, Inc. All rights reserved.
 */
package zzhao.processor;

import org.red5.server.api.event.IEventDispatcher;

/**
 *
 * @author zzhao
 * @version 2017年5月23日
 */
public interface StreamProcessHandler extends IEventDispatcher {

    public void init();
    
    public void close();
}
