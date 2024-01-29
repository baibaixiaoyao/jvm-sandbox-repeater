import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Behavior;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterConfig;
import com.alibaba.jvm.sandbox.repeater.plugin.exception.PluginLifeCycleException;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import java.util.List;

/**
 * apollo子调用插件
 *
 * @author liquan.lq
 */
@MetaInfServices(InvokePlugin.class)
public class ApolloSubInvokePlugin extends AbstractInvokePluginAdapter {

    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        Behavior apolloBehavior = new Behavior();
        apolloBehavior.setClassPattern("com.ctrip.framework.apollo.internals.DefaultConfigManager");
        apolloBehavior.setMethodPatterns(new String[]{"getConfig"});
        apolloBehavior.setIncludeSubClasses(false);

        List<EnhanceModel> ems = Lists.newArrayList();
        ems.add(EnhanceModel.convert(apolloBehavior));
        return ems;
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new ApolloInvocationProcessor(getType());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.APOLLO;
    }

    @Override
    public String identity() {
        return "apollo-subInvoke";
    }

    @Override
    public boolean isEntrance() {
        return false;
    }

    @Override
    public void onConfigChange(RepeaterConfig config) throws PluginLifeCycleException {
        if (configTemporary == null) {
            super.onConfigChange(config);
        } else {
            List<Behavior> current = config.getJavaSubInvokeBehaviors();
            List<Behavior> latest = configTemporary.getJavaSubInvokeBehaviors();
            super.onConfigChange(config);
//            if (JavaPluginUtils.hasDifference(current, latest)) {
//                log.error("onConfigChange,config={},configTemporary={}", config, configTemporary);
//                reWatch0();
//            }
        }
    }
}
