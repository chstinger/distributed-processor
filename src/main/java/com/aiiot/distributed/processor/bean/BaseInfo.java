package com.aiiot.distributed.processor.bean;

import com.aiiot.distributed.processor.Common;

import javax.lang.model.element.Element;
import java.util.ArrayList;

/**
 * Created : 30/07/2023-13.37
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public record BaseInfo(String className, ArrayList<Field> fields)  {
    public static BaseInfo create(Element basis) {
        return new BaseInfo(basis.getSimpleName().toString(), Common.getFields(basis));
    }

    @Override
    public String toString() {
        return "BaseInfo{" +
                "objectName='" + className + '\'' +
                ", fields=" + fields +
                '}';
    }
}
