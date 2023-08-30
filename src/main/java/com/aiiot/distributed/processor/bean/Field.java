package com.aiiot.distributed.processor.bean;

import com.aiiot.distributed.api.Version;
import com.aiiot.distributed.api.field.Index;
import com.aiiot.distributed.api.IndexType;
import com.aiiot.distributed.api.field.Key;
import com.aiiot.distributed.api.field.Mandatory;
import com.aiiot.distributed.api.field.Optional;
import com.aiiot.distributed.processor.Common;

import javax.lang.model.element.Element;

/**
 * Created : 27/07/2023-20.43
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public record Field(String name, FieldTypeInfo fieldTypeInfo, IndexType indexType,boolean isKey, NecessityType necessity,int version) {


    public static Field create( Element enclosedElement) {
        return  new Field(getFieldName(enclosedElement),FieldTypeInfo.create(enclosedElement.asType().toString()),getIndexType(enclosedElement),isKey(enclosedElement),getNecessity(enclosedElement), Common.getVersion(enclosedElement));
    }

    
    private static NecessityType getNecessity(Element enclosedElement) {
        if (enclosedElement.getAnnotation(Mandatory.class)!=null)
            return NecessityType.Mandatory;
        if (enclosedElement.getAnnotation(Optional.class)!=null)
            return NecessityType.Optional;
        return null;
    }

    private static boolean isKey(Element enclosedElement) {
        Key annotation = enclosedElement.getAnnotation(Key.class);
        return annotation!=null;
    }

    private static IndexType getIndexType(Element enclosedElement) {
        Index annotation = enclosedElement.getAnnotation(Index.class);
        if (annotation!=null)
            return annotation.indexType();
        return null;
    }


    private static String getFieldName(Element enclosedElement) {
        return enclosedElement.getSimpleName().toString();
    }

    @Override
    public String toString() {
        return "Field{" +
                "name='" + name + '\'' +
                ", fieldTypeInfo=" + fieldTypeInfo +
                ", indexType=" + indexType +
                ", isKey=" + isKey +
                ", necessity=" + necessity +
                '}';
    }

    public String write() {
        return fieldTypeInfo.write(name);

    }
    public String read() {
        return fieldTypeInfo.read(name);

    }
}
