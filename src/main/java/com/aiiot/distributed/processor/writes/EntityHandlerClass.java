package com.aiiot.distributed.processor.writes;

import com.aiiot.distributed.processor.Common;
import com.aiiot.distributed.processor.bean.*;

import java.util.*;

/**
 * Created : 30/07/2023-22.19
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public class EntityHandlerClass extends ClassWriter {


    private LinkedHashMap<String, String> replaceMap;
    private HashMap<String, TypeInfo> entityMap;
    private List<ChangeQueryInfo> changeQueryInfos;
    private DistributedInfo distributedInfo;

    public EntityHandlerClass(String packageName, String name, HashMap<String, TypeInfo> entityMap, List<ChangeQueryInfo> changeQueryInfos, DistributedInfo distributedInfo) {

        super(packageName, name, true, getImports(changeQueryInfos,entityMap),
                new String[]{ "EntityHandler"}, null);
        this.entityMap = entityMap;
        this.changeQueryInfos = changeQueryInfos;
        this.distributedInfo = distributedInfo;
    }

    private static String[] getImports(List<ChangeQueryInfo> changeQueryInfos, HashMap<String, TypeInfo> entityMap) {
        HashSet<String> res=new HashSet<>();
        changeQueryInfos.forEach(changeQueryInfo -> res.add(changeQueryInfo.typeInfo().getPackageName()));
        entityMap.values().forEach(typeInfo -> res.add(typeInfo.getPackageName()));
        ArrayList<String> res1=new ArrayList<>();
        res1.add("com.aiiot.distributed.api.*");
        res1.add("com.aiiot.distributed.common.handlers.EntityHandler");
        res1.add("com.aiiot.distributed.common.holder.EntityHolder");
        res1.add("com.aiiot.distributed.common.*");
        for (String re : res) {
            res1.add(re+".*");
        }
        return res1.toArray(String[]::new);
    }


    public String getContent() {
        String content = """
                        
                             
                        Sequence sequence=new Sequence("__className__");                    
           
                        __entityHolders__             

                        public __className__() {
                        }
                        
                        @Override
                        public void save() {
                            __entityHoldersSave__
                        }
                        
                        @Override
                        public void restore() {
                            __entityHoldersRestore__
                        }
                        
                        @Override
                        public CommonData handle(CommonData t) {
                            if (!t.ctrl) {
                                t.sequence=sequence.next();
                            }

                            __ifHandling__
                        }
        
                        @Override
                        public Boolean restore(CommonData t) {
                            System.out.println("Restore ="+t);

                            __ifHandlingRestore__
                        }
                    
                        __methods__
                        
                    
                """;

        replaceMap = new LinkedHashMap<>();
        replaceMap.put("entityHolders",geEntityHolders(entityMap));
        replaceMap.put("entityHoldersSave",geEntityHoldersSave(entityMap));
        replaceMap.put("entityHoldersRestore",geEntityHoldersRestore(entityMap));
        replaceMap.put("className",getName());

        replaceMap.put("ifHandling", getIfHandling(changeQueryInfos,"handle",false));
        replaceMap.put("ifHandlingRestore", getIfHandling(changeQueryInfos,"restore",true));
        replaceMap.put("methods", getMethods(changeQueryInfos));
        return content;
    }

    private String geEntityHolders(HashMap<String, TypeInfo> entityMap) {
        StringBuilder builder=new StringBuilder();
        for (TypeInfo value : entityMap.values()) {
            builder.append("protected EntityHolder<"+value.className()+"> "+entitHolderName(value)+" = new EntityHolder<>(\""+value.className());
            builder.append("\",new KeyResolver<"+value.className()+">() {\n");
            builder.append("     public String getKey("+value.className()+" "+Common.lowercase(value.className())+"){\n");
            builder.append("       return "+Common.lowercase(value.className())+".get"+Common.uppercase(value.getKey().name())+"();\n");
            builder.append("}}\n");
            builder.append(");\n");
        }
        return builder.toString();
    }

    private String geEntityHoldersSave(HashMap<String, TypeInfo> entityMap) {
        StringBuilder builder=new StringBuilder();
        for (TypeInfo value : entityMap.values()) {
            builder.append(entitHolderName(value)+".save();\n");

        }
        return builder.toString();
    }

    private String geEntityHoldersRestore(HashMap<String, TypeInfo> entityMap) {
        StringBuilder builder=new StringBuilder();
        for (TypeInfo value : entityMap.values()) {
            builder.append(entitHolderName(value)+".restore(this);\n");

        }
        return builder.toString();
    }

    private String entitHolderName(TypeInfo value) {
        return Common.lowercase(value.className())+"EntityHolder";
    }

    private String getIfHandling(List<ChangeQueryInfo> typeInfoList,String methodPrefix,boolean skipQuery) {
        StringBuilder builder=new StringBuilder();
        int index=0;
        // Todo handle different versions
        for (ChangeQueryInfo changeQueryInfo : typeInfoList) {
            if (skipQuery && changeQueryInfo.query())
                continue;
            if (index>0)
                builder.append("            ");
            TypeInfo typeInfo = changeQueryInfo.typeInfo();
            TypeInfo entityInfo = distributedInfo.getTypeInfo(changeQueryInfo.entityName());
            String key = getKey(typeInfo, entityInfo);

            String instanceName = Common.lowercase(typeInfo.className());
            if (entityInfo==null || key==null) {
                builder.append("if (t.type==").append(typeInfo.typeId()[0]).append("){\n");
                builder.append("                 ").append(typeInfo.className()).append(" ").append(instanceName).append("=(").append(typeInfo.className()).append(")t;\n");
                builder.append("                 ").append("return ").append(getMethodName(methodPrefix, changeQueryInfo)).append("(").append(instanceName).append(");\n");
                builder.append("            ").append("}\n");

            }  else {

                builder.append("if (t.type==").append(typeInfo.typeId()[0]).append("){\n");

                builder.append("                 ").append(typeInfo.className()).append(" ").append(instanceName).append("=(").append(typeInfo.className()).append(")t;\n");
                builder.append("                 ").append(entityInfo.className()).append(" ").append(Common.lowercase(entityInfo.className())).append("=").append(entitHolderName(entityInfo)).append(".getForChange("+instanceName+".get").append(key).append("(),t);\n");
                builder.append("                 ").append("return ").append(getMethodName(methodPrefix, changeQueryInfo)).append("(").append(Common.lowercase(entityInfo.className())).append(",").append(instanceName).append(");\n");
                builder.append("            ").append("}\n");
            }
            index++;
        }
        builder.append("            ").append("return null;\n");
        return builder.toString();
    }

    private static String getKey(TypeInfo typeInfo, TypeInfo entityInfo) {
        String key=null;
        if (entityInfo !=null) {
            Field keyField = entityInfo.getKey();

            Field typeInfoKey = typeInfo.getKey();
            if (typeInfoKey != null) {
                if (!keyField.name().equals(typeInfoKey.name()))
                    keyField=null;
            } else {
                keyField=null;
            }
            if (keyField!=null)
                key = Common.uppercase(keyField.name());
        }
        return key;
    }

    private String getMethods(List<ChangeQueryInfo> changeQueryInfos) {
        StringBuilder builder=new StringBuilder();
        int index=0;
        for (ChangeQueryInfo changeQueryInfo : changeQueryInfos) {
            if (index>0)
                builder.append("        ");

            TypeInfo typeInfo = changeQueryInfo.typeInfo();
            TypeInfo entityInfo = distributedInfo.getTypeInfo(changeQueryInfo.entityName());
            String key = getKey(typeInfo, entityInfo);
            if (entityInfo==null || key==null) {
                builder.append("public abstract ").append(changeQueryInfo.info()).append(" ").append(getMethodName("handle",changeQueryInfo)).append("(").append(getTypeArg(typeInfo)).append(");\n");
                if (!changeQueryInfo.query()) {
                    builder.append("public abstract Boolean ").append(getMethodName("restore",changeQueryInfo)).append("(").append(getTypeArg(typeInfo)).append(");\n");
                }

            } else {
                builder.append("public abstract ").append(changeQueryInfo.info()).append(" ").append(getMethodName("handle",changeQueryInfo)).append("(").append(getTypeArg(entityInfo)).append(", ").append(getTypeArg(typeInfo)).append(");\n");
                if (!changeQueryInfo.query()) {
                    builder.append("public abstract Boolean ").append(getMethodName("restore",changeQueryInfo)).append("(").append(getTypeArg(entityInfo)).append(", ").append(getTypeArg(typeInfo)).append(");\n");
                }


            }
            index++;
        }
        return builder.toString();
    }

    private String getMethodName(String methodPrefix, ChangeQueryInfo changeQueryInfo) {
        return methodPrefix+changeQueryInfo.typeInfo().className();
    }

    private String getTypeArg(TypeInfo typeInfo) {
        return  typeInfo.className()+" "+Common.lowercase(typeInfo.className());
    }


    @Override
    public Map<String, String> getValues() {
        return replaceMap;
    }
}
