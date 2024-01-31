import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.LogUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.internals.DefaultConfigManager;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @Author: liquan.lq
 */
public class ApolloListener extends DefaultEventListener {
    public static Map<String, Config> m_configs = new ConcurrentHashMap<String, Config>();
    public static Object m_target ;

    public ApolloListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }

    @Override
    public void onEvent(Event event) throws Throwable {
        super.onEvent(event);
    }

    @Override
    protected void doBefore(BeforeEvent event) throws ProcessControlException {
        super.doBefore(event);
    }

    @Override
    protected Invocation initInvocation(BeforeEvent beforeEvent) {
        return super.initInvocation(beforeEvent);
    }

    @Override
    protected boolean sample(Event event) {
        return super.sample(event);
    }

    @Override
    protected void doReturn(ReturnEvent event) {
        super.doReturn(event);
    }

    @Override
    protected void doThrow(ThrowsEvent event) {
        super.doThrow(event);
    }

    @Override
    protected boolean isTopEvent(Event event) {
        return super.isTopEvent(event);
    }

    @Override
    protected void initContext(Event event) {
        if (event.type == Event.Type.BEFORE) {
            BeforeEvent beforeEvent = (BeforeEvent) event;
            Object target = beforeEvent.target;
            Object[] args = beforeEvent.argumentArray;

            // 首次启动
            if (target.getClass().getName().equals("com.ctrip.framework.apollo.internals.DefaultConfigManager")) {
                try {
                    Field originConfigs = FieldUtils.getDeclaredField(target.getClass(), "m_configs", true);
                    Map<String, Config> tmpMap = (Map<String, Config>) originConfigs.get(target);
                    for (String key : tmpMap.keySet()) {
                        m_configs.put(key, tmpMap.get(key));
                    }
                    m_target = target;
                } catch (Exception e) {
                    LogUtil.warn("DefaultConfigManager#getConfig exception.", e);
                }
            }
            // 主动、被动推送
            if (target.getClass().getName().equals("com.ctrip.framework.apollo.internals.RemoteConfigRepository")) {
                try {
                    Field newConfigs = FieldUtils.getDeclaredField(DefaultConfigManager.class, "m_configs", true);
                    if (m_configs != null) {
                        newConfigs.set(m_target, m_configs);
                    }
//                    Object retVal = MethodUtils.invokeMethod(target,true,"loadApolloConfig");
//                    if(retVal instanceof ApolloConfig){
//                      apolloConfig  = (ApolloConfig) retVal;
//                    }
                } catch (Exception e) {
                    LogUtil.warn("RemoteConfigRepository#loadApolloConfig exception.", e);
                }
            }
        }
    }

    @Override
    protected boolean isEntranceBegin(Event event) {
        return super.isEntranceBegin(event);
    }

    @Override
    protected boolean isEntranceFinish(Event event) {
        return super.isEntranceFinish(event);
    }

    @Override
    protected boolean access(Event event) {
        return super.access(event);
    }
}