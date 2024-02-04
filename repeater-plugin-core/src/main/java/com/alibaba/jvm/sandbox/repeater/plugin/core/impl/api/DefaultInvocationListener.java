package com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.jvm.sandbox.repeater.plugin.api.Broadcaster;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.RecordCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.ApplicationModel;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializeException;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.SerializerWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Identity;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RecordModel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DefaultInvocationListener} 默认的调用监听实现
 * <p>
 *
 * @author zhaoyb1990
 */
public class DefaultInvocationListener implements InvocationListener {

    private final static Logger log = LoggerFactory.getLogger(DefaultInvocationListener.class);

    private final Broadcaster broadcast;

    public DefaultInvocationListener(Broadcaster broadcast) {
        this.broadcast = broadcast;
    }

    //@todo add apollo invocation
    List<Invocation> retSubInvocation(List<Invocation> originSubInvocation) {
        try {
            Long t = System.currentTimeMillis();
            File file = FileUtils.getFile(this.getClass().getResource("/").getPath() + "/linAo.properties");
            if(file != null){
                String result = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
                JSONObject jsonObject = JSONObject.parseObject(result);
                if (jsonObject != null && !jsonObject.isEmpty()) {
                    Invocation apolloInvocation = new Invocation();
                    apolloInvocation.setIdentity(new Identity("apollo", jsonObject.keySet().toString(), "linAo", new HashMap<String, String>(1)));
                    apolloInvocation.setType(InvokeType.APOLLO);
                    apolloInvocation.setTraceId(originSubInvocation.get(0).getTraceId());
                    apolloInvocation.setIndex(originSubInvocation.size() + 1);
                    apolloInvocation.setResponse(jsonObject);
                    apolloInvocation.setStart(t - 1);
                    apolloInvocation.setEnd(t + 3);
                    apolloInvocation.setSerializeToken(originSubInvocation.get(0).getSerializeToken());
                    SerializerWrapper.inTimeSerialize(apolloInvocation);
                    originSubInvocation.add(apolloInvocation);
                }
            }
        } catch (Exception e) {
            log.error("apollo invocation serialize error", e);
        }
        return originSubInvocation;
    }

    @Override
    public void onInvocation(Invocation invocation) {
        try {
            SerializerWrapper.inTimeSerialize(invocation);
        } catch (SerializeException e) {
            Tracer.getContext().setSampled(false);
            log.error("Error occurred serialize", e);
        }
        if (invocation.isEntrance()) {
            ApplicationModel am = ApplicationModel.instance();
            RecordModel recordModel = new RecordModel();
            recordModel.setAppName(am.getAppName());
            recordModel.setEnvironment(am.getEnvironment());
            recordModel.setHost(am.getHost());
            recordModel.setTraceId(invocation.getTraceId());
            recordModel.setTimestamp(invocation.getStart());
            recordModel.setEntranceInvocation(invocation);
            // 如果有apollo数据则需要插入到subInvocation
            recordModel.setSubInvocations(retSubInvocation(RecordCache.getSubInvocation(invocation.getTraceId())));
            if (log.isDebugEnabled()) {
                log.debug("sampleOnRecord:traceId={},rootType={},subTypes={}", recordModel.getTraceId(), invocation.getType(), assembleTypes(recordModel));
            }
            broadcast.sendRecord(recordModel);
        } else {
            RecordCache.cacheSubInvocation(invocation);
        }
    }

    private String assembleTypes(RecordModel recordModel) {
        StringBuilder builder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(recordModel.getSubInvocations())) {
            Map<InvokeType, AtomicInteger> counter = new HashMap<InvokeType, AtomicInteger>(1);
            for (Invocation invocation : recordModel.getSubInvocations()) {
                if (counter.containsKey(invocation.getType())) {
                    counter.get(invocation.getType()).incrementAndGet();
                } else {
                    counter.put(invocation.getType(), new AtomicInteger(1));
                }
            }
            for (Map.Entry<InvokeType, AtomicInteger> entry : counter.entrySet()) {
                builder.append(entry.getKey().name()).append("=").append(entry.getValue()).append(";");
            }
        }
        return builder.length() > 0 ? builder.substring(0, builder.length() - 1) : "nil";
    }
}
