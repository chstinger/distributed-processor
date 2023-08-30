package com.aiiot.distributed.processor.bean;

import com.aiiot.distributed.api.CommonData;
import com.aiiot.distributed.processor.Common;
import lombok.Data;

import javax.lang.model.element.Element;
import java.util.*;

/**
 * Created : 30/07/2023-13.35
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
@Data
public class DistributedInfo {

    private String packageName = "";

    private LinkedHashMap<String, TypeInfo> baseInfos;
    private LinkedHashMap<String, List<String>> shortLongNames;

    private LinkedHashMap<String, ChangeQueryInfo> changeInfos;
    private LinkedHashMap<String, ChangeQueryInfo> queryInfos;
    private LinkedHashMap<String, ReplyInfo> replyInfos;


    public DistributedInfo(Set<? extends Element> elementsAnnotatedWith) {
        if (elementsAnnotatedWith.isEmpty())
            throw new RuntimeException("No entry point defined");
        if (elementsAnnotatedWith.size() > 1)
            throw new RuntimeException("To many entry points defined");
        Element element = elementsAnnotatedWith.stream().findFirst().orElse(null);
        packageName = Common.getPackageName(element);

    }


    public boolean validateBaseInfo() {
        boolean error = false;

        for (TypeInfo value : baseInfos.values()) {

            if (!value.extendsClass().equals(CommonData.class.getTypeName())) {

                if (!value.isEnum()) { //Dont validate enums extend class
                    if (!baseInfos.containsKey(value.extendsClass())) {
                        error = true;
                        System.out.println("DistributedInfo.validateBaseInfo class not known " + value + " " + value.extendsClass());
                    }
                }
            }
            for (Field field : value.fields()) {

                error = checkAndUpdateField(error, value, field.fieldTypeInfo());
                if (field.fieldTypeInfo().listType()!=null)
                    error = checkAndUpdateField(error, value, field.fieldTypeInfo().listType());
            }

        }
        if (!error) {
            shortLongNames = new LinkedHashMap<>();
            for (TypeInfo value : baseInfos.values()) {
                shortLongNames.computeIfAbsent(value.className(), s -> new ArrayList<>()).add(value.packageClassName());
                value.updateParent(baseInfos.get(value.extendsClass()));
            }

        }
        return !error;

    }

    private boolean checkAndUpdateField(boolean error, TypeInfo value, FieldTypeInfo fieldTypeInfo) {
        if (fieldTypeInfo.objectName() != null) {
            TypeInfo typeInfo = baseInfos.get(fieldTypeInfo.objectName());
            if (typeInfo==null) {
                error = true;
                System.out.println("DistributedInfo.validateBaseInfo class not known " + fieldTypeInfo.objectName() + " in " + value.extendsClass());
            }
            fieldTypeInfo.typeInfo()[0]=typeInfo;
        }
        return error;
    }

    public TypeInfo getTypeInfo(String entityName) {
        List<String> strings = shortLongNames.get(entityName);
        if (strings != null && strings.size() == 1) {
            entityName = strings.get(0);
        }
        return baseInfos.get(entityName);
    }


    public boolean exists(String className) {
        List<String> strings = shortLongNames.get(className);
        if (strings != null && strings.size() == 1)
            return true;
        return baseInfos.containsKey(className);
    }


    public int getCurrentVersion() {
        return baseInfos.values().stream().max((o1, o2) -> Math.max(o1.getVersion(), o2.getVersion())).map(TypeInfo::getVersion).orElse(0);
    }

    // Todo persist type map on start up

    public List<TypeInfo> getVersionTypeMap(int version) {
        ArrayList<TypeInfo> typeInfos = new ArrayList<>(baseInfos.values());
        typeInfos.sort(Comparator.comparing(TypeInfo::packageClassName));
        int index = 0;
        for (TypeInfo typeInfo : typeInfos) {
            if (!typeInfo.isEnum())
                typeInfo.setTypeId(index++);
        }
        return typeInfos;
    }


    public List<Integer> getVersions() {
        ArrayList<Integer> res = new ArrayList<>();
        res.add(getCurrentVersion());
        return res;
    }

}
