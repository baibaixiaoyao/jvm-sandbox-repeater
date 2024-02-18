package com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

    /**
     * add apollo invocation
     * 1、有子调用，直接add
     * 2、无子调用，new SubInvocation后add
     *
     * @param subInvocation
     * @param mainInvocation
     * @return
     */
    List<Invocation> addApolloSubInvocation(List<Invocation> subInvocation, Invocation mainInvocation) {
        try {
            Long t = System.currentTimeMillis();
            File file = FileUtils.getFile(this.getClass().getResource("/").getPath() + "/linAo.properties");
            if (file != null) {
                String result = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
                JSONObject jsonObject = JSONObject.parseObject(result);
                if (jsonObject != null && !jsonObject.isEmpty()) {
                    Invocation apolloInvocation = new Invocation();
                    apolloInvocation.setIdentity(new Identity("apollo", jsonObject.keySet().toString(), "linAo", new HashMap<String, String>(1)));
                    apolloInvocation.setType(InvokeType.APOLLO);
                    apolloInvocation.setTraceId(subInvocation != null ? subInvocation.get(subInvocation.size() - 1).getTraceId() : mainInvocation.getTraceId());
                    apolloInvocation.setIndex(subInvocation != null ? subInvocation.size() + 1 : 0);
                    apolloInvocation.setResponse(jsonObject);
                    apolloInvocation.setStart(t - 1);
                    apolloInvocation.setEnd(t + 3);
                    apolloInvocation.setInvokeId(subInvocation != null ? subInvocation.get(subInvocation.size() - 1).getInvokeId() + 1 : mainInvocation.getInvokeId() + 1);
                    apolloInvocation.setProcessId(subInvocation != null ? subInvocation.get(subInvocation.size() - 1).getProcessId() + 1 : mainInvocation.getProcessId() + 1);
                    apolloInvocation.setSerializeToken(subInvocation != null ? subInvocation.get(subInvocation.size() - 1).getSerializeToken() : mainInvocation.getSerializeToken());
                    SerializerWrapper.inTimeSerialize(apolloInvocation);
                    // 无子调用，new后add
                    if (subInvocation == null) {
                        subInvocation = new ArrayList<Invocation>();
                    }
                    subInvocation.add(apolloInvocation);
                }
            }
        } catch (Exception e) {
            log.error("apollo invocation serialize error", e);
        }
        return subInvocation;
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
            // 如果有apollo数据则需要添加到subInvocation
            recordModel.setSubInvocations(addApolloSubInvocation(RecordCache.getSubInvocation(invocation.getTraceId()), invocation));
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
