import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.RepeatCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.LogUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatContext;
import com.ctrip.framework.apollo.Config;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @Author: liquan.lq
 */
public class ApolloListener extends DefaultEventListener {
    Map<String, Map<String, String>> my_configs = new ConcurrentHashMap<String, Map<String, String>>();
    Map<String, Config> m_configs = null;

    ApolloListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }

    @Override
    public void onEvent(Event event) throws Throwable {
//        super.onEvent(event);
        if (event.type == Event.Type.BEFORE) {
            BeforeEvent beforeEvent = (BeforeEvent) event;
            if (RepeatCache.isRepeatFlow(Tracer.getTraceId())) {
                RepeatContext repeatContext = RepeatCache.getRepeatContext(Tracer.getTraceId());
                if (repeatContext != null) {
                    repeatContext.getRecordModel().getAppName();
                }
            }
        }
    }

    @Override
    protected void doBefore(BeforeEvent event) throws ProcessControlException {
        super.doBefore(event);
//        processor.doMock(event, entrance, invokeType);
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

            try {
                Field filed_m_configs = FieldUtils.getDeclaredField(target.getClass(), "m_configs", true);
                m_configs = (Map<String, Config>) filed_m_configs.get(target);
                for (Object arg : args) {
                    Config config = m_configs.get(arg);
                    Set<String> set = config.getPropertyNames();
                    Map<String, String> configMap = new ConcurrentHashMap<String, String>();

                    for (String s : set) {
                        configMap.put(s, config.getProperty(s, ""));
                    }
                    my_configs.put(arg.toString(), configMap);
                }
            } catch (Exception e) {
                LogUtil.warn("defaultConfigManager#getConfig exception.", e);
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
