import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractRepeater;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.*;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.Repeater;
import org.kohsuke.MetaInfServices;

/**
 * @Description:
 * @Author: liquan.lq
 */
@MetaInfServices(Repeater.class)
public class ApolloRepeater extends AbstractRepeater {

    @Override
    public InvokeType getType() {
        return InvokeType.APOLLO;
    }

    @Override
    public String identity() {
        return "apollo";
    }

    @Override
    protected Object executeRepeat(RepeatContext context) throws Exception {
        Invocation invocation = context.getRecordModel().getEntranceInvocation();
        Identity identity = invocation.getIdentity();
        return null;
    }

    @Override
    public void repeat(RepeatContext context) {
        super.repeat(context);
    }

}
