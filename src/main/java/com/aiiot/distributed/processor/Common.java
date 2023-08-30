package com.aiiot.distributed.processor;

import com.aiiot.distributed.api.Version;
import com.aiiot.distributed.processor.bean.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created : 30/07/2023-13.41
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public class Common {

    public static ArrayList<Field> getFields(Element entity) {
        ArrayList<Field> res=new ArrayList<>();
        for (Element enclosedElement : entity.getEnclosedElements()) {
            if (enclosedElement.getModifiers().contains(Modifier.STATIC))
                continue;
            if (enclosedElement.getModifiers().contains(Modifier.PRIVATE)) {
                if (entity.getKind()==ElementKind.ENUM)
                    continue;
                System.out.println("Common.getFields "+entity.getKind());
                System.out.println("Common.getFields "+enclosedElement);
                throw new RuntimeException("Entity fields cannot be private shall at least be protected in " + entity);
            }
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                res.add(Field.create(enclosedElement));
            }

        }
        return res;
    }

    public static int getVersion(Element enclosedElement) {
        Version annotation = enclosedElement.getAnnotation(Version.class);
        if (annotation!=null)
            return annotation.version();
        return 0;
    }


    public static ArrayList<Field> append(ArrayList<Field> base, ArrayList<Field> fields) {
        ArrayList<Field> fields1 = new ArrayList<>(base);
        fields1.addAll(fields);
        return fields1;
    }

    public static LinkedHashMap<String, TypeInfo> getTypeInfos(Set<? extends Element> typeInfos) {

        LinkedHashMap<String, TypeInfo>  res=new LinkedHashMap<>();
        for (Element element : typeInfos) {
            TypeInfo baseInfo=TypeInfo.create(element);
            res.put(baseInfo.packageClassName(),baseInfo);
        }
        return res;
    }



    public static LinkedHashMap<String, ChangeQueryInfo> getChangeQueryInfo(DistributedInfo distributedInfo, Set<? extends Element> typeInfos) {

        LinkedHashMap<String, ChangeQueryInfo>  res=new LinkedHashMap<>();
        for (Element element : typeInfos) {
            ChangeQueryInfo baseInfo= ChangeQueryInfo.create(distributedInfo, element);
            res.put(baseInfo.typeInfo().packageClassName(),baseInfo);
        }
        return res;
    }

    public static LinkedHashMap<String, ReplyInfo> getReplyInfo(DistributedInfo distributedInfo, Set<? extends Element> typeInfos) {

        LinkedHashMap<String, ReplyInfo>  res=new LinkedHashMap<>();
        for (Element element : typeInfos) {
            ReplyInfo baseInfo= ReplyInfo.create(distributedInfo, element);
            res.put(baseInfo.typeInfo().packageClassName(),baseInfo);
        }
        return res;
    }

    public static String getPackageName(Element element) {
        return element.asType().toString().replace("."+element.getSimpleName(),"");
    }

    public static String lowercase(String className) {
        return className.substring(0,1).toLowerCase()+className.substring(1);
    }

    public static String uppercase(String fieldName) {
        return fieldName.substring(0,1).toUpperCase()+fieldName.substring(1);
    }
}
