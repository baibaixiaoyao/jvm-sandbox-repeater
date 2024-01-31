import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.InvokeEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Identity;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatContext;

import java.util.Map;

/**
 * @Description:
 * @Author: liquan.lq
 */
class ApolloInvocationProcessor extends DefaultInvocationProcessor {

    ApolloInvocationProcessor(InvokeType type) {
        super(type);
    }

    @Override
    public Object[] assembleRequest(BeforeEvent event) {
        return super.assembleRequest(event);
    }

    @Override
    public Object assembleResponse(Event event) {
        return super.assembleResponse(event);
    }

    @Override
    public Throwable assembleThrowable(ThrowsEvent event) {
        return super.assembleThrowable(event);
    }

    @Override
    public void doMock(BeforeEvent event, Boolean entrance, InvokeType type) throws ProcessControlException {
        super.doMock(event, entrance, type);
    }

    @Override
    public Object assembleMockResponse(BeforeEvent event, Invocation invocation) {
        return super.assembleMockResponse(event, invocation);
    }

    @Override
    public boolean inTimeSerializeRequest(Invocation invocation, BeforeEvent event) {
        return super.inTimeSerializeRequest(invocation, event);
    }

    @Override
    protected Map<String, String> getExtra() {
        return super.getExtra();
    }

    @Override
    protected boolean skipMock(BeforeEvent event, Boolean entrance, RepeatContext context) {
        return super.skipMock(event, entrance, context);
    }

    @Override
    public boolean ignoreEvent(InvokeEvent event) {
        return super.ignoreEvent(event);
    }

    @Override
    public Identity assembleIdentity(BeforeEvent event) {
       return super.assembleIdentity(event);
    }

}
