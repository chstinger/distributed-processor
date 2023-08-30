
package com.aiiot.distributed.processor.bean;

import com.aiiot.distributed.api.CommonData;
import com.aiiot.distributed.processor.Common;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Created : 30/07/2023-13.37
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */

public record TypeInfo(String packageClassName,String className,String extendsClass,int version,boolean isEnum,ArrayList<Field> fields,TypeInfo[] parent,Integer[] typeId)  {
    public static TypeInfo create(Element basis) {
        if (basis instanceof TypeElement typeElement) {
            if (typeElement.getSuperclass()==null ) {
                throw new RuntimeException(basis.getSimpleName()+"Entity class shall inherit a entity class or "+ CommonData.class.getSimpleName());
            }
            return new TypeInfo(typeElement.getQualifiedName().toString(),typeElement.getSimpleName().toString(),typeElement.getSuperclass().toString(),Common.getVersion(typeElement),typeElement.getSuperclass().toString().startsWith("java.lang.Enum"),Common.getFields(typeElement),new TypeInfo[1],new Integer[1]);
        } else {
            throw new RuntimeException(basis.getSimpleName()+"Entity class shall inherit a entity class or "+ CommonData.class.getSimpleName());
        }
    }



    public void updateParent(TypeInfo typeInfo) {
        parent[0]=typeInfo;
    }

    public void setTypeId(int id) {
        if (typeId[0]==null)
            typeId[0]=id;
    }

    public String getPackageName()  {
        return packageClassName().replace("."+className(),"");
    }

    @Override
    public String toString() {

        return "TypeInfo{" +
                "packageClassName='" + packageClassName + '\'' +
                ", objectName='" + className + '\'' +
                ", extendsClass='" + extendsClass + '\'' +
                ", \n fields=" + fields.stream().map(Field::toString).collect(Collectors.joining("\n")) +
                '}'+"\n";
    }

    public int getVersion() {
        Integer integer = fields.stream().max((o1, o2) -> Math.max(o1.version(), o2.version())).map(Field::version).orElse(0);
        return Math.max(integer,version());
    }

    public Field getKey() {
        Field field = fields.stream().filter(Field::isKey).findFirst().orElse(null);
        if (field==null && parent[0]!=null )
            return parent[0].getKey();
        return field;

    }
}
