package com.aiiot.distributed.processor.bean;

import com.aiiot.distributed.api.clazz.Change;
import com.aiiot.distributed.api.clazz.Reply;

import javax.lang.model.element.Element;
import java.util.Arrays;
import java.util.Optional;

/**
 * Created : 27/07/2023-20.33
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public record ReplyInfo( TypeInfo typeInfo) {
    public static ReplyInfo create(DistributedInfo distributedInfo, Element entity) {
        TypeInfo typeInfo = distributedInfo.getBaseInfos().get(entity.asType().toString());
        if (typeInfo==null)
            throw new RuntimeException("Reply not defined as Entity "+entity.asType());
        Reply reply = entity.getAnnotation(Reply.class);
        return new ReplyInfo( typeInfo);
    }

    @Override
    public String toString() {
        return "ReplyInfo{" +
                "typeInfo=" + typeInfo +
                '}';
    }
}
