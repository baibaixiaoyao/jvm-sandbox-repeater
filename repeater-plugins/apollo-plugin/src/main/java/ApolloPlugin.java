import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;
import org.kohsuke.MetaInfServices;

import java.util.List;

/**
 * @Description:
 * @Author: liquan.lq
 */
@MetaInfServices(InvokePlugin.class)
public class ApolloPlugin extends AbstractInvokePluginAdapter {
    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        EnhanceModel defaultConfigEM = EnhanceModel.builder()
                .classPattern("com.ctrip.framework.apollo.internals.DefaultConfigManager")
                .methodPatterns(EnhanceModel.MethodPattern.transform(
                        "getConfig"))
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();

        EnhanceModel remoteConfigEM = EnhanceModel.builder()
                .classPattern("com.ctrip.framework.apollo.internals.RemoteConfigRepository")
                .methodPatterns(EnhanceModel.MethodPattern.transform(
                        "loadApolloConfig"))
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Lists.newArrayList(defaultConfigEM, remoteConfigEM);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new ApolloInvocationProcessor(getType());
    }

    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new ApolloListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.APOLLO;
    }

    @Override
    public String identity() {
        return "apollo";
    }

    @Override
    public boolean isEntrance() {
        return false;
    }
}
