package com.aiiot.distributed.processor.bean;

import com.aiiot.distributed.api.clazz.Change;
import com.aiiot.distributed.api.clazz.Query;
import com.aiiot.distributed.api.clazz.Reply;

import javax.lang.model.element.Element;
import java.util.Arrays;
import java.util.Optional;

/**
 * Created : 27/07/2023-20.33
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public record ChangeQueryInfo(String info, String entityName,boolean query, TypeInfo typeInfo) {
    public static ChangeQueryInfo create(DistributedInfo distributedInfo, Element entity) {
        TypeInfo typeInfo = distributedInfo.getBaseInfos().get(entity.asType().toString());
        if (typeInfo==null)
            throw new RuntimeException("Change or Reply not defined as Entity "+entity.asType());
        String info="";
        Change change = entity.getAnnotation(Change.class);
        Query query = entity.getAnnotation(Query.class);
        if (change==null && query ==null) {
            throw new RuntimeException("Not a change or a query "+entity.asType());
        }
        if (change!=null && query !=null) {
            throw new RuntimeException("Both a change or a query "+entity.asType());
        }
        String entityName="";
        if (change!=null) {
            info = change.reply();
            entityName= change.entityName();
        }
        if (query !=null) {
            info = query.reply();
        }
        if (!distributedInfo.exists(info)) {
            throw  new RuntimeException("Reply not found in entity "+entity);
        }
        return new ChangeQueryInfo(info, entityName,query !=null, typeInfo);
    }


    @Override
    public String toString() {
        return "ChangeQueryInfo{" +
                "info='" + info + '\'' +
                ", entityName='" + entityName + '\'' +
                ", query=" + query +
                ", typeInfo=" + typeInfo +
                '}';
    }
}
