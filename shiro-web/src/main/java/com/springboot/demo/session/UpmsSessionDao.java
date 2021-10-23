package com.springboot.demo.session;

import com.alibaba.fastjson.JSON;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.ValidatingSession;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component("sessionDAO")
public class UpmsSessionDao extends CachingSessionDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpmsSessionDao.class);
    // 会话key
    private final static String ZHENG_UPMS_SHIRO_SESSION_ID = "zheng-upms-shiro-session-id";
    // 全局会话key
    private final static String ZHENG_UPMS_SERVER_SESSION_ID = "zheng-upms-server-session-id";
    // 全局会话列表key
    private final static String ZHENG_UPMS_SERVER_SESSION_IDS = "zheng-upms-server-session-ids";
    // code key
    private final static String ZHENG_UPMS_SERVER_CODE = "zheng-upms-server-code";
    // 局部会话key
    public final static String ZHENG_UPMS_CLIENT_SESSION_ID = "zheng-upms-client-session-id";
    // 单点同一个code所有局部会话key
    public final static String ZHENG_UPMS_CLIENT_SESSION_IDS = "zheng-upms-client-session-ids";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = generateSessionId(session);
        assignSessionId(session, sessionId);
        redisTemplate.opsForValue().setIfAbsent (ZHENG_UPMS_SHIRO_SESSION_ID + "_" + sessionId,
                JSON.toJSONString(session), (int) session.getTimeout() / 1000, TimeUnit.SECONDS);
        LOGGER.debug("doCreate >>>>> sessionId={}", sessionId);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        String session = redisTemplate.opsForValue().get(ZHENG_UPMS_SHIRO_SESSION_ID + "_" + sessionId);
        LOGGER.debug("doReadSession >>>>> sessionId={}", sessionId);
        return JSON.parseObject(session, Session.class);
    }

    @Override
    protected void doUpdate(Session session) {
        // 如果会话过期/停止 没必要再更新了
        if(session instanceof ValidatingSession && !((ValidatingSession)session).isValid()) {
            return;
        }
        // 更新session的最后一次访问时间
        UpmsSession upmsSession = (UpmsSession) session;
        UpmsSession cacheUpmsSession = (UpmsSession) doReadSession(session.getId());
        if (null != cacheUpmsSession) {
            upmsSession.setStatus(cacheUpmsSession.getStatus());
            upmsSession.setAttribute("FORCE_LOGOUT", cacheUpmsSession.getAttribute("FORCE_LOGOUT"));
        }
        redisTemplate.opsForValue().set(ZHENG_UPMS_SHIRO_SESSION_ID + "_" + session.getId(),
                JSON.toJSONString(session),(int) session.getTimeout() / 1000, TimeUnit.SECONDS);
        // 更新ZHENG_UPMS_SERVER_SESSION_ID、ZHENG_UPMS_SERVER_CODE过期时间 TODO
        LOGGER.debug("doUpdate >>>>> sessionId={}", session.getId());
    }

    @Override
    protected void doDelete(Session session) {
        String sessionId = session.getId().toString();
        String upmsType = String.valueOf (session.getAttribute("zheng.upms.type"));
        if ("client".equals(upmsType)) {
            // 删除局部会话和同一code注册的局部会话
            String code = redisTemplate.opsForValue().get(ZHENG_UPMS_CLIENT_SESSION_ID + "_" + sessionId);
            redisTemplate.delete (ZHENG_UPMS_CLIENT_SESSION_ID + "_" + sessionId);
            redisTemplate.opsForSet().remove(ZHENG_UPMS_CLIENT_SESSION_IDS + "_" + code, sessionId);
        }
        if ("server".equals(upmsType)) {
            // 当前全局会话code
            String code = redisTemplate.opsForValue().get(ZHENG_UPMS_SERVER_SESSION_ID + "_" + sessionId);
            // 清除全局会话
            redisTemplate.delete(ZHENG_UPMS_SERVER_SESSION_ID + "_" + sessionId);
            // 清除code校验值
            redisTemplate.delete(ZHENG_UPMS_SERVER_CODE + "_" + code);
            // 清除所有局部会话
            Set<String> clientSessionIds = redisTemplate.opsForSet().members(ZHENG_UPMS_CLIENT_SESSION_IDS + "_" + code);
            for (String clientSessionId : clientSessionIds) {
                redisTemplate.delete(ZHENG_UPMS_CLIENT_SESSION_ID + "_" + clientSessionId);
                redisTemplate.opsForSet().remove(ZHENG_UPMS_CLIENT_SESSION_IDS + "_" + code, clientSessionId);
            }
            LOGGER.debug("当前code={}，对应的注册系统个数：{}个", code, redisTemplate.opsForSet().size(ZHENG_UPMS_CLIENT_SESSION_IDS + "_" + code));
            // 维护会话id列表，提供会话分页管理
//            redisTemplate.opsForList().leftPop(ZHENG_UPMS_SERVER_SESSION_IDS, 1, sessionId);
        }
        // 删除session
        redisTemplate.delete(ZHENG_UPMS_SHIRO_SESSION_ID + "_" + sessionId);

        LOGGER.debug("doUpdate >>>>> sessionId={}", sessionId);
    }
}
