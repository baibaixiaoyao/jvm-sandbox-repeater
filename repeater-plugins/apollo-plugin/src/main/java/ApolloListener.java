import com.alibaba.fastjson.JSONObject;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.LogUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @Author: liquan.lq
 */
public class ApolloListener extends DefaultEventListener {
    static JSONObject jsonObject = new JSONObject();

    public ApolloListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }

    @Override
    protected void initContext(Event event) {
        if (event.type == Event.Type.BEFORE) {
            BeforeEvent beforeEvent = (BeforeEvent) event;
            Object target = beforeEvent.target;

            // application startup
            if (target.getClass().getName().equals("com.ctrip.framework.apollo.internals.DefaultConfigManager")) {
                try {
                    Field originConfigs = FieldUtils.getDeclaredField(target.getClass(), "m_configs", true);
                    Map<String, Config> tmpMap = (Map<String, Config>) originConfigs.get(target);
                    // 写apollo初始值
                    File file = FileUtils.getFile(this.getClass().getResource("/").getPath() + "/linAo.properties");
                    if (file.exists()) {
                        file.delete();
                    }

                    for (String namespace : tmpMap.keySet()) {
                        Map<String, String> kv = new ConcurrentHashMap<String, String>();
                        for (String key : tmpMap.get(namespace).getPropertyNames()) {
                            kv.put(key, tmpMap.get(namespace).getProperty(key, ""));
                        }
                        jsonObject.put(namespace, kv);
                    }
                    FileUtils.write(file, jsonObject.toJSONString(), Charset.forName("UTF-8"));
                } catch (Exception ex) {
                    LogUtil.warn("DefaultConfigManager#getConfig exception", ex);
                }
            }
            // push & pull
            if (target.getClass().getName().equals("com.ctrip.framework.apollo.internals.RemoteConfigRepository")) {
                try {
                    Object retValue = MethodUtils.invokeMethod(target, true, "loadApolloConfig");
                    ApolloConfig apolloConfig = (ApolloConfig) retValue;
                    // 更新apollo信息
                    File file = FileUtils.getFile(this.getClass().getResource("/").getPath() + "/linAo.properties");
                    if (file != null) {
                        if (jsonObject != null && apolloConfig != null) {
                            jsonObject.put(apolloConfig.getNamespaceName(), apolloConfig.getConfigurations());
                            FileUtils.write(file, jsonObject.toJSONString(), Charset.forName("UTF-8"));
                        }
                    }
                } catch (Exception ex) {
                    LogUtil.warn("RemoteConfigRepository#loadApolloConfig exception", ex);
                }
            }
        }
    }
}