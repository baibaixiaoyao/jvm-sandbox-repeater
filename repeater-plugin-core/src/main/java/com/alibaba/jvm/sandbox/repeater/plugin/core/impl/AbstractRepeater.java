package com.alibaba.jvm.sandbox.repeater.plugin.core.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.jvm.sandbox.repeater.plugin.api.Broadcaster;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.RepeatCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.*;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.Repeater;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.internals.DefaultConfig;
import com.ctrip.framework.apollo.internals.DefaultConfigManager;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractRepeater} 抽象的回放实现；统一回放基本流程，包括hook和消息反馈，实现类是需要关心{@code executeRepeat}执行回放
 * <p>
 *
 * @author zhaoyb1990
 */
public abstract class AbstractRepeater implements Repeater {

    protected static Logger log = LoggerFactory.getLogger(AbstractRepeater.class);

    private Broadcaster broadcaster;

    @Override
    public void repeat(RepeatContext context) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        RepeatModel record = new RepeatModel();
        record.setRepeatId(context.getMeta().getRepeatId());
        record.setTraceId(context.getTraceId());
        try {
            // 根据之前生成的traceId开启追踪
            Tracer.start(context.getTraceId());
            // before invoke advice
            RepeatInterceptorFacade.instance().beforeInvoke(context.getRecordModel());
            // mock apollo
            if(context.getMeta().isMock()){
                mockApollo(context.getRecordModel());
            }
            Object response = executeRepeat(context);
            // after invoke advice
            RepeatInterceptorFacade.instance().beforeReturn(context.getRecordModel(), response);
            stopwatch.stop();
            record.setCost(stopwatch.elapsed(TimeUnit.MILLISECONDS));
            record.setFinish(true);
            record.setResponse(response);
            record.setMockInvocations(RepeatCache.getMockInvocation(context.getTraceId()));
        } catch (Exception e) {
            stopwatch.stop();
            record.setCost(stopwatch.elapsed(TimeUnit.MILLISECONDS));
            record.setResponse(e);
        } finally {
            Tracer.end();
        }
        sendRepeat(record);
    }

    /**
     * 获取录制时写入的apollo原始值，反射触发apollo本地仓库自动更新，存在首次问题
     *
     * @param recordModel
     */
    void mockApollo(RecordModel recordModel) {
        try {
            List<Invocation> subInvocations = recordModel.getSubInvocations();
            if (subInvocations != null) {
                for (Invocation invocation : subInvocations) {
                    if (invocation.getIdentity().getScheme().equals("apollo")) {
                        JSONObject jsonObject = JSONObject.parseObject(invocation.getResponse().toString());
                        //
                        Field mock_s_instance = ConfigService.class.getDeclaredField("s_instance");
                        mock_s_instance.setAccessible(true);
                        Object configServiceInstance = mock_s_instance.get(null);
                        //
                        Field mock_m_configManager = ConfigService.class.getDeclaredField("m_configManager");
                        mock_m_configManager.setAccessible(true);
                        Object configManagerInstance = mock_m_configManager.get(configServiceInstance);
                        //
                        Field mock_m_configs = DefaultConfigManager.class.getDeclaredField("m_configs");
                        mock_m_configs.setAccessible(true);
                        Map<String, Config> defaultConfigMapInstance = (Map<String, Config>) mock_m_configs.get(configManagerInstance);
                        // replace apollo value
                        // @TODO 需要注意apollo value的类型
                        for (String apolloNameSpace : jsonObject.keySet()) {
                            Map<String, String> resultMap = jsonObject.getObject(apolloNameSpace, Map.class);
                            Properties properties = new Properties();
                            for (String key : resultMap.keySet()) {
                                properties.setProperty(key, resultMap.get(key));
                            }
                            //
                            Method defaultConfigChange = DefaultConfig.class.getDeclaredMethod("onRepositoryChange", String.class, Properties.class);
                            defaultConfigChange.setAccessible(true);
                            defaultConfigChange.invoke(defaultConfigMapInstance.get(apolloNameSpace), apolloNameSpace, properties);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("mock apollo error", ex);
        }
    }

    @Override
    public boolean enable(RepeaterConfig config) {
        return config != null && config.getRepeatIdentities().contains(identity());
    }

    @Override
    public void setBroadcast(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    private void sendRepeat(RepeatModel record) throws RuntimeException {
        if (broadcaster == null) {
            log.error("no valid broadcaster found, ensure that Repeater#setBroadcast has been called before Repeater#repeat");
            return;
        }
        broadcaster.sendRepeat(record);
    }

    /**
     * 执行回放动作
     *
     * @param context 回放上下文
     * @return 返回结果
     * @throws Exception 异常信息
     */
    protected abstract Object executeRepeat(RepeatContext context) throws Exception;
}
